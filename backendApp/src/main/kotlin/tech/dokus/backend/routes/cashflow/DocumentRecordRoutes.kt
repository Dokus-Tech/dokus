package tech.dokus.backend.routes.cashflow

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.resources.delete
import io.ktor.server.resources.get
import io.ktor.server.resources.patch
import io.ktor.server.resources.post
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.ktor.ext.inject
import org.slf4j.LoggerFactory
import tech.dokus.backend.services.documents.DocumentConfirmationService
import tech.dokus.database.repository.cashflow.BillRepository
import tech.dokus.database.repository.cashflow.CashflowEntriesRepository
import tech.dokus.database.repository.cashflow.DocumentDraftRepository
import tech.dokus.database.repository.cashflow.DocumentIngestionRunRepository
import tech.dokus.database.repository.cashflow.DocumentRepository
import tech.dokus.database.repository.cashflow.DraftSummary
import tech.dokus.database.repository.cashflow.ExpenseRepository
import tech.dokus.database.repository.cashflow.IngestionRunSummary
import tech.dokus.database.repository.cashflow.InvoiceRepository
import tech.dokus.domain.Money
import tech.dokus.domain.VatRate
import tech.dokus.domain.enums.CounterpartyIntent
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.enums.DraftStatus
import tech.dokus.domain.enums.IngestionStatus
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.model.ConfirmDocumentRequest
import tech.dokus.domain.model.DocumentDraftDto
import tech.dokus.domain.model.DocumentDto
import tech.dokus.domain.model.DocumentIngestionDto
import tech.dokus.domain.model.DocumentRecordDto
import tech.dokus.domain.model.FinancialDocumentDto
import tech.dokus.domain.model.RejectDocumentRequest
import tech.dokus.domain.model.ReprocessRequest
import tech.dokus.domain.model.ReprocessResponse
import tech.dokus.domain.model.TrackedCorrection
import tech.dokus.domain.model.UpdateDraftRequest
import tech.dokus.domain.model.UpdateDraftResponse
import tech.dokus.domain.model.common.PaginatedResponse
import tech.dokus.domain.routes.Documents
import tech.dokus.foundation.backend.security.authenticateJwt
import tech.dokus.foundation.backend.security.dokusPrincipal
import tech.dokus.foundation.backend.storage.DocumentStorageService as MinioDocumentStorageService

/**
 * Document record routes using new canonical API.
 *
 * Endpoints:
 * - GET /api/v1/documents - List documents with filters
 * - GET /api/v1/documents/{id} - Get full DocumentRecordDto
 * - DELETE /api/v1/documents/{id} - Delete document (cascades)
 * - GET /api/v1/documents/{id}/draft - Get draft
 * - PATCH /api/v1/documents/{id}/draft - Update draft
 * - GET /api/v1/documents/{id}/ingestions - Get ingestion history
 * - POST /api/v1/documents/{id}/reprocess - Reprocess document (idempotent)
 * - POST /api/v1/documents/{id}/confirm - Confirm and create entity (transactional + idempotent)
 */
