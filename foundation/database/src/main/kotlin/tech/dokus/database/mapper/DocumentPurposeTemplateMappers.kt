package tech.dokus.database.mapper

import org.jetbrains.exposed.v1.core.ResultRow
import tech.dokus.database.repository.cashflow.DocumentPurposeTemplateEntity
import tech.dokus.database.tables.documents.DocumentPurposeTemplatesTable
import tech.dokus.domain.ids.TenantId

internal fun DocumentPurposeTemplateEntity.Companion.from(row: ResultRow): DocumentPurposeTemplateEntity {
    return DocumentPurposeTemplateEntity(
        tenantId = TenantId.parse(row[DocumentPurposeTemplatesTable.tenantId].toString()),
        counterpartyKey = row[DocumentPurposeTemplatesTable.counterpartyKey],
        documentType = row[DocumentPurposeTemplatesTable.documentType],
        purposeBase = row[DocumentPurposeTemplatesTable.purposeBase],
        periodMode = row[DocumentPurposeTemplatesTable.periodMode],
        confidence = row[DocumentPurposeTemplatesTable.confidence].toDouble(),
        usageCount = row[DocumentPurposeTemplatesTable.usageCount]
    )
}
