package tech.dokus.database.repository.cashflow

import org.jetbrains.exposed.v1.core.ResultRow
import tech.dokus.database.tables.documents.DocumentsTable
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.DocumentDto

internal fun ResultRow.toDocumentDto(): DocumentDto {
    return DocumentDto(
        id = DocumentId(this[DocumentsTable.id].value),
        tenantId = TenantId(this[DocumentsTable.tenantId]),
        filename = this[DocumentsTable.filename],
        contentType = this[DocumentsTable.contentType],
        sizeBytes = this[DocumentsTable.sizeBytes],
        storageKey = this[DocumentsTable.storageKey],
        source = this[DocumentsTable.documentSource],
        uploadedAt = this[DocumentsTable.uploadedAt],
        downloadUrl = null // Generated on-demand by the service layer
    )
}

