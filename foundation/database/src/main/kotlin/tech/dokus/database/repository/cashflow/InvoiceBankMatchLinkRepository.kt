package tech.dokus.database.repository.cashflow

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.jdbc.insertIgnore
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.v1.jdbc.update
import tech.dokus.database.tables.documents.InvoiceBankMatchLinksTable
import tech.dokus.domain.Money
import tech.dokus.domain.enums.AutoMatchCreatedBy
import tech.dokus.domain.enums.AutoMatchStatus
import tech.dokus.domain.enums.CashflowEntryStatus
import tech.dokus.domain.enums.InvoiceStatus
import tech.dokus.domain.ids.CashflowEntryId
import tech.dokus.domain.ids.ImportedBankTransactionId
import tech.dokus.domain.ids.InvoiceId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.toDbDecimal
import java.util.UUID

data class InvoiceBankMatchSnapshot(
    val invoiceStatusBefore: InvoiceStatus,
    val invoicePaidAmountBefore: Money,
    val invoicePaidAtBefore: LocalDateTime?,
    val cashflowStatusBefore: CashflowEntryStatus,
    val cashflowRemainingBefore: Money,
    val cashflowPaidAtBefore: LocalDateTime?
)

data class InvoiceBankMatchLinkRecord(
    val id: UUID,
    val tenantId: TenantId,
    val invoiceId: InvoiceId,
    val cashflowEntryId: CashflowEntryId,
    val importedBankTransactionId: ImportedBankTransactionId,
    val status: AutoMatchStatus,
    val createdBy: AutoMatchCreatedBy,
    val confidenceScore: Double?,
    val scoreMargin: Double?,
    val reasonsJson: String?,
    val rulesJson: String?,
    val matchedAt: LocalDateTime,
    val autoPaidAt: LocalDateTime?,
    val reversedAt: LocalDateTime?,
    val reversedByUserId: UUID?,
    val reversalReason: String?
)

class InvoiceBankMatchLinkRepository {

    suspend fun upsertAutoMatched(
        tenantId: TenantId,
        invoiceId: InvoiceId,
        cashflowEntryId: CashflowEntryId,
        transactionId: ImportedBankTransactionId,
        confidenceScore: Double,
        scoreMargin: Double,
        reasonsJson: String,
        rulesJson: String,
    ): UUID = newSuspendedTransaction {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        val tenantUuid = UUID.fromString(tenantId.toString())
        val invoiceUuid = UUID.fromString(invoiceId.toString())
        val entryUuid = UUID.fromString(cashflowEntryId.toString())
        val txUuid = UUID.fromString(transactionId.toString())

        val existing = InvoiceBankMatchLinksTable.selectAll().where {
            (InvoiceBankMatchLinksTable.tenantId eq tenantUuid) and
                (InvoiceBankMatchLinksTable.invoiceId eq invoiceUuid) and
                (InvoiceBankMatchLinksTable.importedBankTransactionId eq txUuid)
        }.singleOrNull()

        if (existing == null) {
            val newId = UUID.randomUUID()
            InvoiceBankMatchLinksTable.insertIgnore {
                it[id] = newId
                it[InvoiceBankMatchLinksTable.tenantId] = tenantUuid
                it[InvoiceBankMatchLinksTable.invoiceId] = invoiceUuid
                it[InvoiceBankMatchLinksTable.cashflowEntryId] = entryUuid
                it[importedBankTransactionId] = txUuid
                it[status] = AutoMatchStatus.AutoMatched
                it[createdBy] = AutoMatchCreatedBy.Auto
                it[InvoiceBankMatchLinksTable.confidenceScore] = confidenceScore.toBigDecimal()
                it[InvoiceBankMatchLinksTable.scoreMargin] = scoreMargin.toBigDecimal()
                it[InvoiceBankMatchLinksTable.reasonsJson] = reasonsJson
                it[InvoiceBankMatchLinksTable.rulesJson] = rulesJson
                it[matchedAt] = now
                it[createdAt] = now
                it[updatedAt] = now
            }
            newId
        } else {
            val existingId = existing[InvoiceBankMatchLinksTable.id].value
            InvoiceBankMatchLinksTable.update({
                (InvoiceBankMatchLinksTable.id eq existingId)
            }) {
                it[InvoiceBankMatchLinksTable.cashflowEntryId] = entryUuid
                it[status] = AutoMatchStatus.AutoMatched
                it[InvoiceBankMatchLinksTable.confidenceScore] = confidenceScore.toBigDecimal()
                it[InvoiceBankMatchLinksTable.scoreMargin] = scoreMargin.toBigDecimal()
                it[InvoiceBankMatchLinksTable.reasonsJson] = reasonsJson
                it[InvoiceBankMatchLinksTable.rulesJson] = rulesJson
                it[matchedAt] = now
                it[reversedAt] = null
                it[reversedByUserId] = null
                it[reversalReason] = null
                it[updatedAt] = now
            }
            existingId
        }
    }

