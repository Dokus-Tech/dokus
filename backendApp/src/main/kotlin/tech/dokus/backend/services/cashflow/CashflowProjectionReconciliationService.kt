package tech.dokus.backend.services.cashflow

import tech.dokus.domain.Money
import tech.dokus.domain.enums.CashflowEntryStatus
import tech.dokus.domain.ids.CashflowEntryId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.FinancialDocumentDto
import tech.dokus.database.repository.cashflow.CashflowEntriesRepository
import java.util.UUID

/**
 * Ensures a confirmed document has a cashflow projection when applicable.
 */
class CashflowProjectionReconciliationService(
    private val cashflowEntriesRepository: CashflowEntriesRepository,
    private val cashflowEntriesService: CashflowEntriesService
) {
    suspend fun ensureProjectionIfMissing(
        tenantId: TenantId,
        documentId: DocumentId,
        entity: FinancialDocumentDto
    ): Result<CashflowEntryId?> = runCatching {
        val existingEntryId = cashflowEntriesRepository
            .getByDocumentId(tenantId, documentId)
            .getOrThrow()
            ?.id
        if (existingEntryId != null) return@runCatching existingEntryId

        when (entity) {
            is FinancialDocumentDto.InvoiceDto -> {
                val ensuredEntry = cashflowEntriesService.createFromInvoice(
                    tenantId = tenantId,
                    invoiceId = UUID.fromString(entity.id.toString()),
                    documentId = documentId,
                    dueDate = entity.dueDate,
                    amountGross = entity.totalAmount,
                    amountVat = entity.vatAmount,
                    direction = entity.direction,
                    contactId = entity.contactId
                ).getOrThrow()

                val remainingAmount = entity.totalAmount.minus(entity.paidAmount).let {
                    if (it.isNegative) Money.ZERO else it
                }
                val status = if (remainingAmount.isZero) CashflowEntryStatus.Paid else CashflowEntryStatus.Open
                val paidAt = if (status == CashflowEntryStatus.Paid) entity.paidAt else null

                val needsSync = ensuredEntry.remainingAmount != remainingAmount ||
                    ensuredEntry.status != status ||
                    ensuredEntry.paidAt != paidAt

                if (needsSync) {
                    cashflowEntriesRepository.updateRemainingAmountAndStatus(
                        entryId = ensuredEntry.id,
                        tenantId = tenantId,
                        newRemainingAmount = remainingAmount,
                        newStatus = status,
                        paidAt = paidAt
                    ).getOrThrow()
                }

                ensuredEntry.id
            }

            is FinancialDocumentDto.ExpenseDto -> cashflowEntriesService.createFromExpense(
                tenantId = tenantId,
                expenseId = UUID.fromString(entity.id.toString()),
                documentId = documentId,
                expenseDate = entity.date,
                amountGross = entity.amount,
                amountVat = entity.vatAmount ?: Money.ZERO,
                contactId = entity.contactId
            ).getOrThrow().id

            else -> null
        }
    }
}
