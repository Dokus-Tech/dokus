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
import tech.dokus.domain.enums.DocumentStatus
import tech.dokus.domain.enums.InvoiceStatus
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.BillId
import tech.dokus.domain.ids.CashflowEntryId
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.ExpenseId
import tech.dokus.domain.ids.InvoiceId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.DocumentDraftData
import tech.dokus.domain.model.CreditNoteDraftData
import tech.dokus.domain.model.InvoiceDraftData
import tech.dokus.domain.model.BillDraftData
import tech.dokus.domain.model.ReceiptDraftData
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
     * @param draftData The normalized draft data to use for entity creation
     * @param linkedContactId The linked contact (required for invoices)
     * @return The created financial entity
     */
    @Suppress("CyclomaticComplexMethod")
    suspend fun confirmDocument(
        tenantId: TenantId,
        documentId: DocumentId,
        documentType: DocumentType,
        draftData: DocumentDraftData,
        linkedContactId: ContactId?
    ): Result<ConfirmationResult> = runCatching {
        logger.info("Confirming document: $documentId as $documentType for tenant: $tenantId")

        val invoiceNumber: String? = when (documentType) {
            DocumentType.Invoice -> invoiceNumberGenerator.generateInvoiceNumber(tenantId).getOrThrow()
            else -> null
        }

        val created = dbQuery {
            ensureDraftConfirmable(tenantId = tenantId, documentId = documentId)

            val draftType = when (draftData) {
                is InvoiceDraftData -> DocumentType.Invoice
                is BillDraftData -> DocumentType.Bill
                is ReceiptDraftData -> DocumentType.Receipt
                is CreditNoteDraftData -> DocumentType.CreditNote
            }

            if (draftType != documentType) {
                throw DokusException.BadRequest(
                    "Draft data type $draftType does not match document type $documentType"
                )
            }

            when (draftData) {
                is InvoiceDraftData -> confirmInvoiceTx(
                    tenantId = tenantId,
                    documentId = documentId,
                    draftData = draftData,
                    linkedContactId = linkedContactId,
                    invoiceNumber = requireNotNull(invoiceNumber)
                )

                is BillDraftData -> confirmBillTx(
                    tenantId = tenantId,
                    documentId = documentId,
                    draftData = draftData,
                    linkedContactId = linkedContactId
                )

                is ReceiptDraftData -> confirmReceiptTx(
                    tenantId = tenantId,
                    documentId = documentId,
                    draftData = draftData,
                    linkedContactId = linkedContactId
                )

                is CreditNoteDraftData -> throw DokusException.BadRequest(
                    "CreditNote confirmation is handled via CreditNoteService"
                )
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

            is CreatedConfirmation.Receipt -> {
                // Receipt creates an Expense entity, so retrieve it as Expense
                val expense = expenseRepository.getExpense(
                    expenseId = ExpenseId.parse(created.expenseId.toString()),
                    tenantId = tenantId
                ).getOrThrow() ?: throw DokusException.InternalError("Expense not found after receipt confirmation")
                expense to created.cashflowEntryId
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

        val status = draft[DocumentDraftsTable.documentStatus]
        if (status != DocumentStatus.NeedsReview && status != DocumentStatus.Confirmed) {
            throw DokusException.BadRequest("Draft is not ready for confirmation: $status")
        }
    }

    @Suppress("CyclomaticComplexMethod")
    private fun confirmInvoiceTx(
        tenantId: TenantId,
        documentId: DocumentId,
        draftData: InvoiceDraftData,
        linkedContactId: ContactId?,
        invoiceNumber: String
    ): CreatedConfirmation.Invoice {
        val contactId = linkedContactId
            ?: throw DokusException.BadRequest("Invoice requires a linked contact")

        val items = if (draftData.lineItems.isNotEmpty()) {
            draftData.lineItems.mapIndexed { index, item ->
                val quantity = item.quantity?.takeIf { it > 0 } ?: 1L
                val unitPrice = item.unitPrice?.let { Money(it) }
                    ?: item.netAmount?.let { net ->
                        Money(net / quantity)
                    }
                val lineTotal = item.netAmount?.let { Money(it) }
                    ?: unitPrice?.let { Money(it.minor * quantity) }
                    ?: Money.ZERO
                val vatRate = item.vatRate?.let { VatRate(it) } ?: VatRate.ZERO
                val vatAmount = vatRate.applyTo(lineTotal)

                InvoiceItemDto(
                    description = item.description.ifBlank { "Item" },
                    quantity = quantity.toDouble(),
                    unitPrice = unitPrice ?: lineTotal,
                    vatRate = vatRate,
                    lineTotal = lineTotal,
                    vatAmount = vatAmount,
                    sortOrder = index
                )
            }
        } else {
            val base = draftData.subtotalAmount ?: draftData.totalAmount ?: Money.ZERO
            val vat = draftData.vatAmount ?: Money.ZERO
            val vatRate = if (!base.isZero) {
                VatRate(((vat.minor * 10000L) / base.minor).toInt())
            } else {
                VatRate.ZERO
            }
            listOf(
                InvoiceItemDto(
                    description = "Services",
                    quantity = 1.0,
                    unitPrice = base,
                    vatRate = vatRate,
                    lineTotal = base,
                    vatAmount = vat,
                    sortOrder = 0
                )
            )
        }

        val today = Clock.System.now().toLocalDateTime(TimeZone.UTC).date
        val issueDate = draftData.issueDate ?: today
        val dueDate = draftData.dueDate ?: issueDate.plus(DatePeriod(days = 30))

        val subtotalAmount = draftData.subtotalAmount?.toDbDecimal()
            ?: items.sumOf { it.lineTotal.toDbDecimal() }
        val vatAmount = draftData.vatAmount?.toDbDecimal()
            ?: items.sumOf { it.vatAmount.toDbDecimal() }
        val totalAmount = draftData.totalAmount?.toDbDecimal()
            ?: (subtotalAmount + vatAmount)

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
            it[InvoicesTable.notes] = draftData.notes
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

    @Suppress("ThrowsCount")
    private fun confirmBillTx(
        tenantId: TenantId,
        documentId: DocumentId,
        draftData: BillDraftData,
        linkedContactId: ContactId?,
    ): CreatedConfirmation.Bill {
        val issueDate = draftData.issueDate ?: throw DokusException.BadRequest("Issue date is required")
        val dueDate = draftData.dueDate ?: issueDate
        val subtotalAmount = draftData.subtotalAmount
        val vatAmount = draftData.vatAmount
        val amount = draftData.totalAmount ?: subtotalAmount
            ?: throw DokusException.BadRequest("Amount is required")
        val category = tech.dokus.domain.enums.ExpenseCategory.Other
        val vatRate = if (subtotalAmount != null && vatAmount != null && !subtotalAmount.isZero) {
            VatRate(((vatAmount.minor * 10000L) / subtotalAmount.minor).toInt())
        } else {
            null
        }

        val billId = BillsTable.insertAndGetId {
            it[BillsTable.tenantId] = UUID.fromString(tenantId.toString())
            it[BillsTable.supplierName] = draftData.supplierName ?: "Unknown Supplier"
            it[BillsTable.supplierVatNumber] = draftData.supplierVat?.value
            it[BillsTable.invoiceNumber] = draftData.invoiceNumber
            it[BillsTable.issueDate] = issueDate
            it[BillsTable.dueDate] = dueDate
            it[BillsTable.amount] = amount.toDbDecimal()
            it[BillsTable.vatAmount] = vatAmount?.toDbDecimal()
            it[BillsTable.vatRate] = vatRate?.toDbDecimal()
            it[BillsTable.status] = BillStatus.Pending
            it[BillsTable.category] = category
            it[BillsTable.description] = null
            it[BillsTable.notes] = draftData.notes
            it[BillsTable.currency] = draftData.currency
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
            it[CashflowEntriesTable.amountVat] = (draftData.vatAmount ?: Money.ZERO).toDbDecimal()
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

    /**
     * Confirm a Receipt document.
     * Receipt confirms into an Expense entity + cashflow OUT entry.
     * Uses same logic as Expense confirmation with receipt data.
     */
    @Suppress("ThrowsCount")
    private fun confirmReceiptTx(
        tenantId: TenantId,
        documentId: DocumentId,
        draftData: ReceiptDraftData,
        linkedContactId: ContactId?,
    ): CreatedConfirmation.Receipt {
        val date = draftData.date ?: throw DokusException.BadRequest("Date is required")
        val merchant = draftData.merchantName ?: throw DokusException.BadRequest("Merchant is required")
        val totalAmount = draftData.totalAmount ?: throw DokusException.BadRequest("Amount is required")
        val vatAmount = draftData.vatAmount
        val amount = totalAmount
        val category = tech.dokus.domain.enums.ExpenseCategory.Other
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

        // Receipt creates an Expense entity (same structure)
        val expenseId = ExpensesTable.insertAndGetId {
            it[ExpensesTable.tenantId] = UUID.fromString(tenantId.toString())
            it[ExpensesTable.date] = date
            it[ExpensesTable.merchant] = merchant
            it[ExpensesTable.amount] = amount.toDbDecimal()
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

        // Create cashflow OUT entry
        val entryId = CashflowEntriesTable.insertAndGetId {
            it[CashflowEntriesTable.tenantId] = UUID.fromString(tenantId.toString())
            it[CashflowEntriesTable.sourceType] = CashflowSourceType.Expense
            it[CashflowEntriesTable.sourceId] = expenseId
            it[CashflowEntriesTable.documentId] = UUID.fromString(documentId.toString())
            it[CashflowEntriesTable.direction] = CashflowDirection.Out
            it[CashflowEntriesTable.eventDate] = date
            it[CashflowEntriesTable.amountGross] = amount.toDbDecimal()
            it[CashflowEntriesTable.amountVat] = (vatAmount ?: Money.ZERO).toDbDecimal()
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

    private fun markDraftConfirmed(tenantId: TenantId, documentId: DocumentId) {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        val updated = DocumentDraftsTable.update({
            (DocumentDraftsTable.documentId eq UUID.fromString(documentId.toString())) and
                (DocumentDraftsTable.tenantId eq UUID.fromString(tenantId.toString()))
        }) {
            it[documentStatus] = DocumentStatus.Confirmed
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

    data class Receipt(
        val expenseId: UUID, // Receipt creates an Expense entity
        val cashflowEntryId: CashflowEntryId
    ) : CreatedConfirmation
}
