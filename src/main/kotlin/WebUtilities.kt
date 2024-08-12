package de.griefed

import java.io.*
import java.net.*
import java.nio.channels.Channels

fun URL.downloadToFile(file: File) : Boolean {
    file.create()
    try {
        openStream().use { url ->
            Channels.newChannel(url).use { channel ->
                file.outputStream().use { stream ->
                    stream.channel.transferFrom(channel, 0, Long.MAX_VALUE)
                }
            }
        }
    } catch (ex: IOException) {
        file.deleteQuietly()
    }
    return file.isFile
}
