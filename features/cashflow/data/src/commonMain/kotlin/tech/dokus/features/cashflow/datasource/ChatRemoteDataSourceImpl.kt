package tech.dokus.features.cashflow.datasource

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.resources.get
import io.ktor.client.plugins.resources.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.model.ai.ChatConfiguration
import tech.dokus.domain.model.ai.ChatHistoryResponse
import tech.dokus.domain.model.ai.ChatRequest
import tech.dokus.domain.model.ai.ChatResponse
import tech.dokus.domain.model.ai.ChatScope
import tech.dokus.domain.model.ai.ChatSessionId
import tech.dokus.domain.model.ai.ChatSessionListResponse
import tech.dokus.domain.routes.Chat
import tech.dokus.domain.routes.Documents

/**
 * HTTP-based implementation of ChatRemoteDataSource.
 * Uses Ktor HttpClient with type-safe routing to communicate with the chat API.
 *
 * All operations are authenticated via JWT and scoped to the user's tenant.
 */
internal class ChatRemoteDataSourceImpl(
    private val httpClient: HttpClient
) : ChatRemoteDataSource {

    // ============================================================================
    // CHAT MESSAGING
    // ============================================================================

    override suspend fun sendCrossDocumentMessage(request: ChatRequest): Result<ChatResponse> {
        return runCatching {
            httpClient.post(Chat()) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
        }
    }

    override suspend fun sendSingleDocumentMessage(
        documentId: DocumentId,
        request: ChatRequest
    ): Result<ChatResponse> {
        return runCatching {
            val documentRoute = Documents.Id(id = documentId.toString())
            httpClient.post(Documents.Id.Chat(parent = documentRoute)) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
        }
    }

    override suspend fun sendMessage(request: ChatRequest): Result<ChatResponse> {
        return when (request.scope) {
            ChatScope.SingleDoc -> {
                val documentId = request.documentId
                    ?: return Result.failure(
                        IllegalArgumentException("documentId is required for SINGLE_DOC scope")
                    )
                sendSingleDocumentMessage(documentId, request)
            }
            ChatScope.AllDocs -> {
                sendCrossDocumentMessage(request)
            }
        }
    }

    // ============================================================================
    // SESSION MANAGEMENT
    // ============================================================================

    override suspend fun listSessions(
        scope: ChatScope?,
        documentId: DocumentId?,
        page: Int,
        limit: Int
    ): Result<ChatSessionListResponse> {
        return runCatching {
            httpClient.get(
                Chat.Sessions(
                    scope = scope,
                    documentId = documentId,
                    page = page,
                    limit = limit
                )
            ).body()
        }
    }

    override suspend fun getSessionHistory(
        sessionId: ChatSessionId,
        page: Int,
        limit: Int,
        descending: Boolean
    ): Result<ChatHistoryResponse> {
        return runCatching {
            httpClient.get(
                Chat.Sessions.SessionId(
                    sessionId = sessionId,
                    page = page,
                    limit = limit,
                    descending = descending
                )
            ).body()
        }
    }

    // ============================================================================
    // CONFIGURATION
    // ============================================================================

    override suspend fun getConfiguration(): Result<ChatConfiguration> {
        return runCatching {
            httpClient.get(Chat.Config()).body()
        }
    }
}
