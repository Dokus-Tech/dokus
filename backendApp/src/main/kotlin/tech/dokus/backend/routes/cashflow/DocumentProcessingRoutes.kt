package tech.dokus.backend.routes.cashflow

import ai.dokus.foundation.database.repository.cashflow.BillRepository
import ai.dokus.foundation.database.repository.cashflow.DocumentProcessingRepository
import ai.dokus.foundation.database.repository.cashflow.DocumentRepository
import ai.dokus.foundation.database.repository.cashflow.ExpenseRepository
import ai.dokus.foundation.database.repository.cashflow.InvoiceRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.resources.get
import io.ktor.server.resources.patch
import io.ktor.server.resources.post
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import org.koin.ktor.ext.inject
import org.slf4j.LoggerFactory
import tech.dokus.domain.Money
import tech.dokus.domain.VatRate
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.enums.EntityType
import tech.dokus.domain.enums.ProcessingStatus
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.model.ConfirmDocumentRequest
import tech.dokus.domain.model.ConfirmDocumentResponse
import tech.dokus.domain.model.CreateBillRequest
import tech.dokus.domain.model.CreateExpenseRequest
import tech.dokus.domain.model.CreateInvoiceRequest
import tech.dokus.domain.model.DocumentProcessingListResponse
import tech.dokus.domain.model.InvoiceItemDto
import tech.dokus.domain.model.ReprocessDocumentRequest
import tech.dokus.domain.model.ReprocessDocumentResponse
import tech.dokus.domain.model.TrackedCorrection
import tech.dokus.domain.model.UpdateDraftRequest
import tech.dokus.domain.model.UpdateDraftResponse
import tech.dokus.domain.routes.Documents
import tech.dokus.foundation.ktor.security.authenticateJwt
import tech.dokus.foundation.ktor.security.dokusPrincipal
import java.util.UUID
import tech.dokus.foundation.ktor.storage.DocumentStorageService as MinioDocumentStorageService

/**
 * Document processing routes using Ktor Type-Safe Routing for querying and managing AI extraction.
 *
 * Endpoints:
 * - GET /api/v1/documents/processing - List documents by processing status
 * - GET /api/v1/documents/{id}/processing - Get processing details for a document
 * - PATCH /api/v1/documents/{id}/status - Confirm extraction and create entity
 * - PATCH /api/v1/documents/{id}/draft - Update extracted draft with user corrections
 * - POST /api/v1/documents/{id}/processing-jobs - Trigger re-extraction
 */
