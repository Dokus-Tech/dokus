package tech.dokus.foundation.app.cache

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask
import platform.Foundation.dataWithContentsOfFile
import platform.Foundation.writeToFile

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
        data.toByteArray()
    } catch (e: Exception) {
        null
    }
}

@OptIn(ExperimentalForeignApi::class)
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

        val data = content.toNSData()
        data.writeToFile(path, atomically = true)
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

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val length = this.length.toInt()
    val bytes = ByteArray(length)
    if (length > 0) {
        kotlinx.cinterop.usePinned(bytes) { pinned ->
            platform.posix.memcpy(pinned.addressOf(0), this.bytes, this.length)
        }
    }
    return bytes
}

@OptIn(ExperimentalForeignApi::class)
private fun ByteArray.toNSData(): NSData {
    return kotlinx.cinterop.memScoped {
        NSData.create(
            bytes = kotlinx.cinterop.allocArrayOf(this@toNSData),
            length = this@toNSData.size.toULong()
        )
    }
}
