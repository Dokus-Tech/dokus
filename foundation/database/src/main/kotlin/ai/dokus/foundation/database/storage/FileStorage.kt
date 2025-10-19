package ai.dokus.foundation.database.storage

/**
 * Abstraction for file storage operations
 * Allows swapping between local filesystem, MinIO, S3, etc.
 */
interface FileStorage {
    /**
     * Stores a file
     *
     * @param key Unique file identifier/path (e.g., "receipts/tenant123/expense456/file.pdf")
     * @param content File content as bytes
     * @param contentType MIME type of the file
     * @return The storage URL/path where the file was stored
     */
    suspend fun store(key: String, content: ByteArray, contentType: String): String

    /**
     * Retrieves a file
     *
     * @param key Unique file identifier/path
     * @return File content as bytes
     * @throws IllegalArgumentException if file not found
     */
    suspend fun retrieve(key: String): ByteArray

    /**
     * Deletes a file
     *
     * @param key Unique file identifier/path
     * @return True if deleted, false if not found
     */
    suspend fun delete(key: String): Boolean

    /**
     * Checks if a file exists
     *
     * @param key Unique file identifier/path
     * @return True if exists, false otherwise
     */
    suspend fun exists(key: String): Boolean

    /**
     * Gets a presigned URL for download
     * For local storage, returns a placeholder URL
     * For S3/MinIO, returns a time-limited signed URL
     *
     * @param key Unique file identifier/path
     * @param expiresInSeconds Expiration time in seconds
     * @return Presigned URL
     */
    suspend fun getPresignedDownloadUrl(key: String, expiresInSeconds: Int = 3600): String

    /**
     * Gets a presigned URL for upload
     * For local storage, returns a placeholder URL
     * For S3/MinIO, returns a time-limited signed URL for direct upload
     *
     * @param key Unique file identifier/path
     * @param contentType MIME type
     * @param expiresInSeconds Expiration time in seconds
     * @return Presigned upload URL
     */
    suspend fun getPresignedUploadUrl(
        key: String,
        contentType: String,
        expiresInSeconds: Int = 3600
    ): String

    /**
     * Gets the base URL for this storage
     * For local: "file:///var/dokus/storage"
     * For S3: "https://bucket.s3.region.amazonaws.com"
     * For MinIO: "https://minio.example.com/bucket"
     */
    fun getBaseUrl(): String
}