internal fun Route.documentRecordRoutes() {
    val documentRepository by inject<DocumentRepository>()
    val draftRepository by inject<DocumentDraftRepository>()
    val ingestionRepository by inject<DocumentIngestionRunRepository>()
    val invoiceRepository by inject<InvoiceRepository>()
    val expenseRepository by inject<ExpenseRepository>()
    val billRepository by inject<BillRepository>()
    val cashflowEntriesRepository by inject<CashflowEntriesRepository>()
    val minioStorage by inject<MinioDocumentStorageService>()
    val documentConfirmationService by inject<DocumentConfirmationService>()
    val logger = LoggerFactory.getLogger("DocumentRecordRoutes")

    authenticateJwt {
        /**
         * GET /api/v1/documents
         * List documents with filters and pagination.
         * Now document-centric: includes documents without drafts (queued/processing/failed).
         */
        get<Documents.Paginated> { route ->
            val tenantId = dokusPrincipal.requireTenantId()
            val page = route.page.coerceAtLeast(0)
            val limit = route.limit.coerceIn(1, 100)

            logger.info("Listing documents: tenant=$tenantId, page=$page, limit=$limit")

            // Query documents with optional drafts and ingestion info
            val (documentsWithInfo, total) = documentRepository.listWithDraftsAndIngestion(
                tenantId = tenantId,
                draftStatus = route.draftStatus,
                documentType = route.documentType,
                ingestionStatus = route.ingestionStatus,
                search = route.search,
                page = page,
                limit = limit
            )

            // Build full records
            val records = documentsWithInfo.map { docInfo ->
                val documentWithUrl = addDownloadUrl(docInfo.document, minioStorage, logger)
                val draft = docInfo.draft
                val confirmedEntity = if (draft?.draftStatus == DraftStatus.Confirmed) {
                    findConfirmedEntity(
                        docInfo.document.id,
                        draft.documentType,
                        tenantId,
                        invoiceRepository,
                        billRepository,
                        expenseRepository
                    )
                } else {
                    null
                }

                DocumentRecordDto(
                    document = documentWithUrl,
                    draft = draft?.toDto(),
                    latestIngestion = docInfo.latestIngestion?.toDto(),
                    confirmedEntity = confirmedEntity
                )
            }

            call.respond(
                HttpStatusCode.OK,
                PaginatedResponse(
                    items = records,
                    total = total,
                    limit = limit,
                    offset = page * limit
                )
            )
        }

        /**
         * GET /api/v1/documents/{id}
         * Get full document record.
         */
        get<Documents.Id> { route ->
            val tenantId = dokusPrincipal.requireTenantId()
            val documentId = DocumentId.parse(route.id)

            logger.info("Getting document record: $documentId, tenant=$tenantId")

            val document = documentRepository.getById(tenantId, documentId)
                ?: throw DokusException.NotFound("Document not found")

            val documentWithUrl = addDownloadUrl(document, minioStorage, logger)
            val draft = draftRepository.getByDocumentId(documentId, tenantId)
            val latestIngestion = ingestionRepository.getLatestForDocument(documentId, tenantId)
            val confirmedEntity = if (draft?.draftStatus == DraftStatus.Confirmed) {
                findConfirmedEntity(
                    documentId,
                    draft.documentType,
                    tenantId,
                    invoiceRepository,
                    billRepository,
                    expenseRepository
                )
            } else {
                null
            }

            call.respond(
                HttpStatusCode.OK,
                DocumentRecordDto(
                    document = documentWithUrl,
                    draft = draft?.toDto(),
                    latestIngestion = latestIngestion?.toDto(),
                    confirmedEntity = confirmedEntity
                )
            )
        }

        /**
         * DELETE /api/v1/documents/{id}
         * Delete document (cascades to drafts, ingestion runs, chunks).
         */
        delete<Documents.Id> { route ->
            val tenantId = dokusPrincipal.requireTenantId()
            val documentId = DocumentId.parse(route.id)

            logger.info("Deleting document: $documentId, tenant=$tenantId")

            // Check if document exists
            if (!documentRepository.exists(tenantId, documentId)) {
                throw DokusException.NotFound("Document not found")
            }

            // Get storage key for MinIO cleanup
            val document = documentRepository.getById(tenantId, documentId)
            val storageKey = document?.storageKey

            // Delete document (cascades to drafts, runs, chunks)
            documentRepository.delete(tenantId, documentId)

            // Delete from MinIO
            if (storageKey != null) {
                try {
                    minioStorage.deleteDocument(storageKey)
                } catch (e: Exception) {
                    logger.warn("Failed to delete document from storage: $storageKey", e)
                }
            }

            call.respond(HttpStatusCode.NoContent)
        }

        /**
         * GET /api/v1/documents/{id}/draft
         * Get draft details.
         */
        get<Documents.Id.Draft> { route ->
            val tenantId = dokusPrincipal.requireTenantId()
            val documentId = DocumentId.parse(route.parent.id)

            logger.info("Getting draft: $documentId, tenant=$tenantId")

            val draft = draftRepository.getByDocumentId(documentId, tenantId)
                ?: throw DokusException.NotFound("Draft not found for document")

            call.respond(HttpStatusCode.OK, draft.toDto())
        }

        /**
         * PATCH /api/v1/documents/{id}/draft
         * Update draft with user corrections.
         */
        patch<Documents.Id.Draft> { route ->
            val tenantId = dokusPrincipal.requireTenantId()
            val userId = dokusPrincipal.userId
            val documentId = DocumentId.parse(route.parent.id)

            val request = call.receive<UpdateDraftRequest>()

            logger.info("Updating draft: $documentId, tenant=$tenantId, user=$userId")

            val draft = draftRepository.getByDocumentId(documentId, tenantId)
                ?: throw DokusException.NotFound("Draft not found for document")

            val requestData = request.extractedData
            val hasExtractedData = requestData != null
            val hasContactUpdate = request.contactId != null || request.counterpartyIntent != null

            if (!hasExtractedData && !hasContactUpdate) {
                throw DokusException.BadRequest("No draft changes provided")
            }

            if (draft.draftStatus == DraftStatus.Rejected) {
                throw DokusException.BadRequest("Cannot edit rejected draft")
            }

            if (hasExtractedData) {
                val updatedData = requestData ?: throw DokusException.BadRequest("No extracted data provided")
                // Build tracked corrections
                val now = kotlinx.datetime.Clock.System.now().toString()
                val corrections = buildCorrections(draft.extractedData, updatedData, now)

                // Update draft (may transition Confirmed -> NeedsReview)
                val newVersion = draftRepository.updateDraft(
                    documentId = documentId,
                    tenantId = tenantId,
                    userId = userId,
                    updatedData = updatedData,
                    corrections = corrections
                ) ?: throw DokusException.InternalError("Failed to update draft")

                logger.info("Draft updated: document=$documentId, version=$newVersion")

                if (hasContactUpdate) {
                    updateDraftCounterparty(draftRepository, documentId, tenantId, request)
                }

                call.respond(
                    HttpStatusCode.OK,
                    UpdateDraftResponse(
                        documentId = documentId,
                        draftVersion = newVersion,
                        extractedData = updatedData,
                        updatedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC)
                    )
                )
            } else if (hasContactUpdate) {
                updateDraftCounterparty(draftRepository, documentId, tenantId, request)
                call.respond(HttpStatusCode.NoContent)
            }
        }

        /**
         * GET /api/v1/documents/{id}/ingestions
         * Get ingestion run history.
         */
        get<Documents.Id.Ingestions> { route ->
            val tenantId = dokusPrincipal.requireTenantId()
            val documentId = DocumentId.parse(route.parent.id)

            logger.info("Getting ingestion history: $documentId, tenant=$tenantId")

            val runs = ingestionRepository.listByDocument(documentId, tenantId)

            call.respond(HttpStatusCode.OK, runs.map { it.toDto() })
        }

        /**
         * POST /api/v1/documents/{id}/reprocess
         * Reprocess document. IDEMPOTENT: returns existing Queued/Processing run unless force=true.
         */
        post<Documents.Id.Reprocess> { route ->
            val tenantId = dokusPrincipal.requireTenantId()
            val documentId = DocumentId.parse(route.parent.id)

            val request = try {
                call.receive<ReprocessRequest>()
            } catch (e: Exception) {
                ReprocessRequest()
            }

            logger.info(
                "Reprocessing document: $documentId, force=${request.force}, " +
                    "overrides=[maxPages=${request.maxPages}, dpi=${request.dpi}, " +
                    "timeout=${request.timeoutSeconds}s], tenant=$tenantId"
            )

            // Check document exists
            if (!documentRepository.exists(tenantId, documentId)) {
                throw DokusException.NotFound("Document not found")
            }

            // Check for existing active run (idempotent)
            if (!request.force) {
                val activeRun = ingestionRepository.findActiveRun(documentId, tenantId)
                if (activeRun != null) {
                    call.respond(
                        HttpStatusCode.OK,
                        ReprocessResponse(
                            runId = activeRun.id,
                            status = activeRun.status,
                            message = "Existing ${activeRun.status.name.lowercase()} run found",
                            isExistingRun = true
                        )
                    )
                    return@post
                }
            }

            // Create new ingestion run with optional overrides
            val runId = ingestionRepository.createRun(
                documentId = documentId,
                tenantId = tenantId,
                overrideMaxPages = request.maxPages,
                overrideDpi = request.dpi,
                overrideTimeoutSeconds = request.timeoutSeconds
            )

            call.respond(
                HttpStatusCode.Created,
                ReprocessResponse(
                    runId = runId,
                    status = IngestionStatus.Queued,
                    message = "Document queued for processing",
                    isExistingRun = false
                )
            )
        }

        /**
         * POST /api/v1/documents/{id}/confirm
         * Confirm and create financial entity.
         * TRANSACTIONAL + IDEMPOTENT: fails if entity already exists for documentId.
         */
        post<Documents.Id.Confirm> { route ->
            val tenantId = dokusPrincipal.requireTenantId()
            val documentId = DocumentId.parse(route.parent.id)
            val userId = dokusPrincipal.userId

            val request = call.receive<ConfirmDocumentRequest>()

            logger.info("Confirming document: $documentId, type=${request.documentType}, tenant=$tenantId")

            val draft = draftRepository.getByDocumentId(documentId, tenantId)
                ?: throw DokusException.NotFound("Draft not found for document")

            if (draft.draftStatus == DraftStatus.Rejected) {
                throw DokusException.BadRequest("Cannot confirm a rejected document")
            }

            // Check if already confirmed (idempotent)
            if (draft.draftStatus == DraftStatus.Confirmed) {
                // Return existing confirmed entity
                val confirmedEntity = findConfirmedEntity(
                    documentId,
                    draft.documentType,
                    tenantId,
                    invoiceRepository,
                    billRepository,
                    expenseRepository
                )
                    ?: throw DokusException.InternalError("Draft is confirmed but entity not found")

                val document = documentRepository.getById(tenantId, documentId)!!
                val documentWithUrl = addDownloadUrl(document, minioStorage, logger)
                val latestIngestion = ingestionRepository.getLatestForDocument(documentId, tenantId)

                // Look up the cashflow entry for this document
                val cashflowEntry = cashflowEntriesRepository.getByDocumentId(tenantId, documentId)
                    .getOrNull()

                call.respond(
                    HttpStatusCode.OK,
                    DocumentRecordDto(
                        document = documentWithUrl,
                        draft = draft.toDto(),
                        latestIngestion = latestIngestion?.toDto(),
                        confirmedEntity = confirmedEntity,
                        cashflowEntryId = cashflowEntry?.id
                    )
                )
                return@post
            }

            // Check draft is ready
            if (draft.draftStatus != DraftStatus.NeedsReview && draft.draftStatus != DraftStatus.Ready) {
                throw DokusException.BadRequest("Draft is not ready for confirmation: ${draft.draftStatus}")
            }

            if (draft.counterpartyIntent == CounterpartyIntent.Pending) {
                throw DokusException.BadRequest("Counterparty is pending creation")
            }

            val resolvedType = request.documentType
            if (resolvedType == DocumentType.Unknown) {
                throw DokusException.BadRequest("Document type must be resolved before confirmation")
            }
            if (draft.documentType != null && draft.documentType != resolvedType) {
                throw DokusException.BadRequest("Document type mismatch with draft")
            }

            val requestData = request.extractedData
            if (requestData != null && requestData != draft.extractedData) {
                val now = Clock.System.now().toString()
                val corrections = buildCorrections(draft.extractedData, requestData, now)
                val updatedVersion = draftRepository.updateDraft(
                    documentId = documentId,
                    tenantId = tenantId,
                    userId = userId,
                    updatedData = requestData,
                    corrections = corrections
                ) ?: throw DokusException.InternalError("Failed to persist confirmed data")
                logger.info("Draft updated before confirmation: document=$documentId, version=$updatedVersion")
            }

            val extractedData = requestData ?: draft.extractedData
                ?: throw DokusException.BadRequest("No extracted data available for confirmation")

            // Check if entity already exists for this document (idempotent check)
            val existingEntity = findConfirmedEntity(
                documentId,
                resolvedType,
                tenantId,
                invoiceRepository,
                billRepository,
                expenseRepository
            )
            if (existingEntity != null) {
                throw DokusException.BadRequest("Entity already exists for this document")
            }

            // Confirm document: creates entity + cashflow entry + marks draft confirmed
            val confirmationResult = documentConfirmationService.confirmDocument(
                tenantId = tenantId,
                documentId = documentId,
                documentType = resolvedType,
                extractedData = extractedData,
                linkedContactId = draft.linkedContactId
            ).getOrThrow()

            logger.info("Document confirmed: $documentId -> $resolvedType, cashflowEntryId=${confirmationResult.cashflowEntryId}")

            // Return full record
            val document = documentRepository.getById(tenantId, documentId)!!
            val documentWithUrl = addDownloadUrl(document, minioStorage, logger)
            val updatedDraft = draftRepository.getByDocumentId(documentId, tenantId)!!
            val latestIngestion = ingestionRepository.getLatestForDocument(documentId, tenantId)

            call.respond(
                HttpStatusCode.Created,
                DocumentRecordDto(
                    document = documentWithUrl,
                    draft = updatedDraft.toDto(),
                    latestIngestion = latestIngestion?.toDto(),
                    confirmedEntity = confirmationResult.entity,
                    cashflowEntryId = confirmationResult.cashflowEntryId
                )
            )
        }

        /**
         * POST /api/v1/documents/{id}/reject
         * Reject a document draft with a reason.
         * IDEMPOTENT: if already rejected, returns existing record.
         */
        post<Documents.Id.Reject> { route ->
            val tenantId = dokusPrincipal.requireTenantId()
            val documentId = DocumentId.parse(route.parent.id)
            val request = call.receive<RejectDocumentRequest>()

            val draft = draftRepository.getByDocumentId(documentId, tenantId)
                ?: throw DokusException.NotFound("Draft not found for document")

            if (draft.draftStatus == DraftStatus.Confirmed) {
                throw DokusException.BadRequest("Cannot reject a confirmed document")
            }

            if (draft.draftStatus != DraftStatus.Rejected) {
                draftRepository.rejectDraft(documentId, tenantId, request.reason)
            }

            val document = documentRepository.getById(tenantId, documentId)
                ?: throw DokusException.NotFound("Document not found")
            val documentWithUrl = addDownloadUrl(document, minioStorage, logger)
            val updatedDraft = draftRepository.getByDocumentId(documentId, tenantId)!!
            val latestIngestion = ingestionRepository.getLatestForDocument(documentId, tenantId)

            call.respond(
                HttpStatusCode.OK,
                DocumentRecordDto(
                    document = documentWithUrl,
                    draft = updatedDraft.toDto(),
                    latestIngestion = latestIngestion?.toDto(),
                    confirmedEntity = null
                )
            )
        }
    }
}

