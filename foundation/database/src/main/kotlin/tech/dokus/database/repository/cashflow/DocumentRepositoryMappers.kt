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
        filename = "", // Resolved from preferred source by service layer
        effectiveOrigin = this[DocumentsTable.effectiveOrigin],
        uploadedAt = this[DocumentsTable.uploadedAt],
        sortDate = this[DocumentsTable.sortDate],
        downloadUrl = null // Generated on-demand by the service layer
    )
}

