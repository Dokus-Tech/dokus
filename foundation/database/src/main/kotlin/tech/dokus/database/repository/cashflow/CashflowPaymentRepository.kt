package tech.dokus.database.repository.cashflow

import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import tech.dokus.database.tables.banking.BankTransactionsTable
import tech.dokus.database.tables.cashflow.CashflowEntriesTable
import tech.dokus.database.tables.cashflow.InvoicesTable
import tech.dokus.database.tables.documents.AutoPaymentAuditEventsTable
import tech.dokus.database.tables.payment.PaymentsTable
import tech.dokus.domain.Money
import tech.dokus.domain.enums.AutoPaymentDecision
import tech.dokus.domain.enums.AutoPaymentTriggerSource
import tech.dokus.domain.enums.BankTransactionStatus
import tech.dokus.domain.enums.CashflowEntryStatus
import tech.dokus.domain.enums.CashflowSourceType
import tech.dokus.domain.enums.InvoiceStatus
import tech.dokus.domain.enums.MatchedBy
import tech.dokus.domain.enums.PaymentCreatedBy
import tech.dokus.domain.enums.PaymentSource
import tech.dokus.domain.enums.ResolutionType
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.fromDbDecimal
import tech.dokus.domain.toDbDecimal
import java.util.UUID

/**
 * Holds the result of reading a cashflow entry for payment processing.
 * All fields needed by the service to calculate new state.
 */
data class EntryPaymentSnapshot(
    val remaining: Money,
    val sourceType: CashflowSourceType,
    val sourceId: UUID,
    val currentStatus: CashflowEntryStatus,
    val currentPaidAt: kotlinx.datetime.LocalDateTime?,
)

/**
 * Repository for payment-related DB operations.
 * Methods run within an existing transaction — they do NOT open their own.
 */
class CashflowPaymentRepository {

    /**
     * Read cashflow entry for payment. Must be called inside a transaction.
     */
    fun findEntryForPayment(tenantUuid: UUID, entryUuid: UUID): EntryPaymentSnapshot {
        val entry = CashflowEntriesTable.selectAll().where {
            (CashflowEntriesTable.id eq entryUuid) and
                (CashflowEntriesTable.tenantId eq tenantUuid)
        }.singleOrNull() ?: throw DokusException.NotFound("Cashflow entry not found")

        return EntryPaymentSnapshot(
            remaining = Money.fromDbDecimal(entry[CashflowEntriesTable.remainingAmount]),
            sourceType = entry[CashflowEntriesTable.sourceType],
            sourceId = entry[CashflowEntriesTable.sourceId],
            currentStatus = entry[CashflowEntriesTable.status],
            currentPaidAt = entry[CashflowEntriesTable.paidAt],
        )
    }

    /**
     * Update cashflow entry after payment.
     */
    fun updateEntryAfterPayment(
        tenantUuid: UUID,
        entryUuid: UUID,
        newRemaining: Money,
        newStatus: CashflowEntryStatus,
        newPaidAt: kotlinx.datetime.LocalDateTime?,
    ) {
        CashflowEntriesTable.update({
            (CashflowEntriesTable.id eq entryUuid) and
                (CashflowEntriesTable.tenantId eq tenantUuid)
        }) {
            it[remainingAmount] = newRemaining.toDbDecimal()
            it[status] = newStatus
            it[paidAt] = newPaidAt
        }
    }

    /**
     * Apply payment to invoice: update paid amount, status, and payment method.
     * Returns the updated paid amount for downstream checks.
     */
    fun applyPaymentToInvoice(
        tenantUuid: UUID,
        invoiceId: UUID,
        paymentAmount: Money,
        paymentMethod: tech.dokus.domain.enums.PaymentMethod,
        paidAt: kotlinx.datetime.LocalDateTime,
    ): Money {
        val invoice = InvoicesTable.selectAll().where {
            (InvoicesTable.id eq invoiceId) and
                (InvoicesTable.tenantId eq tenantUuid)
        }.singleOrNull() ?: throw DokusException.NotFound("Invoice not found for cashflow entry")

        val invoiceTotal = Money.fromDbDecimal(invoice[InvoicesTable.totalAmount])
        val currentPaid = Money.fromDbDecimal(invoice[InvoicesTable.paidAmount])
        val updatedPaid = currentPaid + paymentAmount
        if (updatedPaid > invoiceTotal) {
            throw DokusException.BadRequest("Accumulated paid amount would exceed invoice total")
        }

        val invoicePaid = updatedPaid.minor >= invoiceTotal.minor
        val invoiceStatus = if (invoicePaid) InvoiceStatus.Paid else InvoiceStatus.PartiallyPaid
        val invoicePaidAt = if (invoicePaid) paidAt else invoice[InvoicesTable.paidAt]

        InvoicesTable.update({
            (InvoicesTable.id eq invoiceId) and
                (InvoicesTable.tenantId eq tenantUuid)
        }) {
            it[InvoicesTable.paidAmount] = updatedPaid.toDbDecimal()
            it[status] = invoiceStatus
            it[InvoicesTable.paidAt] = invoicePaidAt
            it[InvoicesTable.paymentMethod] = paymentMethod
        }

        return updatedPaid
    }

