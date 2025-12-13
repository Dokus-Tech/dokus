package ai.dokus.foundation.domain.routes

import io.ktor.resources.*
import kotlinx.serialization.Serializable

/**
 * Type-safe route definitions for Attachment API.
 * Base path: /api/v1/attachments
 */
@Serializable
@Resource("/api/v1/attachments")
class Attachments {
    /**
     * /api/v1/attachments/{id} - Single attachment operations
     */
    @Serializable
    @Resource("{id}")
    class Id(val parent: Attachments = Attachments(), val id: String) {
        /**
         * GET /api/v1/attachments/{id}/download-url - Get signed download URL
         */
        @Serializable
        @Resource("download-url")
        class DownloadUrl(val parent: Id)
    }
}