// Helper functions

private suspend fun addDownloadUrl(
    document: DocumentDto,
    minioStorage: MinioDocumentStorageService,
    logger: org.slf4j.Logger
): DocumentDto {
    val downloadUrl = try {
        minioStorage.getDownloadUrl(document.storageKey)
    } catch (e: Exception) {
        logger.warn("Failed to get download URL for ${document.storageKey}: ${e.message}")
        null
    }
    return document.copy(downloadUrl = downloadUrl)
}

private fun validateInvoiceData(data: tech.dokus.domain.model.ExtractedInvoiceFields) {
    val error = firstInvoiceValidationError(data) ?: return
    throw DokusException.BadRequest(error)
}

private fun firstInvoiceValidationError(
    data: tech.dokus.domain.model.ExtractedInvoiceFields
): String? {
    if (data.issueDate == null) {
        return "Issue date is required"
    }
    val subtotal = data.subtotalAmount ?: return "Subtotal is required"
    val total = data.totalAmount
    val vat = data.vatAmount
    if (vat != null && total != null) {
        val expected = subtotal + vat
        val diff = kotlin.math.abs(expected.minor - total.minor)
        if (diff > 1L) {
            return "Amounts are inconsistent"
        }
    }
    return null
}

private fun buildInvoiceItems(
    data: tech.dokus.domain.model.ExtractedInvoiceFields
): List<tech.dokus.domain.model.InvoiceItemDto> {
    val items = data.items
        ?.mapIndexedNotNull { index, item -> buildInvoiceItem(item, index) }
        .orEmpty()

    if (items.isNotEmpty()) return items

    return listOf(buildFallbackInvoiceItem(data))
}

