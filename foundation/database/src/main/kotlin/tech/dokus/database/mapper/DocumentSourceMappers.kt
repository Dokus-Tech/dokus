package tech.dokus.database.mapper

import org.jetbrains.exposed.v1.core.ResultRow
import tech.dokus.database.repository.cashflow.DocumentSourceSummary
import tech.dokus.database.tables.documents.DocumentBlobsTable
import tech.dokus.database.tables.documents.DocumentSourcesTable
import tech.dokus.domain.ids.DocumentBlobId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.DocumentSourceId
import tech.dokus.domain.ids.TenantId
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toKotlinUuid

@OptIn(ExperimentalUuidApi::class)
internal fun ResultRow.toSourceSummary(): DocumentSourceSummary {
    return DocumentSourceSummary(
        id = DocumentSourceId(this[DocumentSourcesTable.id].value.toKotlinUuid()),
        tenantId = TenantId(this[DocumentSourcesTable.tenantId].toKotlinUuid()),
        documentId = DocumentId.parse(this[DocumentSourcesTable.documentId].toString()),
        blobId = DocumentBlobId(this[DocumentSourcesTable.blobId].toKotlinUuid()),
        peppolRawUblBlobId = this[DocumentSourcesTable.peppolRawUblBlobId]?.let {
            DocumentBlobId(
                it.toKotlinUuid()
            )
        },
        sourceChannel = this[DocumentSourcesTable.sourceChannel],
        arrivalAt = this[DocumentSourcesTable.arrivalAt],
        contentHash = this[DocumentSourcesTable.contentHash],
        identityKeyHash = this[DocumentSourcesTable.identityKeyHash],
        status = this[DocumentSourcesTable.status],
        matchType = this[DocumentSourcesTable.matchType],
        isCorrective = this[DocumentSourcesTable.isCorrective],
        extractedSnapshotJson = this[DocumentSourcesTable.extractedSnapshotJson],
        peppolStructuredSnapshotJson = this[DocumentSourcesTable.peppolStructuredSnapshotJson],
        peppolSnapshotVersion = this[DocumentSourcesTable.peppolSnapshotVersion],
        detachedAt = this[DocumentSourcesTable.detachedAt],
        normalizedSupplierVat = this[DocumentSourcesTable.normalizedSupplierVat],
        normalizedDocumentNumber = this[DocumentSourcesTable.normalizedDocumentNumber],
        documentType = this[DocumentSourcesTable.documentType],
        direction = this[DocumentSourcesTable.direction],
        filename = this[DocumentSourcesTable.filename],
        inputHash = this[DocumentBlobsTable.inputHash],
        storageKey = this[DocumentBlobsTable.storageKey],
        contentType = this[DocumentBlobsTable.contentType],
        sizeBytes = this[DocumentBlobsTable.sizeBytes]
    )
}