    suspend fun markAutoPaid(
        linkId: UUID,
        snapshot: InvoiceBankMatchSnapshot
    ): Boolean = newSuspendedTransaction {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        InvoiceBankMatchLinksTable.update({ InvoiceBankMatchLinksTable.id eq linkId }) {
            it[status] = AutoMatchStatus.AutoPaid
            it[autoPaidAt] = now
            it[invoiceStatusBefore] = snapshot.invoiceStatusBefore
            it[invoicePaidAmountBefore] = snapshot.invoicePaidAmountBefore.toDbDecimal()
            it[invoicePaidAtBefore] = snapshot.invoicePaidAtBefore
            it[cashflowStatusBefore] = snapshot.cashflowStatusBefore
            it[cashflowRemainingBefore] = snapshot.cashflowRemainingBefore.toDbDecimal()
            it[cashflowPaidAtBefore] = snapshot.cashflowPaidAtBefore
            it[updatedAt] = now
        } > 0
    }

    suspend fun findActiveByEntry(
        tenantId: TenantId,
        entryId: CashflowEntryId
    ): InvoiceBankMatchLinkRecord? = newSuspendedTransaction {
        InvoiceBankMatchLinksTable.selectAll().where {
            (InvoiceBankMatchLinksTable.tenantId eq UUID.fromString(tenantId.toString())) and
                (InvoiceBankMatchLinksTable.cashflowEntryId eq UUID.fromString(entryId.toString())) and
                (InvoiceBankMatchLinksTable.reversedAt.isNull())
        }.orderBy(InvoiceBankMatchLinksTable.createdAt).limit(1).singleOrNull()?.toRecord()
    }

    suspend fun findActiveByTransaction(
        tenantId: TenantId,
        transactionId: ImportedBankTransactionId
    ): InvoiceBankMatchLinkRecord? = newSuspendedTransaction {
        InvoiceBankMatchLinksTable.selectAll().where {
            (InvoiceBankMatchLinksTable.tenantId eq UUID.fromString(tenantId.toString())) and
                (InvoiceBankMatchLinksTable.importedBankTransactionId eq UUID.fromString(transactionId.toString())) and
                (InvoiceBankMatchLinksTable.reversedAt.isNull())
        }.orderBy(InvoiceBankMatchLinksTable.createdAt).limit(1).singleOrNull()?.toRecord()
    }

    suspend fun markReversed(
        linkId: UUID,
        reversedByUserId: UUID?,
        reason: String?
    ): Boolean = newSuspendedTransaction {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        InvoiceBankMatchLinksTable.update({ InvoiceBankMatchLinksTable.id eq linkId }) {
            it[status] = AutoMatchStatus.Reversed
            it[reversedAt] = now
            it[InvoiceBankMatchLinksTable.reversedByUserId] = reversedByUserId
            it[reversalReason] = reason
            it[updatedAt] = now
        } > 0
    }

    private fun org.jetbrains.exposed.v1.core.ResultRow.toRecord(): InvoiceBankMatchLinkRecord {
        return InvoiceBankMatchLinkRecord(
            id = this[InvoiceBankMatchLinksTable.id].value,
            tenantId = TenantId.parse(this[InvoiceBankMatchLinksTable.tenantId].toString()),
            invoiceId = InvoiceId.parse(this[InvoiceBankMatchLinksTable.invoiceId].toString()),
            cashflowEntryId = CashflowEntryId.parse(this[InvoiceBankMatchLinksTable.cashflowEntryId].toString()),
            importedBankTransactionId = ImportedBankTransactionId.parse(this[InvoiceBankMatchLinksTable.importedBankTransactionId].toString()),
            status = this[InvoiceBankMatchLinksTable.status],
            createdBy = this[InvoiceBankMatchLinksTable.createdBy],
            confidenceScore = this[InvoiceBankMatchLinksTable.confidenceScore]?.toDouble(),
            scoreMargin = this[InvoiceBankMatchLinksTable.scoreMargin]?.toDouble(),
            reasonsJson = this[InvoiceBankMatchLinksTable.reasonsJson],
            rulesJson = this[InvoiceBankMatchLinksTable.rulesJson],
            matchedAt = this[InvoiceBankMatchLinksTable.matchedAt],
            autoPaidAt = this[InvoiceBankMatchLinksTable.autoPaidAt],
            reversedAt = this[InvoiceBankMatchLinksTable.reversedAt],
            reversedByUserId = this[InvoiceBankMatchLinksTable.reversedByUserId],
            reversalReason = this[InvoiceBankMatchLinksTable.reversalReason],
        )
    }
}
