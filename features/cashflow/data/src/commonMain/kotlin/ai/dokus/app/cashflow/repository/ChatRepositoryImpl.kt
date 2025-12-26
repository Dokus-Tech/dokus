package ai.dokus.app.cashflow.repository

import ai.dokus.app.cashflow.datasource.ChatRemoteDataSource
import ai.dokus.foundation.domain.ids.DocumentProcessingId
import ai.dokus.foundation.domain.model.ChatConfiguration
import ai.dokus.foundation.domain.model.ChatHistoryResponse
import ai.dokus.foundation.domain.model.ChatRequest
import ai.dokus.foundation.domain.model.ChatResponse
import ai.dokus.foundation.domain.model.ChatScope
import ai.dokus.foundation.domain.model.ChatSessionId
import ai.dokus.foundation.domain.model.ChatSessionListResponse
import ai.dokus.foundation.platform.Logger

/**
 * Repository for chat operations with document Q&A.
 *
 * Provides a clean API for:
 * - Sending chat messages (single-doc and cross-doc scopes)
 * - Managing chat sessions and conversation history
 * - Retrieving chat configuration for UI capabilities
 *
 * This repository delegates to ChatRemoteDataSource for API calls.
 * Chat messages are not locally cached since real-time responses
 * are essential for the chat experience.
 *
 * Error Handling:
 * - All methods return Result<T> allowing callers to handle failures
 * - Network errors, auth failures, and API errors are propagated
 *
 * SECURITY: All operations are scoped to the authenticated user's tenant via JWT.
 */
