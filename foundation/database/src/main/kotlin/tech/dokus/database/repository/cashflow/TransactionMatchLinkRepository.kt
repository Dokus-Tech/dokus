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
import tech.dokus.database.tables.documents.TransactionMatchLinksTable
import tech.dokus.domain.Money
import tech.dokus.domain.enums.AutoMatchStatus
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.enums.PaymentCreatedBy
import tech.dokus.domain.enums.CashflowEntryStatus
import tech.dokus.domain.enums.InvoiceStatus
import tech.dokus.domain.ids.CashflowEntryId
import tech.dokus.domain.ids.BankTransactionId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.fromDbDecimal
import tech.dokus.domain.toDbDecimal
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toJavaUuid

data class TransactionMatchSnapshot(
    val invoiceStatusBefore: InvoiceStatus,
    val invoicePaidAmountBefore: Money,
    val invoicePaidAtBefore: LocalDateTime?,
    val cashflowStatusBefore: CashflowEntryStatus,
    val cashflowRemainingBefore: Money,
    val cashflowPaidAtBefore: LocalDateTime?,
)

data class TransactionMatchLinkRecord(
    val id: UUID,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val cashflowEntryId: CashflowEntryId,
    val importedBankTransactionId: BankTransactionId,
    val documentType: DocumentType?,
    val allocatedAmount: Money?,
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
    val reversalReason: String?,
)

@OptIn(ExperimentalUuidApi::class)
class TransactionMatchLinkRepository {

    suspend fun upsertAutoMatched(
        tenantId: TenantId,
        documentId: DocumentId,
        cashflowEntryId: CashflowEntryId,
        transactionId: BankTransactionId,
        confidenceScore: Double,
        scoreMargin: Double,
        reasonsJson: String,
        rulesJson: String,
        documentType: DocumentType? = null,
        allocatedAmount: Money? = null,
    ): UUID = newSuspendedTransaction {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        val tenantUuid = tenantId.value.toJavaUuid()
        val docUuid = documentId.value.toJavaUuid()
        val entryUuid = cashflowEntryId.value.toJavaUuid()
        val txUuid = transactionId.value.toJavaUuid()

        val newId = UUID.randomUUID()
        TransactionMatchLinksTable.upsert(
            TransactionMatchLinksTable.tenantId,
            TransactionMatchLinksTable.documentId,
            TransactionMatchLinksTable.importedBankTransactionId,
            onUpdate = { stmt ->
                stmt[TransactionMatchLinksTable.cashflowEntryId] = entryUuid
                stmt[TransactionMatchLinksTable.status] = AutoMatchStatus.AutoMatched
                stmt[TransactionMatchLinksTable.confidenceScore] = confidenceScore.toBigDecimal()
                stmt[TransactionMatchLinksTable.scoreMargin] = scoreMargin.toBigDecimal()
                stmt[TransactionMatchLinksTable.reasonsJson] = reasonsJson
                stmt[TransactionMatchLinksTable.rulesJson] = rulesJson
                stmt[TransactionMatchLinksTable.documentType] = documentType
                if (allocatedAmount != null) {
                    stmt[TransactionMatchLinksTable.allocatedAmount] = allocatedAmount.toDbDecimal()
                }
                stmt[TransactionMatchLinksTable.matchedAt] = now
                stmt[TransactionMatchLinksTable.reversedAt] = null
                stmt[TransactionMatchLinksTable.reversedByUserId] = null
                stmt[TransactionMatchLinksTable.reversalReason] = null
                stmt[TransactionMatchLinksTable.updatedAt] = now
            }
        ) {
            it[id] = newId
            it[TransactionMatchLinksTable.tenantId] = tenantUuid
            it[TransactionMatchLinksTable.documentId] = docUuid
            it[TransactionMatchLinksTable.cashflowEntryId] = entryUuid
            it[importedBankTransactionId] = txUuid
            it[status] = AutoMatchStatus.AutoMatched
            it[createdBy] = PaymentCreatedBy.Auto
            it[TransactionMatchLinksTable.confidenceScore] = confidenceScore.toBigDecimal()
            it[TransactionMatchLinksTable.scoreMargin] = scoreMargin.toBigDecimal()
            it[TransactionMatchLinksTable.reasonsJson] = reasonsJson
            it[TransactionMatchLinksTable.rulesJson] = rulesJson
            it[TransactionMatchLinksTable.documentType] = documentType
            if (allocatedAmount != null) {
                it[TransactionMatchLinksTable.allocatedAmount] = allocatedAmount.toDbDecimal()
            }
            it[matchedAt] = now
            it[createdAt] = now
            it[updatedAt] = now
        }

        TransactionMatchLinksTable.selectAll().where {
            (TransactionMatchLinksTable.tenantId eq tenantUuid) and
                (TransactionMatchLinksTable.documentId eq docUuid) and
                (TransactionMatchLinksTable.importedBankTransactionId eq txUuid)
        }.single()[TransactionMatchLinksTable.id].value
    }

