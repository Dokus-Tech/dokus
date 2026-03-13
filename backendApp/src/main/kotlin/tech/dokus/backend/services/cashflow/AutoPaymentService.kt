package tech.dokus.backend.services.cashflow

import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.v1.jdbc.update
import tech.dokus.database.repository.cashflow.CashflowEntriesRepository
import tech.dokus.database.tables.cashflow.CashflowEntriesTable
import tech.dokus.database.tables.cashflow.InvoicesTable
import tech.dokus.database.tables.documents.AutoPaymentAuditEventsTable
import tech.dokus.database.tables.banking.BankTransactionsTable
import tech.dokus.database.tables.documents.TransactionMatchLinksTable
import tech.dokus.database.tables.payment.PaymentsTable
import tech.dokus.domain.Money
import tech.dokus.domain.enums.AutoMatchStatus
import tech.dokus.domain.enums.AutoPaymentDecision
import tech.dokus.domain.enums.AutoPaymentTriggerSource
import tech.dokus.domain.enums.CashflowEntryStatus
import tech.dokus.domain.enums.CashflowSourceType
import tech.dokus.domain.enums.BankTransactionStatus
import tech.dokus.domain.enums.InvoiceStatus
import tech.dokus.domain.enums.PaymentCreatedBy
import tech.dokus.domain.enums.PaymentMethod
import tech.dokus.domain.enums.PaymentSource
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.fromDbDecimal
import tech.dokus.domain.ids.CashflowEntryId
import tech.dokus.domain.ids.BankTransactionId
import tech.dokus.domain.ids.PaymentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.UserId
import tech.dokus.domain.model.AutoPaymentStatusDto
import tech.dokus.domain.model.CashflowEntry
import tech.dokus.domain.model.BankTransactionDto
import tech.dokus.domain.toDbDecimal
import tech.dokus.foundation.backend.utils.runSuspendCatching
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toJavaUuid

private data class AutoPayApplyResult(
    val applied: Boolean,
    val paymentId: PaymentId?
)

