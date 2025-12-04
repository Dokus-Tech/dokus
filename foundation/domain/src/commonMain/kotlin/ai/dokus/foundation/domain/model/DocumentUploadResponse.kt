package ai.dokus.foundation.domain.model

import ai.dokus.foundation.domain.ids.DocumentProcessingId
import kotlinx.serialization.Serializable

/**
 * Response for document upload including processing info.
 * Shared between backend and frontend.
 */
@Serializable
data class DocumentUploadResponse(
    val document: DocumentDto,
    val processingId: DocumentProcessingId,
    val processingStatus: String
)
