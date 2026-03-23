package tech.dokus.backend.services.cashflow

import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import tech.dokus.database.mapper.from
import tech.dokus.database.repository.cashflow.CashflowEntriesRepository
import tech.dokus.database.repository.cashflow.CashflowPaymentRepository
import tech.dokus.domain.Money
import tech.dokus.domain.enums.CashflowEntryStatus
import tech.dokus.domain.enums.CashflowSourceType
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.CashflowEntryId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.CashflowEntryDto
import tech.dokus.domain.model.CashflowPaymentRequest
import tech.dokus.foundation.backend.utils.runSuspendCatching
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toJavaUuid

@OptIn(ExperimentalUuidApi::class)
class CashflowPaymentService(
    private val cashflowEntriesRepository: CashflowEntriesRepository,
    private val cashflowPaymentRepository: CashflowPaymentRepository,
) {
    suspend fun recordPayment(
        tenantId: TenantId,
        entryId: CashflowEntryId,
        request: CashflowPaymentRequest
    ): Result<CashflowEntryDto> = runSuspendCatching {
        if (request.amount.minor <= 0) {
            throw DokusException.BadRequest("Payment amount must be positive")
        }

        newSuspendedTransaction {
            val tenantUuid = tenantId.value.toJavaUuid()
            val entryUuid = entryId.value.toJavaUuid()

            val snapshot = cashflowPaymentRepository.findEntryForPayment(tenantUuid, entryUuid)

            if (request.amount > snapshot.remaining) {
                throw DokusException.BadRequest("Payment amount exceeds remaining amount")
            }

            val newRemainingRaw = snapshot.remaining - request.amount
            val newRemaining = if (newRemainingRaw.isNegative) {
                Money.zero(
                    snapshot.remaining.currency
                )
            } else {
                newRemainingRaw
            }
            val fullyPaid = newRemaining.isZero
            val newStatus = if (fullyPaid) CashflowEntryStatus.Paid else snapshot.currentStatus
            val newPaidAt = if (fullyPaid) request.paidAt else snapshot.currentPaidAt

            cashflowPaymentRepository.updateEntryAfterPayment(tenantUuid, entryUuid, newRemaining, newStatus, newPaidAt)

            if (snapshot.sourceType == CashflowSourceType.Invoice) {
                cashflowPaymentRepository.applyPaymentToInvoice(
                    tenantUuid = tenantUuid,
                    invoiceId = snapshot.sourceId,
                    paymentAmount = request.amount,
                    paymentMethod = request.paymentMethod,
                    paidAt = request.paidAt,
                )

                cashflowPaymentRepository.insertPaymentRecord(
                    tenantUuid = tenantUuid,
                    invoiceId = snapshot.sourceId,
                    amount = request.amount,
                    paymentDate = request.paidAt.date,
                    paymentMethod = request.paymentMethod,
                    bankTransactionId = request.bankTransactionId?.value?.toJavaUuid(),
                    note = request.note,
                )

                cashflowPaymentRepository.insertManualPaymentAudit(
                    tenantUuid = tenantUuid,
                    invoiceId = snapshot.sourceId,
                    entryUuid = entryUuid,
                    bankTransactionId = request.bankTransactionId?.value?.toJavaUuid(),
                )
            }

            val bankTxId = request.bankTransactionId
            if (bankTxId != null) {
                cashflowPaymentRepository.matchBankTransaction(tenantUuid, bankTxId.value.toJavaUuid(), entryUuid)
            }

            if (request.dismissSuggestedMatch) {
                cashflowPaymentRepository.dismissSuggestedMatches(tenantUuid, entryUuid)
            }

            cashflowPaymentRepository.clearStaleMatches(tenantUuid, entryUuid)
        }

        val entity = cashflowEntriesRepository.getEntry(entryId, tenantId).getOrNull()
            ?: throw DokusException.NotFound("Cashflow entry not found after payment")
        CashflowEntryDto.from(entity)
    }
}