private fun buildInvoiceItem(
    item: tech.dokus.domain.model.ExtractedLineItem,
    index: Int
): tech.dokus.domain.model.InvoiceItemDto? {
    val quantity = item.quantity ?: 1.0
    val lineTotal = resolveLineTotal(item, quantity) ?: return null
    val vatAmount = resolveVatAmount(item, lineTotal)
    val vatRate = resolveVatRate(item, lineTotal, vatAmount)
    val finalVatAmount = vatAmount ?: VatRate.ZERO.applyTo(lineTotal)
    val unitPrice = item.unitPrice ?: Money.fromDouble(lineTotal.toDouble() / quantity)

    return tech.dokus.domain.model.InvoiceItemDto(
        description = item.description ?: "Line item ${index + 1}",
        quantity = quantity,
        unitPrice = unitPrice,
        vatRate = vatRate,
        lineTotal = lineTotal,
        vatAmount = finalVatAmount,
        sortOrder = index
    )
}

private fun resolveLineTotal(
    item: tech.dokus.domain.model.ExtractedLineItem,
    quantity: Double
): Money? {
    return item.lineTotal ?: item.unitPrice?.let { unit ->
        Money.fromDouble(unit.toDouble() * quantity)
    }
}

private fun resolveVatAmount(
    item: tech.dokus.domain.model.ExtractedLineItem,
    lineTotal: Money
): Money? {
    return item.vatAmount ?: item.vatRate?.let { rate -> rate.applyTo(lineTotal) }
}