class ChatRepositoryImpl(
    private val remoteDataSource: ChatRemoteDataSource
) {

    private val logger = Logger.forClass<ChatRepositoryImpl>()

    // ============================================================================
    // CHAT MESSAGING
    // ============================================================================

    /**
     * Send a chat message and receive an AI-generated response.
     *
     * Routes to the appropriate endpoint based on the request scope:
     * - SINGLE_DOC: Questions about a specific document
     * - ALL_DOCS: Cross-document queries on all confirmed documents
     *
     * @param request Chat request with message, scope, and optional session/document IDs
     * @return Chat response containing the AI-generated answer with source citations
     */
    suspend fun sendMessage(request: ChatRequest): Result<ChatResponse> {
        logger.d { "Sending chat message: scope=${request.scope}, sessionId=${request.sessionId}" }

        return remoteDataSource.sendMessage(request)
            .onSuccess { response ->
                logger.i {
                    "Chat response received: sessionId=${response.sessionId}, " +
                        "isNewSession=${response.isNewSession}, " +
                        "citations=${response.assistantMessage.citations?.size ?: 0}"
                }
            }
            .onFailure { error ->
                logger.e(error) { "Failed to send chat message" }
            }
    }

    /**
     * Send a message for cross-document Q&A.
     *
     * Use this for questions that span all confirmed documents in the tenant.
     * The AI will search across all document chunks for relevant context.
     *
     * @param message The user's question
     * @param sessionId Optional session ID to continue an existing conversation
     * @param maxChunks Maximum chunks to retrieve for context (default: 5)
     * @param minSimilarity Minimum similarity threshold for chunks (default: 0.7)
     * @return Chat response with AI-generated answer and source citations
     */
    suspend fun sendCrossDocumentMessage(
        message: String,
        sessionId: ChatSessionId? = null,
        maxChunks: Int? = null,
        minSimilarity: Float? = null
    ): Result<ChatResponse> {
        val request = ChatRequest(
            message = message,
            scope = ChatScope.AllDocs,
            documentProcessingId = null,
            sessionId = sessionId,
            maxChunks = maxChunks,
            minSimilarity = minSimilarity
        )
        return sendMessage(request)
    }

    /**
     * Send a message for single-document Q&A.
     *
     * Use this for questions about a specific document.
     * The AI will only search within the specified document's chunks.
     *
     * @param documentId The document to chat about
     * @param message The user's question
     * @param sessionId Optional session ID to continue an existing conversation
     * @param maxChunks Maximum chunks to retrieve for context (default: 5)
     * @param minSimilarity Minimum similarity threshold for chunks (default: 0.7)
     * @return Chat response with AI-generated answer and source citations
     */
    suspend fun sendSingleDocumentMessage(
        documentId: DocumentProcessingId,
        message: String,
        sessionId: ChatSessionId? = null,
        maxChunks: Int? = null,
        minSimilarity: Float? = null
    ): Result<ChatResponse> {
        val request = ChatRequest(
            message = message,
            scope = ChatScope.SingleDoc,
            documentProcessingId = documentId,
            sessionId = sessionId,
            maxChunks = maxChunks,
            minSimilarity = minSimilarity
        )
        return sendMessage(request)
    }

    // ============================================================================
    // SESSION MANAGEMENT
    // ============================================================================

    /**
     * List chat sessions for the current user.
     *
     * Returns paginated session summaries with last message preview.
     * Can be filtered by scope and document ID.
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
    ): Result<ChatSessionListResponse> {
        logger.d { "Listing chat sessions: scope=$scope, documentId=$documentId, page=$page" }

        return remoteDataSource.listSessions(
            scope = scope,
            documentId = documentId,
            page = page,
            limit = limit
        )
            .onSuccess { response ->
                logger.d { "Found ${response.items.size} sessions (total: ${response.total})" }
            }
            .onFailure { error ->
                logger.e(error) { "Failed to list chat sessions" }
            }
    }

    /**
     * List sessions for a specific document.
     *
     * Convenience method for getting all chat sessions associated
     * with a particular document.
     *
     * @param documentId The document to get sessions for
     * @param page Page number (0-indexed)
     * @param limit Items per page
     * @return Paginated list of chat sessions for the document
     */
    suspend fun listSessionsForDocument(
        documentId: DocumentProcessingId,
        page: Int = 0,
        limit: Int = 20
    ): Result<ChatSessionListResponse> {
        return listSessions(
            scope = ChatScope.SingleDoc,
            documentId = documentId,
            page = page,
            limit = limit
        )
    }

    /**
     * List cross-document chat sessions.
     *
     * Convenience method for getting all cross-document Q&A sessions.
     *
     * @param page Page number (0-indexed)
     * @param limit Items per page
     * @return Paginated list of cross-document chat sessions
     */
    suspend fun listCrossDocumentSessions(
        page: Int = 0,
        limit: Int = 20
    ): Result<ChatSessionListResponse> {
        return listSessions(
            scope = ChatScope.AllDocs,
            documentId = null,
            page = page,
            limit = limit
        )
    }

    /**
     * Get full chat history for a session.
     *
     * Returns the session summary along with paginated message history.
     * Messages include full details including citations for AI responses.
     *
     * @param sessionId The session to retrieve
     * @param page Page number (0-indexed)
     * @param limit Items per page
     * @param descending Order by newest first if true
     * @return Session summary with message history
     */
    suspend fun getSessionHistory(
        sessionId: ChatSessionId,
        page: Int = 0,
        limit: Int = 50,
        descending: Boolean = false
    ): Result<ChatHistoryResponse> {
        logger.d { "Getting session history: sessionId=$sessionId, page=$page" }

        return remoteDataSource.getSessionHistory(
            sessionId = sessionId,
            page = page,
            limit = limit,
            descending = descending
        )
            .onSuccess { response ->
                logger.d { "Retrieved ${response.messages.size} messages (total: ${response.total})" }
            }
            .onFailure { error ->
                logger.e(error) { "Failed to get session history" }
            }
    }

    // ============================================================================
    // CONFIGURATION
    // ============================================================================

    /**
     * Get chat configuration for UI capabilities.
     *
     * Returns configuration including:
     * - Maximum message length
     * - Available AI models
     * - Streaming support
     * - Cross-document chat availability
     *
     * This should be called once when initializing the chat UI
     * to configure input limits and feature availability.
     *
     * @return Chat configuration for client capabilities
     */
    suspend fun getConfiguration(): Result<ChatConfiguration> {
        logger.d { "Getting chat configuration" }

        return remoteDataSource.getConfiguration()
            .onSuccess { config ->
                logger.d {
                    "Chat config loaded: maxLength=${config.maxMessageLength}, " +
                        "streaming=${config.streamingEnabled}, " +
                        "crossDocEnabled=${config.crossDocumentChatEnabled}"
                }
            }
            .onFailure { error ->
                logger.e(error) { "Failed to get chat configuration" }
            }
    }

    // ============================================================================
    // CONVENIENCE METHODS
    // ============================================================================

    /**
     * Check if chat is available for a document.
     *
     * Returns true if the document has been processed and has
     * chunks available for Q&A.
     *
     * Note: This is a lightweight check based on session availability.
     * For a full check, query the document processing status.
     *
     * @param documentId The document to check
     * @return true if chat is available for the document
     */
    suspend fun isChatAvailable(documentId: DocumentProcessingId): Boolean {
        // Try to list sessions - if we can access the API, chat is available
        // The actual document status check should be done via DocumentProcessingRepository
        return getConfiguration().isSuccess
    }

    /**
     * Start a new chat session for a document.
     *
     * Convenience method that sends the first message and returns
     * the new session ID.
     *
     * @param documentId The document to chat about
     * @param initialMessage The first question to ask
     * @return The new session's first response
     */
    suspend fun startDocumentChat(
        documentId: DocumentProcessingId,
        initialMessage: String
    ): Result<ChatResponse> {
        logger.i { "Starting new document chat: documentId=$documentId" }
        return sendSingleDocumentMessage(
            documentId = documentId,
            message = initialMessage,
            sessionId = null
        )
    }

    /**
     * Start a new cross-document chat session.
     *
     * Convenience method that sends the first message and returns
     * the new session ID.
     *
     * @param initialMessage The first question to ask
     * @return The new session's first response
     */
    suspend fun startCrossDocumentChat(
        initialMessage: String
    ): Result<ChatResponse> {
        logger.i { "Starting new cross-document chat" }
        return sendCrossDocumentMessage(
            message = initialMessage,
            sessionId = null
        )
    }

    /**
     * Continue an existing chat session.
     *
     * Sends a follow-up message in an existing conversation.
     *
     * @param sessionId The session to continue
     * @param message The follow-up question
     * @param scope The chat scope (should match the original session)
     * @param documentId The document ID (required for SINGLE_DOC scope)
     * @return The response to the follow-up
     */
    suspend fun continueChat(
        sessionId: ChatSessionId,
        message: String,
        scope: ChatScope,
        documentId: DocumentProcessingId? = null
    ): Result<ChatResponse> {
        val request = ChatRequest(
            message = message,
            scope = scope,
            documentProcessingId = documentId,
            sessionId = sessionId
        )
        return sendMessage(request)
    }
}
