@file:Suppress("LongParameterList") // Chat API requires multiple optional parameters

package tech.dokus.features.cashflow.usecase

import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.model.ai.ChatRequest
import tech.dokus.domain.model.ai.ChatResponse
import tech.dokus.domain.model.ai.ChatScope
import tech.dokus.domain.model.ai.ChatSessionId
import tech.dokus.features.cashflow.datasource.ChatRemoteDataSource
import tech.dokus.features.cashflow.usecases.SendChatMessageUseCase

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
 */
class SendChatMessageUseCaseImpl(
    private val chatRemoteDataSource: ChatRemoteDataSource
) : SendChatMessageUseCase {

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
    override suspend operator fun invoke(
        message: String,
        scope: ChatScope,
        documentId: DocumentId?,
        sessionId: ChatSessionId?,
        maxChunks: Int?,
        minSimilarity: Float?
    ): Result<ChatResponse> {
        require(message.isNotBlank()) { "Message cannot be blank" }

        val trimmed = message.trim()
        return when (scope) {
            ChatScope.SingleDoc -> {
                requireNotNull(documentId) {
                    "Document ID is required for single document chat"
                }
                chatRemoteDataSource.sendSingleDocumentMessage(
                    documentId = documentId,
                    request = ChatRequest(
                        message = trimmed,
                        scope = ChatScope.SingleDoc,
                        documentId = documentId,
                        sessionId = sessionId,
                        maxChunks = maxChunks,
                        minSimilarity = minSimilarity
                    )
                )
            }
            ChatScope.AllDocs -> {
                chatRemoteDataSource.sendCrossDocumentMessage(
                    request = ChatRequest(
                        message = trimmed,
                        scope = ChatScope.AllDocs,
                        documentId = null,
                        sessionId = sessionId,
                        maxChunks = maxChunks,
                        minSimilarity = minSimilarity
                    )
                )
            }
        }
    }
}
