package tech.dokus.database.mapper

import org.jetbrains.exposed.v1.core.ResultRow
import tech.dokus.database.repository.cashflow.DocumentPurposeTemplateSummary
import tech.dokus.database.tables.documents.DocumentPurposeTemplatesTable
import tech.dokus.domain.ids.TenantId

internal fun ResultRow.toTemplateSummary(): DocumentPurposeTemplateSummary {
    return DocumentPurposeTemplateSummary(
        tenantId = TenantId.parse(this[DocumentPurposeTemplatesTable.tenantId].toString()),
        counterpartyKey = this[DocumentPurposeTemplatesTable.counterpartyKey],
        documentType = this[DocumentPurposeTemplatesTable.documentType],
        purposeBase = this[DocumentPurposeTemplatesTable.purposeBase],
        periodMode = this[DocumentPurposeTemplatesTable.periodMode],
        confidence = this[DocumentPurposeTemplatesTable.confidence].toDouble(),
        usageCount = this[DocumentPurposeTemplatesTable.usageCount]
    )
}
