@file:Suppress("TooGenericExceptionCaught", "SwallowedException") // File operations fail gracefully

package tech.dokus.foundation.app.cache

import java.io.File

actual fun getCacheDirectory(): String {
    // Use user home directory + .dokus/cache
    val userHome = System.getProperty("user.home")
    return "$userHome/.dokus/cache"
}

actual fun readFile(path: String): ByteArray? {
    return try {
        val file = File(path)
        if (file.exists()) file.readBytes() else null
    } catch (e: Exception) {
        null
    }
}

actual fun writeFile(path: String, content: ByteArray) {
    try {
        val file = File(path)
        file.parentFile?.mkdirs()
        file.writeBytes(content)
    } catch (e: Exception) {
        // Log error if needed
    }
}

actual fun deleteFile(path: String) {
    try {
        File(path).delete()
    } catch (e: Exception) {
        // Ignore
    }
}

actual fun getFileSize(path: String): Long {
    return try {
        val file = File(path)
        if (file.exists()) file.length() else 0L
    } catch (e: Exception) {
        0L
    }
}

actual fun listFiles(directory: String): List<String> {
    return try {
        val dir = File(directory)
        if (dir.exists() && dir.isDirectory) {
            dir.listFiles()?.map { it.absolutePath } ?: emptyList()
        } else {
            emptyList()
        }
    } catch (e: Exception) {
        emptyList()
    }
}
