package tech.dokus.backend.routes.cashflow

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.resources.get
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.Route
import org.koin.ktor.ext.inject
import org.slf4j.LoggerFactory
import tech.dokus.backend.services.pdf.PdfPreviewService
import tech.dokus.database.repository.cashflow.DocumentRepository
import tech.dokus.database.repository.cashflow.DocumentSourceRepository
import tech.dokus.database.repository.cashflow.DocumentSourceSummary
import tech.dokus.database.repository.cashflow.selectDefaultSourceFromList
import tech.dokus.domain.enums.DocumentSourceStatus
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.DocumentSourceId
import tech.dokus.domain.routes.Documents
import tech.dokus.foundation.backend.security.authenticateJwt
import tech.dokus.foundation.backend.security.dokusPrincipal

/**
 * PDF page preview routes.
 *
 * Endpoints:
 * - GET /api/v1/documents/{id}/pages - List available pages with preview URLs
 * - GET /api/v1/documents/{id}/pages/{page}.png - Get rendered page as PNG
 *
 * Security:
 * - All routes require JWT authentication
 * - Tenant isolation enforced: document must belong to authenticated tenant
 */
internal fun Route.documentPageRoutes() {
    val documentRepository by inject<DocumentRepository>()
    val documentSourceRepository by inject<DocumentSourceRepository>()
    val pdfPreviewService by inject<PdfPreviewService>()
    val logger = LoggerFactory.getLogger("DocumentPageRoutes")

    authenticateJwt {
        /**
         * GET /api/v1/documents/{id}/pages
         * List available PDF pages for preview.
         *
         * Query params:
         * - dpi: Resolution for rendered pages (72-300, default 150)
         * - maxPages: Maximum pages to return (1-50, default 10)
         */
        get<Documents.Id.Pages> { route ->
            val tenantId = dokusPrincipal.requireTenantId()
            val documentId = DocumentId.parse(route.parent.id)

            logger.debug("Listing pages for document {} (tenant: {})", documentId, tenantId)
            val resolved = resolveDefaultSourceForPreview(
                tenantId = tenantId,
                documentId = documentId,
                documentRepository = documentRepository,
                sourceRepository = documentSourceRepository
            )
            ensurePdfContentType(resolved.contentType)

            try {
                val response = pdfPreviewService.listPages(
                    tenantId = tenantId,
                    documentId = documentId,
                    storageKey = resolved.storageKey,
                    dpi = route.dpi,
                    maxPages = route.maxPages,
                    pageImageBasePath = "/api/v1/documents/$documentId/pages"
                )

                call.respond(HttpStatusCode.OK, response)
            } catch (e: IllegalArgumentException) {
                throw DokusException.BadRequest(e.message ?: "Invalid request")
            } catch (e: Exception) {
                logger.error("Failed to list pages for document {}", documentId, e)
                throw DokusException.InternalError("Failed to process PDF: ${e.message}")
            }
        }

        /**
         * GET /api/v1/documents/{id}/pages/{page}.png
         * Get a rendered PDF page as PNG image.
         *
         * Path params:
         * - page: 1-based page number
         *
         * Query params:
         * - dpi: Resolution for rendered page (72-300, default 150)
         */
        get<Documents.Id.PageImage> { route ->
            val tenantId = dokusPrincipal.requireTenantId()
            val documentId = DocumentId.parse(route.parent.id)
            val page = route.page

            logger.debug("Getting page {} for document {} (tenant: {})", page, documentId, tenantId)
            validatePageNumber(page)
            val resolved = resolveDefaultSourceForPreview(
                tenantId = tenantId,
                documentId = documentId,
                documentRepository = documentRepository,
                sourceRepository = documentSourceRepository
            )
            ensurePdfContentType(resolved.contentType)

            try {
                val imageBytes = pdfPreviewService.getPageImage(
                    tenantId = tenantId,
                    documentId = documentId,
                    storageKey = resolved.storageKey,
                    page = page,
                    dpi = route.dpi,
                    cacheScope = resolved.cacheScope
                )

                call.respondBytes(
                    bytes = imageBytes,
                    contentType = ContentType.Image.PNG,
                    status = HttpStatusCode.OK
                )
            } catch (e: IllegalArgumentException) {
                throw DokusException.BadRequest(e.message ?: "Invalid request")
            } catch (e: Exception) {
                logger.error("Failed to render page {} for document {}", page, documentId, e)
                throw DokusException.InternalError("Failed to render page: ${e.message}")
            }
        }

        /**
         * GET /api/v1/documents/{id}/sources/{sourceId}/pages
         * List available PDF pages for a specific source.
         */
        get<Documents.Id.SourcePages> { route ->
            val tenantId = dokusPrincipal.requireTenantId()
            val documentId = DocumentId.parse(route.parent.id)
            val sourceId = DocumentSourceId.parse(route.sourceId)

            logger.debug(
                "Listing pages for source {} of document {} (tenant: {})",
                sourceId,
                documentId,
                tenantId
            )
            val resolved = resolveExplicitSourceForPreview(
                tenantId = tenantId,
                documentId = documentId,
                sourceId = sourceId,
                documentRepository = documentRepository,
                sourceRepository = documentSourceRepository
            )
            ensurePdfContentType(resolved.contentType)

            try {
                val response = pdfPreviewService.listPages(
                    tenantId = tenantId,
                    documentId = documentId,
                    storageKey = resolved.storageKey,
                    dpi = route.dpi,
                    maxPages = route.maxPages,
                    pageImageBasePath = "/api/v1/documents/$documentId/sources/$sourceId/pages"
                )

                call.respond(HttpStatusCode.OK, response)
            } catch (e: IllegalArgumentException) {
                throw DokusException.BadRequest(e.message ?: "Invalid request")
            } catch (e: Exception) {
                logger.error(
                    "Failed to list pages for source {} of document {}",
                    sourceId,
                    documentId,
                    e
                )
                throw DokusException.InternalError("Failed to process PDF: ${e.message}")
            }
        }

        /**
         * GET /api/v1/documents/{id}/sources/{sourceId}/pages/{page}.png
         * Get a rendered PDF page as PNG image for a specific source.
         */
        get<Documents.Id.SourcePageImage> { route ->
            val tenantId = dokusPrincipal.requireTenantId()
            val documentId = DocumentId.parse(route.parent.id)
            val sourceId = DocumentSourceId.parse(route.sourceId)
            val page = route.page

            logger.debug(
                "Getting page {} for source {} of document {} (tenant: {})",
                page,
                sourceId,
                documentId,
                tenantId
            )
            validatePageNumber(page)
            val resolved = resolveExplicitSourceForPreview(
                tenantId = tenantId,
                documentId = documentId,
                sourceId = sourceId,
                documentRepository = documentRepository,
                sourceRepository = documentSourceRepository
            )
            ensurePdfContentType(resolved.contentType)

            try {
                val imageBytes = pdfPreviewService.getPageImage(
                    tenantId = tenantId,
                    documentId = documentId,
                    storageKey = resolved.storageKey,
                    page = page,
                    dpi = route.dpi,
                    cacheScope = resolved.cacheScope
                )

                call.respondBytes(
                    bytes = imageBytes,
                    contentType = ContentType.Image.PNG,
                    status = HttpStatusCode.OK
                )
            } catch (e: IllegalArgumentException) {
                throw DokusException.BadRequest(e.message ?: "Invalid request")
            } catch (e: Exception) {
                logger.error(
                    "Failed to render page {} for source {} of document {}",
                    page,
                    sourceId,
                    documentId,
                    e
                )
                throw DokusException.InternalError("Failed to render page: ${e.message}")
            }
        }
    }
}

