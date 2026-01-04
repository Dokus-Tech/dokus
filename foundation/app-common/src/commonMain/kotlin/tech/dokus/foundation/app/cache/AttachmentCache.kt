package tech.dokus.foundation.app.cache

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import tech.dokus.domain.ids.AttachmentId
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Platform-specific function to get the cache directory path.
 * Returns the path to a directory suitable for caching files.
 */
expect fun getCacheDirectory(): String

/**
 * Platform-specific function to read a file as bytes.
 * Returns null if file doesn't exist.
 */
expect fun readFile(path: String): ByteArray?

/**
 * Platform-specific function to write bytes to a file.
 * Creates parent directories if needed.
 */
expect fun writeFile(path: String, content: ByteArray)

/**
 * Platform-specific function to delete a file.
 */
expect fun deleteFile(path: String)

/**
 * Platform-specific function to get file size in bytes.
 * Returns 0 if file doesn't exist.
 */
expect fun getFileSize(path: String): Long

/**
 * Platform-specific function to list files in a directory.
 * Returns list of file paths.
 */
expect fun listFiles(directory: String): List<String>

/**
 * LRU cache for attachment files.
 *
 * Features:
 * - Configurable max size (default 50MB)
 * - LRU eviction when size limit exceeded
 * - Thread-safe operations
 * - On-demand caching (attachments cached when first viewed)
 */
@OptIn(ExperimentalTime::class)
class AttachmentCache(
    private val maxSizeBytes: Long = DEFAULT_MAX_SIZE_BYTES
) {
    private val mutex = Mutex()
    private val cacheDir: String by lazy { "${getCacheDirectory()}/attachments" }

    // Track access times for LRU eviction
    private val accessTimes = mutableMapOf<String, Long>()

    /**
     * Get an attachment from cache.
     * Returns null if not cached.
     */
    suspend fun get(attachmentId: AttachmentId): ByteArray? = mutex.withLock {
        val path = getFilePath(attachmentId)
        val content = readFile(path)

        if (content != null) {
            // Update access time for LRU
            accessTimes[attachmentId.toString()] = Clock.System.now().toEpochMilliseconds()
        }

        content
    }

    /**
     * Store an attachment in cache.
     * Will evict least recently used files if cache is full.
     */
    suspend fun put(attachmentId: AttachmentId, content: ByteArray) = mutex.withLock {
        // Evict if needed to make room
        evictIfNeeded(content.size.toLong())

        val path = getFilePath(attachmentId)
        writeFile(path, content)
        accessTimes[attachmentId.toString()] = Clock.System.now().toEpochMilliseconds()
    }

    /**
     * Check if an attachment is cached.
     */
    suspend fun contains(attachmentId: AttachmentId): Boolean = mutex.withLock {
        val path = getFilePath(attachmentId)
        readFile(path) != null
    }

    /**
     * Remove an attachment from cache.
     */
    suspend fun remove(attachmentId: AttachmentId) = mutex.withLock {
        val path = getFilePath(attachmentId)
        deleteFile(path)
        accessTimes.remove(attachmentId.toString())
    }

    /**
     * Clear all cached attachments.
     */
    suspend fun clear() = mutex.withLock {
        listFiles(cacheDir).forEach { path ->
            deleteFile(path)
        }
        accessTimes.clear()
    }

    /**
     * Get current cache size in bytes.
     */
    suspend fun getCurrentSizeBytes(): Long = mutex.withLock {
        listFiles(cacheDir).sumOf { getFileSize(it) }
    }

    /**
     * Evict least recently used files until cache has room for new content.
     */
    private fun evictIfNeeded(newContentSize: Long) {
        val files = listFiles(cacheDir)
        var currentSize = files.sumOf { getFileSize(it) }

        // If adding new content would exceed limit, evict LRU files
        while (currentSize + newContentSize > maxSizeBytes && accessTimes.isNotEmpty()) {
            // Find least recently used
            val lru = accessTimes.minByOrNull { it.value }
            if (lru != null) {
                val path = "$cacheDir/${lru.key}"
                val fileSize = getFileSize(path)
                deleteFile(path)
                accessTimes.remove(lru.key)
                currentSize -= fileSize
            } else {
                break
            }
        }
    }

    private fun getFilePath(attachmentId: AttachmentId): String {
        return "$cacheDir/$attachmentId"
    }

    companion object {
        /**
         * Default max cache size: 50MB
         */
        const val DEFAULT_MAX_SIZE_BYTES = 50L * 1024L * 1024L
    }
}
