package ai.dokus.cashflow.backend.service

import ai.dokus.cashflow.backend.repository.BillRepository
import ai.dokus.cashflow.backend.repository.ExpenseRepository
import ai.dokus.cashflow.backend.repository.InvoiceRepository
import ai.dokus.foundation.domain.Money
import ai.dokus.foundation.domain.Percentage
import ai.dokus.foundation.domain.VatRate
import ai.dokus.foundation.domain.enums.EntityType
import ai.dokus.foundation.domain.enums.ExpenseCategory
import ai.dokus.foundation.domain.enums.MediaDocumentType
import ai.dokus.foundation.domain.enums.MediaStatus
import ai.dokus.foundation.domain.exceptions.DokusException
import ai.dokus.foundation.domain.ids.ClientId
import ai.dokus.foundation.domain.ids.MediaId
import ai.dokus.foundation.domain.ids.TenantId
import ai.dokus.foundation.domain.model.BillCorrections
import ai.dokus.foundation.domain.model.CreateBillRequest
import ai.dokus.foundation.domain.model.CreateExpenseRequest
import ai.dokus.foundation.domain.model.CreateInvoiceRequest
import ai.dokus.foundation.domain.model.ExpenseCorrections
import ai.dokus.foundation.domain.model.FinancialDocumentDto
import ai.dokus.foundation.domain.model.InvoiceCorrections
import ai.dokus.foundation.domain.model.InvoiceItemDto
import ai.dokus.foundation.domain.model.MediaDto
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import org.slf4j.LoggerFactory

/**
 * Result of creating an entity from media.
 */
data class FromMediaResult<T>(
    val entity: T,
    val mediaId: MediaId,
    val createdFrom: String = "MEDIA_EXTRACTION"
)

/**
 * Service for creating financial entities from processed media extractions.
 *
 * This service handles the business logic for:
 * 1. Validating media status and extraction data
 * 2. Building entity requests from extraction data + corrections
 * 3. Creating entities via repositories
 * 4. Attaching media to created entities
 */
