package tech.dokus.backend.routes.cashflow

import io.ktor.http.ContentDisposition
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.resources.delete
import io.ktor.server.resources.get
import io.ktor.server.resources.patch
import io.ktor.server.resources.post
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondOutputStream
import io.ktor.server.routing.Route
import io.ktor.server.sse.heartbeat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.launch
import org.koin.ktor.ext.inject
import org.slf4j.LoggerFactory
import tech.dokus.backend.mappers.from
import tech.dokus.domain.model.DocumentDraftDto
import tech.dokus.domain.model.DocumentIngestionDto
import tech.dokus.domain.model.DocumentSourceDto
import tech.dokus.backend.security.requireTenantId
import tech.dokus.backend.services.documents.DocumentIntakeServiceResult
import tech.dokus.backend.services.documents.DocumentLifecycleService
import tech.dokus.backend.services.documents.DocumentListingService
import tech.dokus.backend.services.documents.DocumentRecordLoader
import tech.dokus.backend.services.documents.DocumentTruthService
import tech.dokus.backend.services.documents.ProcessingHealthService
import tech.dokus.backend.services.documents.sse.DocumentCollectionEventHub
import tech.dokus.backend.services.documents.sse.DocumentSnapshotEventHub
import tech.dokus.backend.services.documents.sse.DocumentSnapshotSignal
import tech.dokus.backend.services.documents.sse.DocumentSsePublisher
import tech.dokus.database.repository.cashflow.selectPreferredSource
import tech.dokus.domain.enums.IngestionStatus
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.DocumentMatchReviewId
import tech.dokus.domain.ids.DocumentSourceId
import tech.dokus.domain.model.BulkReprocessRequest
import tech.dokus.domain.model.DocumentCollectionChangedEventDto
import tech.dokus.domain.model.DocumentDeletedEventDto
import tech.dokus.domain.model.DocumentDetailDto
import tech.dokus.domain.model.DocumentStreamEventNames
import tech.dokus.domain.model.DownloadZipRequest
import tech.dokus.domain.model.RejectDocumentRequest
import tech.dokus.domain.model.ReprocessRequest
import tech.dokus.domain.model.ResolveDocumentMatchReviewRequest
import tech.dokus.domain.model.UpdateDraftRequest
import tech.dokus.domain.routes.Documents
import tech.dokus.domain.utils.json
import tech.dokus.foundation.backend.security.authenticateJwt
import tech.dokus.foundation.backend.security.dokusPrincipal
import tech.dokus.foundation.backend.utils.defaultSseHeartbeatPeriod
import tech.dokus.foundation.backend.utils.respondSse
import tech.dokus.foundation.backend.utils.sendJsonEvent
import tech.dokus.foundation.backend.storage.DocumentStorageService as MinioDocumentStorageService

/**
 * Document record routes using canonical API.
 *
 * Endpoints:
 * - GET /api/v1/documents - List documents with filters
 * - GET /api/v1/documents/{id} - Get full DocumentDetailDto
 * - DELETE /api/v1/documents/{id} - Delete document (cascades)
 * - GET /api/v1/documents/{id}/draft - Get draft
 * - PATCH /api/v1/documents/{id}/draft - Update draft
 * - GET /api/v1/documents/{id}/ingestions - Get ingestion history
 * - POST /api/v1/documents/{id}/reprocess - Reprocess document (idempotent)
 * - POST /api/v1/documents/{id}/confirm - Confirm using latest draft (transactional + idempotent)
 * - GET /api/v1/documents/events (SSE) - Collection change notifications
 * - GET /api/v1/documents/{id}/events (SSE) - Document snapshot stream
 */
