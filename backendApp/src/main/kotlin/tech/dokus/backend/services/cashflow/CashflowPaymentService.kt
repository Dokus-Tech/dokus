package tech.dokus.backend.services.cashflow

import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.v1.jdbc.update
import tech.dokus.database.repository.cashflow.CashflowEntriesRepository
import tech.dokus.database.tables.cashflow.CashflowEntriesTable
import tech.dokus.database.tables.cashflow.InvoicesTable
import tech.dokus.database.tables.documents.AutoPaymentAuditEventsTable
import tech.dokus.database.tables.documents.CashflowPaymentCandidatesTable
import tech.dokus.database.tables.documents.ImportedBankTransactionsTable
import tech.dokus.database.tables.payment.PaymentsTable
import tech.dokus.domain.Money
import tech.dokus.domain.enums.AutoPaymentDecision
import tech.dokus.domain.enums.AutoPaymentTriggerSource
import tech.dokus.domain.enums.CashflowEntryStatus
import tech.dokus.domain.enums.CashflowSourceType
import tech.dokus.domain.enums.ImportedBankTransactionStatus
import tech.dokus.domain.enums.InvoiceStatus
import tech.dokus.domain.enums.PaymentCreatedBy
import tech.dokus.domain.enums.PaymentMethod
import tech.dokus.domain.enums.PaymentSource
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.CashflowEntryId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.CashflowEntry
import tech.dokus.domain.model.CashflowPaymentRequest
import tech.dokus.domain.fromDbDecimal
import tech.dokus.domain.toDbDecimal
import tech.dokus.foundation.backend.utils.runSuspendCatching
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toJavaUuid

