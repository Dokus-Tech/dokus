package tech.dokus.foundation.backend.storage

import java.io.ByteArrayInputStream
import java.io.InputStream
import kotlin.time.Duration

/**
 * Interface for object storage operations.
 * Implementations can use MinIO, S3, or local filesystem.
 */
interface ObjectStorage {
    /**
     * Store an object in the storage.
     *
     * @param key The unique key (path) for the object
     * @param data The file content as bytes
     * @param contentType The MIME type of the file
     * @return The storage key for the stored object
     */
    suspend fun put(key: String, data: ByteArray, contentType: String): String

    /**
     * Retrieve an object from storage.
     *
     * @param key The storage key
     * @return The file content as bytes
     * @throws NoSuchElementException if the key doesn't exist
     */
    suspend fun get(key: String): ByteArray

    /**
     * Open an object stream for incremental reads.
     *
     * Default implementation falls back to [get] for compatibility with non-streaming backends.
     */
    suspend fun openStream(key: String): InputStream {
        return ByteArrayInputStream(get(key))
    }

    /**
     * Delete an object from storage.
     *
     * @param key The storage key
     */
    suspend fun delete(key: String)

    /**
     * Check if an object exists in storage.
     *
     * @param key The storage key
     * @return true if exists, false otherwise
     */
    suspend fun exists(key: String): Boolean

    /**
     * Generate a presigned URL for temporary access to the object.
     *
     * @param key The storage key
     * @param expiry How long the URL should be valid
     * @return A presigned URL for direct access
     */
    suspend fun getSignedUrl(key: String, expiry: Duration): String
}

/**
 * Result of a file upload operation.
 */
data class UploadResult(
    val key: String,
    val url: String,
    val filename: String,
    val contentType: String,
    val sizeBytes: Long
)
