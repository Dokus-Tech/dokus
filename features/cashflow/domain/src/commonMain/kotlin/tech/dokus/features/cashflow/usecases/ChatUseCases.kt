@file:Suppress("LongParameterList") // Chat APIs require multiple optional parameters

package tech.dokus.features.cashflow.usecases

import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.model.ai.ChatConfiguration
import tech.dokus.domain.model.ai.ChatHistoryResponse
import tech.dokus.domain.model.ai.ChatResponse
import tech.dokus.domain.model.ai.ChatScope
import tech.dokus.domain.model.ai.ChatSessionId
import tech.dokus.domain.model.ai.ChatSessionListResponse

/**
 * Use case for sending chat messages.
 */
interface SendChatMessageUseCase {
    suspend operator fun invoke(
        message: String,
        scope: ChatScope,
        documentId: DocumentId? = null,
        sessionId: ChatSessionId? = null,
        maxChunks: Int? = null,
        minSimilarity: Float? = null
    ): Result<ChatResponse>
}

/**
 * Use case for retrieving chat UI configuration.
 */
interface GetChatConfigurationUseCase {
    suspend operator fun invoke(): Result<ChatConfiguration>
}

/**
 * Use case for listing chat sessions.
 */
interface ListChatSessionsUseCase {
    suspend operator fun invoke(
        scope: ChatScope? = null,
        documentId: DocumentId? = null,
        page: Int = 0,
        limit: Int = 20
    ): Result<ChatSessionListResponse>
}

/**
 * Use case for fetching chat session history.
 */
interface GetChatSessionHistoryUseCase {
    suspend operator fun invoke(
        sessionId: ChatSessionId,
        page: Int = 0,
        limit: Int = 50,
        descending: Boolean = false
    ): Result<ChatHistoryResponse>
}