class FromMediaService(
    private val invoiceRepository: InvoiceRepository,
    private val expenseRepository: ExpenseRepository,
    private val billRepository: BillRepository,
    private val mediaService: IMediaService
) {
    private val logger = LoggerFactory.getLogger(FromMediaService::class.java)

    /**
     * Create an invoice from processed media extraction.
     *
     * @param mediaId The media ID containing extraction data
     * @param tenantId The tenant creating the invoice
     * @param clientId The client ID for the invoice
     * @param corrections Optional corrections to apply to extracted data
     */
    suspend fun createInvoiceFromMedia(
        mediaId: MediaId,
        tenantId: TenantId,
        clientId: ClientId,
        corrections: InvoiceCorrections? = null
    ): Result<FromMediaResult<FinancialDocumentDto.InvoiceDto>> = runCatching {
        logger.info("Creating invoice from media: $mediaId for tenant: $tenantId")

        // Fetch and validate media
        val media = fetchAndValidateMedia(mediaId, tenantId)

        // Validate document type
        val extraction = media.extraction
            ?: throw DokusException.BadRequest("Media has no extraction data")

        if (extraction.documentType != MediaDocumentType.Invoice) {
            throw DokusException.BadRequest("Media is not an invoice document. Type: ${extraction.documentType}")
        }

        val invoiceData = extraction.invoice
            ?: throw DokusException.BadRequest("No invoice data extracted from media")

        // Build invoice request from extraction + corrections
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val createRequest = CreateInvoiceRequest(
            clientId = clientId,
            items = invoiceData.items ?: listOf(
                InvoiceItemDto(
                    description = "Services",
                    quantity = 1.0,
                    unitPrice = invoiceData.subtotal ?: Money.ZERO,
                    vatRate = VatRate.STANDARD_BE,
                    lineTotal = invoiceData.subtotal ?: Money.ZERO,
                    vatAmount = invoiceData.vatAmount ?: Money.ZERO
                )
            ),
            issueDate = corrections?.issueDate ?: invoiceData.issueDate ?: today,
            dueDate = corrections?.dueDate ?: invoiceData.dueDate ?: today.plus(DatePeriod(days = 30)),
            notes = corrections?.notes ?: invoiceData.notes
        )

        // Create invoice
        val invoice = invoiceRepository.createInvoice(tenantId, createRequest)
            .getOrThrow()

        // Attach media to invoice
        mediaService.attachMedia(mediaId, tenantId, EntityType.Invoice, invoice.id.toString())

        logger.info("Invoice created from media: ${invoice.id} (media: $mediaId)")

        FromMediaResult(
            entity = invoice,
            mediaId = mediaId
        )
    }.onFailure {
        logger.error("Failed to create invoice from media: $mediaId", it)
    }

    /**
     * Create an expense from processed media extraction.
     *
     * @param mediaId The media ID containing extraction data
     * @param tenantId The tenant creating the expense
     * @param corrections Optional corrections to apply to extracted data
     */
    suspend fun createExpenseFromMedia(
        mediaId: MediaId,
        tenantId: TenantId,
        corrections: ExpenseCorrections? = null
    ): Result<FromMediaResult<FinancialDocumentDto.ExpenseDto>> = runCatching {
        logger.info("Creating expense from media: $mediaId for tenant: $tenantId")

        // Fetch and validate media
        val media = fetchAndValidateMedia(mediaId, tenantId)

        // Validate document type
        val extraction = media.extraction
            ?: throw DokusException.BadRequest("Media has no extraction data")

        if (extraction.documentType != MediaDocumentType.Expense) {
            throw DokusException.BadRequest("Media is not an expense document. Type: ${extraction.documentType}")
        }

        val expenseData = extraction.expense
            ?: throw DokusException.BadRequest("No expense data extracted from media")

        // Build expense request from extraction + corrections
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

        val createRequest = CreateExpenseRequest(
            date = corrections?.date ?: expenseData.date ?: today,
            merchant = corrections?.merchant ?: expenseData.merchant ?: "Unknown",
            amount = corrections?.amount ?: expenseData.amount ?: Money.ZERO,
            vatAmount = corrections?.vatAmount ?: expenseData.vatAmount,
            vatRate = corrections?.vatRate ?: expenseData.vatRate,
            category = corrections?.category ?: expenseData.category ?: ExpenseCategory.Other,
            description = null,
            documentId = null, // Document will be linked separately
            isDeductible = corrections?.isDeductible ?: true,
            deductiblePercentage = corrections?.deductiblePercentage ?: Percentage.FULL,
            isRecurring = false,
            paymentMethod = null,
            notes = corrections?.notes ?: expenseData.notes
        )

        // Create expense
        val expense = expenseRepository.createExpense(tenantId, createRequest)
            .getOrThrow()

        // Attach media to expense
        mediaService.attachMedia(mediaId, tenantId, EntityType.Expense, expense.id.toString())

        logger.info("Expense created from media: ${expense.id} (media: $mediaId)")

        FromMediaResult(
            entity = expense,
            mediaId = mediaId
        )
    }.onFailure {
        logger.error("Failed to create expense from media: $mediaId", it)
    }

    /**
     * Create a bill from processed media extraction.
     *
     * @param mediaId The media ID containing extraction data
     * @param tenantId The tenant creating the bill
     * @param corrections Optional corrections to apply to extracted data
     */
    suspend fun createBillFromMedia(
        mediaId: MediaId,
        tenantId: TenantId,
        corrections: BillCorrections? = null
    ): Result<FromMediaResult<FinancialDocumentDto.BillDto>> = runCatching {
        logger.info("Creating bill from media: $mediaId for tenant: $tenantId")

        // Fetch and validate media
        val media = fetchAndValidateMedia(mediaId, tenantId)

        // Validate document type
        val extraction = media.extraction
            ?: throw DokusException.BadRequest("Media has no extraction data")

        if (extraction.documentType != MediaDocumentType.Bill) {
            throw DokusException.BadRequest("Media is not a bill document. Type: ${extraction.documentType}")
        }

        val billData = extraction.bill
            ?: throw DokusException.BadRequest("No bill data extracted from media")

        // Build bill request from extraction + corrections
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

        val createRequest = CreateBillRequest(
            supplierName = corrections?.supplierName ?: billData.supplierName ?: "Unknown Supplier",
            supplierVatNumber = corrections?.supplierVatNumber ?: billData.supplierVatNumber,
            invoiceNumber = corrections?.invoiceNumber ?: billData.invoiceNumber,
            issueDate = corrections?.issueDate ?: billData.issueDate ?: today,
            dueDate = corrections?.dueDate ?: billData.dueDate ?: today.plus(DatePeriod(days = 30)),
            amount = corrections?.amount ?: billData.amount ?: Money.ZERO,
            vatAmount = corrections?.vatAmount ?: billData.vatAmount,
            vatRate = corrections?.vatRate ?: billData.vatRate,
            category = corrections?.category ?: billData.category ?: ExpenseCategory.Other,
            description = corrections?.description ?: billData.description,
            notes = corrections?.notes ?: billData.notes,
            documentId = null // Document will be linked separately
        )

        // Create bill
        val bill = billRepository.createBill(tenantId, createRequest)
            .getOrThrow()

        // Attach media to bill
        mediaService.attachMedia(mediaId, tenantId, EntityType.Bill, bill.id.toString())

        logger.info("Bill created from media: ${bill.id} (media: $mediaId)")

        FromMediaResult(
            entity = bill,
            mediaId = mediaId
        )
    }.onFailure {
        logger.error("Failed to create bill from media: $mediaId", it)
    }

    /**
     * Fetch and validate media for entity creation.
     */
    private suspend fun fetchAndValidateMedia(
        mediaId: MediaId,
        tenantId: TenantId
    ): MediaDto {
        val media = mediaService.getMedia(mediaId, tenantId)
            ?: throw DokusException.NotFound("Media not found")

        if (media.status != MediaStatus.Processed) {
            throw DokusException.BadRequest("Media is not yet processed. Current status: ${media.status}")
        }

        return media
    }
}