private fun resolveVatRate(
    item: tech.dokus.domain.model.ExtractedLineItem,
    lineTotal: Money,
    vatAmount: Money?
): VatRate {
    val explicitRate = item.vatRate
    if (explicitRate != null) return explicitRate
    if (vatAmount == null || lineTotal.minor == 0L) return VatRate.ZERO
    return VatRate.fromMultiplier(vatAmount.toDouble() / lineTotal.toDouble())
}

private fun buildFallbackInvoiceItem(
    data: tech.dokus.domain.model.ExtractedInvoiceFields
): tech.dokus.domain.model.InvoiceItemDto {
    val subtotal = data.subtotalAmount
        ?: throw DokusException.BadRequest("Subtotal is required")
    val vatAmount = data.vatAmount ?: Money.ZERO
    val vatRate = if (subtotal.minor != 0L && vatAmount.minor != 0L) {
        VatRate.fromMultiplier(vatAmount.toDouble() / subtotal.toDouble())
    } else {
        VatRate.ZERO
    }

    return tech.dokus.domain.model.InvoiceItemDto(
        description = data.invoiceNumber ?: "Document total",
        quantity = 1.0,
        unitPrice = subtotal,
        vatRate = vatRate,
        lineTotal = subtotal,
        vatAmount = vatAmount,
        sortOrder = 0
    )
}

