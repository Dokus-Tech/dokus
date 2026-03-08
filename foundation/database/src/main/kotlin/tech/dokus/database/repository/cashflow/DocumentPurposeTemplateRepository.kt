package tech.dokus.database.repository.cashflow

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.v1.jdbc.update
import tech.dokus.database.tables.documents.DocumentPurposeTemplatesTable
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.enums.PurposePeriodMode
import tech.dokus.domain.ids.TenantId
import java.util.UUID

data class DocumentPurposeTemplateSummary(
    val tenantId: TenantId,
    val counterpartyKey: String,
    val documentType: DocumentType,
    val purposeBase: String,
    val periodMode: PurposePeriodMode,
    val confidence: Double,
    val usageCount: Int
)

class DocumentPurposeTemplateRepository {
    suspend fun findByCounterparty(
        tenantId: TenantId,
        counterpartyKey: String,
        documentType: DocumentType
    ): DocumentPurposeTemplateSummary? = newSuspendedTransaction {
        DocumentPurposeTemplatesTable.selectAll()
            .where {
                (DocumentPurposeTemplatesTable.tenantId eq UUID.fromString(tenantId.toString())) and
                    (DocumentPurposeTemplatesTable.counterpartyKey eq counterpartyKey) and
                    (DocumentPurposeTemplatesTable.documentType eq documentType)
            }
            .singleOrNull()
            ?.toTemplateSummary()
    }

    suspend fun upsert(
        tenantId: TenantId,
        counterpartyKey: String,
        documentType: DocumentType,
        purposeBase: String,
        periodMode: PurposePeriodMode,
        confidence: Double = 1.0,
        incrementUsage: Boolean = true
    ) = newSuspendedTransaction {
        val tenantUuid = UUID.fromString(tenantId.toString())
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        val whereClause =
            (DocumentPurposeTemplatesTable.tenantId eq tenantUuid) and
                (DocumentPurposeTemplatesTable.counterpartyKey eq counterpartyKey) and
                (DocumentPurposeTemplatesTable.documentType eq documentType)
        val existing = DocumentPurposeTemplatesTable.selectAll()
            .where { whereClause }
            .singleOrNull()

        if (existing == null) {
            DocumentPurposeTemplatesTable.insert {
                it[DocumentPurposeTemplatesTable.tenantId] = tenantUuid
                it[DocumentPurposeTemplatesTable.counterpartyKey] = counterpartyKey
                it[DocumentPurposeTemplatesTable.documentType] = documentType
                it[DocumentPurposeTemplatesTable.purposeBase] = purposeBase
                it[DocumentPurposeTemplatesTable.periodMode] = periodMode
                it[DocumentPurposeTemplatesTable.confidence] = confidence.toBigDecimal()
                it[usageCount] = if (incrementUsage) 1 else 0
                it[createdAt] = now
                it[updatedAt] = now
            }
        } else {
            val currentUsageCount = existing[DocumentPurposeTemplatesTable.usageCount]
            DocumentPurposeTemplatesTable.update({ whereClause }) {
                it[DocumentPurposeTemplatesTable.purposeBase] = purposeBase
                it[DocumentPurposeTemplatesTable.periodMode] = periodMode
                it[DocumentPurposeTemplatesTable.confidence] = confidence.toBigDecimal()
                if (incrementUsage) {
                    it[DocumentPurposeTemplatesTable.usageCount] = currentUsageCount + 1
                }
                it[DocumentPurposeTemplatesTable.updatedAt] = now
            }
        }
    }

    private fun ResultRow.toTemplateSummary(): DocumentPurposeTemplateSummary {
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
}
