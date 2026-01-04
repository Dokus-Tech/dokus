package tech.dokus.features.cashflow.usecase

import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.model.ai.ChatConfiguration
import tech.dokus.domain.model.ai.ChatHistoryResponse
import tech.dokus.domain.model.ai.ChatScope
import tech.dokus.domain.model.ai.ChatSessionId
import tech.dokus.domain.model.ai.ChatSessionListResponse
import tech.dokus.features.cashflow.datasource.ChatRemoteDataSource
import tech.dokus.features.cashflow.usecases.GetChatConfigurationUseCase
import tech.dokus.features.cashflow.usecases.GetChatSessionHistoryUseCase
import tech.dokus.features.cashflow.usecases.ListChatSessionsUseCase

internal class GetChatConfigurationUseCaseImpl(
    private val chatRemoteDataSource: ChatRemoteDataSource
) : GetChatConfigurationUseCase {
    override suspend fun invoke(): Result<ChatConfiguration> {
        return chatRemoteDataSource.getConfiguration()
    }
}

internal class ListChatSessionsUseCaseImpl(
    private val chatRemoteDataSource: ChatRemoteDataSource
) : ListChatSessionsUseCase {
    override suspend fun invoke(
        scope: ChatScope?,
        documentId: DocumentId?,
        page: Int,
        limit: Int
    ): Result<ChatSessionListResponse> {
        return chatRemoteDataSource.listSessions(
            scope = scope,
            documentId = documentId,
            page = page,
            limit = limit
        )
    }
}

internal class GetChatSessionHistoryUseCaseImpl(
    private val chatRemoteDataSource: ChatRemoteDataSource
) : GetChatSessionHistoryUseCase {
    override suspend fun invoke(
        sessionId: ChatSessionId,
        page: Int,
        limit: Int,
        descending: Boolean
    ): Result<ChatHistoryResponse> {
        return chatRemoteDataSource.getSessionHistory(
            sessionId = sessionId,
            page = page,
            limit = limit,
            descending = descending
        )
    }
}
