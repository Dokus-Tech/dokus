package ai.dokus.app.cashflow.datasource

import ai.dokus.foundation.domain.ids.DocumentProcessingId
import tech.dokus.domain.model.ChatConfiguration
import tech.dokus.domain.model.ChatHistoryResponse
import tech.dokus.domain.model.ChatRequest
import tech.dokus.domain.model.ChatResponse
import tech.dokus.domain.model.ChatScope
import tech.dokus.domain.model.ChatSessionId
import tech.dokus.domain.model.ChatSessionListResponse
import tech.dokus.domain.routes.Chat
import tech.dokus.domain.routes.Documents
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.resources.get
import io.ktor.client.plugins.resources.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

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
        documentId: DocumentProcessingId,
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
                val documentId = request.documentProcessingId
                    ?: return Result.failure(
                        IllegalArgumentException("documentProcessingId is required for SINGLE_DOC scope")
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
        documentId: DocumentProcessingId?,
        page: Int,
        limit: Int
    ): Result<ChatSessionListResponse> {
        return runCatching {
            httpClient.get(
                Chat.Sessions(
                    scope = scope?.dbValue,
                    documentId = documentId?.toString(),
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
                    sessionId = sessionId.toString(),
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
