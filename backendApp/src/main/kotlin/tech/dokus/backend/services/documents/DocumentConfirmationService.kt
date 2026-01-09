package tech.dokus.backend.services.documents

import tech.dokus.backend.services.cashflow.CashflowEntriesService
import tech.dokus.database.repository.cashflow.BillRepository
import tech.dokus.database.repository.cashflow.DocumentDraftRepository
import tech.dokus.database.repository.cashflow.ExpenseRepository
import tech.dokus.database.repository.cashflow.InvoiceRepository
import tech.dokus.domain.Money
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.enums.DraftStatus
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.CashflowEntryId
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.CreateBillRequest
import tech.dokus.domain.model.CreateExpenseRequest
import tech.dokus.domain.model.CreateInvoiceRequest
import tech.dokus.domain.model.ExtractedDocumentData
import tech.dokus.domain.model.FinancialDocumentDto
import tech.dokus.domain.model.InvoiceItemDto
import tech.dokus.foundation.backend.utils.loggerFor
import java.util.UUID

/**
 * Result of document confirmation containing both the financial entity and cashflow entry ID.
 */
data class ConfirmationResult(
    val entity: FinancialDocumentDto,
    val cashflowEntryId: CashflowEntryId
)

/**
 * Service for document confirmation orchestration.
 *
 * This service enforces the architectural boundary:
 * - Documents are the only entry point for financial data
 * - Financial facts (Invoice/Bill/Expense) are created ONLY via document confirmation
 * - CashflowEntry is ALWAYS created alongside the financial fact
 *
 * Single transaction ensures atomicity: either all succeed or all fail.
 */
