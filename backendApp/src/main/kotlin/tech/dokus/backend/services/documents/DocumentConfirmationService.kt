package tech.dokus.backend.services.documents

import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import tech.dokus.database.repository.cashflow.BillRepository
import tech.dokus.database.repository.cashflow.ExpenseRepository
import tech.dokus.database.repository.cashflow.InvoiceRepository
import tech.dokus.database.services.InvoiceNumberGenerator
import tech.dokus.database.tables.cashflow.BillsTable
import tech.dokus.database.tables.cashflow.CashflowEntriesTable
import tech.dokus.database.tables.cashflow.ExpensesTable
import tech.dokus.database.tables.cashflow.InvoiceItemsTable
import tech.dokus.database.tables.cashflow.InvoicesTable
import tech.dokus.database.tables.documents.DocumentDraftsTable
import tech.dokus.domain.Money
import tech.dokus.domain.Percentage
import tech.dokus.domain.VatRate
import tech.dokus.domain.enums.BillStatus
import tech.dokus.domain.enums.CashflowDirection
import tech.dokus.domain.enums.CashflowEntryStatus
import tech.dokus.domain.enums.CashflowSourceType
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.enums.DraftStatus
import tech.dokus.domain.enums.InvoiceStatus
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.BillId
import tech.dokus.domain.ids.CashflowEntryId
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.ExpenseId
import tech.dokus.domain.ids.InvoiceId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.ExtractedDocumentData
import tech.dokus.domain.model.FinancialDocumentDto
import tech.dokus.domain.model.InvoiceItemDto
import tech.dokus.domain.toDbDecimal
import tech.dokus.foundation.backend.database.dbQuery
import tech.dokus.foundation.backend.utils.loggerFor
import java.math.BigDecimal
import java.util.UUID

/**
 * Result of document confirmation containing the financial entity and cashflow entry ID.
 *
 * Note: For ProForma confirmation, both entity and cashflowEntryId will be null
 * since ProForma creates no financial entity and no cashflow entry.
 */
data class ConfirmationResult(
    val entity: FinancialDocumentDto?,
    val cashflowEntryId: CashflowEntryId?,
    val documentId: DocumentId
)

/**
 * Service for document confirmation orchestration.
 *
 * This service enforces the architectural boundary:
 * - Documents are the only entry point for financial data
 * - Financial facts (Invoice/Bill/Expense) are created ONLY via document confirmation
 * - CashflowEntry is ALWAYS created alongside the financial fact
 *
 * A single DB transaction is used for:
 * - creating the financial entity (Invoice/Bill/Expense)
 * - creating the corresponding CashflowEntry
 * - marking the draft as Confirmed
 *
 * Note: invoice number generation uses a separate transaction (by design in [InvoiceNumberGenerator]),
 * so invoice numbers may be consumed even if confirmation fails.
 */
