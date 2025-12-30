package tech.dokus.backend.routes.cashflow

import ai.dokus.foundation.database.repository.cashflow.DocumentRepository
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.resources.get
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.Route
import org.koin.ktor.ext.inject
import org.slf4j.LoggerFactory
import tech.dokus.backend.services.pdf.PdfPreviewService
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.DocumentId
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

            // Verify document exists and belongs to tenant
            val document = documentRepository.getById(tenantId, documentId)
                ?: throw DokusException.NotFound("Document not found: $documentId")

            // Check if document is a PDF
            if (!document.contentType.contains("pdf", ignoreCase = true)) {
                throw DokusException.BadRequest("Document is not a PDF: ${document.contentType}")
            }

            try {
                val response = pdfPreviewService.listPages(
                    tenantId = tenantId,
                    documentId = documentId,
                    storageKey = document.storageKey,
                    dpi = route.dpi,
                    maxPages = route.maxPages
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

            // Validate page number
            if (page < 1) {
                throw DokusException.BadRequest("Page number must be >= 1, got: $page")
            }

            // Verify document exists and belongs to tenant
            val document = documentRepository.getById(tenantId, documentId)
                ?: throw DokusException.NotFound("Document not found: $documentId")

            // Check if document is a PDF
            if (!document.contentType.contains("pdf", ignoreCase = true)) {
                throw DokusException.BadRequest("Document is not a PDF: ${document.contentType}")
            }

            try {
                val imageBytes = pdfPreviewService.getPageImage(
                    tenantId = tenantId,
                    documentId = documentId,
                    storageKey = document.storageKey,
                    page = page,
                    dpi = route.dpi
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
    }
}
