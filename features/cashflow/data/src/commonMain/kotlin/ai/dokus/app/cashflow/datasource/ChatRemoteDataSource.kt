package ai.dokus.app.cashflow.datasource

import tech.dokus.domain.ids.DocumentProcessingId
import tech.dokus.domain.model.ChatConfiguration
import tech.dokus.domain.model.ChatHistoryResponse
import tech.dokus.domain.model.ChatRequest
import tech.dokus.domain.model.ChatResponse
import tech.dokus.domain.model.ChatScope
import tech.dokus.domain.model.ChatSessionId
import tech.dokus.domain.model.ChatSessionListResponse
import io.ktor.client.HttpClient

/**
 * Remote data source for chat operations.
 * Provides HTTP-based access to the document chat and RAG Q&A endpoints.
 *
 * This interface supports two chat scopes:
 * - SINGLE_DOC: Questions about a specific document (uses /api/v1/documents/{id}/chat)
 * - ALL_DOCS: Cross-document queries on all confirmed documents (uses /api/v1/chat)
 *
 * SECURITY: All operations are scoped to the authenticated user's tenant via JWT.
 */
interface ChatRemoteDataSource {

    // ============================================================================
    // CHAT MESSAGING
    // ============================================================================

    /**
     * Send a chat message for cross-document Q&A.
     * POST /api/v1/chat
     *
     * Use this for questions across all confirmed documents.
     * The request scope should be ALL_DOCS.
     *
     * @param request Chat request with message, scope, and optional session ID
     * @return Chat response with AI-generated answer and source citations
     */
    suspend fun sendCrossDocumentMessage(request: ChatRequest): Result<ChatResponse>

    /**
     * Send a chat message for single-document Q&A.
     * POST /api/v1/documents/{id}/chat
     *
     * Use this for questions about a specific document.
     * The request scope should be SINGLE_DOC.
     *
     * @param documentId The document to chat about
     * @param request Chat request with message, scope, and optional session ID
     * @return Chat response with AI-generated answer and source citations
     */
    suspend fun sendSingleDocumentMessage(
        documentId: DocumentProcessingId,
        request: ChatRequest
    ): Result<ChatResponse>

    /**
     * Convenience method that routes to the appropriate endpoint based on scope.
     *
     * @param request Chat request with message, scope, and optional session/document IDs
     * @return Chat response with AI-generated answer and source citations
     */
    suspend fun sendMessage(request: ChatRequest): Result<ChatResponse>

    // ============================================================================
    // SESSION MANAGEMENT
    // ============================================================================

    /**
     * List chat sessions for the current user.
     * GET /api/v1/chat/sessions
     *
     * @param scope Optional filter by scope (SINGLE_DOC or ALL_DOCS)
     * @param documentId Optional filter by document ID (for SINGLE_DOC sessions)
     * @param page Page number (0-indexed)
     * @param limit Items per page
     * @return Paginated list of chat sessions
     */
    suspend fun listSessions(
        scope: ChatScope? = null,
        documentId: DocumentProcessingId? = null,
        page: Int = 0,
        limit: Int = 20
    ): Result<ChatSessionListResponse>

    /**
     * Get session history with all messages.
     * GET /api/v1/chat/sessions/{sessionId}
     *
     * @param sessionId The session to retrieve
     * @param page Page number (0-indexed)
     * @param limit Items per page
     * @param descending Order by newest first if true
     * @return Session summary with paginated message history
     */
    suspend fun getSessionHistory(
        sessionId: ChatSessionId,
        page: Int = 0,
        limit: Int = 50,
        descending: Boolean = false
    ): Result<ChatHistoryResponse>

    // ============================================================================
    // CONFIGURATION
    // ============================================================================

    /**
     * Get chat configuration for client UI capabilities.
     * GET /api/v1/chat/config
     *
     * Use this to determine:
     * - Maximum message length
     * - Available AI models
     * - Streaming support
     * - Cross-document chat availability
     *
     * @return Chat configuration with client capabilities
     */
    suspend fun getConfiguration(): Result<ChatConfiguration>

    companion object {
        internal fun create(httpClient: HttpClient): ChatRemoteDataSource {
            return ChatRemoteDataSourceImpl(httpClient)
        }
    }
}
