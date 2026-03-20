package tech.dokus.database.mapper

import org.jetbrains.exposed.v1.core.ResultRow
import tech.dokus.database.tables.documents.DocumentsTable
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.DocumentDto
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toKotlinUuid

@OptIn(ExperimentalUuidApi::class)
internal fun DocumentDto.Companion.from(row: ResultRow): DocumentDto {
    return DocumentDto(
        id = DocumentId.parse(row[DocumentsTable.id].toString()),
        tenantId = TenantId(row[DocumentsTable.tenantId].toKotlinUuid()),
        filename = "", // Resolved from preferred source by service layer
        uploadedAt = row[DocumentsTable.uploadedAt],
        sortDate = row[DocumentsTable.sortDate] ?: row[DocumentsTable.uploadedAt].date,
        downloadUrl = null // Generated on-demand by the service layer
    )
}