@OptIn(ExperimentalUuidApi::class)
class CashflowPaymentService(
    private val cashflowEntriesRepository: CashflowEntriesRepository,
) {
    suspend fun recordPayment(
        tenantId: TenantId,
        entryId: CashflowEntryId,
        request: CashflowPaymentRequest
    ): Result<CashflowEntry> = runSuspendCatching {
        if (request.amount.minor <= 0) {
            throw DokusException.BadRequest("Payment amount must be positive")
        }

        newSuspendedTransaction {
            val tenantUuid = tenantId.value.toJavaUuid()
            val entryUuid = entryId.value.toJavaUuid()

            val entry = CashflowEntriesTable.selectAll().where {
                (CashflowEntriesTable.id eq entryUuid) and
                    (CashflowEntriesTable.tenantId eq tenantUuid)
            }.singleOrNull() ?: throw DokusException.NotFound("Cashflow entry not found")

            val remaining = Money.fromDbDecimal(entry[CashflowEntriesTable.remainingAmount])
            if (request.amount > remaining) {
                throw DokusException.BadRequest("Payment amount exceeds remaining amount")
            }

            val newRemainingRaw = remaining - request.amount
            val newRemaining = if (newRemainingRaw.isNegative) Money.ZERO else newRemainingRaw
            val fullyPaid = newRemaining.isZero
            val newStatus = if (fullyPaid) CashflowEntryStatus.Paid else entry[CashflowEntriesTable.status]
            val newPaidAt = if (fullyPaid) request.paidAt else entry[CashflowEntriesTable.paidAt]

            CashflowEntriesTable.update({
                (CashflowEntriesTable.id eq entryUuid) and
                    (CashflowEntriesTable.tenantId eq tenantUuid)
            }) {
                it[remainingAmount] = newRemaining.toDbDecimal()
                it[status] = newStatus
                it[paidAt] = newPaidAt
            }

            if (entry[CashflowEntriesTable.sourceType] == CashflowSourceType.Invoice) {
                val invoiceId = entry[CashflowEntriesTable.sourceId]
                val invoice = InvoicesTable.selectAll().where {
                    (InvoicesTable.id eq invoiceId) and
                        (InvoicesTable.tenantId eq tenantUuid)
                }.singleOrNull() ?: throw DokusException.NotFound("Invoice not found for cashflow entry")

                val invoiceTotal = Money.fromDbDecimal(invoice[InvoicesTable.totalAmount])
                val currentPaid = Money.fromDbDecimal(invoice[InvoicesTable.paidAmount])
                val updatedPaidRaw = currentPaid + request.amount
                if (updatedPaidRaw > invoiceTotal) {
                    throw DokusException.BadRequest("Accumulated paid amount would exceed invoice total")
                }
                val updatedPaid = updatedPaidRaw
                val invoicePaid = updatedPaid.minor >= invoiceTotal.minor
                val invoiceStatus = if (invoicePaid) InvoiceStatus.Paid else InvoiceStatus.PartiallyPaid
                val invoicePaidAt = if (invoicePaid) request.paidAt else invoice[InvoicesTable.paidAt]

                InvoicesTable.update({
                    (InvoicesTable.id eq invoiceId) and
                        (InvoicesTable.tenantId eq tenantUuid)
                }) {
                    it[paidAmount] = updatedPaid.toDbDecimal()
                    it[status] = invoiceStatus
                    it[paidAt] = invoicePaidAt
                    it[paymentMethod] = request.paymentMethod
                }

                PaymentsTable.insertAndGetId {
                    it[PaymentsTable.tenantId] = tenantUuid
                    it[PaymentsTable.invoiceId] = invoiceId
                    it[amount] = request.amount.toDbDecimal()
                    it[paymentDate] = request.paidAt.date
                    it[paymentMethod] = request.paymentMethod
                    it[transactionId] = request.bankTransactionId?.toString()
                    it[bankTransactionId] = request.bankTransactionId?.value?.toJavaUuid()
                    it[paymentSource] = PaymentSource.Manual
                    it[createdBy] = PaymentCreatedBy.User
                    it[notes] = request.note
                }

                AutoPaymentAuditEventsTable.insertAndGetId {
                    it[id] = UUID.randomUUID()
                    it[AutoPaymentAuditEventsTable.tenantId] = tenantUuid
                    it[triggerSource] = AutoPaymentTriggerSource.ManualPayment
                    it[decision] = AutoPaymentDecision.ManualPaid
                    it[AutoPaymentAuditEventsTable.invoiceId] = invoiceId
                    it[cashflowEntryId] = entryUuid
                    it[importedBankTransactionId] = request.bankTransactionId?.value?.toJavaUuid()
                }
            }

            val bankTxId = request.bankTransactionId
            if (bankTxId != null) {
                val txUuid = bankTxId.value.toJavaUuid()
                val tx = ImportedBankTransactionsTable.selectAll().where {
                    (ImportedBankTransactionsTable.id eq txUuid) and
                        (ImportedBankTransactionsTable.tenantId eq tenantUuid)
                }.singleOrNull() ?: throw DokusException.NotFound("Imported bank transaction not found")

                if (tx[ImportedBankTransactionsTable.linkedCashflowEntryId] != null &&
                    tx[ImportedBankTransactionsTable.linkedCashflowEntryId] != entryUuid
                ) {
                    throw DokusException.BadRequest("Imported transaction is already linked")
                }

                ImportedBankTransactionsTable.update({
                    (ImportedBankTransactionsTable.id eq txUuid) and
                        (ImportedBankTransactionsTable.tenantId eq tenantUuid)
                }) {
                    it[status] = ImportedBankTransactionStatus.Linked
                    it[linkedCashflowEntryId] = entryUuid
                    it[suggestedCashflowEntryId] = entryUuid
                }
            }

            CashflowPaymentCandidatesTable.deleteWhere {
                (CashflowPaymentCandidatesTable.tenantId eq tenantUuid) and
                    (CashflowPaymentCandidatesTable.cashflowEntryId eq entryUuid)
            }

            if (request.dismissSuggestedMatch) {
                ImportedBankTransactionsTable.update({
                    (ImportedBankTransactionsTable.tenantId eq tenantUuid) and
                        (ImportedBankTransactionsTable.suggestedCashflowEntryId eq entryUuid) and
                        (ImportedBankTransactionsTable.status eq ImportedBankTransactionStatus.Suggested)
                }) {
                    it[suggestedCashflowEntryId] = null
                    it[suggestedScore] = null
                    it[suggestedTier] = null
                    it[status] = ImportedBankTransactionStatus.Ignored
                }
            }

            ImportedBankTransactionsTable.update({
                (ImportedBankTransactionsTable.tenantId eq tenantUuid) and
                    (ImportedBankTransactionsTable.suggestedCashflowEntryId eq entryUuid) and
                    (ImportedBankTransactionsTable.status neq ImportedBankTransactionStatus.Linked) and
                    (ImportedBankTransactionsTable.status neq ImportedBankTransactionStatus.Ignored)
            }) {
                it[suggestedCashflowEntryId] = null
                it[suggestedScore] = null
                it[suggestedTier] = null
                it[status] = ImportedBankTransactionStatus.Unmatched
            }
        }

        cashflowEntriesRepository.getEntry(entryId, tenantId).getOrNull()
            ?: throw DokusException.NotFound("Cashflow entry not found after payment")
    }
}
