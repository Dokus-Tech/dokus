package tech.dokus.backend.services.documents.confirmation

import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import tech.dokus.database.repository.cashflow.ExpenseRepository
import tech.dokus.database.tables.cashflow.CashflowEntriesTable
import tech.dokus.database.tables.cashflow.ExpensesTable
import tech.dokus.domain.Money
import tech.dokus.domain.Percentage
import tech.dokus.domain.VatRate
import tech.dokus.domain.enums.CashflowDirection
import tech.dokus.domain.enums.CashflowEntryStatus
import tech.dokus.domain.enums.CashflowSourceType
import tech.dokus.domain.enums.ExpenseCategory
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.CashflowEntryId
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.ExpenseId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.ReceiptDraftData
import tech.dokus.domain.toDbDecimal
import tech.dokus.foundation.backend.database.dbQuery
import tech.dokus.foundation.backend.utils.loggerFor
import java.util.*

/**
 * Confirms Receipt documents: creates Expense entity + CashflowEntry (Direction.Out).
 */
class ReceiptConfirmationService(
    private val expenseRepository: ExpenseRepository,
) {
    private val logger = loggerFor()

    @Suppress("ThrowsCount")
    suspend fun confirm(
        tenantId: TenantId,
        documentId: DocumentId,
        draftData: ReceiptDraftData,
        linkedContactId: ContactId?
    ): Result<ConfirmationResult> = runCatching {
        logger.info("Confirming receipt document: $documentId for tenant: $tenantId")

        val (expenseId, cashflowEntryId) = dbQuery {
            ensureDraftConfirmable(tenantId, documentId)

            val date = draftData.date ?: throw DokusException.BadRequest("Date is required")
            val merchant = draftData.merchantName ?: throw DokusException.BadRequest("Merchant is required")
            val totalAmount = draftData.totalAmount ?: throw DokusException.BadRequest("Amount is required")
            val vatAmount = draftData.vatAmount
            val category = ExpenseCategory.Other
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

            val expenseId = ExpensesTable.insertAndGetId {
                it[ExpensesTable.tenantId] = UUID.fromString(tenantId.toString())
                it[ExpensesTable.date] = date
                it[ExpensesTable.merchant] = merchant
                it[ExpensesTable.amount] = totalAmount.toDbDecimal()
                it[ExpensesTable.vatAmount] = vatAmount?.toDbDecimal()
                it[ExpensesTable.vatRate] = vatRate?.toDbDecimal()
                it[ExpensesTable.category] = category
                it[ExpensesTable.description] = null
                it[ExpensesTable.documentId] = UUID.fromString(documentId.toString())
                it[ExpensesTable.contactId] = linkedContactId?.let { id -> UUID.fromString(id.toString()) }
                it[ExpensesTable.isDeductible] = true
                it[ExpensesTable.deductiblePercentage] = Percentage.FULL.toDbDecimal()
                it[ExpensesTable.paymentMethod] = draftData.paymentMethod
                it[ExpensesTable.isRecurring] = false
                it[ExpensesTable.notes] = draftData.notes
            }.value

            val entryId = CashflowEntriesTable.insertAndGetId {
                it[CashflowEntriesTable.tenantId] = UUID.fromString(tenantId.toString())
                it[CashflowEntriesTable.sourceType] = CashflowSourceType.Expense
                it[CashflowEntriesTable.sourceId] = expenseId
                it[CashflowEntriesTable.documentId] = UUID.fromString(documentId.toString())
                it[CashflowEntriesTable.direction] = CashflowDirection.Out
                it[CashflowEntriesTable.eventDate] = date
                it[CashflowEntriesTable.amountGross] = totalAmount.toDbDecimal()
                it[CashflowEntriesTable.amountVat] = (vatAmount ?: Money.ZERO).toDbDecimal()
                it[CashflowEntriesTable.remainingAmount] = totalAmount.toDbDecimal()
                it[CashflowEntriesTable.status] = CashflowEntryStatus.Open
                it[CashflowEntriesTable.counterpartyId] = linkedContactId?.let { id -> UUID.fromString(id.toString()) }
            }.value

            markDraftConfirmed(tenantId, documentId)

            expenseId to CashflowEntryId.parse(entryId.toString())
        }

        // Fetch full entity outside transaction
        val expense = expenseRepository.getExpense(
            expenseId = ExpenseId.parse(expenseId.toString()),
            tenantId = tenantId
        ).getOrThrow() ?: throw DokusException.InternalError("Expense not found after receipt confirmation")

        logger.info("Receipt confirmed: $documentId -> expenseId=$expenseId, entryId=$cashflowEntryId")
        ConfirmationResult(entity = expense, cashflowEntryId = cashflowEntryId, documentId = documentId)
    }
}