    suspend fun markAutoPaid(
        linkId: UUID,
        tenantId: TenantId,
        snapshot: TransactionMatchSnapshot,
    ): Boolean = newSuspendedTransaction {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        TransactionMatchLinksTable.update({
            (TransactionMatchLinksTable.id eq linkId) and
                (TransactionMatchLinksTable.tenantId eq tenantId.value.toJavaUuid())
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
        entryId: CashflowEntryId,
    ): TransactionMatchLinkRecord? = newSuspendedTransaction {
        TransactionMatchLinksTable.selectAll().where {
            (TransactionMatchLinksTable.tenantId eq tenantId.value.toJavaUuid()) and
                (TransactionMatchLinksTable.cashflowEntryId eq entryId.value.toJavaUuid()) and
                (TransactionMatchLinksTable.reversedAt.isNull())
        }.orderBy(TransactionMatchLinksTable.createdAt).limit(1).singleOrNull()?.toRecord()
    }

    suspend fun findActiveByTransaction(
        tenantId: TenantId,
        transactionId: BankTransactionId,
    ): TransactionMatchLinkRecord? = newSuspendedTransaction {
        TransactionMatchLinksTable.selectAll().where {
            (TransactionMatchLinksTable.tenantId eq tenantId.value.toJavaUuid()) and
                (TransactionMatchLinksTable.importedBankTransactionId eq transactionId.value.toJavaUuid()) and
                (TransactionMatchLinksTable.reversedAt.isNull())
        }.orderBy(TransactionMatchLinksTable.createdAt).limit(1).singleOrNull()?.toRecord()
    }

    suspend fun markReversed(
        linkId: UUID,
        tenantId: TenantId,
        reversedByUserId: UUID?,
        reason: String?,
    ): Boolean = newSuspendedTransaction {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        TransactionMatchLinksTable.update({
            (TransactionMatchLinksTable.id eq linkId) and
                (TransactionMatchLinksTable.tenantId eq tenantId.value.toJavaUuid())
        }) {
            it[status] = AutoMatchStatus.Reversed
            it[reversedAt] = now
            it[TransactionMatchLinksTable.reversedByUserId] = reversedByUserId
            it[reversalReason] = reason
            it[updatedAt] = now
        } > 0
    }

    private fun ResultRow.toRecord(): TransactionMatchLinkRecord {
        return TransactionMatchLinkRecord(
            id = this[TransactionMatchLinksTable.id].value,
            tenantId = TenantId.parse(this[TransactionMatchLinksTable.tenantId].toString()),
            documentId = DocumentId.parse(this[TransactionMatchLinksTable.documentId].toString()),
            cashflowEntryId = CashflowEntryId.parse(this[TransactionMatchLinksTable.cashflowEntryId].toString()),
            importedBankTransactionId = BankTransactionId.parse(this[TransactionMatchLinksTable.importedBankTransactionId].toString()),
            documentType = this[TransactionMatchLinksTable.documentType],
            allocatedAmount = this[TransactionMatchLinksTable.allocatedAmount]?.let { Money.fromDbDecimal(it) },
            status = this[TransactionMatchLinksTable.status],
            createdBy = this[TransactionMatchLinksTable.createdBy],
            confidenceScore = this[TransactionMatchLinksTable.confidenceScore]?.toDouble(),
            scoreMargin = this[TransactionMatchLinksTable.scoreMargin]?.toDouble(),
            reasonsJson = this[TransactionMatchLinksTable.reasonsJson],
            rulesJson = this[TransactionMatchLinksTable.rulesJson],
            matchedAt = this[TransactionMatchLinksTable.matchedAt],
            autoPaidAt = this[TransactionMatchLinksTable.autoPaidAt],
            reversedAt = this[TransactionMatchLinksTable.reversedAt],
            reversedByUserId = this[TransactionMatchLinksTable.reversedByUserId],
            reversalReason = this[TransactionMatchLinksTable.reversalReason],
        )
    }
}