internal suspend fun findConfirmedEntity(
    documentId: DocumentId,
    documentType: DocumentType?,
    tenantId: tech.dokus.domain.ids.TenantId,
    invoiceRepository: InvoiceRepository,
    billRepository: BillRepository,
    expenseRepository: ExpenseRepository
): FinancialDocumentDto? {
    return when (documentType) {
        DocumentType.Invoice -> invoiceRepository.findByDocumentId(tenantId, documentId)
        DocumentType.Bill -> billRepository.findByDocumentId(tenantId, documentId)
        DocumentType.Expense -> expenseRepository.findByDocumentId(tenantId, documentId)
        else -> {
            // Try all types
            invoiceRepository.findByDocumentId(tenantId, documentId)
                ?: billRepository.findByDocumentId(tenantId, documentId)
                ?: expenseRepository.findByDocumentId(tenantId, documentId)
        }
    }
}

private fun buildCorrections(
    oldData: tech.dokus.domain.model.ExtractedDocumentData?,
    newData: tech.dokus.domain.model.ExtractedDocumentData,
    now: String
): List<TrackedCorrection> {
    val corrections = mutableListOf<TrackedCorrection>()

    if (oldData?.documentType != newData.documentType) {
        corrections.add(
            TrackedCorrection(
                "documentType",
                oldData?.documentType?.name,
                newData.documentType.name,
                now
            )
        )
    }

    // Invoice fields
    oldData?.invoice?.let { old ->
        newData.invoice?.let { new ->
            if (old.clientName != new.clientName) {
                corrections.add(
                    TrackedCorrection(
                        "invoice.clientName",
                        old.clientName,
                        new.clientName,
                        now
                    )
                )
            }
            if (old.invoiceNumber != new.invoiceNumber) {
                corrections.add(
                    TrackedCorrection(
                        "invoice.invoiceNumber",
                        old.invoiceNumber,
                        new.invoiceNumber,
                        now
                    )
                )
            }
            if (old.totalAmount != new.totalAmount) {
                corrections.add(
                    TrackedCorrection(
                        "invoice.totalAmount",
                        old.totalAmount?.toString(),
                        new.totalAmount?.toString(),
                        now
                    )
                )
            }
        }
    }

    // Bill fields
    oldData?.bill?.let { old ->
        newData.bill?.let { new ->
            if (old.supplierName != new.supplierName) {
                corrections.add(
                    TrackedCorrection(
                        "bill.supplierName",
                        old.supplierName,
                        new.supplierName,
                        now
                    )
                )
            }
            if (old.invoiceNumber != new.invoiceNumber) {
                corrections.add(
                    TrackedCorrection(
                        "bill.invoiceNumber",
                        old.invoiceNumber,
                        new.invoiceNumber,
                        now
                    )
                )
            }
            if (old.amount != new.amount) {
                corrections.add(
                    TrackedCorrection(
                        "bill.amount",
                        old.amount?.toString(),
                        new.amount?.toString(),
                        now
                    )
                )
            }
        }
    }

    // Expense fields
    oldData?.expense?.let { old ->
        newData.expense?.let { new ->
            if (old.merchant != new.merchant) {
                corrections.add(
                    TrackedCorrection(
                        "expense.merchant",
                        old.merchant,
                        new.merchant,
                        now
                    )
                )
            }
            if (old.amount != new.amount) {
                corrections.add(
                    TrackedCorrection(
                        "expense.amount",
                        old.amount?.toString(),
                        new.amount?.toString(),
                        now
                    )
                )
            }
            if (old.category != new.category) {
                corrections.add(
                    TrackedCorrection(
                        "expense.category",
                        old.category?.name,
                        new.category?.name,
                        now
                    )
                )
            }
        }
    }

    return corrections
}

