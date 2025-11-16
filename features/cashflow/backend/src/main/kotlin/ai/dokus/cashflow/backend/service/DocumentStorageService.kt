package ai.dokus.cashflow.backend.service

import ai.dokus.foundation.domain.AttachmentId
import ai.dokus.foundation.domain.TenantId
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID

/**
 * Service for handling document storage
 * Supports local filesystem (dev) and S3 (prod)
 */
class DocumentStorageService(
    private val storageBasePath: String = "./storage/documents",
    private val maxFileSizeMb: Long = 10,
    private val allowedMimeTypes: Set<String> = setOf(
        // Images
        "image/jpeg",
        "image/jpg",
        "image/png",
        "image/gif",
        "image/webp",
        // Documents
        "application/pdf",
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.ms-excel",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        // Text
        "text/plain",
        "text/csv"
    )
) {
    private val logger = LoggerFactory.getLogger(DocumentStorageService::class.java)

    init {
        // Ensure storage directory exists
        File(storageBasePath).mkdirs()
        logger.info("DocumentStorageService initialized with base path: $storageBasePath")
    }

    /**
     * Validates file before upload
     * Returns error message if validation fails, null if valid
     */
    fun validateFile(
        fileContent: ByteArray,
        filename: String,
        mimeType: String
    ): String? {
        // Check file size
        val fileSizeBytes = fileContent.size
        val fileSizeMb = fileSizeBytes / (1024.0 * 1024.0)

        if (fileSizeMb > maxFileSizeMb) {
            return "File size (${"%.2f".format(fileSizeMb)}MB) exceeds maximum allowed size (${maxFileSizeMb}MB)"
        }

        // Check MIME type
        if (mimeType !in allowedMimeTypes) {
            return "File type '$mimeType' is not allowed. Supported types: ${allowedMimeTypes.joinToString()}"
        }

        // Check filename
        if (filename.isBlank()) {
            return "Filename cannot be empty"
        }

        // Check for path traversal attempts
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            return "Invalid filename: path traversal not allowed"
        }

        return null // Valid
    }

    /**
     * Store file locally (development)
     * Returns the storage key (file path) on success
     */
    suspend fun storeFileLocally(
        tenantId: TenantId,
        entityType: String,
        entityId: String,
        filename: String,
        fileContent: ByteArray
    ): Result<String> = runCatching {
        // Create tenant-specific directory structure: storage/documents/{tenantId}/{entityType}/{entityId}/
        val tenantDir = File(storageBasePath, tenantId.toString())
        val entityTypeDir = File(tenantDir, entityType.lowercase())
        val entityDir = File(entityTypeDir, entityId)

        // Ensure directories exist
        entityDir.mkdirs()

        // Generate unique filename to avoid collisions
        val uniqueFilename = "${UUID.randomUUID()}_$filename"
        val targetFile = File(entityDir, uniqueFilename)

        // Write file
        targetFile.writeBytes(fileContent)

        // Return storage key (relative path from base)
        val storageKey = targetFile.relativeTo(File(storageBasePath)).path.replace(File.separatorChar, '/')

        logger.info("File stored locally: $storageKey (${fileContent.size} bytes)")
        storageKey
    }

    /**
     * Retrieve file from local storage
     * Returns file content as ByteArray
     */
    suspend fun retrieveFileLocally(storageKey: String): Result<ByteArray> = runCatching {
        // Security: Validate storage key doesn't contain path traversal
        if (storageKey.contains("..")) {
            throw SecurityException("Invalid storage key: path traversal not allowed")
        }

        val file = File(storageBasePath, storageKey.replace('/', File.separatorChar))

        if (!file.exists()) {
            throw IllegalArgumentException("File not found: $storageKey")
        }

        if (!file.canonicalPath.startsWith(File(storageBasePath).canonicalPath)) {
            throw SecurityException("Invalid storage key: outside storage directory")
        }

        logger.info("File retrieved locally: $storageKey (${file.length()} bytes)")
        file.readBytes()
    }

    /**
     * Delete file from local storage
     */
    suspend fun deleteFileLocally(storageKey: String): Result<Boolean> = runCatching {
        // Security: Validate storage key doesn't contain path traversal
        if (storageKey.contains("..")) {
            throw SecurityException("Invalid storage key: path traversal not allowed")
        }

        val file = File(storageBasePath, storageKey.replace('/', File.separatorChar))

        if (!file.exists()) {
            logger.warn("File not found for deletion: $storageKey")
            return@runCatching false
        }

        if (!file.canonicalPath.startsWith(File(storageBasePath).canonicalPath)) {
            throw SecurityException("Invalid storage key: outside storage directory")
        }

        val deleted = file.delete()
        if (deleted) {
            logger.info("File deleted locally: $storageKey")
        } else {
            logger.warn("Failed to delete file: $storageKey")
        }
        deleted
    }

    /**
     * Generate a download URL for local storage
     * In local dev, this would be a path to serve the file via HTTP endpoint
     * In production, this would generate a presigned S3 URL
     */
    fun generateDownloadUrl(
        storageKey: String,
        expirationSeconds: Long = 3600
    ): String {
        // For local storage, return a relative URL that will be handled by an HTTP endpoint
        // In production, this would call S3's generatePresignedUrl
        return "/api/attachments/download/$storageKey"
    }

    /**
     * Upload file to S3 (production)
     * TODO: Implement S3 upload when needed
     */
    suspend fun storeFileS3(
        tenantId: TenantId,
        entityType: String,
        entityId: String,
        filename: String,
        fileContent: ByteArray,
        bucket: String
    ): Result<String> = runCatching {
        // TODO: Implement S3 upload
        // For now, fall back to local storage
        storeFileLocally(tenantId, entityType, entityId, filename, fileContent).getOrThrow()
    }

    /**
     * Get file extension from filename
     */
    private fun getFileExtension(filename: String): String {
        val lastDot = filename.lastIndexOf('.')
        return if (lastDot > 0) filename.substring(lastDot) else ""
    }

    /**
     * Get MIME type category (image, document, etc.)
     */
    fun getMimeTypeCategory(mimeType: String): String {
        return when {
            mimeType.startsWith("image/") -> "image"
            mimeType.startsWith("application/pdf") -> "document"
            mimeType.startsWith("application/vnd.") -> "document"
            mimeType.startsWith("text/") -> "text"
            else -> "other"
        }
    }
}
