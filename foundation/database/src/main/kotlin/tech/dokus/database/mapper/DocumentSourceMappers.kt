package tech.dokus.database.mapper

import org.jetbrains.exposed.v1.core.ResultRow
import tech.dokus.database.entity.DocumentSourceEntity
import tech.dokus.database.tables.documents.DocumentBlobsTable
import tech.dokus.database.tables.documents.DocumentSourcesTable
import tech.dokus.domain.ids.DocumentBlobId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.DocumentSourceId
import tech.dokus.domain.ids.TenantId
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toKotlinUuid

@OptIn(ExperimentalUuidApi::class)
internal fun DocumentSourceEntity.Companion.from(row: ResultRow): DocumentSourceEntity {
    return DocumentSourceEntity(
        id = DocumentSourceId(row[DocumentSourcesTable.id].value.toKotlinUuid()),
        tenantId = TenantId(row[DocumentSourcesTable.tenantId].toKotlinUuid()),
        documentId = DocumentId.parse(row[DocumentSourcesTable.documentId].toString()),
        blobId = DocumentBlobId(row[DocumentSourcesTable.blobId].toKotlinUuid()),
        peppolRawUblBlobId = row[DocumentSourcesTable.peppolRawUblBlobId]?.let {
            DocumentBlobId(
                it.toKotlinUuid()
            )
        },
        sourceChannel = row[DocumentSourcesTable.sourceChannel],
        arrivalAt = row[DocumentSourcesTable.arrivalAt],
        contentHash = row[DocumentSourcesTable.contentHash],
        identityKeyHash = row[DocumentSourcesTable.identityKeyHash],
        status = row[DocumentSourcesTable.status],
        matchType = row[DocumentSourcesTable.matchType],
        isCorrective = row[DocumentSourcesTable.isCorrective],
        extractedSnapshotJson = row[DocumentSourcesTable.extractedSnapshotJson],
        peppolStructuredSnapshotJson = row[DocumentSourcesTable.peppolStructuredSnapshotJson],
        peppolSnapshotVersion = row[DocumentSourcesTable.peppolSnapshotVersion],
        detachedAt = row[DocumentSourcesTable.detachedAt],
        normalizedSupplierVat = row[DocumentSourcesTable.normalizedSupplierVat],
        normalizedDocumentNumber = row[DocumentSourcesTable.normalizedDocumentNumber],
        documentType = row[DocumentSourcesTable.documentType],
        direction = row[DocumentSourcesTable.direction],
        filename = row[DocumentSourcesTable.filename],
        inputHash = row[DocumentBlobsTable.inputHash],
        storageKey = row[DocumentBlobsTable.storageKey],
        contentType = row[DocumentBlobsTable.contentType],
        sizeBytes = row[DocumentBlobsTable.sizeBytes]
    )
}
