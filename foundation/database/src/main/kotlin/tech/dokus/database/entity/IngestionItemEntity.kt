package tech.dokus.database.entity

import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.IngestionRunId
import tech.dokus.domain.ids.DocumentSourceId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.enums.DocumentSource
import tech.dokus.domain.model.Dpi

/**
 * Item representing a document ready for processing.
 * Contains all info needed by the worker to process a document.
 */
data class IngestionItemEntity(
    val runId: IngestionRunId,
    val documentId: DocumentId,
    val tenantId: TenantId,
    val sourceId: DocumentSourceId? = null,
    val sourceChannel: DocumentSource? = null,
    val effectiveOrigin: DocumentSource = DocumentSource.Upload,
    val peppolStructuredSnapshotJson: String? = null,
    val peppolSnapshotVersion: Int? = null,
    val userFeedback: String? = null,
    val overrideMaxPages: Int? = null,
    val overrideDpi: Dpi? = null,
)
