package de.griefed

import java.io.File
import kotlin.io.path.createParentDirectories

fun File.create(createFileOrDir: Boolean = false, asDirectory: Boolean = false) {
    absoluteFile.toPath().createParentDirectories()
    if (createFileOrDir) {
        if (asDirectory) {
            this.mkdirs()
        } else {
            this.createNewFile()
        }
    }
}

fun File.deleteQuietly(): Boolean =
    if (this.isFile) {
        try {
            this.delete()
        } catch (ignored: Exception) {
            false
        }
    } else {
        try {
            this.deleteRecursively()
        } catch (ignored: Exception) {
            false
        }
    }