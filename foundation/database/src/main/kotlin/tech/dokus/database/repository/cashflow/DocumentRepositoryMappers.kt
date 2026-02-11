package tech.dokus.database.repository.cashflow

import org.jetbrains.exposed.v1.core.ResultRow
import tech.dokus.database.tables.documents.DocumentsTable
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.DocumentDto
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toKotlinUuid

@OptIn(ExperimentalUuidApi::class)
internal fun ResultRow.toDocumentDto(): DocumentDto {
    return DocumentDto(
        id = DocumentId.parse(this[DocumentsTable.id].toString()),
        tenantId = TenantId(this[DocumentsTable.tenantId].toKotlinUuid()),
        filename = this[DocumentsTable.filename],
        contentType = this[DocumentsTable.contentType],
        sizeBytes = this[DocumentsTable.sizeBytes],
        storageKey = this[DocumentsTable.storageKey],
        source = this[DocumentsTable.documentSource],
        uploadedAt = this[DocumentsTable.uploadedAt],
        downloadUrl = null // Generated on-demand by the service layer
    )
}

