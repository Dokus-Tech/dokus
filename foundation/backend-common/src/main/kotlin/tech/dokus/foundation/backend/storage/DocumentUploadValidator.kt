package tech.dokus.foundation.backend.storage

/**
 * Central validation rules for uploaded documents (invoices, receipts, attachments, etc).
 *
 * Keep this strict and boring:
 * - Prevent path traversal via filenames
 * - Enforce size limits to reduce DoS risk
 * - Enforce an allow-list of MIME types
 */
class DocumentUploadValidator(
    private val maxFileSizeBytes: Long = DEFAULT_MAX_FILE_SIZE_BYTES,
    private val allowedMimeTypes: Set<String> = DEFAULT_ALLOWED_MIME_TYPES,
) {
    fun validate(
        fileContent: ByteArray,
        filename: String,
        mimeType: String,
    ): String? {
        if (filename.isBlank()) {
            return "Filename cannot be empty"
        }

        // Prevent path traversal attempts
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            return "Invalid filename: path traversal not allowed"
        }

        if (fileContent.size > maxFileSizeBytes) {
            val maxMb = maxFileSizeBytes / (1024.0 * 1024.0)
            val actualMb = fileContent.size / (1024.0 * 1024.0)
            return "File size (${"%.2f".format(actualMb)}MB) exceeds maximum allowed size (${"%.2f".format(maxMb)}MB)"
        }

        if (mimeType !in allowedMimeTypes) {
            return "File type '$mimeType' is not allowed. Supported types: ${allowedMimeTypes.joinToString()}"
        }

        return null
    }

    companion object {
        const val DEFAULT_MAX_FILE_SIZE_BYTES: Long = 10L * 1024 * 1024

        val DEFAULT_ALLOWED_MIME_TYPES: Set<String> = setOf(
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
            "text/csv",
        )
    }
}
