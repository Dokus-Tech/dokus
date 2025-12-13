package ai.dokus.foundation.domain.routes

import io.ktor.resources.*
import kotlinx.serialization.Serializable

/**
 * Type-safe route definitions for Attachment API.
 * Base path: /api/v1/attachments
 *
 * Note: Attachments are typically accessed through their parent resources
 * (invoices, expenses, etc.). This endpoint provides direct access by ID.
 *
 * SECURITY: All operations are scoped to the authenticated user's tenant via JWT.
 */
@Serializable
@Resource("/api/v1/attachments")
class Attachments {
    /**
     * /api/v1/attachments/{id} - Single attachment operations
     * GET - Get attachment metadata
     * DELETE - Delete attachment
     */
    @Serializable
    @Resource("{id}")
    class Id(val parent: Attachments = Attachments(), val id: String) {
        /**
         * GET /api/v1/attachments/{id}/url
         * Get signed download URL for attachment
         */
        @Serializable
        @Resource("url")
        class Url(val parent: Id)
    }
}
