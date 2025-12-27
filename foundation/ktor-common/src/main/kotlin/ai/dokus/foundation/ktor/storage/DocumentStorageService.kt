package ai.dokus.foundation.ktor.storage

import ai.dokus.foundation.domain.ids.TenantId
import ai.dokus.foundation.ktor.utils.loggerFor
import java.text.Normalizer
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

/**
 * High-level service for document storage operations.
 * Handles key generation, URL signing, and tenant isolation.
 */
class DocumentStorageService(
    private val storage: ObjectStorage,
    private val defaultUrlExpiry: Duration = 1.hours
) {
    private val logger = loggerFor()

    /**
     * Upload a document and return the result with a signed URL.
     *
     * @param tenantId The tenant this document belongs to
     * @param prefix The storage prefix (e.g., "invoices", "bills", "expenses")
     * @param filename The original filename
     * @param data The file content
     * @param contentType The MIME type
     * @return Upload result with storage key and signed URL
     */
    suspend fun uploadDocument(
        tenantId: TenantId,
        prefix: String,
        filename: String,
        data: ByteArray,
        contentType: String
    ): UploadResult {
        val sanitizedFilename = sanitizeFilename(filename)
        val key = generateStorageKey(tenantId, prefix, sanitizedFilename)

        logger.info("Uploading document: tenant=$tenantId, prefix=$prefix, filename=$sanitizedFilename, size=${data.size}")

        storage.put(key, data, contentType)

        val url = storage.getSignedUrl(key, defaultUrlExpiry)

        return UploadResult(
            key = key,
            url = url,
            filename = sanitizedFilename,
            contentType = contentType,
            sizeBytes = data.size.toLong()
        )
    }

    /**
     * Get a signed URL for downloading a document.
     *
     * @param key The storage key
     * @param expiry How long the URL should be valid (defaults to 1 hour)
     * @return Presigned download URL
     */
    suspend fun getDownloadUrl(key: String, expiry: Duration = defaultUrlExpiry): String {
        return storage.getSignedUrl(key, expiry)
    }

    /**
     * Delete a document.
     *
     * @param key The storage key
     */
    suspend fun deleteDocument(key: String) {
        logger.info("Deleting document: $key")
        storage.delete(key)
    }

    /**
     * Check if a document exists.
     *
     * @param key The storage key
     * @return true if exists
     */
    suspend fun documentExists(key: String): Boolean {
        return storage.exists(key)
    }

    /**
     * Download a document's content.
     *
     * @param key The storage key
     * @return File content as bytes
     */
    suspend fun downloadDocument(key: String): ByteArray {
        return storage.get(key)
    }

    /**
     * Generate a storage key for a document.
     * Format: {prefix}/{tenantId}/{uuid}_{filename}
     */
    private fun generateStorageKey(tenantId: TenantId, prefix: String, filename: String): String {
        val uuid = UUID.randomUUID().toString()
        return "$prefix/$tenantId/${uuid}_$filename"
    }

    /**
     * Sanitize filename to prevent path traversal and invalid characters.
     *
     * Security measures:
     * - Normalizes Unicode to prevent homograph attacks
     * - Removes path separators and dangerous characters
     * - Removes directory traversal sequences
     * - Removes leading dots/spaces (hidden files)
     * - Limits filename length
     */
    private fun sanitizeFilename(filename: String): String {
        // Normalize Unicode to canonical form to prevent homograph attacks
        val normalized = Normalizer.normalize(filename, Normalizer.Form.NFKC)

        return normalized
            // Remove all path separators and dangerous characters
            .replace(Regex("[/\\\\<>:\"|?*\u0000]"), "_")
            // Remove fullwidth characters that could be path separators
            .replace('\uFF0F', '_') // Fullwidth solidus
            .replace('\uFF3C', '_') // Fullwidth reverse solidus
            // Remove directory traversal sequences
            .replace(Regex("\\.\\.+"), "_")
            // Remove leading dots and spaces (prevents hidden files)
            .replace(Regex("^[.\\s]+"), "")
            .trim()
            .take(255) // Limit filename length
            .ifEmpty { "unnamed_file" } // Ensure filename is never empty
    }
}