private data class PreviewSourceSelection(
    val storageKey: String,
    val contentType: String,
    val cacheScope: String
)

private suspend fun resolveDefaultSourceForPreview(
    tenantId: tech.dokus.domain.ids.TenantId,
    documentId: DocumentId,
    documentRepository: DocumentRepository,
    sourceRepository: DocumentSourceRepository
): PreviewSourceSelection {
    val document = documentRepository.getById(tenantId, documentId)
        ?: throw DokusException.NotFound("Document not found: $documentId")
    val sources = sourceRepository.listByDocument(tenantId, documentId)
    val defaultSource = selectDefaultSourceFromList(sources)
    return if (defaultSource != null) {
        defaultSource.toPreviewSelection()
    } else {
        PreviewSourceSelection(
            storageKey = document.storageKey,
            contentType = document.contentType,
            cacheScope = "default"
        )
    }
}

private suspend fun resolveExplicitSourceForPreview(
    tenantId: tech.dokus.domain.ids.TenantId,
    documentId: DocumentId,
    sourceId: DocumentSourceId,
    documentRepository: DocumentRepository,
    sourceRepository: DocumentSourceRepository
): PreviewSourceSelection {
    if (!documentRepository.exists(tenantId, documentId)) {
        throw DokusException.NotFound("Document not found: $documentId")
    }
    val source = sourceRepository.getById(tenantId, sourceId)
        ?: throw DokusException.NotFound("Source not found: $sourceId")
    if (source.documentId != documentId || source.status != DocumentSourceStatus.Linked) {
        throw DokusException.NotFound("Source not found: $sourceId")
    }
    return source.toPreviewSelection()
}

private fun DocumentSourceSummary.toPreviewSelection(): PreviewSourceSelection {
    return PreviewSourceSelection(
        storageKey = storageKey,
        contentType = contentType,
        cacheScope = "source-$id"
    )
}

private fun ensurePdfContentType(contentType: String) {
    if (!contentType.contains("pdf", ignoreCase = true)) {
        throw DokusException.BadRequest("Document is not a PDF: $contentType")
    }
}

private fun validatePageNumber(page: Int) {
    if (page < 1) {
        throw DokusException.BadRequest("Page number must be >= 1, got: $page")
    }
}
