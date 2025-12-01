package ai.dokus.cashflow.backend.routes

import ai.dokus.cashflow.backend.repository.BillRepository
import ai.dokus.cashflow.backend.repository.ExpenseRepository
import ai.dokus.cashflow.backend.repository.InvoiceRepository
import ai.dokus.cashflow.backend.service.MediaService
import ai.dokus.foundation.domain.Money
import ai.dokus.foundation.domain.Percentage
import ai.dokus.foundation.domain.VatRate
import ai.dokus.foundation.domain.enums.EntityType
import ai.dokus.foundation.domain.enums.ExpenseCategory
import ai.dokus.foundation.domain.enums.MediaDocumentType
import ai.dokus.foundation.domain.enums.MediaStatus
import ai.dokus.foundation.domain.exceptions.DokusException
import ai.dokus.foundation.domain.model.CreateBillFromMediaRequest
import ai.dokus.foundation.domain.model.CreateBillRequest
import ai.dokus.foundation.domain.model.CreateExpenseFromMediaRequest
import ai.dokus.foundation.domain.model.CreateExpenseRequest
import ai.dokus.foundation.domain.model.CreateInvoiceFromMediaRequest
import ai.dokus.foundation.domain.model.CreateInvoiceRequest
import ai.dokus.foundation.domain.model.InvoiceItemDto
import ai.dokus.foundation.ktor.security.authenticateJwt
import ai.dokus.foundation.ktor.security.dokusPrincipal
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.inject
import org.slf4j.LoggerFactory

/**
 * From-Media Creation Routes
 *
 * These routes create financial entities (invoices, expenses, bills) from
 * processed media extraction data. The media must be in PROCESSED status
 * with extraction data available.
 *
 * Flow:
 * 1. Client uploads document to /api/v1/media
 * 2. AI processes the document and extracts data
 * 3. Client calls these routes with mediaId and optional corrections
 * 4. Entity is created from extraction data + corrections
 * 5. Media is attached to the created entity
 */
