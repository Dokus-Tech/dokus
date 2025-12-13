package ai.dokus.foundation.domain.routes

import io.ktor.resources.*
import kotlinx.serialization.Serializable

/**
 * Type-safe route definitions for Document Management API.
 * Base path: /api/v1/documents
 */
@Serializable
@Resource("/api/v1/documents")
class Documents {
    /**
     * POST /api/v1/documents/upload - Upload a document for processing
     */
    @Serializable
    @Resource("upload")
    class Upload(val parent: Documents = Documents())

    /**
     * GET /api/v1/documents/processing - List documents in processing queue
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
     */
    @Serializable
    @Resource("{id}")
    class Id(val parent: Documents = Documents(), val id: String) {
        /**
         * GET /api/v1/documents/{id}/processing - Get document processing status
         */
        @Serializable
        @Resource("processing")
        class Processing(val parent: Id)

        /**
         * POST /api/v1/documents/{id}/confirm - Confirm processed document
         */
        @Serializable
        @Resource("confirm")
        class Confirm(val parent: Id)

        /**
         * POST /api/v1/documents/{id}/reject - Reject processed document
         */
        @Serializable
        @Resource("reject")
        class Reject(val parent: Id)

        /**
         * POST /api/v1/documents/{id}/reprocess - Reprocess document
         */
        @Serializable
        @Resource("reprocess")
        class Reprocess(val parent: Id)
    }
}