internal fun Route.documentRecordRoutes() {
    val listingService by inject<DocumentListingService>()
    val lifecycleService by inject<DocumentLifecycleService>()
    val truthService by inject<DocumentTruthService>()
    val minioStorage by inject<MinioDocumentStorageService>()
    val documentRecordLoader by inject<DocumentRecordLoader>()
    val documentCollectionEventHub by inject<DocumentCollectionEventHub>()
    val documentSnapshotEventHub by inject<DocumentSnapshotEventHub>()
    val documentSsePublisher by inject<DocumentSsePublisher>()
    val processingHealthService by inject<ProcessingHealthService>()
    val logger = LoggerFactory.getLogger("DocumentRecordRoutes")

    authenticateJwt {
        /**
         * GET /api/v1/documents
         * List documents with filters and pagination.
         */
        get<Documents.Paginated> { route ->
            val tenantId = requireTenantId()
            val page = route.page.coerceAtLeast(0)
            val limit = route.limit.coerceIn(1, 100)
            val filter = route.filter

            logger.info("Listing documents: tenant=$tenantId, filter=$filter, page=$page, limit=${limit}, sortBy=${route.sortBy}")

            if (filter != null && (route.documentStatus != null || route.ingestionStatus != null)) {
                throw DokusException.BadRequest("Do not combine 'filter' with 'documentStatus' or 'ingestionStatus'")
            }

            val result = listingService.listDocuments(
                tenantId = tenantId,
                filter = filter,
                documentStatus = route.documentStatus,
                documentType = route.documentType,
                ingestionStatus = route.ingestionStatus,
                sortBy = route.sortBy,
                page = page,
                limit = limit
            )

            call.respond(HttpStatusCode.OK, result)
        }

        get<Documents.Counts> {
            val tenantId = requireTenantId()
            val counts = listingService.getDocumentCounts(tenantId)
            call.respond(HttpStatusCode.OK, counts)
        }

        // ── Processing health ────────────────────────────────────────

        get<Documents.ProcessingHealth> {
            val tenantId = requireTenantId()
            val recommendation = processingHealthService.getRecommendation(tenantId)
            call.respond(HttpStatusCode.OK, recommendation)
        }

        post<Documents.BulkReprocess> {
            val tenantId = requireTenantId()
            val request = try {
                call.receive<BulkReprocessRequest>()
            } catch (_: Exception) {
                BulkReprocessRequest()
            }
            val result = processingHealthService.executeBulkReprocess(tenantId, request.maxDocuments)
            call.respond(HttpStatusCode.OK, result)
        }

        /**
         * POST /api/v1/documents/download-zip
         */
        post<Documents.DownloadZip> {
            val tenantId = requireTenantId()
            val request = call.receive<DownloadZipRequest>()

            if (request.documentIds.isEmpty()) {
                throw DokusException.BadRequest("No document IDs provided")
            }

            val documentIds = request.documentIds.map { DocumentId.parse(it) }

            // Resolve preferred source for each document
            val resolvedSources = documentIds.mapNotNull { docId ->
                val docSources = truthService.listSources(tenantId, docId)
                val preferred = selectPreferredSource(docSources) ?: return@mapNotNull null
                docId to preferred
            }

            if (resolvedSources.isEmpty()) {
                throw DokusException.NotFound("No downloadable documents found")
            }

            call.response.header(
                HttpHeaders.ContentDisposition,
                ContentDisposition.Attachment.withParameter(
                    ContentDisposition.Parameters.FileName,
                    "dokus-documents.zip"
                ).toString()
            )

            call.respondOutputStream(
                contentType = ContentType.Application.Zip,
                status = HttpStatusCode.OK,
            ) {
                val zipOut = java.util.zip.ZipOutputStream(this)
                for ((docId, source) in resolvedSources) {
                    try {
                        val stream = minioStorage.openDocumentStream(source.storageKey)
                        val filename = source.filename ?: "$docId.pdf"
                        zipOut.putNextEntry(java.util.zip.ZipEntry(filename))
                        stream.use { it.copyTo(this) }
                        zipOut.closeEntry()
                    } catch (e: Exception) {
                        logger.warn("Failed to include document {} in ZIP: {}", docId, e.message)
                    }
                }
                zipOut.finish()
            }
        }

        get<Documents.Events> {
            val tenantId = requireTenantId()
            call.respondSse {
                heartbeat { period = defaultSseHeartbeatPeriod }
                documentCollectionEventHub.eventsFor(tenantId).collect { event ->
                    sendJsonEvent(
                        event = DocumentStreamEventNames.CollectionChanged,
                        payload = event,
                        encode = { json.encodeToString(DocumentCollectionChangedEventDto.serializer(), it) }
                    )
                }
            }
        }

        /**
         * GET /api/v1/documents/{id}
         */
        get<Documents.Id> { route ->
            val tenantId = requireTenantId()
            val documentId = DocumentId.parse(route.id)
            val record = documentRecordLoader.load(tenantId, documentId)
                ?: throw DokusException.NotFound("Document not found")
            call.respond(HttpStatusCode.OK, record)
        }

        get<Documents.Id.Events> { route ->
            val tenantId = requireTenantId()
            val documentId = DocumentId.parse(route.parent.id)

            call.respondSse {
                heartbeat { period = defaultSseHeartbeatPeriod }

                suspend fun sendDeletedEvent() {
                    sendJsonEvent(
                        event = DocumentStreamEventNames.Deleted,
                        payload = DocumentDeletedEventDto(documentId),
                        encode = { json.encodeToString(DocumentDeletedEventDto.serializer(), it) }
                    )
                }

                suspend fun sendSnapshotOrDeleted(): Boolean {
                    val currentRecord = documentRecordLoader.load(tenantId, documentId)
                    return if (currentRecord != null) {
                        sendJsonEvent(
                            event = DocumentStreamEventNames.Snapshot,
                            payload = currentRecord,
                            encode = { json.encodeToString(DocumentDetailDto.serializer(), it) }
                        )
                        true
                    } else {
                        sendDeletedEvent()
                        false
                    }
                }

                val signalQueue = Channel<DocumentSnapshotSignal>(capacity = Channel.BUFFERED)
                val subscriptionReady = CompletableDeferred<Unit>()
                val signalCollector = launch(start = CoroutineStart.UNDISPATCHED) {
                    documentSnapshotEventHub.eventsFor(tenantId, documentId)
                        .onSubscription { subscriptionReady.complete(Unit) }
                        .collect { signalQueue.send(it) }
                }

                try {
                    subscriptionReady.await()
                    val initialRecord = documentRecordLoader.load(tenantId, documentId)
                    if (initialRecord == null) {
                        sendDeletedEvent()
                        return@respondSse
                    }
                    sendJsonEvent(
                        event = DocumentStreamEventNames.Snapshot,
                        payload = initialRecord,
                        encode = { json.encodeToString(DocumentDetailDto.serializer(), it) }
                    )

                    while (true) {
                        when (signalQueue.receive()) {
                            DocumentSnapshotSignal.Changed -> { if (!sendSnapshotOrDeleted()) break }
                            DocumentSnapshotSignal.Deleted -> { sendDeletedEvent(); break }
                        }
                    }
                } finally {
                    signalCollector.cancelAndJoin()
                    signalQueue.close()
                }
            }
        }

        /**
         * GET /api/v1/documents/{id}/content
         */
        get<Documents.Id.Content> { route ->
            val tenantId = requireTenantId()
            val documentId = DocumentId.parse(route.parent.id)

            val info = lifecycleService.resolveDocumentContent(tenantId, documentId)

            val stream = try {
                minioStorage.openDocumentStream(info.storageKey)
            } catch (e: NoSuchElementException) {
                logger.warn("Document content object missing: document=$documentId, storageKey=${info.storageKey}")
                throw DokusException.NotFound("Document content not found in storage")
            } catch (e: Exception) {
                logger.error("Failed to open document stream: $documentId", e)
                throw DokusException.InternalError("Failed to download document content")
            }

            val contentType = runCatching { ContentType.parse(info.contentType) }
                .getOrDefault(ContentType.Application.OctetStream)

            call.response.header(
                HttpHeaders.ContentDisposition,
                ContentDisposition.Attachment
                    .withParameter(ContentDisposition.Parameters.FileName, info.filename)
                    .toString()
            )
            call.respondOutputStream(contentType = contentType, status = HttpStatusCode.OK) {
                stream.use { input -> input.copyTo(this) }
            }
        }

        /**
         * GET /api/v1/documents/{id}/sources/{sourceId}/content
         */
        get<Documents.Id.SourceContent> { route ->
            val tenantId = requireTenantId()
            val documentId = DocumentId.parse(route.parent.id)
            val sourceId = DocumentSourceId.parse(route.sourceId)

            val info = lifecycleService.resolveSourceContent(tenantId, documentId, sourceId)

            val stream = try {
                minioStorage.openDocumentStream(info.storageKey)
            } catch (e: NoSuchElementException) {
                throw DokusException.NotFound("Source content not found")
            } catch (e: Exception) {
                logger.error("Failed to open source stream: sourceId=$sourceId", e)
                throw DokusException.InternalError("Failed to download source content")
            }

            val contentType = runCatching { ContentType.parse(info.contentType) }
                .getOrDefault(ContentType.Application.OctetStream)

            call.response.header(
                HttpHeaders.ContentDisposition,
                ContentDisposition.Attachment
                    .withParameter(ContentDisposition.Parameters.FileName, info.filename)
                    .toString()
            )
            call.respondOutputStream(contentType = contentType, status = HttpStatusCode.OK) {
                stream.use { input -> input.copyTo(this) }
            }
        }

        /**
         * GET /api/v1/documents/{id}/sources
         */
        get<Documents.Id.Sources> { route ->
            val tenantId = requireTenantId()
            val documentId = DocumentId.parse(route.parent.id)

            if (!truthService.documentExists(tenantId, documentId)) {
                throw DokusException.NotFound("Document not found")
            }

            val sources = truthService.listSources(tenantId, documentId)
            call.respond(HttpStatusCode.OK, sources.map { DocumentSourceDto.from(it) })
        }

        /**
         * DELETE /api/v1/documents/{id}/sources/{sourceId}
         */
        delete<Documents.Id.Source> { route ->
            val tenantId = requireTenantId()
            val documentId = DocumentId.parse(route.parent.id)
            val sourceId = DocumentSourceId.parse(route.sourceId)

            val deleteResult = truthService.deleteSource(
                tenantId = tenantId,
                documentId = documentId,
                sourceId = sourceId,
                confirmLastOnConfirmed = route.confirm
            )

            if (deleteResult.requiresConfirmation) {
                throw DokusException.BadRequest(
                    "Deleting the last source from a confirmed document requires confirm=true"
                )
            }
            if (!deleteResult.deleted) {
                throw DokusException.NotFound("Source not found")
            }

            if (deleteResult.cascadedDocumentDelete) {
                documentSsePublisher.publishDocumentDeleted(tenantId, documentId)
            } else {
                documentSsePublisher.publishDocumentChanged(tenantId, documentId)
            }
            call.respond(HttpStatusCode.NoContent)
        }

        /**
         * DELETE /api/v1/documents/{id}
         */
        delete<Documents.Id> { route ->
            val tenantId = requireTenantId()
            val documentId = DocumentId.parse(route.id)
            logger.info("Deleting document: $documentId, tenant=$tenantId")
            lifecycleService.deleteDocument(tenantId, documentId)
            call.respond(HttpStatusCode.NoContent)
        }

        /**
         * GET /api/v1/documents/{id}/draft
         */
        get<Documents.Id.Draft> { route ->
            val tenantId = requireTenantId()
            val documentId = DocumentId.parse(route.parent.id)

            val draft = lifecycleService.getDraft(tenantId, documentId)
                ?: throw DokusException.NotFound("Draft not found for document")

            call.respond(HttpStatusCode.OK, DocumentDraftDto.from(draft))
        }

        /**
         * PATCH /api/v1/documents/{id}/draft
         */
        patch<Documents.Id.Draft> { route ->
            val tenantId = requireTenantId()
            val userId = dokusPrincipal.userId
            val documentId = DocumentId.parse(route.parent.id)
            val request = call.receive<UpdateDraftRequest>()

            val response = lifecycleService.updateDraft(tenantId, documentId, userId, request)

            if (response != null) {
                call.respond(HttpStatusCode.OK, response)
            } else {
                call.respond(HttpStatusCode.NoContent)
            }
        }

        /**
         * GET /api/v1/documents/{id}/ingestions
         */
        get<Documents.Id.Ingestions> { route ->
            val tenantId = requireTenantId()
            val documentId = DocumentId.parse(route.parent.id)
            val runs = lifecycleService.getIngestionHistory(tenantId, documentId)
            call.respond(HttpStatusCode.OK, runs.map { DocumentIngestionDto.from(it, includeRawExtraction = true, includeTrace = true) })
        }

        /**
         * POST /api/v1/documents/{id}/reprocess
         */
        post<Documents.Id.Reprocess> { route ->
            val tenantId = requireTenantId()
            val documentId = DocumentId.parse(route.parent.id)
            val request = try {
                call.receive<ReprocessRequest>()
            } catch (_: Exception) {
                ReprocessRequest()
            }

            val result = lifecycleService.reprocessDocument(tenantId, documentId, request)
            val status = if (result.isExistingRun) HttpStatusCode.OK else HttpStatusCode.Created
            call.respond(status, result)
        }

        /**
         * POST /api/v1/documents/{id}/confirm
         */
        post<Documents.Id.Confirm> { route ->
            val tenantId = requireTenantId()
            val documentId = DocumentId.parse(route.parent.id)
            logger.info("Confirming document: $documentId, tenant=$tenantId")

            val result = lifecycleService.confirmDocument(tenantId, documentId)
            val status = if (result.wasAlreadyConfirmed) HttpStatusCode.OK else HttpStatusCode.Created
            call.respond(status, result.record)
        }

        /**
         * POST /api/v1/documents/{id}/unconfirm
         */
        post<Documents.Id.Unconfirm> { route ->
            val tenantId = requireTenantId()
            val documentId = DocumentId.parse(route.parent.id)
            logger.info("Unconfirming document: $documentId, tenant=$tenantId")
            val record = lifecycleService.unconfirmDocument(tenantId, documentId)
            call.respond(HttpStatusCode.OK, record)
        }

        /**
         * POST /api/v1/documents/{id}/reject
         */
        post<Documents.Id.Reject> { route ->
            val tenantId = requireTenantId()
            val documentId = DocumentId.parse(route.parent.id)
            val request = call.receive<RejectDocumentRequest>()
            val record = lifecycleService.rejectDocument(tenantId, documentId, request)
            call.respond(HttpStatusCode.OK, record)
        }

        /**
         * POST /api/v1/documents/match-reviews/{reviewId}/resolve
         */
        post<Documents.MatchReviews.Resolve> { route ->
            val tenantId = requireTenantId()
            val userId = dokusPrincipal.userId
            val reviewId = DocumentMatchReviewId.parse(route.reviewId)
            val request = call.receive<ResolveDocumentMatchReviewRequest>()

            val result = truthService.resolveMatchReview(
                tenantId = tenantId,
                userId = userId,
                reviewId = reviewId,
                decision = request.decision
            ) ?: throw DokusException.NotFound("Match review not found")

            publishAffectedDocuments(
                documentSsePublisher = documentSsePublisher,
                tenantId = tenantId,
                result = result
            )

            val resolvedRecord = documentRecordLoader.load(tenantId, result.documentId)
                ?: throw DokusException.NotFound("Document not found")

            call.respond(HttpStatusCode.OK, resolvedRecord)
        }
    }
}

internal fun isInboxLifecycle(status: IngestionStatus?): Boolean {
    return status == IngestionStatus.Queued || status == IngestionStatus.Processing
}

private fun publishAffectedDocuments(
    documentSsePublisher: DocumentSsePublisher,
    tenantId: tech.dokus.domain.ids.TenantId,
    result: DocumentIntakeServiceResult,
) {
    val sourceDocumentId = result.sourceDocumentId
    val targetDocumentId = result.documentId

    if (sourceDocumentId != null && sourceDocumentId != targetDocumentId) {
        if (result.orphanedDocumentId == sourceDocumentId) {
            documentSsePublisher.publishDocumentDeleted(tenantId, sourceDocumentId)
        } else {
            documentSsePublisher.publishDocumentChanged(tenantId, sourceDocumentId)
        }
    }

    if (result.orphanedDocumentId == targetDocumentId) {
        documentSsePublisher.publishDocumentDeleted(tenantId, targetDocumentId)
    } else {
        documentSsePublisher.publishDocumentChanged(tenantId, targetDocumentId)
    }
}
