package tech.dokus.features.cashflow.presentation.cashflow.model.usecase

import tech.dokus.features.cashflow.repository.ChatRepositoryImpl
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.model.ai.ChatResponse
import tech.dokus.domain.model.ai.ChatScope
import tech.dokus.domain.model.ai.ChatSessionId

/**
 * Use case for sending chat messages to the document Q&A system.
 *
 * Supports two chat scopes:
 * - [ChatScope.SingleDoc]: Questions about a specific document
 * - [ChatScope.AllDocs]: Cross-document queries on all confirmed documents
 *
 * The AI uses RAG (Retrieval-Augmented Generation) to search relevant document
 * chunks and provide answers with source citations.
 *
 * Example usage:
 * ```
 * // Single document chat
 * val response = sendChatMessageUseCase(
 *     message = "What is the total VAT?",
 *     scope = ChatScope.SingleDoc,
 *     documentId = documentId
 * )
 *
 * // Cross-document chat
 * val response = sendChatMessageUseCase(
 *     message = "What are my total expenses this month?",
 *     scope = ChatScope.AllDocs
 * )
 * ```
 *
 * @see ChatRepositoryImpl for the underlying repository operations
 */
class SendChatMessageUseCase(
    private val chatRepository: ChatRepositoryImpl
) {

    /**
     * Send a chat message and receive an AI-generated response.
     *
     * The response includes the AI answer along with source citations
     * referencing the document chunks used to generate the answer.
     *
     * @param message The user's question or message
     * @param scope The chat scope (single document or all documents)
     * @param documentId The document ID (required for [ChatScope.SingleDoc])
     * @param sessionId Optional session ID to continue an existing conversation
     * @param maxChunks Maximum chunks to retrieve for context (default: 5)
     * @param minSimilarity Minimum similarity threshold for chunks (default: 0.7)
     * @return Result containing the AI response with citations or an error
     * @throws IllegalArgumentException if scope is SINGLE_DOC but documentId is null
     */
    suspend operator fun invoke(
        message: String,
        scope: ChatScope,
        documentId: DocumentId? = null,
        sessionId: ChatSessionId? = null,
        maxChunks: Int? = null,
        minSimilarity: Float? = null
    ): Result<ChatResponse> {
        require(message.isNotBlank()) { "Message cannot be blank" }

        return when (scope) {
            ChatScope.SingleDoc -> {
                requireNotNull(documentId) {
                    "Document ID is required for single document chat"
                }
                chatRepository.sendSingleDocumentMessage(
                    documentId = documentId,
                    message = message.trim(),
                    sessionId = sessionId,
                    maxChunks = maxChunks,
                    minSimilarity = minSimilarity
                )
            }
            ChatScope.AllDocs -> {
                chatRepository.sendCrossDocumentMessage(
                    message = message.trim(),
                    sessionId = sessionId,
                    maxChunks = maxChunks,
                    minSimilarity = minSimilarity
                )
            }
        }
    }

    /**
     * Start a new single-document chat session.
     *
     * Creates a new conversation session for the specified document.
     * Returns the session ID in the response for continuing the conversation.
     *
     * @param documentId The document to chat about
     * @param initialMessage The first question to ask
     * @return Result containing the initial response with new session ID
     */
    suspend fun startDocumentChat(
        documentId: DocumentId,
        initialMessage: String
    ): Result<ChatResponse> {
        require(initialMessage.isNotBlank()) { "Initial message cannot be blank" }

        return chatRepository.startDocumentChat(
            documentId = documentId,
            initialMessage = initialMessage.trim()
        )
    }

    /**
     * Start a new cross-document chat session.
     *
     * Creates a new conversation session for cross-document Q&A.
     * Returns the session ID in the response for continuing the conversation.
     *
     * @param initialMessage The first question to ask
     * @return Result containing the initial response with new session ID
     */
    suspend fun startCrossDocumentChat(
        initialMessage: String
    ): Result<ChatResponse> {
        require(initialMessage.isNotBlank()) { "Initial message cannot be blank" }

        return chatRepository.startCrossDocumentChat(
            initialMessage = initialMessage.trim()
        )
    }

    /**
     * Continue an existing chat session.
     *
     * Sends a follow-up message in an existing conversation,
     * maintaining the context from previous messages.
     *
     * @param sessionId The session to continue
     * @param message The follow-up question or message
     * @param scope The chat scope (should match the original session)
     * @param documentId The document ID (required for [ChatScope.SingleDoc])
     * @return Result containing the AI response
     */
    suspend fun continueChat(
        sessionId: ChatSessionId,
        message: String,
        scope: ChatScope,
        documentId: DocumentId? = null
    ): Result<ChatResponse> {
        require(message.isNotBlank()) { "Message cannot be blank" }

        return chatRepository.continueChat(
            sessionId = sessionId,
            message = message.trim(),
            scope = scope,
            documentId = documentId
        )
    }
}
