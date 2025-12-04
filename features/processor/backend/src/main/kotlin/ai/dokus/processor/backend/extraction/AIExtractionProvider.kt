package ai.dokus.processor.backend.extraction

import ai.dokus.foundation.domain.enums.DocumentType
import ai.dokus.foundation.domain.model.ExtractedDocumentData

/**
 * Result of document extraction.
 */
data class ExtractionResult(
    val documentType: DocumentType,
    val extractedData: ExtractedDocumentData,
    val rawText: String,
    val confidence: Double,
    val processingTimeMs: Long
)

/**
 * Interface for AI-powered document extraction providers.
 *
 * Implementations should:
 * 1. Accept document bytes and content type
 * 2. Extract text/data from the document
 * 3. Use AI to parse and structure the data
 * 4. Return structured extraction results
 */
interface AIExtractionProvider {

    /**
     * Provider name for logging and tracking.
     */
    val name: String

    /**
     * Check if this provider is available and configured.
     */
    suspend fun isAvailable(): Boolean

    /**
     * Extract structured data from a document.
     *
     * @param documentBytes The raw document bytes
     * @param contentType MIME type of the document (e.g., "application/pdf", "image/jpeg")
     * @param filename Original filename for context
     * @return Extraction result with structured data
     * @throws ExtractionException if extraction fails
     */
    suspend fun extract(
        documentBytes: ByteArray,
        contentType: String,
        filename: String
    ): ExtractionResult
}

/**
 * Exception thrown when document extraction fails.
 */
class ExtractionException(
    message: String,
    val provider: String,
    val isRetryable: Boolean = true,
    cause: Throwable? = null
) : Exception(message, cause)