internal fun Route.documentProcessingRoutes() {
    val processingRepository by inject<DocumentProcessingRepository>()
    val documentRepository by inject<DocumentRepository>()
    val invoiceRepository by inject<InvoiceRepository>()
    val expenseRepository by inject<ExpenseRepository>()
    val billRepository by inject<BillRepository>()
    val minioStorage by inject<MinioDocumentStorageService>()
    val logger = LoggerFactory.getLogger("DocumentProcessingRoutes")

    authenticateJwt {
        /**
         * GET /api/v1/documents/processing
         * List documents by processing status with pagination.
         *
         * Query parameters:
         * - status: Comma-separated processing status filter (PENDING,QUEUED,PROCESSING,PROCESSED)
         * - page: Page number (default 1)
         * - limit: Items per page (default 20, max 100)
         *
         * Response: DocumentProcessingListResponse
         */
        get<Documents.Processing> { route ->
            val tenantId = dokusPrincipal.requireTenantId()

            // Parse comma-separated status filter
            val statusValues = route.status
                ?.split(",")
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }

            val statuses = statusValues.orEmpty().map(ProcessingStatus::fromDbValue)

            if (statuses.isEmpty()) throw DokusException.BadRequest("No valid status values provided")

            val page = route.page.coerceAtLeast(0)
            val limit = route.limit.coerceIn(1, 100)
            val offset = page * limit

            logger.info("Listing processing records: tenant=$tenantId, statuses=$statuses, page=$page, limit=$limit, offset=$offset")

            val (items, total) = processingRepository.listByStatus(
                tenantId = tenantId,
                statuses = statuses,
                limit = limit,
                offset = offset,
                includeDocument = true
            )

            // Add download URLs to documents
            val itemsWithUrls = items.map { processing ->
                processing.document?.let { doc ->
                    val downloadUrl = try {
                        minioStorage.getDownloadUrl(doc.storageKey)
                    } catch (e: Exception) {
                        logger.warn("Failed to get download URL for ${doc.storageKey}: ${e.message}")
                        null
                    }
                    processing.copy(document = doc.copy(downloadUrl = downloadUrl))
                } ?: processing
            }

            call.respond(
                HttpStatusCode.OK,
                DocumentProcessingListResponse(
                    items = itemsWithUrls,
                    total = total,
                    page = page,
                    limit = limit,
                    hasMore = (page + 1) * limit < total
                )
            )
        }

        /**
         * GET /api/v1/documents/{id}/processing
         * Get processing details for a specific document.
         *
         * Path parameters:
         * - id: Document ID (UUID)
         *
         * Response: DocumentProcessingDto
         */
        get<Documents.Id.Processing> { route ->
            val tenantId = dokusPrincipal.requireTenantId()
            val documentId = DocumentId.parse(route.parent.id)

            logger.info("Getting processing for document: $documentId, tenant=$tenantId")

            val processing = processingRepository.getByDocumentId(
                documentId = documentId,
                tenantId = tenantId,
                includeDocument = true
            ) ?: throw DokusException.NotFound("Processing record not found for document")

            // Add download URL
            val processingWithUrl = processing.document?.let { doc ->
                val downloadUrl = try {
                    minioStorage.getDownloadUrl(doc.storageKey)
                } catch (e: Exception) {
                    null
                }
                processing.copy(document = doc.copy(downloadUrl = downloadUrl))
            } ?: processing

            call.respond(HttpStatusCode.OK, processingWithUrl)
        }

        /**
         * PATCH /api/v1/documents/{id}/status
         * Update document status (confirm or reject).
         *
         * Path parameters:
         * - id: Document ID (UUID)
         *
         * Request body: ConfirmDocumentRequest (for confirm) or UpdateStatusRequest (for reject)
         * - entityType: Type of entity to create (Invoice, Bill, Expense) - for confirm
         * - corrections: Optional field corrections - for confirm
         * - status: "rejected" - for reject
         *
         * Response: ConfirmDocumentResponse (for confirm) or 204 No Content (for reject)
         */
        patch<Documents.Id.Status> { route ->
            val tenantId = dokusPrincipal.requireTenantId()
            val documentId = DocumentId.parse(route.parent.id)

            val request = call.receive<ConfirmDocumentRequest>()

            logger.info("Confirming document: $documentId, entityType=${request.entityType}, tenant=$tenantId")

            // Get processing record
            val processing = processingRepository.getByDocumentId(
                documentId = documentId,
                tenantId = tenantId,
                includeDocument = false
            ) ?: throw DokusException.NotFound("Processing record not found for document")

            // Verify status allows confirmation
            if (processing.status != ProcessingStatus.Processed) {
                throw DokusException.BadRequest(
                    "Document cannot be confirmed in status: ${processing.status}. Must be PROCESSED."
                )
            }

            // Get extracted data
            val extractedData = processing.extractedData
                ?: throw DokusException.BadRequest("No extracted data available for confirmation")

            // Create entity based on type
            val entityId: UUID = when (request.entityType) {
                DocumentType.Invoice -> {
                    val invoiceData = extractedData.invoice
                        ?: throw DokusException.BadRequest("No invoice data extracted from document")

                    // Contact ID is required for invoices
                    val contactIdStr = request.corrections?.contactId
                        ?: throw DokusException.BadRequest("Contact ID is required for invoice creation")

                    val contactId = try {
                        ContactId.parse(contactIdStr)
                    } catch (e: Exception) {
                        throw DokusException.BadRequest("Invalid contact ID format")
                    }

                    // Create invoice from extracted + corrected data
                    val createRequest = CreateInvoiceRequest(
                        contactId = contactId,
                        items = request.corrections?.items?.map { item ->
                            InvoiceItemDto(
                                description = item.description ?: "",
                                quantity = item.quantity ?: 1.0,
                                unitPrice = item.unitPrice ?: Money("0"),
                                vatRate = item.vatRate ?: VatRate.STANDARD_BE,
                                lineTotal = item.lineTotal ?: Money("0"),
                                vatAmount = item.vatAmount ?: Money("0")
                            )
                        } ?: emptyList(),
                        issueDate = request.corrections?.date ?: invoiceData.issueDate,
                        dueDate = request.corrections?.dueDate ?: invoiceData.dueDate,
                        notes = request.corrections?.notes ?: invoiceData.notes
                    )

                    val invoice = invoiceRepository.createInvoice(tenantId, createRequest)
                        .getOrThrow()

                    // Link document to invoice
                    documentRepository.linkToEntity(
                        tenantId = tenantId,
                        documentId = documentId,
                        entityType = EntityType.Invoice,
                        entityId = invoice.id.toString()
                    )

                    UUID.fromString(invoice.id.toString())
                }

                DocumentType.Bill -> {
                    val billData = extractedData.bill
                        ?: throw DokusException.BadRequest("No bill data extracted from document")

                    val createRequest = CreateBillRequest(
                        supplierName = request.corrections?.supplierName ?: billData.supplierName ?: "Unknown",
                        supplierVatNumber = request.corrections?.supplierVatNumber ?: billData.supplierVatNumber,
                        invoiceNumber = request.corrections?.invoiceNumber ?: billData.invoiceNumber,
                        issueDate = request.corrections?.date ?: billData.issueDate
                        ?: throw DokusException.BadRequest("Issue date is required"),
                        dueDate = request.corrections?.dueDate ?: billData.dueDate
                        ?: throw DokusException.BadRequest("Due date is required"),
                        amount = request.corrections?.amount ?: billData.amount
                        ?: throw DokusException.BadRequest("Amount is required"),
                        vatAmount = request.corrections?.vatAmount ?: billData.vatAmount,
                        vatRate = request.corrections?.vatRate ?: billData.vatRate,
                        category = request.corrections?.category ?: billData.category
                        ?: throw DokusException.BadRequest("Category is required"),
                        description = request.corrections?.description ?: billData.description,
                        notes = request.corrections?.notes ?: billData.notes,
                        documentId = documentId
                    )

                    val bill = billRepository.createBill(tenantId, createRequest)
                        .getOrThrow()

                    UUID.fromString(bill.id.toString())
                }

                DocumentType.Expense -> {
                    val expenseData = extractedData.expense
                        ?: throw DokusException.BadRequest("No expense data extracted from document")

                    val createRequest = CreateExpenseRequest(
                        date = request.corrections?.date ?: expenseData.date
                        ?: throw DokusException.BadRequest("Date is required"),
                        merchant = request.corrections?.merchant ?: expenseData.merchant
                        ?: throw DokusException.BadRequest("Merchant is required"),
                        amount = request.corrections?.amount ?: expenseData.amount
                        ?: throw DokusException.BadRequest("Amount is required"),
                        vatAmount = request.corrections?.vatAmount ?: expenseData.vatAmount,
                        vatRate = request.corrections?.vatRate ?: expenseData.vatRate,
                        category = request.corrections?.category ?: expenseData.category
                        ?: throw DokusException.BadRequest("Category is required"),
                        description = request.corrections?.description ?: expenseData.description,
                        documentId = documentId,
                        isDeductible = request.corrections?.isDeductible ?: expenseData.isDeductible,
                        deductiblePercentage = request.corrections?.deductiblePercentage
                            ?: expenseData.deductiblePercentage,
                        paymentMethod = request.corrections?.paymentMethod ?: expenseData.paymentMethod,
                        notes = request.corrections?.notes ?: expenseData.notes
                    )

                    val expense = expenseRepository.createExpense(tenantId, createRequest)
                        .getOrThrow()

                    UUID.fromString(expense.id.toString())
                }

                DocumentType.Unknown -> {
                    throw DokusException.BadRequest("Cannot confirm document with unknown type. Please specify entityType.")
                }
            }

            // Mark processing as confirmed
            val entityType = when (request.entityType) {
                DocumentType.Invoice -> EntityType.Invoice
                DocumentType.Bill -> EntityType.Bill
                DocumentType.Expense -> EntityType.Expense
                DocumentType.Unknown -> throw IllegalStateException("Should not reach here")
            }

            processingRepository.markAsConfirmed(
                processingId = processing.id,
                tenantId = tenantId,
                entityType = entityType,
                entityId = entityId
            )

            logger.info("Document confirmed: $documentId -> ${request.entityType}:$entityId")

            call.respond(
                HttpStatusCode.Created,
                ConfirmDocumentResponse(
                    entityId = entityId.toString(),
                    entityType = request.entityType,
                    processingId = processing.id.toString()
                )
            )
        }

        /**
         * PATCH /api/v1/documents/{id}/draft
         * Update extracted draft data with user corrections.
         * Preserves the original AI draft for audit trail.
         *
         * Path parameters:
         * - id: Document ID (UUID)
         *
         * Request body: UpdateDraftRequest
         * - extractedData: The updated extracted data with user corrections
         * - changeDescription: Optional description of what was changed
         *
         * Response: UpdateDraftResponse
         */
        patch<Documents.Id.Draft> { route ->
            val tenantId = dokusPrincipal.requireTenantId()
            val userId = dokusPrincipal.userId
            val documentId = DocumentId.parse(route.parent.id)

            val request = call.receive<UpdateDraftRequest>()

            logger.info("Updating draft for document: $documentId, tenant=$tenantId, user=$userId")

            // Get processing record
            val processing = processingRepository.getByDocumentId(
                documentId = documentId,
                tenantId = tenantId,
                includeDocument = false
            ) ?: throw DokusException.NotFound("Processing record not found for document")

            // Verify status allows editing (only PROCESSED documents can have drafts edited)
            if (processing.status != ProcessingStatus.Processed) {
                throw DokusException.BadRequest(
                    "Draft cannot be edited in status: ${processing.status}. Must be PROCESSED."
                )
            }

            // Build tracked corrections for audit trail
            val now = kotlinx.datetime.Clock.System.now().toString()
            val corrections = mutableListOf<TrackedCorrection>()

            // Compare old and new data to track what changed
            val oldData = processing.extractedData
            val newData = request.extractedData

            // Track changes at a high level (document type specific tracking can be enhanced later)
            if (oldData?.documentType != newData.documentType) {
                corrections.add(TrackedCorrection(
                    field = "documentType",
                    aiValue = oldData?.documentType?.name,
                    userValue = newData.documentType.name,
                    editedAt = now
                ))
            }

            // For invoices, track key field changes
            oldData?.invoice?.let { oldInvoice ->
                newData.invoice?.let { newInvoice ->
                    if (oldInvoice.clientName != newInvoice.clientName) {
                        corrections.add(TrackedCorrection("invoice.clientName", oldInvoice.clientName, newInvoice.clientName, now))
                    }
                    if (oldInvoice.invoiceNumber != newInvoice.invoiceNumber) {
                        corrections.add(TrackedCorrection("invoice.invoiceNumber", oldInvoice.invoiceNumber, newInvoice.invoiceNumber, now))
                    }
                    if (oldInvoice.totalAmount != newInvoice.totalAmount) {
                        corrections.add(TrackedCorrection("invoice.totalAmount", oldInvoice.totalAmount?.toString(), newInvoice.totalAmount?.toString(), now))
                    }
                }
            }

            // For bills, track key field changes
            oldData?.bill?.let { oldBill ->
                newData.bill?.let { newBill ->
                    if (oldBill.supplierName != newBill.supplierName) {
                        corrections.add(TrackedCorrection("bill.supplierName", oldBill.supplierName, newBill.supplierName, now))
                    }
                    if (oldBill.invoiceNumber != newBill.invoiceNumber) {
                        corrections.add(TrackedCorrection("bill.invoiceNumber", oldBill.invoiceNumber, newBill.invoiceNumber, now))
                    }
                    if (oldBill.amount != newBill.amount) {
                        corrections.add(TrackedCorrection("bill.amount", oldBill.amount?.toString(), newBill.amount?.toString(), now))
                    }
                }
            }

            // For expenses, track key field changes
            oldData?.expense?.let { oldExpense ->
                newData.expense?.let { newExpense ->
                    if (oldExpense.merchant != newExpense.merchant) {
                        corrections.add(TrackedCorrection("expense.merchant", oldExpense.merchant, newExpense.merchant, now))
                    }
                    if (oldExpense.amount != newExpense.amount) {
                        corrections.add(TrackedCorrection("expense.amount", oldExpense.amount?.toString(), newExpense.amount?.toString(), now))
                    }
                    if (oldExpense.category != newExpense.category) {
                        corrections.add(TrackedCorrection("expense.category", oldExpense.category?.name, newExpense.category?.name, now))
                    }
                }
            }

            // Update the draft
            val newVersion = processingRepository.updateDraft(
                processingId = processing.id,
                tenantId = tenantId,
                userId = userId,
                updatedData = request.extractedData,
                corrections = corrections
            ) ?: throw DokusException.InternalError("Failed to update draft")

            logger.info("Draft updated: document=$documentId, version=$newVersion, corrections=${corrections.size}")

            call.respond(
                HttpStatusCode.OK,
                UpdateDraftResponse(
                    processingId = processing.id.toString(),
                    draftVersion = newVersion,
                    extractedData = request.extractedData,
                    updatedAt = now
                )
            )
        }

        /**
         * POST /api/v1/documents/{id}/processing-jobs
         * Trigger re-extraction of document (creates a new processing job).
         *
         * Path parameters:
         * - id: Document ID (UUID)
         *
         * Request body (optional): ReprocessDocumentRequest
         * - force: Force reprocessing even if already processed
         * - preferredProvider: Preferred AI provider to use
         *
         * Response: ReprocessDocumentResponse
         */
        post<Documents.Id.ProcessingJobs> { route ->
            val tenantId = dokusPrincipal.requireTenantId()
            val documentId = DocumentId.parse(route.parent.id)

            val request = try {
                call.receive<ReprocessDocumentRequest>()
            } catch (e: Exception) {
                ReprocessDocumentRequest()
            }

            logger.info("Reprocessing document: $documentId, force=${request.force}, tenant=$tenantId")

            // Get processing record
            val processing = processingRepository.getByDocumentId(
                documentId = documentId,
                tenantId = tenantId,
                includeDocument = false
            ) ?: throw DokusException.NotFound("Processing record not found for document")

            // Check if reprocessing is allowed
            val allowedStatuses = if (request.force) {
                listOf(
                    ProcessingStatus.Pending,
                    ProcessingStatus.Processed,
                    ProcessingStatus.Failed,
                    ProcessingStatus.Rejected
                )
            } else {
                listOf(ProcessingStatus.Failed, ProcessingStatus.Rejected)
            }

            if (processing.status !in allowedStatuses) {
                throw DokusException.BadRequest(
                    "Document cannot be reprocessed in status: ${processing.status}. " +
                            if (request.force) "Already in final state." else "Use force=true to override."
                )
            }

            // Reset for reprocessing
            processingRepository.resetForReprocessing(processing.id, tenantId)

            call.respond(
                HttpStatusCode.OK,
                ReprocessDocumentResponse(
                    processingId = processing.id,
                    status = ProcessingStatus.Pending,
                    message = "Document queued for reprocessing"
                )
            )
        }
    }
}
