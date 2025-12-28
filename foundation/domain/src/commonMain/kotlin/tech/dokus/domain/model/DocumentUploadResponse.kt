package tech.dokus.domain.model

import tech.dokus.domain.ids.IngestionRunId
import kotlinx.serialization.Serializable

/**
 * Response for document upload including processing info.
 * Shared between backend and frontend.
 */
@Serializable
data class DocumentUploadResponse(
    val document: DocumentDto,
    val processingId: IngestionRunId,
    val processingStatus: String
)
