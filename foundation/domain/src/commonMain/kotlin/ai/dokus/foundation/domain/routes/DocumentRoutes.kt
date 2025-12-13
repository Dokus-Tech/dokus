package ai.dokus.foundation.domain.routes

import io.ktor.resources.*
import kotlinx.serialization.Serializable

/**
 * Type-safe route definitions for Document Management API.
 * Base path: /api/v1/documents
 *
 * SECURITY: All operations are scoped to the authenticated user's tenant via JWT.
 */
@Serializable
@Resource("/api/v1/documents")
class Documents(
    val limit: Int = 50,
    val offset: Int = 0
) {
    /**
     * POST /api/v1/documents/upload
     * Upload a new document
     */
    @Serializable
    @Resource("upload")
    class Upload(val parent: Documents = Documents())

    /**
     * GET /api/v1/documents/processing
     * List documents in processing queue with status filter
     * Status can be comma-separated list (e.g., "PENDING,PROCESSING")
     */
    @Serializable
    @Resource("processing")
    class Processing(
        val parent: Documents = Documents(),
        val status: String? = null,
        val page: Int = 1,
        val limit: Int = 20
    )

    /**
     * /api/v1/documents/{id} - Single document operations
     * GET - Retrieve document
     * DELETE - Delete document
     */
    @Serializable
    @Resource("{id}")
    class Id(val parent: Documents = Documents(), val id: String) {
        /**
         * GET /api/v1/documents/{id}/processing
         * Get processing details for this document
         */
        @Serializable
        @Resource("processing")
        class Processing(val parent: Id)

        /**
         * GET/PATCH /api/v1/documents/{id}/status
         * GET - Get document processing status
         * PATCH - Update status (confirm, reject)
         */
        @Serializable
        @Resource("status")
        class Status(val parent: Id)

        /**
         * POST /api/v1/documents/{id}/processing-jobs
         * Creates a new processing job (reprocesses document)
         */
        @Serializable
        @Resource("processing-jobs")
        class ProcessingJobs(val parent: Id)
    }
}