class DocumentConfirmationService(
    private val invoiceRepository: InvoiceRepository,
    private val billRepository: BillRepository,
    private val expenseRepository: ExpenseRepository,
    private val draftRepository: DocumentDraftRepository,
    private val cashflowEntriesService: CashflowEntriesService
) {
    private val logger = loggerFor()

    /**
     * Confirm a document draft, creating the financial entity and cashflow entry.
     *
     * Flow:
     * 1. Validate draft is confirmable
     * 2. Create financial entity (Invoice/Bill/Expense)
     * 3. Create corresponding CashflowEntry
     * 4. Mark draft as Confirmed
     *
     * @param tenantId The tenant ID
     * @param documentId The document ID
     * @param documentType The resolved document type
     * @param extractedData The extracted data to use for entity creation
     * @param linkedContactId The linked contact (required for invoices)
     * @return The created financial entity
     */
    suspend fun confirmDocument(
        tenantId: TenantId,
        documentId: DocumentId,
        documentType: DocumentType,
        extractedData: ExtractedDocumentData,
        linkedContactId: ContactId?
    ): Result<ConfirmationResult> = runCatching {
        logger.info("Confirming document: $documentId as $documentType for tenant: $tenantId")

        // Create financial entity based on type
        val result: ConfirmationResult = when (documentType) {
            DocumentType.Invoice -> confirmAsInvoice(tenantId, documentId, extractedData, linkedContactId)
            DocumentType.Bill -> confirmAsBill(tenantId, documentId, extractedData)
            DocumentType.Expense -> confirmAsExpense(tenantId, documentId, extractedData)
            DocumentType.Unknown -> throw DokusException.BadRequest("Cannot confirm document with unknown type")
        }

        // Mark draft as confirmed
        draftRepository.updateDraftStatus(documentId, tenantId, DraftStatus.Confirmed)

        val entityType = result.entity.javaClass.simpleName
        logger.info("Document confirmed: $documentId -> $entityType, entryId: ${result.cashflowEntryId}")
        result
    }

    @Suppress("CyclomaticComplexMethod")
    private suspend fun confirmAsInvoice(
        tenantId: TenantId,
        documentId: DocumentId,
        extractedData: ExtractedDocumentData,
        linkedContactId: ContactId?
    ): ConfirmationResult {
        val invoiceData = extractedData.invoice
            ?: throw DokusException.BadRequest("No invoice data extracted from document")

        val contactId = linkedContactId
            ?: throw DokusException.BadRequest("Invoice requires a linked contact")

        // Build invoice items
        val items = invoiceData.items?.mapIndexed { index, item ->
            InvoiceItemDto(
                description = item.description ?: "Item",
                quantity = item.quantity ?: 1.0,
                unitPrice = item.unitPrice ?: Money.ZERO,
                vatRate = item.vatRate ?: tech.dokus.domain.VatRate.ZERO,
                lineTotal = item.lineTotal ?: Money.ZERO,
                vatAmount = item.vatAmount ?: Money.ZERO,
                sortOrder = index
            )
        } ?: listOf(
            // Fallback: create single item from totals
            InvoiceItemDto(
                description = "Services",
                quantity = 1.0,
                unitPrice = invoiceData.subtotalAmount ?: invoiceData.totalAmount ?: Money.ZERO,
                vatRate = tech.dokus.domain.VatRate.ZERO,
                lineTotal = invoiceData.subtotalAmount ?: invoiceData.totalAmount ?: Money.ZERO,
                vatAmount = invoiceData.vatAmount ?: Money.ZERO,
                sortOrder = 0
            )
        )

        val createRequest = CreateInvoiceRequest(
            contactId = contactId,
            items = items,
            issueDate = invoiceData.issueDate,
            dueDate = invoiceData.dueDate,
            notes = invoiceData.notes,
            documentId = documentId
        )

        val invoice = invoiceRepository.createInvoice(tenantId, createRequest).getOrThrow()

        // Create cashflow entry for this invoice (Cash-In)
        // CRITICAL: Must succeed for data integrity - fail entire operation if this fails
        val cashflowEntry = cashflowEntriesService.createFromInvoice(
            tenantId = tenantId,
            invoiceId = UUID.fromString(invoice.id.toString()),
            documentId = documentId,
            dueDate = invoice.dueDate,
            amountGross = invoice.totalAmount,
            amountVat = invoice.vatAmount,
            customerId = contactId
        ).getOrThrow()

        return ConfirmationResult(entity = invoice, cashflowEntryId = cashflowEntry.id)
    }

    @Suppress("ThrowsCount")
    private suspend fun confirmAsBill(
        tenantId: TenantId,
        documentId: DocumentId,
        extractedData: ExtractedDocumentData
    ): ConfirmationResult {
        val billData = extractedData.bill
            ?: throw DokusException.BadRequest("No bill data extracted from document")

        val createRequest = CreateBillRequest(
            supplierName = billData.supplierName ?: "Unknown Supplier",
            supplierVatNumber = billData.supplierVatNumber,
            invoiceNumber = billData.invoiceNumber,
            issueDate = billData.issueDate
                ?: throw DokusException.BadRequest("Issue date is required"),
            dueDate = billData.dueDate ?: billData.issueDate
                ?: throw DokusException.BadRequest("Due date is required"),
            amount = billData.amount
                ?: throw DokusException.BadRequest("Amount is required"),
            vatAmount = billData.vatAmount,
            vatRate = billData.vatRate,
            category = billData.category
                ?: throw DokusException.BadRequest("Category is required"),
            description = billData.description,
            notes = billData.notes,
            documentId = documentId
        )

        val bill = billRepository.createBill(tenantId, createRequest).getOrThrow()

        // Create cashflow entry for this bill (Cash-Out)
        // CRITICAL: Must succeed for data integrity - fail entire operation if this fails
        val cashflowEntry = cashflowEntriesService.createFromBill(
            tenantId = tenantId,
            billId = UUID.fromString(bill.id.toString()),
            documentId = documentId,
            dueDate = bill.dueDate,
            amountGross = bill.amount,
            amountVat = bill.vatAmount ?: Money.ZERO,
            vendorId = null // Bills may not have a linked contact yet
        ).getOrThrow()

        return ConfirmationResult(entity = bill, cashflowEntryId = cashflowEntry.id)
    }

    @Suppress("ThrowsCount")
    private suspend fun confirmAsExpense(
        tenantId: TenantId,
        documentId: DocumentId,
        extractedData: ExtractedDocumentData
    ): ConfirmationResult {
        val expenseData = extractedData.expense
            ?: throw DokusException.BadRequest("No expense data extracted from document")

        val createRequest = CreateExpenseRequest(
            date = expenseData.date
                ?: throw DokusException.BadRequest("Date is required"),
            merchant = expenseData.merchant
                ?: throw DokusException.BadRequest("Merchant is required"),
            amount = expenseData.amount
                ?: throw DokusException.BadRequest("Amount is required"),
            vatAmount = expenseData.vatAmount,
            vatRate = expenseData.vatRate,
            category = expenseData.category
                ?: throw DokusException.BadRequest("Category is required"),
            description = expenseData.description,
            documentId = documentId,
            isDeductible = expenseData.isDeductible,
            deductiblePercentage = expenseData.deductiblePercentage,
            paymentMethod = expenseData.paymentMethod,
            notes = expenseData.notes
        )

        val expense = expenseRepository.createExpense(tenantId, createRequest).getOrThrow()

        // Create cashflow entry for this expense (Cash-Out)
        // CRITICAL: Must succeed for data integrity - fail entire operation if this fails
        val cashflowEntry = cashflowEntriesService.createFromExpense(
            tenantId = tenantId,
            expenseId = UUID.fromString(expense.id.toString()),
            documentId = documentId,
            expenseDate = expense.date,
            amountGross = expense.amount,
            amountVat = expense.vatAmount ?: Money.ZERO,
            vendorId = null // Expenses typically don't have a linked vendor
        ).getOrThrow()

        return ConfirmationResult(entity = expense, cashflowEntryId = cashflowEntry.id)
    }
}