class DocumentConfirmationService(
    private val invoiceNumberGenerator: InvoiceNumberGenerator,
    private val invoiceRepository: InvoiceRepository,
    private val billRepository: BillRepository,
    private val expenseRepository: ExpenseRepository,
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

        val invoiceNumber: String? = when (documentType) {
            DocumentType.Invoice -> invoiceNumberGenerator.generateInvoiceNumber(tenantId).getOrThrow()
            DocumentType.Bill, DocumentType.Expense, DocumentType.CreditNote, DocumentType.Receipt, DocumentType.ProForma, DocumentType.Unknown -> null
        }

        val created = dbQuery {
            ensureDraftConfirmable(tenantId = tenantId, documentId = documentId)

            when (documentType) {
                DocumentType.Invoice -> confirmInvoiceTx(
                    tenantId = tenantId,
                    documentId = documentId,
                    extractedData = extractedData,
                    linkedContactId = linkedContactId,
                    invoiceNumber = requireNotNull(invoiceNumber)
                )

                DocumentType.Bill -> confirmBillTx(
                    tenantId = tenantId,
                    documentId = documentId,
                    extractedData = extractedData,
                    linkedContactId = linkedContactId
                )

                DocumentType.Expense -> confirmExpenseTx(
                    tenantId = tenantId,
                    documentId = documentId,
                    extractedData = extractedData,
                    linkedContactId = linkedContactId
                )

                DocumentType.Receipt -> confirmReceiptTx(
                    tenantId = tenantId,
                    documentId = documentId,
                    extractedData = extractedData,
                    linkedContactId = linkedContactId
                )

                DocumentType.ProForma -> confirmProFormaTx(
                    tenantId = tenantId,
                    documentId = documentId
                )

                DocumentType.CreditNote -> throw DokusException.BadRequest(
                    "CreditNote confirmation is handled via CreditNoteService"
                )

                DocumentType.Unknown -> throw DokusException.BadRequest("Cannot confirm document with type: $documentType")
            }
        }

        val (entity, cashflowEntryId) = when (created) {
            is CreatedConfirmation.Invoice -> {
                val invoice = invoiceRepository.getInvoice(
                    invoiceId = InvoiceId.parse(created.invoiceId.toString()),
                    tenantId = tenantId
                ).getOrThrow() ?: throw DokusException.InternalError("Invoice not found after confirmation")
                invoice to created.cashflowEntryId
            }

            is CreatedConfirmation.Bill -> {
                val bill = billRepository.getBill(
                    billId = BillId.parse(created.billId.toString()),
                    tenantId = tenantId
                ).getOrThrow() ?: throw DokusException.InternalError("Bill not found after confirmation")
                bill to created.cashflowEntryId
            }

            is CreatedConfirmation.Expense -> {
                val expense = expenseRepository.getExpense(
                    expenseId = ExpenseId.parse(created.expenseId.toString()),
                    tenantId = tenantId
                ).getOrThrow() ?: throw DokusException.InternalError("Expense not found after confirmation")
                expense to created.cashflowEntryId
            }

            is CreatedConfirmation.Receipt -> {
                // Receipt creates an Expense entity, so retrieve it as Expense
                val expense = expenseRepository.getExpense(
                    expenseId = ExpenseId.parse(created.expenseId.toString()),
                    tenantId = tenantId
                ).getOrThrow() ?: throw DokusException.InternalError("Expense not found after receipt confirmation")
                expense to created.cashflowEntryId
            }

            is CreatedConfirmation.ProForma -> {
                // ProForma has no financial entity and no cashflow entry
                null to null
            }
        }

        val entityName = entity?.javaClass?.simpleName ?: "ProForma (no entity)"
        logger.info("Document confirmed: $documentId -> $entityName, entryId: $cashflowEntryId")
        ConfirmationResult(entity = entity, cashflowEntryId = cashflowEntryId, documentId = documentId)
    }

    private fun ensureDraftConfirmable(tenantId: TenantId, documentId: DocumentId) {
        val draft = DocumentDraftsTable.selectAll()
            .where {
                (DocumentDraftsTable.documentId eq UUID.fromString(documentId.toString())) and
                    (DocumentDraftsTable.tenantId eq UUID.fromString(tenantId.toString()))
            }
            .singleOrNull() ?: throw DokusException.NotFound("Draft not found for document")

        val status = draft[DocumentDraftsTable.draftStatus]
        if (status != DraftStatus.NeedsReview && status != DraftStatus.Ready) {
            throw DokusException.BadRequest("Draft is not ready for confirmation: $status")
        }
    }

    @Suppress("CyclomaticComplexMethod")
    private fun confirmInvoiceTx(
        tenantId: TenantId,
        documentId: DocumentId,
        extractedData: ExtractedDocumentData,
        linkedContactId: ContactId?,
        invoiceNumber: String
    ): CreatedConfirmation.Invoice {
        val invoiceData = extractedData.invoice
            ?: throw DokusException.BadRequest("No invoice data extracted from document")

        val contactId = linkedContactId
            ?: throw DokusException.BadRequest("Invoice requires a linked contact")

        val items = invoiceData.items?.mapIndexed { index, item ->
            InvoiceItemDto(
                description = item.description ?: "Item",
                quantity = item.quantity ?: 1.0,
                unitPrice = item.unitPrice ?: Money.ZERO,
                vatRate = item.vatRate ?: VatRate.ZERO,
                lineTotal = item.lineTotal ?: Money.ZERO,
                vatAmount = item.vatAmount ?: Money.ZERO,
                sortOrder = index
            )
        } ?: listOf(
            InvoiceItemDto(
                description = "Services",
                quantity = 1.0,
                unitPrice = invoiceData.subtotalAmount ?: invoiceData.totalAmount ?: Money.ZERO,
                vatRate = VatRate.ZERO,
                lineTotal = invoiceData.subtotalAmount ?: invoiceData.totalAmount ?: Money.ZERO,
                vatAmount = invoiceData.vatAmount ?: Money.ZERO,
                sortOrder = 0
            )
        )

        val today = Clock.System.now().toLocalDateTime(TimeZone.UTC).date
        val issueDate = invoiceData.issueDate ?: today
        val dueDate = invoiceData.dueDate ?: issueDate.plus(DatePeriod(days = 30))

        val subtotalAmount = items.sumOf { it.lineTotal.toDbDecimal() }
        val vatAmount = items.sumOf { it.vatAmount.toDbDecimal() }
        val totalAmount = items.sumOf { it.lineTotal.toDbDecimal() + it.vatAmount.toDbDecimal() }

        val invoiceId = InvoicesTable.insertAndGetId {
            it[InvoicesTable.tenantId] = UUID.fromString(tenantId.toString())
            it[InvoicesTable.contactId] = UUID.fromString(contactId.toString())
            it[InvoicesTable.invoiceNumber] = invoiceNumber
            it[InvoicesTable.issueDate] = issueDate
            it[InvoicesTable.dueDate] = dueDate
            it[InvoicesTable.subtotalAmount] = subtotalAmount
            it[InvoicesTable.vatAmount] = vatAmount
            it[InvoicesTable.totalAmount] = totalAmount
            it[InvoicesTable.paidAmount] = Money.ZERO.toDbDecimal()
            it[InvoicesTable.status] = InvoiceStatus.Draft
            it[InvoicesTable.notes] = invoiceData.notes
            it[InvoicesTable.documentId] = UUID.fromString(documentId.toString())
        }.value

        items.forEachIndexed { index, item ->
            InvoiceItemsTable.insert {
                it[InvoiceItemsTable.invoiceId] = invoiceId
                it[InvoiceItemsTable.description] = item.description
                it[InvoiceItemsTable.quantity] = BigDecimal.valueOf(item.quantity)
                it[InvoiceItemsTable.unitPrice] = item.unitPrice.toDbDecimal()
                it[InvoiceItemsTable.vatRate] = item.vatRate.toDbDecimal()
                it[InvoiceItemsTable.lineTotal] = item.lineTotal.toDbDecimal()
                it[InvoiceItemsTable.vatAmount] = item.vatAmount.toDbDecimal()
                it[InvoiceItemsTable.sortOrder] = index
            }
        }

        val entryId = CashflowEntriesTable.insertAndGetId {
            it[CashflowEntriesTable.tenantId] = UUID.fromString(tenantId.toString())
            it[CashflowEntriesTable.sourceType] = CashflowSourceType.Invoice
            it[CashflowEntriesTable.sourceId] = invoiceId
            it[CashflowEntriesTable.documentId] = UUID.fromString(documentId.toString())
            it[CashflowEntriesTable.direction] = CashflowDirection.In
            it[CashflowEntriesTable.eventDate] = dueDate
            it[CashflowEntriesTable.amountGross] = totalAmount
            it[CashflowEntriesTable.amountVat] = vatAmount
            it[CashflowEntriesTable.remainingAmount] = totalAmount
            it[CashflowEntriesTable.status] = CashflowEntryStatus.Open
            it[CashflowEntriesTable.counterpartyId] = UUID.fromString(contactId.toString())
        }.value

        markDraftConfirmed(tenantId = tenantId, documentId = documentId)

        return CreatedConfirmation.Invoice(
            invoiceId = invoiceId,
            cashflowEntryId = CashflowEntryId.parse(entryId.toString())
        )
    }

    private fun confirmBillTx(
        tenantId: TenantId,
        documentId: DocumentId,
        extractedData: ExtractedDocumentData,
        linkedContactId: ContactId?
    ): CreatedConfirmation.Bill {
        val billData = extractedData.bill
            ?: throw DokusException.BadRequest("No bill data extracted from document")

        val issueDate = billData.issueDate ?: throw DokusException.BadRequest("Issue date is required")
        val dueDate = billData.dueDate ?: issueDate
        val amount = billData.amount ?: throw DokusException.BadRequest("Amount is required")
        val category = billData.category ?: throw DokusException.BadRequest("Category is required")

        val billId = BillsTable.insertAndGetId {
            it[BillsTable.tenantId] = UUID.fromString(tenantId.toString())
            it[BillsTable.supplierName] = billData.supplierName ?: "Unknown Supplier"
            it[BillsTable.supplierVatNumber] = billData.supplierVatNumber
            it[BillsTable.invoiceNumber] = billData.invoiceNumber
            it[BillsTable.issueDate] = issueDate
            it[BillsTable.dueDate] = dueDate
            it[BillsTable.amount] = amount.toDbDecimal()
            it[BillsTable.vatAmount] = billData.vatAmount?.toDbDecimal()
            it[BillsTable.vatRate] = billData.vatRate?.toDbDecimal()
            it[BillsTable.status] = BillStatus.Pending
            it[BillsTable.category] = category
            it[BillsTable.description] = billData.description
            it[BillsTable.notes] = billData.notes
            it[BillsTable.documentId] = UUID.fromString(documentId.toString())
            it[BillsTable.contactId] = linkedContactId?.let { id -> UUID.fromString(id.toString()) }
        }.value

        val entryId = CashflowEntriesTable.insertAndGetId {
            it[CashflowEntriesTable.tenantId] = UUID.fromString(tenantId.toString())
            it[CashflowEntriesTable.sourceType] = CashflowSourceType.Bill
            it[CashflowEntriesTable.sourceId] = billId
            it[CashflowEntriesTable.documentId] = UUID.fromString(documentId.toString())
            it[CashflowEntriesTable.direction] = CashflowDirection.Out
            it[CashflowEntriesTable.eventDate] = dueDate
            it[CashflowEntriesTable.amountGross] = amount.toDbDecimal()
            it[CashflowEntriesTable.amountVat] = (billData.vatAmount ?: Money.ZERO).toDbDecimal()
            it[CashflowEntriesTable.remainingAmount] = amount.toDbDecimal()
            it[CashflowEntriesTable.status] = CashflowEntryStatus.Open
            it[CashflowEntriesTable.counterpartyId] = linkedContactId?.let { id -> UUID.fromString(id.toString()) }
        }.value

        markDraftConfirmed(tenantId = tenantId, documentId = documentId)

        return CreatedConfirmation.Bill(
            billId = billId,
            cashflowEntryId = CashflowEntryId.parse(entryId.toString())
        )
    }

    private fun confirmExpenseTx(
        tenantId: TenantId,
        documentId: DocumentId,
        extractedData: ExtractedDocumentData,
        linkedContactId: ContactId?
    ): CreatedConfirmation.Expense {
        val expenseData = extractedData.expense
            ?: throw DokusException.BadRequest("No expense data extracted from document")

        val date = expenseData.date ?: throw DokusException.BadRequest("Date is required")
        val merchant = expenseData.merchant ?: throw DokusException.BadRequest("Merchant is required")
        val amount = expenseData.amount ?: throw DokusException.BadRequest("Amount is required")
        val category = expenseData.category ?: throw DokusException.BadRequest("Category is required")

        val expenseId = ExpensesTable.insertAndGetId {
            it[ExpensesTable.tenantId] = UUID.fromString(tenantId.toString())
            it[ExpensesTable.date] = date
            it[ExpensesTable.merchant] = merchant
            it[ExpensesTable.amount] = amount.toDbDecimal()
            it[ExpensesTable.vatAmount] = expenseData.vatAmount?.toDbDecimal()
            it[ExpensesTable.vatRate] = expenseData.vatRate?.toDbDecimal()
            it[ExpensesTable.category] = category
            it[ExpensesTable.description] = expenseData.description
            it[ExpensesTable.documentId] = UUID.fromString(documentId.toString())
            it[ExpensesTable.contactId] = linkedContactId?.let { id -> UUID.fromString(id.toString()) }
            it[ExpensesTable.isDeductible] = expenseData.isDeductible ?: true
            it[ExpensesTable.deductiblePercentage] =
                (expenseData.deductiblePercentage ?: Percentage.FULL).toDbDecimal()
            it[ExpensesTable.paymentMethod] = expenseData.paymentMethod
            it[ExpensesTable.isRecurring] = false
            it[ExpensesTable.notes] = expenseData.notes
        }.value

        val entryId = CashflowEntriesTable.insertAndGetId {
            it[CashflowEntriesTable.tenantId] = UUID.fromString(tenantId.toString())
            it[CashflowEntriesTable.sourceType] = CashflowSourceType.Expense
            it[CashflowEntriesTable.sourceId] = expenseId
            it[CashflowEntriesTable.documentId] = UUID.fromString(documentId.toString())
            it[CashflowEntriesTable.direction] = CashflowDirection.Out
            it[CashflowEntriesTable.eventDate] = date
            it[CashflowEntriesTable.amountGross] = amount.toDbDecimal()
            it[CashflowEntriesTable.amountVat] = (expenseData.vatAmount ?: Money.ZERO).toDbDecimal()
            it[CashflowEntriesTable.remainingAmount] = amount.toDbDecimal()
            it[CashflowEntriesTable.status] = CashflowEntryStatus.Open
            it[CashflowEntriesTable.counterpartyId] = linkedContactId?.let { id -> UUID.fromString(id.toString()) }
        }.value

        markDraftConfirmed(tenantId = tenantId, documentId = documentId)

        return CreatedConfirmation.Expense(
            expenseId = expenseId,
            cashflowEntryId = CashflowEntryId.parse(entryId.toString())
        )
    }

    /**
     * Confirm a Receipt document.
     * Receipt confirms into an Expense entity + cashflow OUT entry.
     * Uses same logic as Expense confirmation with receipt data.
     */
    private fun confirmReceiptTx(
        tenantId: TenantId,
        documentId: DocumentId,
        extractedData: ExtractedDocumentData,
        linkedContactId: ContactId?
    ): CreatedConfirmation.Receipt {
        val receiptData = extractedData.receipt
            ?: throw DokusException.BadRequest("No receipt data extracted from document")

        val date = receiptData.date ?: throw DokusException.BadRequest("Date is required")
        val merchant = receiptData.merchant ?: throw DokusException.BadRequest("Merchant is required")
        val amount = receiptData.amount ?: throw DokusException.BadRequest("Amount is required")
        val category = receiptData.category ?: throw DokusException.BadRequest("Category is required")

        // Receipt creates an Expense entity (same structure)
        val expenseId = ExpensesTable.insertAndGetId {
            it[ExpensesTable.tenantId] = UUID.fromString(tenantId.toString())
            it[ExpensesTable.date] = date
            it[ExpensesTable.merchant] = merchant
            it[ExpensesTable.amount] = amount.toDbDecimal()
            it[ExpensesTable.vatAmount] = receiptData.vatAmount?.toDbDecimal()
            it[ExpensesTable.vatRate] = receiptData.vatRate?.toDbDecimal()
            it[ExpensesTable.category] = category
            it[ExpensesTable.description] = receiptData.description
            it[ExpensesTable.documentId] = UUID.fromString(documentId.toString())
            it[ExpensesTable.contactId] = linkedContactId?.let { id -> UUID.fromString(id.toString()) }
            it[ExpensesTable.isDeductible] = receiptData.isDeductible ?: true
            it[ExpensesTable.deductiblePercentage] =
                (receiptData.deductiblePercentage ?: Percentage.FULL).toDbDecimal()
            it[ExpensesTable.paymentMethod] = receiptData.paymentMethod
            it[ExpensesTable.isRecurring] = false
            it[ExpensesTable.notes] = receiptData.notes
        }.value

        // Create cashflow OUT entry
        val entryId = CashflowEntriesTable.insertAndGetId {
            it[CashflowEntriesTable.tenantId] = UUID.fromString(tenantId.toString())
            it[CashflowEntriesTable.sourceType] = CashflowSourceType.Expense
            it[CashflowEntriesTable.sourceId] = expenseId
            it[CashflowEntriesTable.documentId] = UUID.fromString(documentId.toString())
            it[CashflowEntriesTable.direction] = CashflowDirection.Out
            it[CashflowEntriesTable.eventDate] = date
            it[CashflowEntriesTable.amountGross] = amount.toDbDecimal()
            it[CashflowEntriesTable.amountVat] = (receiptData.vatAmount ?: Money.ZERO).toDbDecimal()
            it[CashflowEntriesTable.remainingAmount] = amount.toDbDecimal()
            it[CashflowEntriesTable.status] = CashflowEntryStatus.Open
            it[CashflowEntriesTable.counterpartyId] = linkedContactId?.let { id -> UUID.fromString(id.toString()) }
        }.value

        markDraftConfirmed(tenantId = tenantId, documentId = documentId)

        return CreatedConfirmation.Receipt(
            expenseId = expenseId,
            cashflowEntryId = CashflowEntryId.parse(entryId.toString())
        )
    }

    /**
     * Confirm a ProForma document.
     * ProForma is informational only - marks draft as Confirmed but creates NO cashflow/VAT impact.
     * Conversion to Invoice is handled separately via ProFormaService.convertToInvoice().
     */
    private fun confirmProFormaTx(
        tenantId: TenantId,
        documentId: DocumentId
    ): CreatedConfirmation.ProForma {
        // ProForma creates no financial entity and no cashflow entry
        // Just mark the draft as confirmed
        markDraftConfirmed(tenantId = tenantId, documentId = documentId)

        // ProForma is document-only - no financial entity, no cashflow entry
        return CreatedConfirmation.ProForma(documentId = documentId)
    }

    private fun markDraftConfirmed(tenantId: TenantId, documentId: DocumentId) {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        val updated = DocumentDraftsTable.update({
            (DocumentDraftsTable.documentId eq UUID.fromString(documentId.toString())) and
                (DocumentDraftsTable.tenantId eq UUID.fromString(tenantId.toString()))
        }) {
            it[draftStatus] = DraftStatus.Confirmed
            it[rejectReason] = null
            it[updatedAt] = now
        }
        if (updated == 0) {
            throw DokusException.InternalError("Failed to update draft status to Confirmed")
        }
    }
}

private sealed interface CreatedConfirmation {
    data class Invoice(
        val invoiceId: UUID,
        val cashflowEntryId: CashflowEntryId
    ) : CreatedConfirmation

    data class Bill(
        val billId: UUID,
        val cashflowEntryId: CashflowEntryId
    ) : CreatedConfirmation

    data class Expense(
        val expenseId: UUID,
        val cashflowEntryId: CashflowEntryId
    ) : CreatedConfirmation

    data class Receipt(
        val expenseId: UUID, // Receipt creates an Expense entity
        val cashflowEntryId: CashflowEntryId
    ) : CreatedConfirmation

    data class ProForma(
        val documentId: DocumentId
    ) : CreatedConfirmation
}
