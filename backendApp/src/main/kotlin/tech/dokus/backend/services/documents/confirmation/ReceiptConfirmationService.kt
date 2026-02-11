package tech.dokus.backend.services.documents.confirmation

import tech.dokus.backend.util.isUniqueViolation
import tech.dokus.backend.services.cashflow.CashflowEntriesService
import tech.dokus.backend.util.runSuspendCatching
import tech.dokus.database.repository.cashflow.DocumentDraftRepository
import tech.dokus.database.repository.cashflow.ExpenseRepository
import tech.dokus.domain.Money
import tech.dokus.domain.Percentage
import tech.dokus.domain.VatRate
import tech.dokus.domain.enums.DocumentStatus
import tech.dokus.domain.enums.ExpenseCategory
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.CreateExpenseRequest
import tech.dokus.domain.model.ReceiptDraftData
import tech.dokus.foundation.backend.utils.loggerFor
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toJavaUuid

/**
 * Confirms Receipt documents: creates Expense entity + CashflowEntry (Direction.Out).
 *
 * Uses repositories for all persistence — no direct table access.
 */
class ReceiptConfirmationService(
    private val expenseRepository: ExpenseRepository,
    private val cashflowEntriesService: CashflowEntriesService,
    private val draftRepository: DocumentDraftRepository,
) {
    private val logger = loggerFor()

    @OptIn(ExperimentalUuidApi::class)
    @Suppress("ThrowsCount")
    suspend fun confirm(
        tenantId: TenantId,
        documentId: DocumentId,
        draftData: ReceiptDraftData,
        linkedContactId: ContactId?
    ): Result<ConfirmationResult> = runSuspendCatching {
        logger.info("Confirming receipt document: $documentId for tenant: $tenantId")

        val draft = requireConfirmableDraft(draftRepository, tenantId, documentId)
        val isReconfirm = draft.documentStatus == DocumentStatus.NeedsReview

        val date = draftData.date ?: throw DokusException.BadRequest("Date is required")
        val merchant = draftData.merchantName ?: throw DokusException.BadRequest("Merchant is required")
        val totalAmount = draftData.totalAmount ?: throw DokusException.BadRequest("Amount is required")
        val vatAmount = draftData.vatAmount
        val vatRate = if (vatAmount != null) {
            val baseMinor = (totalAmount - vatAmount).minor
            if (baseMinor > 0L) {
                VatRate(((vatAmount.minor * 10000L) / baseMinor).toInt())
            } else {
                null
            }
        } else {
            null
        }

        val request = CreateExpenseRequest(
            date = date,
            merchant = merchant,
            amount = totalAmount,
            vatAmount = vatAmount,
            vatRate = vatRate,
            category = ExpenseCategory.Other,
            documentId = documentId,
            contactId = linkedContactId,
            isDeductible = true,
            deductiblePercentage = Percentage.FULL,
            paymentMethod = draftData.paymentMethod,
            isRecurring = false,
            notes = draftData.notes
        )

        val existingExpense = expenseRepository.findByDocumentId(tenantId, documentId)
        val expense = when {
            existingExpense == null -> {
                expenseRepository.createExpense(tenantId, request).getOrElse { t ->
                    if (!t.isUniqueViolation()) throw t
                    expenseRepository.findByDocumentId(tenantId, documentId) ?: throw t
                }
            }

            isReconfirm -> {
                expenseRepository.updateExpense(existingExpense.id, tenantId, request).getOrThrow()
            }

            else -> existingExpense
        }

        val cashflowEntry = if (existingExpense != null && isReconfirm) {
            cashflowEntriesService.updateFromExpense(
                tenantId = tenantId,
                expenseId = expense.id.value.toJavaUuid(),
                documentId = documentId,
                expenseDate = date,
                amountGross = expense.amount,
                amountVat = expense.vatAmount ?: Money.ZERO,
                contactId = linkedContactId
            ).getOrThrow()
        } else {
            cashflowEntriesService.createFromExpense(
                tenantId = tenantId,
                expenseId = expense.id.value.toJavaUuid(),
                documentId = documentId,
                expenseDate = date,
                amountGross = expense.amount,
                amountVat = expense.vatAmount ?: Money.ZERO,
                contactId = linkedContactId
            ).getOrThrow()
        }

        draftRepository.updateDocumentStatus(documentId, tenantId, DocumentStatus.Confirmed)

        logger.info("Receipt confirmed: $documentId -> expenseId=${expense.id}, entryId=${cashflowEntry.id}")
        ConfirmationResult(entity = expense, cashflowEntryId = cashflowEntry.id, documentId = documentId)
    }
}
