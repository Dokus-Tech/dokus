package tech.dokus.domain.routes

import io.ktor.resources.Resource
import kotlinx.serialization.Serializable
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.enums.DocumentListFilter
import tech.dokus.domain.enums.DocumentStatus
import tech.dokus.domain.enums.IngestionStatus

/**
 * Type-safe route definitions for Document Management API.
 * Base path: /api/v1/documents
 *
 * SECURITY: All operations are scoped to the authenticated user's tenant via JWT.
 *
 * Canonical endpoints:
 * - POST /upload - Upload document (creates ingestion run with status=Queued)
 * - GET / - List documents with filters (uses Documents.List)
     * - GET /{id} - Get full DocumentRecordDto
 * - GET /{id}/content - Download document bytes via API
 * - DELETE /{id} - Delete document (cascades to drafts, runs)
 * - POST /{id}/reprocess - Reprocess document (idempotent: returns existing run unless force)
 * - GET /{id}/ingestions - Get ingestion history
 * - GET /{id}/draft - Get draft
 * - PATCH /{id}/draft - Update draft
 * - POST /{id}/confirm - Confirm and create financial entity
 * - POST /{id}/chat - Document Q&A
 * - GET /{id}/pages - List PDF page previews
 * - GET /{id}/pages/{page}.png - Get rendered PDF page as PNG
 * - GET /{id}/sources/{sourceId}/content - Download source bytes via API
 * - GET /{id}/sources/{sourceId}/pages - List source PDF page previews
 * - GET /{id}/sources/{sourceId}/pages/{page}.png - Get rendered source PDF page as PNG
 *
 * NOTE: Query parameters for listing are defined in Documents.List, NOT in the base class.
 * This prevents parameter leakage to single-document routes.
 */
@Serializable
@Resource("/api/v1/documents")
class Documents {
    /**
     * GET /api/v1/documents
     * List documents with optional filters and pagination.
     *
     * Query parameters are defined here, NOT in the parent class,
     * to prevent them from leaking to child routes like /{id}.
     */
    @Serializable
    @Resource("")
    @Suppress("LongParameterList") // Query parameters for document filtering
    class Paginated(
        val parent: Documents = Documents(),
        val filter: DocumentListFilter? = null,
        val documentStatus: DocumentStatus? = null,
        val documentType: DocumentType? = null,
        val ingestionStatus: IngestionStatus? = null,
        val search: String? = null,
        val page: Int = 0,
        val limit: Int = 20
    )

    /**
     * POST /api/v1/documents/upload
     * Upload a new document
     * Returns DocumentRecordDto (document + ingestion with status=Queued; draft is created after processing)
     */
    @Serializable
    @Resource("upload")
    class Upload(val parent: Documents = Documents())

    /**
     * /api/v1/documents/match-reviews/...
     */
    @Serializable
    @Resource("match-reviews")
    class MatchReviews(val parent: Documents = Documents()) {
        /**
         * POST /api/v1/documents/match-reviews/{reviewId}/resolve
         */
        @Serializable
        @Resource("{reviewId}/resolve")
        class Resolve(
            val parent: MatchReviews = MatchReviews(),
            val reviewId: String
        )
    }

    /**
     * /api/v1/documents/{id} - Single document operations
     * GET - Retrieve full DocumentRecordDto
     * DELETE - Delete document (cascades to drafts, ingestion runs, chunks)
     */
    @Serializable
    @Resource("{id}")
    class Id(val parent: Documents = Documents(), val id: String) {
        /**
         * GET /api/v1/documents/{id}/content
         * Download raw document bytes for authenticated clients.
         */
        @Serializable
        @Resource("content")
        class Content(val parent: Id)

        /**
         * GET /api/v1/documents/{id}/draft
         * Get draft details
         */
        @Serializable
        @Resource("draft")
        class Draft(val parent: Id)

        /**
         * GET /api/v1/documents/{id}/ingestions
         * Get ingestion run history
         */
        @Serializable
        @Resource("ingestions")
        class Ingestions(val parent: Id)

        /**
         * GET /api/v1/documents/{id}/sources
         * List all evidence sources linked to this canonical document.
         */
        @Serializable
        @Resource("sources")
        class Sources(val parent: Id)

        /**
         * DELETE /api/v1/documents/{id}/sources/{sourceId}
         * Detach/delete a single source.
         */
        @Serializable
        @Resource("sources/{sourceId}")
        class Source(
            val parent: Id,
            val sourceId: String,
            val confirm: Boolean = false
        )

        /**
         * GET /api/v1/documents/{id}/sources/{sourceId}/content
         * Download raw source bytes for authenticated clients.
         */
        @Serializable
        @Resource("sources/{sourceId}/content")
        class SourceContent(
            val parent: Id,
            val sourceId: String
        )

        /**
         * GET /api/v1/documents/{id}/sources/{sourceId}/pages
         * List available PDF pages for a specific source.
         */
        @Serializable
        @Resource("sources/{sourceId}/pages")
        class SourcePages(
            val parent: Id,
            val sourceId: String,
            val dpi: Int = 150,
            val maxPages: Int = 10
        )

        /**
         * GET /api/v1/documents/{id}/sources/{sourceId}/pages/{page}.png
         * Get a rendered PDF page as PNG image for a specific source.
         */
        @Serializable
        @Resource("sources/{sourceId}/pages/{page}.png")
        class SourcePageImage(
            val parent: Id,
            val sourceId: String,
            val page: Int,
            val dpi: Int = 150
        )

        /**
         * POST /api/v1/documents/{id}/reprocess
         * Reprocess document
         * IDEMPOTENT: Returns existing Queued/Processing run unless force=true
         */
        @Serializable
        @Resource("reprocess")
        class Reprocess(val parent: Id)

        /**
         * POST /api/v1/documents/{id}/confirm
         * Confirm extraction and create financial entity
         * TRANSACTIONAL + IDEMPOTENT:
         * - If entity does not exist yet, creates it.
         * - If entity exists, confirmation is idempotent and may re-confirm after edits (if allowed by cashflow rules).
         */
        @Serializable
        @Resource("confirm")
        class Confirm(val parent: Id)

        /**
         * POST /api/v1/documents/{id}/reject
         * Reject extraction with a reason (idempotent).
         */
        @Serializable
        @Resource("reject")
        class Reject(val parent: Id)

        /**
         * POST /api/v1/documents/{id}/chat
         * Send a chat message for single-document Q&A
         */
        @Serializable
        @Resource("chat")
        class Chat(val parent: Id)

        /**
         * GET /api/v1/documents/{id}/pages
         * List available PDF pages for preview.
         *
         * @param dpi Resolution for rendered pages (72-300, default 150)
         * @param maxPages Maximum pages to return (1-50, default 10)
         */
        @Serializable
        @Resource("pages")
        class Pages(
            val parent: Id,
            val dpi: Int = 150,
            val maxPages: Int = 10
        )

        /**
         * GET /api/v1/documents/{id}/pages/{page}.png
         * Get a rendered PDF page as PNG image.
         *
         * @param page 1-based page number
         * @param dpi Resolution for rendered page (72-300, default 150)
         */
        @Serializable
        @Resource("pages/{page}.png")
        class PageImage(
            val parent: Id,
            val page: Int,
            val dpi: Int = 150
        )
    }
}
