package de.griefed

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

class GitHubRepo {

    companion object {
        private val log by lazy { cachedLoggerOf(GitHubRepo::class.java) }
        private val objectMapper: ObjectMapper = ObjectMapper()

        init {
            objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            objectMapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
        }

        fun downloadAssets(user: String, repo: String, token: String = "") {
            val tags = getTags(user, repo, token)
            val assets = getReleaseAssets(user, repo, tags, token)
            for (asset in assets) {
                val downloadDestination = File("downloads/$user/$repo", asset.tag())
                downloadDestination.mkdirs()
                log.info("Downloading ${asset.tag()}/${asset.name()} from ${asset.url()}")
                var success: Boolean = false
                do {
                    try {
                        success = asset.url().downloadToFile(File(downloadDestination,asset.name()))
                    } catch(ex: IOException) {
                        success = evalException(ex)
                        if (!success) {
                            log.info("Rate limited or timeout. Waiting 1 minute... ${ex.message}")
                            Thread.sleep(1000*60)
                        }
                    }
                } while(!success)
            }
        }

        private fun getTags(user: String, repo: String, token: String): List<String> {
            val tags = mutableListOf<String>()
            var response : JsonNode? = null
            var success: Boolean = false
            do {
                try {
                    response = objectMapper.readTree(getResponse(URI("https://api.github.com/repos/$user/$repo/git/refs/tags").toURL(), token))
                    success = evalResponse(response)
                    if (!success) {
                        log.info("Rate limited or timeout. Waiting 1 minute...")
                        Thread.sleep(1000*60)
                    }
                } catch(ex: IOException) {
                    success = evalException(ex)
                    if (!success) {
                        log.info("Rate limited or timeout. Waiting 1 minute... ${ex.message}")
                        Thread.sleep(1000*60)
                    }
                }
            } while(!success)
            if (response != null) {
                for (tag in response) {
                    tags.add(tag["ref"].asText().replace("refs/tags/", ""))
                }
            }
            return tags.toList()
        }

        private fun getReleaseAssets(user: String, repo: String, tags: List<String>, token: String) : List<ReleaseAsset> {
            val assets = mutableListOf<ReleaseAsset>()
            for (tag in tags) {
                var success : Boolean = false
                do {
                    try {
                        val release = getRelease(tag, user, repo, token)
                        for (asset in release["assets"]) {
                            assets.add(
                                ReleaseAsset(
                                    tag,
                                    asset["name"].asText(),
                                    URI(asset["browser_download_url"].asText()).toURL()
                                )
                            )
                        }
                        success = true
                    } catch (ex: IOException) {
                        success = evalException(ex)
                        if (!success) {
                            log.info("Rate limited or timeout. Waiting 1 minute... ${ex.message}")
                            Thread.sleep(1000*60)
                        }
                    }
                } while (!success)
                assets.add(
                    ReleaseAsset(
                        tag,
                        "source.tar.gz",
                        URI("https://github.com/$user/$repo/archive/refs/tags/$tag/source.tar.gz").toURL()
                    )
                )
                assets.add(
                    ReleaseAsset(
                        tag,
                        "source.zip",
                        URI("https://github.com/$user/$repo/archive/refs/tags/$tag/source.zip").toURL()
                    )
                )
            }
            return assets
        }

        private fun getRelease(tag: String, user: String, repo: String, token: String) : JsonNode {
            var response : JsonNode
            var success: Boolean = false
            do {
                response = objectMapper.readTree(getResponse(URI("https://api.github.com/repos/$user/$repo/releases/tags/$tag").toURL(), token))
                success = evalResponse(response)
                if (!success) {
                    log.info("Rate limited or timeout. Waiting 1 minute...")
                    Thread.sleep(1000*60)
                }
            } while(!success)
            return response
        }

        private fun evalException(ex: IOException) : Boolean {
            return ex.message!!.matches(".*responded with 404".toRegex())
        }

        private fun evalResponse(response: JsonNode) : Boolean {
            return !(response.has("message") && response.has("documentation_url"))
        }

        private fun getResponse(requestUrl: URL, token: String): String {
            val httpURLConnection = requestUrl.openConnection() as HttpURLConnection
            httpURLConnection.requestMethod = "GET"
            if (token.isNotBlank()) {
                httpURLConnection.setRequestProperty("Authorization", "Bearer $token")
            }
            if (httpURLConnection.responseCode != 200) throw IOException("Request for " + requestUrl + " responded with " + httpURLConnection.responseCode)
            val bufferedReader = BufferedReader(
                InputStreamReader(httpURLConnection.inputStream)
            )
            var inputLine: String?
            val response = StringBuilder()
            while (bufferedReader.readLine().also { inputLine = it } != null) {
                response.append(inputLine)
            }
            bufferedReader.close()
            return response.toString()
        }
    }
}