package tech.dokus.backend.routes.cashflow

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.resources.*
import io.ktor.server.resources.patch
import io.ktor.server.resources.post
import io.ktor.server.response.*
import io.ktor.server.routing.Route
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.ktor.ext.inject
import org.slf4j.LoggerFactory
import tech.dokus.backend.routes.cashflow.documents.addDownloadUrl
import tech.dokus.backend.routes.cashflow.documents.findConfirmedEntity
import tech.dokus.backend.routes.cashflow.documents.toDto
import tech.dokus.backend.routes.cashflow.documents.updateDraftCounterparty
import tech.dokus.backend.services.cashflow.CashflowProjectionReconciliationService
import tech.dokus.backend.services.documents.confirmation.DocumentConfirmationDispatcher
import tech.dokus.database.repository.cashflow.CashflowEntriesRepository
import tech.dokus.database.repository.cashflow.CreditNoteRepository
import tech.dokus.database.repository.cashflow.DocumentDraftRepository
import tech.dokus.database.repository.cashflow.DocumentIngestionRunRepository
import tech.dokus.database.repository.cashflow.DocumentRepository
import tech.dokus.database.repository.cashflow.ExpenseRepository
import tech.dokus.database.repository.cashflow.InvoiceRepository
import tech.dokus.domain.enums.CounterpartyIntent
import tech.dokus.domain.enums.DocumentStatus
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.enums.IngestionStatus
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.model.DocumentRecordDto
import tech.dokus.domain.model.FinancialDocumentDto
import tech.dokus.domain.model.RejectDocumentRequest
import tech.dokus.domain.model.ReprocessRequest
import tech.dokus.domain.model.ReprocessResponse
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
 * - POST /api/v1/documents/{id}/confirm - Confirm using latest draft (transactional + idempotent)
 */
