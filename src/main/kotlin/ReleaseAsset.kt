package de.griefed

import java.net.URL

class ReleaseAsset(private val tag: String, private val assetName: String, private val assetUrl: URL) {
    fun tag(): String {
        return tag
    }

    fun name(): String {
        return assetName
    }

    fun url(): URL {
        return assetUrl
    }
}