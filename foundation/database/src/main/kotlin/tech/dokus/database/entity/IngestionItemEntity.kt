package tech.dokus.database.entity

import tech.dokus.domain.enums.DocumentSource
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.DocumentSourceId
import tech.dokus.domain.ids.IngestionRunId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.Dpi
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

/**
 * Item representing a document ready for processing.
 * Contains all info needed by the worker to process a document.
 */
sealed interface IngestionItemEntity {
    val runId: IngestionRunId
    val documentId: DocumentId
    val tenantId: TenantId
    val sourceId: DocumentSourceId?
    val userFeedback: String?
    val overrideMaxPages: Int?
    val overrideDpi: Dpi?
    val effectiveOrigin: DocumentSource

    data class Upload(
        override val runId: IngestionRunId,
        override val documentId: DocumentId,
        override val tenantId: TenantId,
        override val sourceId: DocumentSourceId? = null,
        override val userFeedback: String? = null,
        override val overrideMaxPages: Int? = null,
        override val overrideDpi: Dpi? = null,
    ) : IngestionItemEntity {
        override val effectiveOrigin: DocumentSource = DocumentSource.Upload
    }

    data class Peppol(
        override val runId: IngestionRunId,
        override val documentId: DocumentId,
        override val tenantId: TenantId,
        override val sourceId: DocumentSourceId? = null,
        val peppolStructuredSnapshotJson: String,
        val peppolSnapshotVersion: Int,
        override val userFeedback: String? = null,
        override val overrideMaxPages: Int? = null,
        override val overrideDpi: Dpi? = null,
    ) : IngestionItemEntity {
        override val effectiveOrigin: DocumentSource = DocumentSource.Peppol
    }

    companion object {
        operator fun invoke(
            runId: IngestionRunId,
            documentId: DocumentId,
            tenantId: TenantId,
            sourceId: DocumentSourceId? = null,
            peppolStructuredSnapshotJson: String? = null,
            peppolSnapshotVersion: Int? = null,
            userFeedback: String? = null,
            overrideMaxPages: Int? = null,
            overrideDpi: Dpi? = null,
            sourceChannel: DocumentSource? = null,
        ): IngestionItemEntity {
            return when (sourceChannel) {
                DocumentSource.Peppol -> Peppol(
                    runId = runId,
                    documentId = documentId,
                    tenantId = tenantId,
                    sourceId = sourceId,
                    peppolStructuredSnapshotJson = requireNotNull(peppolStructuredSnapshotJson) { "Peppol snapshot JSON cannot be null" },
                    peppolSnapshotVersion = requireNotNull(peppolSnapshotVersion) { "Peppol snapshot version cannot be null" },
                    userFeedback = userFeedback,
                    overrideMaxPages = overrideMaxPages,
                    overrideDpi = overrideDpi
                )

                DocumentSource.Email,
                DocumentSource.Manual,
                DocumentSource.Upload -> Upload(
                    runId = runId,
                    documentId = documentId,
                    tenantId = tenantId,
                    sourceId = sourceId,
                    userFeedback = userFeedback,
                    overrideMaxPages = overrideMaxPages,
                    overrideDpi = overrideDpi
                )

                null -> Upload(
                    runId = runId,
                    documentId = documentId,
                    tenantId = tenantId,
                    sourceId = sourceId,
                    userFeedback = userFeedback,
                    overrideMaxPages = overrideMaxPages,
                    overrideDpi = overrideDpi
                )
            }
        }
    }
}

@OptIn(ExperimentalContracts::class)
fun IngestionItemEntity?.isPeppol(): Boolean {
    contract {
        returns(true) implies (this@isPeppol is IngestionItemEntity.Peppol)
    }
    return this is IngestionItemEntity.Peppol
}