// Extension functions

internal fun DraftSummary.toDto(): DocumentDraftDto = DocumentDraftDto(
    documentId = documentId,
    tenantId = tenantId,
    draftStatus = draftStatus,
    documentType = documentType,
    extractedData = extractedData,
    aiDraftData = aiDraftData,
    aiDraftSourceRunId = aiDraftSourceRunId,
    draftVersion = draftVersion,
    draftEditedAt = draftEditedAt,
    draftEditedBy = draftEditedBy,
    suggestedContactId = suggestedContactId,
    contactSuggestionConfidence = contactSuggestionConfidence,
    contactSuggestionReason = contactSuggestionReason,
    linkedContactId = linkedContactId,
    counterpartyIntent = counterpartyIntent,
    rejectReason = rejectReason,
    lastSuccessfulRunId = lastSuccessfulRunId,
    createdAt = createdAt,
    updatedAt = updatedAt
)

internal fun IngestionRunSummary.toDto(): DocumentIngestionDto = DocumentIngestionDto(
    id = id,
    documentId = documentId,
    tenantId = tenantId,
    status = status,
    provider = provider,
    queuedAt = queuedAt,
    startedAt = startedAt,
    finishedAt = finishedAt,
    errorMessage = errorMessage,
    confidence = confidence
)

private suspend fun updateDraftCounterparty(
    draftRepository: DocumentDraftRepository,
    documentId: DocumentId,
    tenantId: tech.dokus.domain.ids.TenantId,
    request: UpdateDraftRequest
) {
    val contactId = request.contactId?.let { tech.dokus.domain.ids.ContactId.parse(it) }
    val updated = draftRepository.updateCounterparty(
        documentId = documentId,
        tenantId = tenantId,
        contactId = contactId,
        intent = request.counterpartyIntent
    )
    if (!updated) {
        throw DokusException.InternalError("Failed to update document counterparty")
    }
}
