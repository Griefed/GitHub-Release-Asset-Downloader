package de.griefed

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

class GitHubRepo {

    companion object {
        private val objectMapper: ObjectMapper = ObjectMapper()

        init {
            objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            objectMapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
        }

        fun downloadAssets(user: String, repo: String, destination: String) {
            val assets = getReleaseAssets(user, repo)
            for (asset in assets) {
                val folder = File(destination, asset.tag())
                folder.mkdirs()
                asset.url().downloadToFile(File(folder,asset.name()))
            }
        }

        private fun getTags(user: String, repo: String): List<String> {
            val tags = mutableListOf<String>()
            val response = objectMapper.readTree(getResponse(URI("https://api.github.com/repos/$user/$repo/git/refs/tags").toURL()))
            for (tag in response) {
                tags.add(tag["ref"].asText().replace("refs/tags/", ""))
            }
            return tags.toList()
        }

        private fun getReleaseAssets(user: String, repo: String) : List<ReleaseAsset> {
            val tags = getTags(user, repo)
            val assets = mutableListOf<ReleaseAsset>()
            for (tag in tags) {
                try {
                    val release = getRelease(tag, user, repo)
                    for (asset in release["assets"]) {
                        assets.add(
                            ReleaseAsset(
                                tag,
                                asset["name"].asText(),
                                URI(asset["browser_download_url"].asText()).toURL()
                            )
                        )
                    }
                    assets.add(
                        ReleaseAsset(
                            tag,
                            "source.tar.gz",
                            URI(release["tarball_url"].asText()).toURL()
                        )
                    )
                    assets.add(
                        ReleaseAsset(
                            tag,
                            "source.zip",
                            URI(release["tarball_url"].asText()).toURL()
                        )
                    )
                } catch (_: Exception) {
                    //No release available, download sources.
                    assets.add(
                        ReleaseAsset(
                            tag,
                            "source.zip",
                            URI("https://github.com/$user/$repo/archive/refs/tags/$tag/source.zip").toURL()
                        )
                    )
                    assets.add(
                        ReleaseAsset(
                            tag,
                            "source.tar.gz",
                            URI("https://github.com/$user/$repo/archive/refs/tags/$tag/source.tar.gz").toURL()
                        )
                    )
                }
            }
            return assets
        }

        private fun getRelease(tag: String, user: String, repo: String) : JsonNode {
            return objectMapper.readTree(getResponse(URI("https://api.github.com/repos/$user/$repo/releases/tags/$tag").toURL()))
        }

        private fun getResponse(requestUrl: URL): String {
            val httpURLConnection = requestUrl.openConnection() as HttpURLConnection
            httpURLConnection.requestMethod = "GET"
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