fun Route.fromMediaRoutes() {
    val invoiceRepository by inject<InvoiceRepository>()
    val expenseRepository by inject<ExpenseRepository>()
    val billRepository by inject<BillRepository>()
    val mediaService by inject<MediaService>()
    val logger = LoggerFactory.getLogger("FromMediaRoutes")

    // Invoice from media: POST /api/v1/cashflow/cash-in/invoices/from-media/{mediaId}
    route("/api/v1/cashflow/cash-in/invoices/from-media/{mediaId}") {
        authenticateJwt {
            post {
                val tenantId = dokusPrincipal.requireTenantId()
                val mediaId = call.parameters.mediaId
                    ?: throw DokusException.BadRequest("Media ID is required")
                val request = call.receive<CreateInvoiceFromMediaRequest>()

                logger.info("Creating invoice from media: $mediaId for tenant: $tenantId")

                // Fetch media with extraction data
                val media = mediaService.getMedia(mediaId, tenantId)
                    ?: throw DokusException.NotFound("Media not found")

                // Validate media status
                if (media.status != MediaStatus.Processed) {
                    throw DokusException.BadRequest("Media is not yet processed. Current status: ${media.status}")
                }

                val extraction = media.extraction
                    ?: throw DokusException.BadRequest("Media has no extraction data")

                // Validate document type
                if (extraction.documentType != MediaDocumentType.Invoice) {
                    throw DokusException.BadRequest("Media is not an invoice document. Type: ${extraction.documentType}")
                }

                val invoiceData = extraction.invoice
                    ?: throw DokusException.BadRequest("No invoice data extracted from media")

                // Build invoice request from extraction + corrections
                val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
                val createRequest = CreateInvoiceRequest(
                    clientId = request.clientId,
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
                    issueDate = request.corrections?.issueDate ?: invoiceData.issueDate ?: today,
                    dueDate = request.corrections?.dueDate ?: invoiceData.dueDate ?: today.plus(kotlinx.datetime.DatePeriod(days = 30)),
                    notes = request.corrections?.notes ?: invoiceData.notes
                )

                // Create invoice
                val invoice = invoiceRepository.createInvoice(tenantId, createRequest)
                    .onFailure {
                        logger.error("Failed to create invoice from media: $mediaId", it)
                        throw DokusException.InternalError("Failed to create invoice: ${it.message}")
                    }
                    .getOrThrow()

                // Attach media to invoice
                mediaService.attachMedia(mediaId, tenantId, EntityType.Invoice, invoice.id.toString())

                logger.info("Invoice created from media: ${invoice.id} (media: $mediaId)")

                call.respond(HttpStatusCode.Created, CreatedFromMediaResult(
                    entity = invoice,
                    mediaId = mediaId.toString(),
                    createdFrom = "MEDIA_EXTRACTION"
                ))
            }
        }
    }

    // Expense from media: POST /api/v1/cashflow/cash-out/expenses/from-media/{mediaId}
    route("/api/v1/cashflow/cash-out/expenses/from-media/{mediaId}") {
        authenticateJwt {
            post {
                val tenantId = dokusPrincipal.requireTenantId()
                val mediaId = call.parameters.mediaId
                    ?: throw DokusException.BadRequest("Media ID is required")
                val request = call.receive<CreateExpenseFromMediaRequest>()

                logger.info("Creating expense from media: $mediaId for tenant: $tenantId")

                // Fetch media with extraction data
                val media = mediaService.getMedia(mediaId, tenantId)
                    ?: throw DokusException.NotFound("Media not found")

                // Validate media status
                if (media.status != MediaStatus.Processed) {
                    throw DokusException.BadRequest("Media is not yet processed. Current status: ${media.status}")
                }

                val extraction = media.extraction
                    ?: throw DokusException.BadRequest("Media has no extraction data")

                // Validate document type
                if (extraction.documentType != MediaDocumentType.Expense) {
                    throw DokusException.BadRequest("Media is not an expense document. Type: ${extraction.documentType}")
                }

                val expenseData = extraction.expense
                    ?: throw DokusException.BadRequest("No expense data extracted from media")

                // Build expense request from extraction + corrections
                val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
                val corrections = request.corrections

                val createRequest = CreateExpenseRequest(
                    date = corrections?.date ?: expenseData.date ?: today,
                    merchant = corrections?.merchant ?: expenseData.merchant ?: "Unknown",
                    amount = corrections?.amount ?: expenseData.amount ?: Money.ZERO,
                    vatAmount = corrections?.vatAmount ?: expenseData.vatAmount,
                    vatRate = corrections?.vatRate ?: expenseData.vatRate,
                    category = corrections?.category ?: expenseData.category ?: ExpenseCategory.Other,
                    description = null,
                    receiptUrl = null,
                    receiptFilename = media.filename,
                    isDeductible = corrections?.isDeductible ?: true,
                    deductiblePercentage = corrections?.deductiblePercentage ?: Percentage.FULL,
                    isRecurring = false,
                    paymentMethod = null,
                    notes = corrections?.notes ?: expenseData.notes
                )

                // Create expense
                val expense = expenseRepository.createExpense(tenantId, createRequest)
                    .onFailure {
                        logger.error("Failed to create expense from media: $mediaId", it)
                        throw DokusException.InternalError("Failed to create expense: ${it.message}")
                    }
                    .getOrThrow()

                // Attach media to expense
                mediaService.attachMedia(mediaId, tenantId, EntityType.Expense, expense.id.toString())

                logger.info("Expense created from media: ${expense.id} (media: $mediaId)")

                call.respond(HttpStatusCode.Created, CreatedFromMediaResult(
                    entity = expense,
                    mediaId = mediaId.toString(),
                    createdFrom = "MEDIA_EXTRACTION"
                ))
            }
        }
    }

    // Bill from media: POST /api/v1/cashflow/cash-out/bills/from-media/{mediaId}
    route("/api/v1/cashflow/cash-out/bills/from-media/{mediaId}") {
        authenticateJwt {
            post {
                val tenantId = dokusPrincipal.requireTenantId()
                val mediaId = call.parameters.mediaId
                    ?: throw DokusException.BadRequest("Media ID is required")
                val request = call.receive<CreateBillFromMediaRequest>()

                logger.info("Creating bill from media: $mediaId for tenant: $tenantId")

                // Fetch media with extraction data
                val media = mediaService.getMedia(mediaId, tenantId)
                    ?: throw DokusException.NotFound("Media not found")

                // Validate media status
                if (media.status != MediaStatus.Processed) {
                    throw DokusException.BadRequest("Media is not yet processed. Current status: ${media.status}")
                }

                val extraction = media.extraction
                    ?: throw DokusException.BadRequest("Media has no extraction data")

                // Validate document type
                if (extraction.documentType != MediaDocumentType.Bill) {
                    throw DokusException.BadRequest("Media is not a bill document. Type: ${extraction.documentType}")
                }

                val billData = extraction.bill
                    ?: throw DokusException.BadRequest("No bill data extracted from media")

                // Build bill request from extraction + corrections
                val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
                val corrections = request.corrections

                val createRequest = CreateBillRequest(
                    supplierName = corrections?.supplierName ?: billData.supplierName ?: "Unknown Supplier",
                    supplierVatNumber = corrections?.supplierVatNumber ?: billData.supplierVatNumber,
                    invoiceNumber = corrections?.invoiceNumber ?: billData.invoiceNumber,
                    issueDate = corrections?.issueDate ?: billData.issueDate ?: today,
                    dueDate = corrections?.dueDate ?: billData.dueDate ?: today.plus(kotlinx.datetime.DatePeriod(days = 30)),
                    amount = corrections?.amount ?: billData.amount ?: Money.ZERO,
                    vatAmount = corrections?.vatAmount ?: billData.vatAmount,
                    vatRate = corrections?.vatRate ?: billData.vatRate,
                    category = corrections?.category ?: billData.category ?: ExpenseCategory.Other,
                    description = corrections?.description ?: billData.description,
                    notes = corrections?.notes ?: billData.notes,
                    mediaId = mediaId
                )

                // Create bill
                val bill = billRepository.createBill(tenantId, createRequest)
                    .onFailure {
                        logger.error("Failed to create bill from media: $mediaId", it)
                        throw DokusException.InternalError("Failed to create bill: ${it.message}")
                    }
                    .getOrThrow()

                // Attach media to bill
                mediaService.attachMedia(mediaId, tenantId, EntityType.Bill, bill.id.toString())

                logger.info("Bill created from media: ${bill.id} (media: $mediaId)")

                call.respond(HttpStatusCode.Created, CreatedFromMediaResult(
                    entity = bill,
                    mediaId = mediaId.toString(),
                    createdFrom = "MEDIA_EXTRACTION"
                ))
            }
        }
    }
}

/**
 * Response for creating an entity from media
 */
@Serializable
data class CreatedFromMediaResult<T>(
    val entity: T,
    val mediaId: String,
    val createdFrom: String
)