    /**
     * Insert a payment record.
     */
    fun insertPaymentRecord(
        tenantUuid: UUID,
        invoiceId: UUID,
        amount: Money,
        paymentDate: kotlinx.datetime.LocalDate,
        paymentMethod: tech.dokus.domain.enums.PaymentMethod,
        bankTransactionId: UUID?,
        note: String?,
    ) {
        PaymentsTable.insertAndGetId {
            it[PaymentsTable.tenantId] = tenantUuid
            it[PaymentsTable.invoiceId] = invoiceId
            it[PaymentsTable.amount] = amount.toDbDecimal()
            it[PaymentsTable.paymentDate] = paymentDate
            it[PaymentsTable.paymentMethod] = paymentMethod
            it[transactionId] = bankTransactionId?.toString()
            it[PaymentsTable.bankTransactionId] = bankTransactionId
            it[paymentSource] = PaymentSource.Manual
            it[createdBy] = PaymentCreatedBy.User
            it[notes] = note
        }
    }

    /**
     * Insert a manual payment audit event.
     */
    fun insertManualPaymentAudit(
        tenantUuid: UUID,
        invoiceId: UUID,
        entryUuid: UUID,
        bankTransactionId: UUID?,
    ) {
        AutoPaymentAuditEventsTable.insertAndGetId {
            it[id] = UUID.randomUUID()
            it[AutoPaymentAuditEventsTable.tenantId] = tenantUuid
            it[triggerSource] = AutoPaymentTriggerSource.ManualPayment
            it[decision] = AutoPaymentDecision.ManualPaid
            it[AutoPaymentAuditEventsTable.invoiceId] = invoiceId
            it[cashflowEntryId] = entryUuid
            it[importedBankTransactionId] = bankTransactionId
        }
    }

    /**
     * Match a bank transaction to a cashflow entry.
     */
    fun matchBankTransaction(tenantUuid: UUID, txUuid: UUID, entryUuid: UUID) {
        val tx = BankTransactionsTable.selectAll().where {
            (BankTransactionsTable.id eq txUuid) and
                (BankTransactionsTable.tenantId eq tenantUuid)
        }.singleOrNull() ?: throw DokusException.NotFound("Imported bank transaction not found")

        if (tx[BankTransactionsTable.matchedCashflowId] != null &&
            tx[BankTransactionsTable.matchedCashflowId] != entryUuid
        ) {
            throw DokusException.BadRequest("Imported transaction is already matched")
        }

        BankTransactionsTable.update({
            (BankTransactionsTable.id eq txUuid) and
                (BankTransactionsTable.tenantId eq tenantUuid)
        }) {
            it[status] = BankTransactionStatus.Matched
            it[matchedCashflowId] = entryUuid
            it[matchedBy] = MatchedBy.Manual
            it[resolutionType] = ResolutionType.Document
        }
    }

    /**
     * Dismiss suggested (NeedsReview) matches for a cashflow entry.
     */
    fun dismissSuggestedMatches(tenantUuid: UUID, entryUuid: UUID) {
        BankTransactionsTable.update({
            (BankTransactionsTable.tenantId eq tenantUuid) and
                (BankTransactionsTable.matchedCashflowId eq entryUuid) and
                (BankTransactionsTable.status eq BankTransactionStatus.NeedsReview)
        }) {
            it[matchedCashflowId] = null
            it[matchScore] = null
            it[status] = BankTransactionStatus.Ignored
        }
    }

    /**
     * Clear stale (non-matched, non-ignored) candidate links for a cashflow entry.
     */
    fun clearStaleMatches(tenantUuid: UUID, entryUuid: UUID) {
        BankTransactionsTable.update({
            (BankTransactionsTable.tenantId eq tenantUuid) and
                (BankTransactionsTable.matchedCashflowId eq entryUuid) and
                (BankTransactionsTable.status neq BankTransactionStatus.Matched) and
                (BankTransactionsTable.status neq BankTransactionStatus.Ignored)
        }) {
            it[matchedCashflowId] = null
            it[matchScore] = null
            it[status] = BankTransactionStatus.Unmatched
        }
    }
}
