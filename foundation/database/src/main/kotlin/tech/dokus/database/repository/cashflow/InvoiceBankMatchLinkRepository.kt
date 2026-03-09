package tech.dokus.database.repository.cashflow

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.upsert
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.v1.jdbc.update
import tech.dokus.database.tables.documents.InvoiceBankMatchLinksTable
import tech.dokus.domain.Money
import tech.dokus.domain.enums.AutoMatchStatus
import tech.dokus.domain.enums.PaymentCreatedBy
import tech.dokus.domain.enums.CashflowEntryStatus
import tech.dokus.domain.enums.InvoiceStatus
import tech.dokus.domain.ids.CashflowEntryId
import tech.dokus.domain.ids.BankTransactionId
import tech.dokus.domain.ids.InvoiceId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.toDbDecimal
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toJavaUuid

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
    val importedBankTransactionId: BankTransactionId,
    val status: AutoMatchStatus,
    val createdBy: PaymentCreatedBy,
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

@OptIn(ExperimentalUuidApi::class)
class InvoiceBankMatchLinkRepository {

    suspend fun upsertAutoMatched(
        tenantId: TenantId,
        invoiceId: InvoiceId,
        cashflowEntryId: CashflowEntryId,
        transactionId: BankTransactionId,
        confidenceScore: Double,
        scoreMargin: Double,
        reasonsJson: String,
        rulesJson: String,
    ): UUID = newSuspendedTransaction {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        val tenantUuid = tenantId.value.toJavaUuid()
        val invoiceUuid = invoiceId.value.toJavaUuid()
        val entryUuid = cashflowEntryId.value.toJavaUuid()
        val txUuid = transactionId.value.toJavaUuid()

        val newId = UUID.randomUUID()
        InvoiceBankMatchLinksTable.upsert(
            InvoiceBankMatchLinksTable.tenantId,
            InvoiceBankMatchLinksTable.invoiceId,
            InvoiceBankMatchLinksTable.importedBankTransactionId,
            onUpdate = { stmt ->
                stmt[InvoiceBankMatchLinksTable.cashflowEntryId] = entryUuid
                stmt[InvoiceBankMatchLinksTable.status] = AutoMatchStatus.AutoMatched
                stmt[InvoiceBankMatchLinksTable.confidenceScore] = confidenceScore.toBigDecimal()
                stmt[InvoiceBankMatchLinksTable.scoreMargin] = scoreMargin.toBigDecimal()
                stmt[InvoiceBankMatchLinksTable.reasonsJson] = reasonsJson
                stmt[InvoiceBankMatchLinksTable.rulesJson] = rulesJson
                stmt[InvoiceBankMatchLinksTable.matchedAt] = now
                stmt[InvoiceBankMatchLinksTable.reversedAt] = null
                stmt[InvoiceBankMatchLinksTable.reversedByUserId] = null
                stmt[InvoiceBankMatchLinksTable.reversalReason] = null
                stmt[InvoiceBankMatchLinksTable.updatedAt] = now
            }
        ) {
            it[id] = newId
            it[InvoiceBankMatchLinksTable.tenantId] = tenantUuid
            it[InvoiceBankMatchLinksTable.invoiceId] = invoiceUuid
            it[InvoiceBankMatchLinksTable.cashflowEntryId] = entryUuid
            it[importedBankTransactionId] = txUuid
            it[status] = AutoMatchStatus.AutoMatched
            it[createdBy] = PaymentCreatedBy.Auto
            it[InvoiceBankMatchLinksTable.confidenceScore] = confidenceScore.toBigDecimal()
            it[InvoiceBankMatchLinksTable.scoreMargin] = scoreMargin.toBigDecimal()
            it[InvoiceBankMatchLinksTable.reasonsJson] = reasonsJson
            it[InvoiceBankMatchLinksTable.rulesJson] = rulesJson
            it[matchedAt] = now
            it[createdAt] = now
            it[updatedAt] = now
        }

        InvoiceBankMatchLinksTable.selectAll().where {
            (InvoiceBankMatchLinksTable.tenantId eq tenantUuid) and
                (InvoiceBankMatchLinksTable.invoiceId eq invoiceUuid) and
                (InvoiceBankMatchLinksTable.importedBankTransactionId eq txUuid)
        }.single()[InvoiceBankMatchLinksTable.id].value
    }

    suspend fun markAutoPaid(
        linkId: UUID,
        tenantId: TenantId,
        snapshot: InvoiceBankMatchSnapshot
    ): Boolean = newSuspendedTransaction {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        InvoiceBankMatchLinksTable.update({
            (InvoiceBankMatchLinksTable.id eq linkId) and
                (InvoiceBankMatchLinksTable.tenantId eq tenantId.value.toJavaUuid())
        }) {
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
            (InvoiceBankMatchLinksTable.tenantId eq tenantId.value.toJavaUuid()) and
                (InvoiceBankMatchLinksTable.cashflowEntryId eq entryId.value.toJavaUuid()) and
                (InvoiceBankMatchLinksTable.reversedAt.isNull())
        }.orderBy(InvoiceBankMatchLinksTable.createdAt).limit(1).singleOrNull()?.toRecord()
    }

    suspend fun findActiveByTransaction(
        tenantId: TenantId,
        transactionId: BankTransactionId
    ): InvoiceBankMatchLinkRecord? = newSuspendedTransaction {
        InvoiceBankMatchLinksTable.selectAll().where {
            (InvoiceBankMatchLinksTable.tenantId eq tenantId.value.toJavaUuid()) and
                (InvoiceBankMatchLinksTable.importedBankTransactionId eq transactionId.value.toJavaUuid()) and
                (InvoiceBankMatchLinksTable.reversedAt.isNull())
        }.orderBy(InvoiceBankMatchLinksTable.createdAt).limit(1).singleOrNull()?.toRecord()
    }

    suspend fun markReversed(
        linkId: UUID,
        tenantId: TenantId,
        reversedByUserId: UUID?,
        reason: String?
    ): Boolean = newSuspendedTransaction {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        InvoiceBankMatchLinksTable.update({
            (InvoiceBankMatchLinksTable.id eq linkId) and
                (InvoiceBankMatchLinksTable.tenantId eq tenantId.value.toJavaUuid())
        }) {
            it[status] = AutoMatchStatus.Reversed
            it[reversedAt] = now
            it[InvoiceBankMatchLinksTable.reversedByUserId] = reversedByUserId
            it[reversalReason] = reason
            it[updatedAt] = now
        } > 0
    }

    private fun ResultRow.toRecord(): InvoiceBankMatchLinkRecord {
        return InvoiceBankMatchLinkRecord(
            id = this[InvoiceBankMatchLinksTable.id].value,
            tenantId = TenantId.parse(this[InvoiceBankMatchLinksTable.tenantId].toString()),
            invoiceId = InvoiceId.parse(this[InvoiceBankMatchLinksTable.invoiceId].toString()),
            cashflowEntryId = CashflowEntryId.parse(this[InvoiceBankMatchLinksTable.cashflowEntryId].toString()),
            importedBankTransactionId = BankTransactionId.parse(this[InvoiceBankMatchLinksTable.importedBankTransactionId].toString()),
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