internal fun Route.documentRecordRoutes() {
    val documentRepository by inject<DocumentRepository>()
    val draftRepository by inject<DocumentDraftRepository>()
    val ingestionRepository by inject<DocumentIngestionRunRepository>()
    val invoiceRepository by inject<InvoiceRepository>()
    val expenseRepository by inject<ExpenseRepository>()
    val creditNoteRepository by inject<CreditNoteRepository>()
    val cashflowEntriesRepository by inject<CashflowEntriesRepository>()
    val projectionReconciliationService by inject<CashflowProjectionReconciliationService>()
    val minioStorage by inject<MinioDocumentStorageService>()
    val confirmationDispatcher by inject<DocumentConfirmationDispatcher>()
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
            val filter = route.filter

            logger.info("Listing documents: tenant=$tenantId, filter=$filter, page=$page, limit=$limit")

            if (filter != null && (route.documentStatus != null || route.ingestionStatus != null)) {
                throw DokusException.BadRequest("Do not combine 'filter' with 'documentStatus' or 'ingestionStatus'")
            }

            // Query documents with optional drafts and ingestion info
            val (documentsWithInfo, total) = documentRepository.listWithDraftsAndIngestion(
                tenantId = tenantId,
                filter = filter,
                documentStatus = route.documentStatus,
                documentType = route.documentType,
                ingestionStatus = route.ingestionStatus,
                search = route.search,
                page = page,
                limit = limit
            )

            val confirmedDocumentIds = documentsWithInfo
                .filter { it.draft?.documentStatus == DocumentStatus.Confirmed }
                .map { it.document.id }
            val cashflowEntryIdsByDocumentId = cashflowEntriesRepository
                .getIdsByDocumentIds(tenantId, confirmedDocumentIds)
                .getOrThrow()

            // Build full records
            val records = documentsWithInfo.map { docInfo ->
                val documentWithUrl = addDownloadUrl(docInfo.document, minioStorage, logger)
                val draft = docInfo.draft
                val confirmedEntity = if (draft?.documentStatus == DocumentStatus.Confirmed) {
                    findConfirmedEntity(
                        docInfo.document.id,
                        draft.documentType,
                        tenantId,
                        invoiceRepository,
                        expenseRepository,
                        creditNoteRepository
                    )
                } else {
                    null
                }

                DocumentRecordDto(
                    document = documentWithUrl,
                    draft = draft?.toDto(),
                    latestIngestion = docInfo.latestIngestion?.toDto(),
                    confirmedEntity = confirmedEntity,
                    cashflowEntryId = cashflowEntryIdsByDocumentId[docInfo.document.id]
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
            val confirmedEntity = if (draft?.documentStatus == DocumentStatus.Confirmed) {
                findConfirmedEntity(
                    documentId,
                    draft.documentType,
                    tenantId,
                    invoiceRepository,
                    expenseRepository,
                    creditNoteRepository
                )
            } else {
                null
            }
            val cashflowEntryId = if (draft?.documentStatus == DocumentStatus.Confirmed) {
                cashflowEntriesRepository.getByDocumentId(tenantId, documentId).getOrNull()?.id
            } else {
                null
            }

            call.respond(
                HttpStatusCode.OK,
                DocumentRecordDto(
                    document = documentWithUrl,
                    draft = draft?.toDto(),
                    latestIngestion = latestIngestion?.toDto(
                        includeRawExtraction = true,
                        includeTrace = true
                    ),
                    confirmedEntity = confirmedEntity,
                    cashflowEntryId = cashflowEntryId
                )
            )
        }

        /**
         * GET /api/v1/documents/{id}/content
         * Download raw document bytes through authenticated API.
         */
        get<Documents.Id.Content> { route ->
            val tenantId = dokusPrincipal.requireTenantId()
            val documentId = DocumentId.parse(route.parent.id)

            logger.info("Downloading document content: $documentId, tenant=$tenantId")

            val document = documentRepository.getById(tenantId, documentId)
                ?: throw DokusException.NotFound("Document not found")

            val stream = try {
                minioStorage.openDocumentStream(document.storageKey)
            } catch (e: Exception) {
                logger.error("Failed to open document stream: $documentId", e)
                throw DokusException.InternalError("Failed to download document content")
            }

            val contentType = runCatching { ContentType.parse(document.contentType) }
                .getOrDefault(ContentType.Application.OctetStream)

            call.response.header(
                HttpHeaders.ContentDisposition,
                ContentDisposition.Attachment
                    .withParameter(ContentDisposition.Parameters.FileName, document.filename)
                    .toString()
            )
            call.respondOutputStream(contentType = contentType, status = HttpStatusCode.OK) {
                stream.use { input ->
                    input.copyTo(this)
                }
            }
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

            val latestIngestion = ingestionRepository.getLatestForDocument(documentId, tenantId)
            if (!isInboxLifecycle(latestIngestion?.status)) {
                throw DokusException.BadRequest(
                    "Only Inbox documents in Queued or Processing state can be deleted"
                )
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

            if (draft.documentStatus == DocumentStatus.Rejected) {
                throw DokusException.BadRequest("Cannot edit rejected draft")
            }

            if (hasExtractedData) {
                // Update draft (may transition Confirmed -> NeedsReview)
                val newVersion = draftRepository.updateDraft(
                    documentId = documentId,
                    tenantId = tenantId,
                    userId = userId,
                    updatedData = requestData
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
                        extractedData = requestData,
                        updatedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC)
                    )
                )
            } else {
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

            call.respond(
                HttpStatusCode.OK,
                runs.map { it.toDto(includeRawExtraction = true, includeTrace = true) }
            )
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
                userFeedback = request.userFeedback,
                overrideMaxPages = request.maxPages,
                overrideDpi = request.dpi,
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
         * Confirm using the latest draft and create financial entity.
         * IDEMPOTENT + RECONFIRM-SAFE:
         * - If already confirmed and entity exists, returns existing record.
         * - If draft was edited after confirmation, re-confirm updates the existing entity + projection (when allowed).
         */
        post<Documents.Id.Confirm> { route ->
            val tenantId = dokusPrincipal.requireTenantId()
            val documentId = DocumentId.parse(route.parent.id)
            logger.info("Confirming document: $documentId, tenant=$tenantId")

            val draft = draftRepository.getByDocumentId(documentId, tenantId)
                ?: throw DokusException.NotFound("Draft not found for document")

            if (draft.documentStatus == DocumentStatus.Rejected) {
                throw DokusException.BadRequest("Cannot confirm a rejected document")
            }

            val draftType = draft.documentType ?: DocumentType.Unknown

            // Check if already confirmed (idempotent)
            if (draft.documentStatus == DocumentStatus.Confirmed) {
                val confirmedEntity = findConfirmedEntity(
                    documentId,
                    draftType,
                    tenantId,
                    invoiceRepository,
                    expenseRepository,
                    creditNoteRepository
                )

                if (confirmedEntity != null) {
                    val document = documentRepository.getById(tenantId, documentId)!!
                    val documentWithUrl = addDownloadUrl(document, minioStorage, logger)
                    val latestIngestion = ingestionRepository.getLatestForDocument(documentId, tenantId)

                    if (confirmedEntity is FinancialDocumentDto.InvoiceDto &&
                        confirmedEntity.paidAmount.minor >= confirmedEntity.totalAmount.minor &&
                        confirmedEntity.paidAt == null
                    ) {
                        logger.warn(
                            "Detected paid invoice without paidAt during confirm-path reconciliation: tenant={}, document={}, invoice={}",
                            tenantId,
                            documentId,
                            confirmedEntity.id
                        )
                    }

                    val repairedEntryId = projectionReconciliationService
                        .ensureProjectionIfMissing(tenantId, documentId, confirmedEntity)
                        .getOrElse {
                            throw DokusException.InternalError(
                                "Failed to repair missing cashflow projection: ${it.message}"
                            )
                        }

                    call.respond(
                        HttpStatusCode.OK,
                        DocumentRecordDto(
                            document = documentWithUrl,
                            draft = draft.toDto(),
                            latestIngestion = latestIngestion?.toDto(
                                includeRawExtraction = true,
                                includeTrace = true
                            ),
                            confirmedEntity = confirmedEntity,
                            cashflowEntryId = repairedEntryId
                        )
                    )
                    return@post
                }

                logger.warn(
                    "Draft is confirmed but entity not found; proceeding with confirmation: document=$documentId"
                )
            }

            // Check draft is ready
            if (draft.documentStatus != DocumentStatus.NeedsReview &&
                draft.documentStatus != DocumentStatus.Confirmed
            ) {
                throw DokusException.BadRequest("Draft is not ready for confirmation: ${draft.documentStatus}")
            }

            if (draft.counterpartyIntent == CounterpartyIntent.Pending) {
                throw DokusException.BadRequest("Counterparty is pending creation")
            }

            if (draftType == DocumentType.Unknown) {
                throw DokusException.BadRequest("Document type must be resolved before confirmation")
            }

            val draftData = draft.extractedData
                ?: throw DokusException.BadRequest("No draft data available for confirmation")

            // Determine if an entity already exists (re-confirm path)
            val existingEntityBeforeConfirm = findConfirmedEntity(
                documentId,
                draftType,
                tenantId,
                invoiceRepository,
                expenseRepository,
                creditNoteRepository
            )

            // Confirm document: creates entity + cashflow entry + marks draft confirmed
            val confirmationResult = confirmationDispatcher.confirm(
                tenantId, documentId, draftData, draft.linkedContactId
            ).getOrThrow()

            val entryId = confirmationResult.cashflowEntryId
            logger.info("Document confirmed: $documentId -> $draftType, cashflowEntryId=$entryId")

            // Return full record
            val document = documentRepository.getById(tenantId, documentId)!!
            val documentWithUrl = addDownloadUrl(document, minioStorage, logger)
            val updatedDraft = draftRepository.getByDocumentId(documentId, tenantId)!!
            val latestIngestion = ingestionRepository.getLatestForDocument(documentId, tenantId)

            call.respond(
                if (existingEntityBeforeConfirm != null) HttpStatusCode.OK else HttpStatusCode.Created,
                DocumentRecordDto(
                    document = documentWithUrl,
                    draft = updatedDraft.toDto(),
                    latestIngestion = latestIngestion?.toDto(
                        includeRawExtraction = true,
                        includeTrace = true
                    ),
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

            if (draft.documentStatus == DocumentStatus.Confirmed) {
                throw DokusException.BadRequest("Cannot reject a confirmed document")
            }

            if (draft.documentStatus != DocumentStatus.Rejected) {
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
                    latestIngestion = latestIngestion?.toDto(
                        includeRawExtraction = true,
                        includeTrace = true
                    ),
                    confirmedEntity = null
                )
            )
        }
    }
}

internal fun isInboxLifecycle(status: IngestionStatus?): Boolean {
    return status == IngestionStatus.Queued || status == IngestionStatus.Processing
}
