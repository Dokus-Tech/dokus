package tech.dokus.domain.routes

import io.ktor.resources.*
import kotlinx.serialization.Serializable
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.model.ai.ChatScope

/**
 * Type-safe route definitions for Chat API.
 * Base path: /api/v1/chat
 *
 * Provides endpoints for:
 * - Cross-document chat (questions across all confirmed documents)
 * - Session management (listing and retrieving chat history)
 * - Configuration (client capabilities)
 *
 * For single-document chat, use Documents.Id.Chat instead.
 *
 * SECURITY: All operations are scoped to the authenticated user's tenant via JWT.
 */
@Serializable
@Resource("/api/v1/chat")
class Chat {

    /**
     * GET /api/v1/chat/sessions
     * List chat sessions for the current user
     *
     * Query parameters:
     * - scope: Optional filter by scope (SingleDoc or AllDocs)
     * - documentId: Optional filter by document ID
     * - page: Page number (default: 0)
     * - limit: Items per page (default: 20)
     */
    @Serializable
    @Resource("sessions")
    class Sessions(
        val parent: Chat = Chat(),
        val scope: ChatScope? = null,
        val documentId: DocumentId? = null,
        val page: Int = 0,
        val limit: Int = 20
    ) {
        /**
         * GET /api/v1/chat/sessions/{sessionId}
         * Get session history with all messages
         *
         * Query parameters:
         * - page: Page number (default: 0)
         * - limit: Items per page (default: 50)
         * - descending: Order by newest first (default: false)
         */
        @Serializable
        @Resource("{sessionId}")
        class SessionId(
            val parent: Sessions = Sessions(),
            val sessionId: String,
            val page: Int = 0,
            val limit: Int = 50,
            val descending: Boolean = false
        )
    }

    /**
     * GET /api/v1/chat/config
     * Get chat configuration for client UI capabilities
     */
    @Serializable
    @Resource("config")
    class Config(val parent: Chat = Chat())
}