@OptIn(ExperimentalUuidApi::class)
class AutoPaymentService(
    private val cashflowEntriesRepository: CashflowEntriesRepository
) {
    suspend fun applyAutoPayment(
        tenantId: TenantId,
        entry: CashflowEntry,
        transaction: BankTransactionDto,
        confidenceScore: Double,
        scoreMargin: Double,
        reasonsJson: String,
        rulesJson: String,
        triggerSource: AutoPaymentTriggerSource
    ): Result<Boolean> = runSuspendCatching {
        if (entry.sourceType != CashflowSourceType.Invoice) return@runSuspendCatching false
        if (entry.remainingAmount.isZero) return@runSuspendCatching false

        val applyResult = applyAutoPaymentInTransaction(
            tenantId = tenantId,
            entry = entry,
            transaction = transaction,
            confidenceScore = confidenceScore,
            scoreMargin = scoreMargin,
            reasonsJson = reasonsJson,
            rulesJson = rulesJson,
            triggerSource = triggerSource
        )

        applyResult.applied
    }

    suspend fun getAutoPaymentStatus(
        tenantId: TenantId,
        entryId: CashflowEntryId
    ): AutoPaymentStatusDto = newSuspendedTransaction {
        val tenantUuid = tenantId.value.toJavaUuid()
        val entryUuid = entryId.value.toJavaUuid()

        val link = TransactionMatchLinksTable.selectAll().where {
            (TransactionMatchLinksTable.tenantId eq tenantUuid) and
                (TransactionMatchLinksTable.cashflowEntryId eq entryUuid) and
                (TransactionMatchLinksTable.reversedAt.isNull())
        }.orderBy(TransactionMatchLinksTable.createdAt)
            .lastOrNull() ?: return@newSuspendedTransaction AutoPaymentStatusDto()

        val invoiceUuid = link[TransactionMatchLinksTable.documentId]
        val paymentRow = PaymentsTable.selectAll().where {
            (PaymentsTable.tenantId eq tenantUuid) and
                (PaymentsTable.invoiceId eq invoiceUuid) and
                (PaymentsTable.bankTransactionId eq link[TransactionMatchLinksTable.importedBankTransactionId]) and
                (PaymentsTable.reversedAt.isNull())
        }.singleOrNull()

        val nonReversedPayments = PaymentsTable.selectAll().where {
            (PaymentsTable.tenantId eq tenantUuid) and
                (PaymentsTable.invoiceId eq invoiceUuid) and
                (PaymentsTable.reversedAt.isNull())
        }.count()

        AutoPaymentStatusDto(
            matchStatus = link[TransactionMatchLinksTable.status],
            paymentId = paymentRow?.let { PaymentId.parse(it[PaymentsTable.id].value.toString()) },
            bankTransactionId = BankTransactionId.parse(
                link[TransactionMatchLinksTable.importedBankTransactionId].toString()
            ),
            confidenceScore = link[TransactionMatchLinksTable.confidenceScore]?.toDouble(),
            scoreMargin = link[TransactionMatchLinksTable.scoreMargin]?.toDouble(),
            reasons = parseJsonArray(link[TransactionMatchLinksTable.reasonsJson]),
            matchSignals = parseJsonArray(link[TransactionMatchLinksTable.rulesJson]),
            matchedAt = link[TransactionMatchLinksTable.matchedAt],
            autoPaidAt = link[TransactionMatchLinksTable.autoPaidAt],
            canUndo =
            link[TransactionMatchLinksTable.status] == AutoMatchStatus.AutoPaid &&
                paymentRow != null &&
                nonReversedPayments == 1L
        )
    }

    suspend fun undoAutoPayment(
        tenantId: TenantId,
        entryId: CashflowEntryId,
        actorUserId: UserId?,
        reason: String?
    ): Result<CashflowEntry> = runSuspendCatching {
        val tenantUuid = tenantId.value.toJavaUuid()
        val entryUuid = entryId.value.toJavaUuid()
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)

        newSuspendedTransaction {
            val link = TransactionMatchLinksTable.selectAll().where {
                (TransactionMatchLinksTable.tenantId eq tenantUuid) and
                    (TransactionMatchLinksTable.cashflowEntryId eq entryUuid) and
                    (TransactionMatchLinksTable.status eq AutoMatchStatus.AutoPaid) and
                    (TransactionMatchLinksTable.reversedAt.isNull())
            }.orderBy(TransactionMatchLinksTable.createdAt).lastOrNull()
                ?: throw DokusException.BadRequest("No active auto payment found")

            val invoiceId = link[TransactionMatchLinksTable.documentId]
            val txId = link[TransactionMatchLinksTable.importedBankTransactionId]

            val payment = PaymentsTable.selectAll().where {
                (PaymentsTable.tenantId eq tenantUuid) and
                    (PaymentsTable.invoiceId eq invoiceId) and
                    (PaymentsTable.bankTransactionId eq txId) and
                    (PaymentsTable.paymentSource eq PaymentSource.BankImport) and
                    (PaymentsTable.createdBy eq PaymentCreatedBy.Auto) and
                    (PaymentsTable.reversedAt.isNull())
            }.singleOrNull() ?: throw DokusException.BadRequest("Auto payment record not found")

            val nonReversedCount = PaymentsTable.selectAll().where {
                (PaymentsTable.tenantId eq tenantUuid) and
                    (PaymentsTable.invoiceId eq invoiceId) and
                    (PaymentsTable.reversedAt.isNull())
            }.count()
            if (nonReversedCount != 1L) {
                appendAudit(
                    tenantUuid = tenantUuid,
                    triggerSource = AutoPaymentTriggerSource.UndoRequest,
                    decision = AutoPaymentDecision.UndoRejected,
                    invoiceId = invoiceId,
                    entryId = entryUuid,
                    txId = txId,
                    score = link[TransactionMatchLinksTable.confidenceScore]?.toDouble(),
                    margin = link[TransactionMatchLinksTable.scoreMargin]?.toDouble(),
                    reasonsJson = link[TransactionMatchLinksTable.reasonsJson],
                    rulesJson = link[TransactionMatchLinksTable.rulesJson],
                    actorUserId = actorUserId
                )
                throw DokusException.BadRequest("Undo is blocked because dependent payments exist")
            }

            val invoiceStatusBefore = link[TransactionMatchLinksTable.invoiceStatusBefore]
                ?: throw DokusException.BadRequest("Undo snapshot missing invoice status")
            val invoicePaidAmountBefore = link[TransactionMatchLinksTable.invoicePaidAmountBefore]
                ?: throw DokusException.BadRequest("Undo snapshot missing invoice paid amount")
            val cashflowStatusBefore = link[TransactionMatchLinksTable.cashflowStatusBefore]
                ?: throw DokusException.BadRequest("Undo snapshot missing cashflow status")
            val cashflowRemainingBefore = link[TransactionMatchLinksTable.cashflowRemainingBefore]
                ?: throw DokusException.BadRequest("Undo snapshot missing cashflow remaining amount")

            PaymentsTable.update({
                (PaymentsTable.id eq payment[PaymentsTable.id].value) and
                    (PaymentsTable.tenantId eq tenantUuid)
            }) {
                it[reversedAt] = now
                it[reversedByUserId] = actorUserId?.value?.toJavaUuid()
                it[reversalReason] = reason
            }

            InvoicesTable.update({
                (InvoicesTable.id eq invoiceId) and (InvoicesTable.tenantId eq tenantUuid)
            }) {
                it[paidAmount] = invoicePaidAmountBefore
                it[status] = invoiceStatusBefore
                it[paidAt] = link[TransactionMatchLinksTable.invoicePaidAtBefore]
            }

            CashflowEntriesTable.update({
                (CashflowEntriesTable.id eq entryUuid) and (CashflowEntriesTable.tenantId eq tenantUuid)
            }) {
                it[remainingAmount] = cashflowRemainingBefore
                it[status] = cashflowStatusBefore
                it[paidAt] = link[TransactionMatchLinksTable.cashflowPaidAtBefore]
            }

            BankTransactionsTable.update({
                (BankTransactionsTable.id eq txId) and
                    (BankTransactionsTable.tenantId eq tenantUuid)
            }) {
                it[status] = BankTransactionStatus.Unmatched
                it[matchedCashflowId] = null
                it[matchedDocumentId] = null
                it[matchScore] = null
                it[matchEvidence] = null
                it[matchedBy] = null
                it[matchedAt] = null
                it[resolutionType] = null
                it[updatedAt] = now
            }

            TransactionMatchLinksTable.update(
                { TransactionMatchLinksTable.id eq link[TransactionMatchLinksTable.id].value }
            ) {
                it[status] = AutoMatchStatus.Reversed
                it[reversedAt] = now
                it[reversedByUserId] = actorUserId?.value?.toJavaUuid()
                it[reversalReason] = reason
                it[updatedAt] = now
            }

            appendAudit(
                tenantUuid = tenantUuid,
                triggerSource = AutoPaymentTriggerSource.UndoRequest,
                decision = AutoPaymentDecision.UndoApplied,
                invoiceId = invoiceId,
                entryId = entryUuid,
                txId = txId,
                score = link[TransactionMatchLinksTable.confidenceScore]?.toDouble(),
                margin = link[TransactionMatchLinksTable.scoreMargin]?.toDouble(),
                reasonsJson = link[TransactionMatchLinksTable.reasonsJson],
                rulesJson = link[TransactionMatchLinksTable.rulesJson],
                actorUserId = actorUserId
            )
        }

        cashflowEntriesRepository.getEntry(entryId, tenantId).getOrNull()
            ?: throw DokusException.NotFound("Cashflow entry not found after undo")
    }

    private suspend fun applyAutoPaymentInTransaction(
        tenantId: TenantId,
        entry: CashflowEntry,
        transaction: BankTransactionDto,
        confidenceScore: Double,
        scoreMargin: Double,
        reasonsJson: String,
        rulesJson: String,
        triggerSource: AutoPaymentTriggerSource
    ): AutoPayApplyResult = newSuspendedTransaction {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        val tenantUuid = tenantId.value.toJavaUuid()
        val entryUuid = entry.id.value.toJavaUuid()
        val txUuid = transaction.id.value.toJavaUuid()
        val invoiceUuid = runCatching { UUID.fromString(entry.sourceId) }
            .getOrElse { throw DokusException.BadRequest("Cashflow entry source is not an invoice UUID") }

        val invoiceRow = InvoicesTable.selectAll().where {
            (InvoicesTable.id eq invoiceUuid) and (InvoicesTable.tenantId eq tenantUuid)
        }.singleOrNull() ?: throw DokusException.NotFound("Invoice not found")

        if (invoiceRow[InvoicesTable.status] in listOf(InvoiceStatus.Paid, InvoiceStatus.Cancelled, InvoiceStatus.Refunded)) {
            appendAudit(
                tenantUuid = tenantUuid,
                triggerSource = triggerSource,
                decision = AutoPaymentDecision.Skipped,
                invoiceId = invoiceUuid,
                entryId = entryUuid,
                txId = txUuid,
                score = confidenceScore,
                margin = scoreMargin,
                reasonsJson = reasonsJson,
                rulesJson = rulesJson,
                actorUserId = null
            )
            return@newSuspendedTransaction AutoPayApplyResult(false, null)
        }

        val currentPaid = Money.fromDbDecimal(invoiceRow[InvoicesTable.paidAmount])
        if (!currentPaid.isZero) {
            appendAudit(
                tenantUuid = tenantUuid,
                triggerSource = triggerSource,
                decision = AutoPaymentDecision.NeedsReviewOnly,
                invoiceId = invoiceUuid,
                entryId = entryUuid,
                txId = txUuid,
                score = confidenceScore,
                margin = scoreMargin,
                reasonsJson = reasonsJson,
                rulesJson = rulesJson,
                actorUserId = null
            )
            return@newSuspendedTransaction AutoPayApplyResult(false, null)
        }

        val absoluteAmount = transaction.signedAmount.absolute()
        if (absoluteAmount != entry.remainingAmount) {
            appendAudit(
                tenantUuid = tenantUuid,
                triggerSource = triggerSource,
                decision = AutoPaymentDecision.NeedsReviewOnly,
                invoiceId = invoiceUuid,
                entryId = entryUuid,
                txId = txUuid,
                score = confidenceScore,
                margin = scoreMargin,
                reasonsJson = reasonsJson,
                rulesJson = rulesJson,
                actorUserId = null
            )
            return@newSuspendedTransaction AutoPayApplyResult(false, null)
        }

        val txRow = BankTransactionsTable.selectAll().where {
            (BankTransactionsTable.id eq txUuid) and
                (BankTransactionsTable.tenantId eq tenantUuid)
        }.singleOrNull() ?: throw DokusException.NotFound("Imported bank transaction not found")

        if (txRow[BankTransactionsTable.matchedCashflowId] != null) {
            return@newSuspendedTransaction AutoPayApplyResult(false, null)
        }

        val matchingTxIds = BankTransactionsTable.selectAll().where {
            (BankTransactionsTable.tenantId eq tenantUuid) and
                (BankTransactionsTable.dedupHash eq txRow[BankTransactionsTable.dedupHash])
        }.map { it[BankTransactionsTable.id].value }

        val existingPayment = PaymentsTable.selectAll().where {
            (PaymentsTable.tenantId eq tenantUuid) and
                (PaymentsTable.reversedAt.isNull()) and
                (PaymentsTable.bankTransactionId inList matchingTxIds)
        }.singleOrNull()
        if (existingPayment != null) {
            return@newSuspendedTransaction AutoPayApplyResult(
                applied = false,
                paymentId = PaymentId.parse(existingPayment[PaymentsTable.id].value.toString())
            )
        }

        val linkId = upsertMatchLink(
            tenantUuid = tenantUuid,
            invoiceUuid = invoiceUuid,
            entryUuid = entryUuid,
            txUuid = txUuid,
            confidenceScore = confidenceScore,
            scoreMargin = scoreMargin,
            reasonsJson = reasonsJson,
            rulesJson = rulesJson,
            now = now,
            status = AutoMatchStatus.AutoMatched
        )

        val bookingDateTime = LocalDateTime(transaction.transactionDate, LocalTime(0, 0))

        val invoiceStatusBefore = invoiceRow[InvoicesTable.status]
        val invoicePaidAmountBefore = Money.fromDbDecimal(invoiceRow[InvoicesTable.paidAmount])
        val invoicePaidAtBefore = invoiceRow[InvoicesTable.paidAt]

        val entryRow = CashflowEntriesTable.selectAll().where {
            (CashflowEntriesTable.id eq entryUuid) and (CashflowEntriesTable.tenantId eq tenantUuid)
        }.singleOrNull() ?: throw DokusException.NotFound("Cashflow entry not found")

        val cashflowStatusBefore = entryRow[CashflowEntriesTable.status]
        val cashflowRemainingBefore = Money.fromDbDecimal(entryRow[CashflowEntriesTable.remainingAmount])
        val cashflowPaidAtBefore = entryRow[CashflowEntriesTable.paidAt]

        CashflowEntriesTable.update({
            (CashflowEntriesTable.id eq entryUuid) and (CashflowEntriesTable.tenantId eq tenantUuid)
        }) {
            it[remainingAmount] = Money.ZERO.toDbDecimal()
            it[status] = CashflowEntryStatus.Paid
            it[paidAt] = bookingDateTime
        }

        val totalAmount = Money.fromDbDecimal(invoiceRow[InvoicesTable.totalAmount])
        InvoicesTable.update({
            (InvoicesTable.id eq invoiceUuid) and (InvoicesTable.tenantId eq tenantUuid)
        }) {
            it[paidAmount] = totalAmount.toDbDecimal()
            it[status] = InvoiceStatus.Paid
            it[paidAt] = bookingDateTime
            it[paymentMethod] = PaymentMethod.BankTransfer
        }

        val paymentId = PaymentsTable.insertAndGetId {
            it[id] = UUID.randomUUID()
            it[PaymentsTable.tenantId] = tenantUuid
            it[PaymentsTable.invoiceId] = invoiceUuid
            it[amount] = absoluteAmount.toDbDecimal()
            it[paymentDate] = transaction.transactionDate
            it[paymentMethod] = PaymentMethod.BankTransfer
            it[paymentSource] = PaymentSource.BankImport
            it[createdBy] = PaymentCreatedBy.Auto
            it[transactionId] = transaction.id.toString()
            it[bankTransactionId] = txUuid
            it[notes] = "Auto-paid from imported bank transaction"
        }

        BankTransactionsTable.update({
            (BankTransactionsTable.id eq txUuid) and
                (BankTransactionsTable.tenantId eq tenantUuid)
        }) {
            it[status] = BankTransactionStatus.Matched
            it[matchedCashflowId] = entryUuid
            it[matchScore] = confidenceScore.toBigDecimal()
            it[matchedBy] = tech.dokus.domain.enums.MatchedBy.Auto
            it[matchedAt] = now
            it[resolutionType] = tech.dokus.domain.enums.ResolutionType.Document
            it[updatedAt] = now
        }

        TransactionMatchLinksTable.update({ TransactionMatchLinksTable.id eq linkId }) {
            it[status] = AutoMatchStatus.AutoPaid
            it[autoPaidAt] = now
            it[TransactionMatchLinksTable.invoiceStatusBefore] = invoiceStatusBefore
            it[TransactionMatchLinksTable.invoicePaidAmountBefore] = invoicePaidAmountBefore.toDbDecimal()
            it[TransactionMatchLinksTable.invoicePaidAtBefore] = invoicePaidAtBefore
            it[TransactionMatchLinksTable.cashflowStatusBefore] = cashflowStatusBefore
            it[TransactionMatchLinksTable.cashflowRemainingBefore] = cashflowRemainingBefore.toDbDecimal()
            it[TransactionMatchLinksTable.cashflowPaidAtBefore] = cashflowPaidAtBefore
            it[updatedAt] = now
        }

        appendAudit(
            tenantUuid = tenantUuid,
            triggerSource = triggerSource,
            decision = AutoPaymentDecision.AutoPaid,
            invoiceId = invoiceUuid,
            entryId = entryUuid,
            txId = txUuid,
            score = confidenceScore,
            margin = scoreMargin,
            reasonsJson = reasonsJson,
            rulesJson = rulesJson,
            actorUserId = null
        )

        AutoPayApplyResult(applied = true, paymentId = PaymentId.parse(paymentId.value.toString()))
    }

    private fun upsertMatchLink(
        tenantUuid: UUID,
        invoiceUuid: UUID,
        entryUuid: UUID,
        txUuid: UUID,
        confidenceScore: Double,
        scoreMargin: Double,
        reasonsJson: String,
        rulesJson: String,
        now: LocalDateTime,
        status: AutoMatchStatus
    ): UUID {
        val existing = TransactionMatchLinksTable.selectAll().where {
            (TransactionMatchLinksTable.tenantId eq tenantUuid) and
                (TransactionMatchLinksTable.documentId eq invoiceUuid) and
                (TransactionMatchLinksTable.importedBankTransactionId eq txUuid)
        }.singleOrNull()

        return if (existing == null) {
            val id = UUID.randomUUID()
            TransactionMatchLinksTable.insertAndGetId {
                it[TransactionMatchLinksTable.id] = id
                it[tenantId] = tenantUuid
                it[documentId] = invoiceUuid
                it[cashflowEntryId] = entryUuid
                it[importedBankTransactionId] = txUuid
                it[TransactionMatchLinksTable.status] = status
                it[createdBy] = PaymentCreatedBy.Auto
                it[TransactionMatchLinksTable.confidenceScore] = confidenceScore.toBigDecimal()
                it[TransactionMatchLinksTable.scoreMargin] = scoreMargin.toBigDecimal()
                it[TransactionMatchLinksTable.reasonsJson] = reasonsJson
                it[TransactionMatchLinksTable.rulesJson] = rulesJson
                it[matchedAt] = now
                it[createdAt] = now
                it[updatedAt] = now
            }.value
        } else {
            val existingId = existing[TransactionMatchLinksTable.id].value
            TransactionMatchLinksTable.update({ TransactionMatchLinksTable.id eq existingId }) {
                it[cashflowEntryId] = entryUuid
                it[TransactionMatchLinksTable.status] = status
                it[TransactionMatchLinksTable.confidenceScore] = confidenceScore.toBigDecimal()
                it[TransactionMatchLinksTable.scoreMargin] = scoreMargin.toBigDecimal()
                it[TransactionMatchLinksTable.reasonsJson] = reasonsJson
                it[TransactionMatchLinksTable.rulesJson] = rulesJson
                it[matchedAt] = now
                it[reversedAt] = null
                it[reversedByUserId] = null
                it[reversalReason] = null
                it[updatedAt] = now
            }
            existingId
        }
    }

    private fun appendAudit(
        tenantUuid: UUID,
        triggerSource: AutoPaymentTriggerSource,
        decision: AutoPaymentDecision,
        invoiceId: UUID?,
        entryId: UUID?,
        txId: UUID?,
        score: Double?,
        margin: Double?,
        reasonsJson: String?,
        rulesJson: String?,
        actorUserId: UserId?
    ) {
        AutoPaymentAuditEventsTable.insertAndGetId {
            it[id] = UUID.randomUUID()
            it[tenantId] = tenantUuid
            it[AutoPaymentAuditEventsTable.triggerSource] = triggerSource
            it[AutoPaymentAuditEventsTable.decision] = decision
            it[AutoPaymentAuditEventsTable.invoiceId] = invoiceId
            it[cashflowEntryId] = entryId
            it[importedBankTransactionId] = txId
            it[AutoPaymentAuditEventsTable.score] = score?.toBigDecimal()
            it[AutoPaymentAuditEventsTable.margin] = margin?.toBigDecimal()
            it[AutoPaymentAuditEventsTable.reasonsJson] = reasonsJson
            it[AutoPaymentAuditEventsTable.rulesJson] = rulesJson
            it[AutoPaymentAuditEventsTable.actorUserId] = actorUserId?.value?.toJavaUuid()
        }
    }

    private fun parseJsonArray(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            Json.decodeFromString<List<String>>(raw)
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun Money.absolute(): Money = if (isNegative) -this else this
}
