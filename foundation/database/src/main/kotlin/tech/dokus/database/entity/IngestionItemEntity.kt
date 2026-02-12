package tech.dokus.database.entity

import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.IngestionRunId
import tech.dokus.domain.ids.TenantId

/**
 * Item representing a document ready for processing.
 * Contains all info needed by the worker to process a document.
 */
data class IngestionItemEntity(
    val runId: IngestionRunId,
    val documentId: DocumentId,
    val tenantId: TenantId,
    val storageKey: String,
    val filename: String,
    val contentType: String,
    val userFeedback: String? = null,
    val overrideMaxPages: Int? = null,
    val overrideDpi: Int? = null,
)
