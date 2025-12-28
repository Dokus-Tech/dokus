package tech.dokus.domain.routes

import io.ktor.resources.*
import kotlinx.serialization.Serializable

/**
 * Type-safe route definitions for Document Management API.
 * Base path: /api/v1/documents
 *
 * SECURITY: All operations are scoped to the authenticated user's tenant via JWT.
 *
 * Canonical endpoints:
 * - POST /upload - Upload document (creates ingestion run with status=Queued)
 * - GET / - List documents with filters
 * - GET /{id} - Get full DocumentRecordDto
 * - DELETE /{id} - Delete document (cascades to drafts, runs)
 * - POST /{id}/reprocess - Reprocess document (idempotent: returns existing run unless force)
 * - GET /{id}/ingestions - Get ingestion history
 * - GET /{id}/draft - Get draft
 * - PATCH /{id}/draft - Update draft
 * - POST /{id}/confirm - Confirm and create financial entity
 * - POST /{id}/chat - Document Q&A
 */
@Serializable
@Resource("/api/v1/documents")
class Documents(
    val draftStatus: String? = null,
    val documentType: String? = null,
    val ingestionStatus: String? = null,
    val search: String? = null,
    val page: Int = 0,
    val limit: Int = 20
) {
    /**
     * POST /api/v1/documents/upload
     * Upload a new document
     * Returns DocumentRecordDto (document + draft with status=NeedsReview + ingestion with status=Queued)
     */
    @Serializable
    @Resource("upload")
    class Upload(val parent: Documents = Documents())

    /**
     * /api/v1/documents/{id} - Single document operations
     * GET - Retrieve full DocumentRecordDto
     * DELETE - Delete document (cascades to drafts, ingestion runs, chunks)
     */
    @Serializable
    @Resource("{id}")
    class Id(val parent: Documents = Documents(), val id: String) {
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
         * TRANSACTIONAL + IDEMPOTENT: Fails if entity already exists for documentId
         */
        @Serializable
        @Resource("confirm")
        class Confirm(val parent: Id)

        /**
         * POST /api/v1/documents/{id}/chat
         * Send a chat message for single-document Q&A
         */
        @Serializable
        @Resource("chat")
        class Chat(val parent: Id)
    }
}
