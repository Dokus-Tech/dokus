package tech.dokus.foundation.app.cache

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.allocArrayOf
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.usePinned
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.dataWithContentsOfFile
import platform.Foundation.writeToFile
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class)
actual fun getCacheDirectory(): String {
    val paths = NSSearchPathForDirectoriesInDomains(
        NSCachesDirectory,
        NSUserDomainMask,
        true
    )
    return (paths.firstOrNull() as? String) ?: "/tmp"
}

@OptIn(ExperimentalForeignApi::class)
actual fun readFile(path: String): ByteArray? {
    return try {
        val data = NSData.dataWithContentsOfFile(path) ?: return null
        val length = data.length.toInt()
        if (length == 0) return ByteArray(0)

        val bytes = ByteArray(length)
        bytes.usePinned { pinned ->
            memcpy(pinned.addressOf(0), data.bytes, data.length)
        }
        bytes
    } catch (e: Exception) {
        null
    }
}

@OptIn(ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)
actual fun writeFile(path: String, content: ByteArray) {
    try {
        val fileManager = NSFileManager.defaultManager
        val directoryPath = path.substringBeforeLast('/')

        // Create parent directory if needed
        if (!fileManager.fileExistsAtPath(directoryPath)) {
            fileManager.createDirectoryAtPath(
                directoryPath,
                withIntermediateDirectories = true,
                attributes = null,
                error = null
            )
        }

        memScoped {
            val data = NSData.create(
                bytes = allocArrayOf(content),
                length = content.size.toULong()
            )
            data.writeToFile(path, atomically = true)
        }
    } catch (e: Exception) {
        // Log error if needed
    }
}

@OptIn(ExperimentalForeignApi::class)
actual fun deleteFile(path: String) {
    try {
        NSFileManager.defaultManager.removeItemAtPath(path, error = null)
    } catch (e: Exception) {
        // Ignore
    }
}

@OptIn(ExperimentalForeignApi::class)
actual fun getFileSize(path: String): Long {
    return try {
        val fileManager = NSFileManager.defaultManager
        val attributes = fileManager.attributesOfItemAtPath(path, error = null) ?: return 0L
        (attributes["NSFileSize"] as? Long) ?: 0L
    } catch (e: Exception) {
        0L
    }
}

@OptIn(ExperimentalForeignApi::class)
actual fun listFiles(directory: String): List<String> {
    return try {
        val fileManager = NSFileManager.defaultManager
        val contents = fileManager.contentsOfDirectoryAtPath(directory, error = null) ?: return emptyList()
        contents.filterIsInstance<String>().map { "$directory/$it" }
    } catch (e: Exception) {
        emptyList()
    }
}
