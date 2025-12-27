package tech.dokus.backend.routes.cashflow

import ai.dokus.ai.agents.ChatAgent
import ai.dokus.ai.agents.ConversationMessage
import ai.dokus.ai.config.AIConfig
import ai.dokus.ai.config.AIProviderFactory
import ai.dokus.ai.config.ModelPurpose
import ai.dokus.ai.services.EmbeddingService
import ai.dokus.ai.services.RAGService
import ai.dokus.foundation.database.repository.cashflow.DocumentProcessingRepository
import ai.dokus.foundation.domain.repository.ChatRepository
import ai.dokus.foundation.domain.repository.ChunkRepository
import ai.dokus.foundation.domain.exceptions.DokusException
import ai.dokus.foundation.domain.ids.DocumentProcessingId
import ai.dokus.foundation.domain.ids.TenantId
import ai.dokus.foundation.domain.ids.UserId
import ai.dokus.foundation.domain.model.ChatCitation
import ai.dokus.foundation.domain.model.ChatConfiguration
import ai.dokus.foundation.domain.model.ChatHistoryResponse
import ai.dokus.foundation.domain.model.ChatMessageDto
import ai.dokus.foundation.domain.model.ChatMessageId
import ai.dokus.foundation.domain.model.ChatRequest
import ai.dokus.foundation.domain.model.ChatResponse
import ai.dokus.foundation.domain.model.ChatResponseMetadata
import ai.dokus.foundation.domain.model.ChatScope
import ai.dokus.foundation.domain.model.ChatSessionId
import ai.dokus.foundation.domain.model.ChatSessionListResponse
import ai.dokus.foundation.domain.model.MessageRole
import ai.dokus.foundation.ktor.security.authenticateJwt
import ai.dokus.foundation.ktor.security.dokusPrincipal
import io.ktor.client.HttpClient
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.ktor.ext.inject
import org.slf4j.LoggerFactory
import ai.dokus.ai.agents.MessageRole as AgentMessageRole

/**
 * Chat routes for RAG-powered document Q&A.
 *
 * Provides endpoints for both single-document and cross-document chat scopes:
 *
 * Endpoints:
 * - POST /api/v1/chat - Cross-document chat (questions across all confirmed documents)
 * - POST /api/v1/documents/{id}/chat - Single-document chat (questions about a specific document)
 * - GET /api/v1/chat/sessions - List chat sessions for the current user
 * - GET /api/v1/chat/sessions/{sessionId} - Get session history with messages
 * - GET /api/v1/chat/config - Get chat configuration for client capabilities
 *
 * SECURITY: All endpoints filter by tenantId from the authenticated user's JWT.
 */
internal fun Route.chatRoutes() {
    val chatRepository by inject<ChatRepository>()
    val chunksRepository by inject<ChunkRepository>()
    val processingRepository by inject<DocumentProcessingRepository>()
    val httpClient by inject<HttpClient>()
    val aiConfig by inject<AIConfig>()
    val logger = LoggerFactory.getLogger("ChatRoutes")

    // Create AI services for RAG and chat
    val embeddingService = EmbeddingService(httpClient, aiConfig)
    val ragService = RAGService(embeddingService, chunksRepository)
    val executor = AIProviderFactory.createExecutor(aiConfig)
    val model = AIProviderFactory.getModel(aiConfig, ModelPurpose.CHAT)
    val chatAgent = ChatAgent(executor, model, ragService)

    authenticateJwt {

        // =========================================================================
        // Cross-Document Chat
        // =========================================================================

        /**
         * POST /api/v1/chat
         * Send a chat message for cross-document Q&A.
         *
         * Request body: ChatRequest
         * - message: The user's question
         * - scope: Should be ALL_DOCS for cross-document chat
         * - sessionId: Optional session ID to continue a conversation
         * - maxChunks: Optional max chunks to retrieve (default: 5)
         * - minSimilarity: Optional minimum similarity threshold
         *
         * Response: ChatResponse with AI-generated answer and citations
         */
        post("/api/v1/chat") {
            val tenantId = dokusPrincipal.requireTenantId()
            val userId = dokusPrincipal.userId
            val request = call.receive<ChatRequest>()

            logger.info("Cross-doc chat request: tenant=$tenantId, sessionId=${request.sessionId}")

            // Validate scope
            if (request.scope != ChatScope.AllDocs) {
                throw DokusException.BadRequest(
                    "Invalid scope for cross-document chat. Use /api/v1/documents/{id}/chat for single-document chat."
                )
            }

            val response = processChat(
                request = request,
                tenantId = tenantId,
                userId = userId,
                documentId = null,
                chatAgent = chatAgent,
                chatRepository = chatRepository,
                aiConfig = aiConfig,
                logger = logger
            )

            call.respond(HttpStatusCode.OK, response)
        }

        // =========================================================================
        // Single-Document Chat
        // =========================================================================

        /**
         * POST /api/v1/documents/{id}/chat
         * Send a chat message for single-document Q&A.
         *
         * Path parameters:
         * - id: Document processing ID
         *
         * Request body: ChatRequest
         * - message: The user's question
         * - scope: Should be SINGLE_DOC for single-document chat
         * - sessionId: Optional session ID to continue a conversation
         *
         * Response: ChatResponse with AI-generated answer and citations
         */
        post("/api/v1/documents/{id}/chat") {
            val tenantId = dokusPrincipal.requireTenantId()
            val userId = dokusPrincipal.userId
            val documentIdParam = call.parameters["id"]
                ?: throw DokusException.BadRequest("Document ID is required")

            val documentId = try {
                DocumentProcessingId.parse(documentIdParam)
            } catch (e: Exception) {
                throw DokusException.BadRequest("Invalid document ID format")
            }

            val request = call.receive<ChatRequest>()

            logger.info("Single-doc chat request: tenant=$tenantId, document=$documentId")

            // Verify document exists and belongs to tenant
            val processing = processingRepository.getById(
                processingId = documentId,
                tenantId = tenantId,
                includeDocument = false
            )
            if (processing == null) {
                throw DokusException.NotFound("Document not found")
            }

            // Validate scope
            if (request.scope != ChatScope.SingleDoc) {
                throw DokusException.BadRequest(
                    "Invalid scope for single-document chat. Use /api/v1/chat for cross-document chat."
                )
            }

            val response = processChat(
                request = request,
                tenantId = tenantId,
                userId = userId,
                documentId = documentId,
                chatAgent = chatAgent,
                chatRepository = chatRepository,
                aiConfig = aiConfig,
                logger = logger
            )

            call.respond(HttpStatusCode.OK, response)
        }

        // =========================================================================
        // Session Management
        // =========================================================================

        /**
         * GET /api/v1/chat/sessions
         * List chat sessions for the current user.
         *
         * Query parameters:
         * - scope: Optional filter by scope (SINGLE_DOC or ALL_DOCS)
         * - documentId: Optional filter by document ID
         * - page: Page number (default: 0)
         * - limit: Items per page (default: 20)
         *
         * Response: ChatSessionListResponse
         */
        get("/api/v1/chat/sessions") {
            val tenantId = dokusPrincipal.requireTenantId()
            val scopeParam = call.request.queryParameters["scope"]
            val documentIdParam = call.request.queryParameters["documentId"]
            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 0
            val limit = call.request.queryParameters["limit"]?.toIntOrNull()?.coerceIn(1, 100) ?: 20

            val scope = scopeParam?.let {
                try {
                    ChatScope.valueOf(it.uppercase().replace("-", "_"))
                } catch (e: Exception) {
                    ChatScope.fromDbValue(it)
                }
            }

            val documentId = documentIdParam?.let {
                try {
                    DocumentProcessingId.parse(it)
                } catch (e: Exception) {
                    throw DokusException.BadRequest("Invalid document ID format")
                }
            }

            logger.debug("Listing chat sessions: tenant=$tenantId, scope=$scope, page=$page")

            val (sessions, total) = chatRepository.listSessions(
                tenantId = tenantId,
                scope = scope,
                documentProcessingId = documentId,
                limit = limit,
                offset = page * limit
            )

            call.respond(
                HttpStatusCode.OK,
                ChatSessionListResponse(
                    items = sessions,
                    total = total,
                    page = page,
                    limit = limit,
                    hasMore = (page + 1) * limit < total
                )
            )
        }

        /**
         * GET /api/v1/chat/sessions/{sessionId}
         * Get session history with all messages.
         *
         * Path parameters:
         * - sessionId: Chat session ID
         *
         * Query parameters:
         * - page: Page number (default: 0)
         * - limit: Items per page (default: 50)
         * - descending: Order by newest first (default: false)
         *
         * Response: ChatHistoryResponse
         */
        get("/api/v1/chat/sessions/{sessionId}") {
            val tenantId = dokusPrincipal.requireTenantId()
            val sessionIdParam = call.parameters["sessionId"]
                ?: throw DokusException.BadRequest("Session ID is required")

            val sessionId = try {
                ChatSessionId.parse(sessionIdParam)
            } catch (e: Exception) {
                throw DokusException.BadRequest("Invalid session ID format")
            }

            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 0
            val limit = call.request.queryParameters["limit"]?.toIntOrNull()?.coerceIn(1, 100) ?: 50
            val descending = call.request.queryParameters["descending"]?.toBoolean() ?: false

            logger.debug("Getting session history: tenant=$tenantId, session=$sessionId")

            // Get session summary
            val sessionSummary = chatRepository.getSessionSummary(tenantId, sessionId)
                ?: throw DokusException.NotFound("Chat session not found")

            // Get messages
            val (messages, total) = chatRepository.getSessionMessages(
                tenantId = tenantId,
                sessionId = sessionId,
                limit = limit,
                offset = page * limit,
                descending = descending
            )

            call.respond(
                HttpStatusCode.OK,
                ChatHistoryResponse(
                    sessionId = sessionId,
                    session = sessionSummary,
                    messages = messages,
                    total = total,
                    page = page,
                    limit = limit,
                    hasMore = (page + 1) * limit < total
                )
            )
        }

        // =========================================================================
        // Configuration
        // =========================================================================

        /**
         * GET /api/v1/chat/config
         * Get chat configuration for client UI capabilities.
         *
         * Response: ChatConfiguration
         */
        get("/api/v1/chat/config") {
            val configuration = ChatConfiguration(
                maxMessageLength = 4000,
                maxChunksPerQuery = 10,
                defaultChunksPerQuery = 5,
                streamingEnabled = false,
                availableModels = listOf(aiConfig.models.chat),
                defaultModel = aiConfig.models.chat,
                crossDocumentChatEnabled = true,
                minDocumentsForCrossDocChat = 1
            )

            call.respond(HttpStatusCode.OK, configuration)
        }
    }
}

/**
 * Process a chat request and generate a response.
 *
 * Handles:
 * - Session management (new or existing)
 * - Conversation history retrieval
 * - ChatAgent invocation
 * - Message persistence
 * - Citation mapping
 */
private suspend fun processChat(
    request: ChatRequest,
    tenantId: TenantId,
    userId: UserId,
    documentId: DocumentProcessingId?,
    chatAgent: ChatAgent,
    chatRepository: ChatRepository,
    aiConfig: AIConfig,
    logger: org.slf4j.Logger
): ChatResponse {
    val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)

    // Determine session
    val isNewSession = request.sessionId == null
    val sessionId = request.sessionId ?: ChatSessionId.generate()

    // Validate existing session if provided
    if (!isNewSession) {
        val sessionExists = chatRepository.sessionExists(tenantId, sessionId)
        if (!sessionExists) {
            throw DokusException.NotFound("Chat session not found")
        }
    }

    // Get conversation history for context
    val conversationHistory = if (!isNewSession) {
        val (messages, _) = chatRepository.getSessionMessages(
            tenantId = tenantId,
            sessionId = sessionId,
            limit = 10,
            offset = 0,
            descending = false
        )
        messages.map { msg ->
            ConversationMessage(
                role = when (msg.role) {
                    MessageRole.User -> AgentMessageRole.USER
                    MessageRole.Assistant -> AgentMessageRole.ASSISTANT
                    MessageRole.System -> AgentMessageRole.SYSTEM
                },
                content = msg.content
            )
        }
    } else {
        null
    }

    // Get next sequence number
    val userSequence = chatRepository.getNextSequenceNumber(tenantId, sessionId)

    // Create and save user message
    val userMessageId = ChatMessageId.generate()
    val userMessage = ChatMessageDto(
        id = userMessageId,
        tenantId = tenantId,
        userId = userId,
        sessionId = sessionId,
        role = MessageRole.User,
        content = request.message,
        scope = request.scope,
        documentProcessingId = documentId,
        citations = null,
        chunksRetrieved = null,
        aiModel = null,
        aiProvider = null,
        generationTimeMs = null,
        promptTokens = null,
        completionTokens = null,
        sequenceNumber = userSequence,
        createdAt = now
    )
    chatRepository.saveMessage(userMessage)

    logger.debug("Saved user message: id=$userMessageId, session=$sessionId")

    // Generate AI response using ChatAgent
    val startTime = System.currentTimeMillis()
    val chatResult = try {
        chatAgent.chat(
            tenantId = tenantId,
            question = request.message,
            documentId = documentId,
            conversationHistory = conversationHistory,
            topK = request.maxChunks ?: ChatAgent.DEFAULT_TOP_K,
            minSimilarity = request.minSimilarity ?: ChatAgent.DEFAULT_MIN_SIMILARITY
        )
    } catch (e: Exception) {
        logger.error("Chat agent failed", e)
        // Return a fallback response
        ChatAgent.ChatResponse(
            answer = "I apologize, but I encountered an error while processing your question. Please try again.",
            citations = emptyList(),
            chunksRetrieved = 0,
            usedContext = false,
            generationTimeMs = System.currentTimeMillis() - startTime,
            confidence = 0f
        )
    }

    val generationTimeMs = (System.currentTimeMillis() - startTime).toInt()

    // Map citations to domain model
    val citations = if (request.includeCitations && chatResult.citations.isNotEmpty()) {
        chatResult.citations.map { citation ->
            ChatCitation(
                chunkId = citation.chunkId,
                documentId = citation.documentId,
                documentName = citation.documentName,
                pageNumber = citation.pageNumber,
                excerpt = citation.excerpt,
                relevanceScore = citation.similarityScore
            )
        }
    } else {
        null
    }

    // Create and save assistant message
    val assistantMessageId = ChatMessageId.generate()
    val assistantSequence = userSequence + 1
    val assistantMessage = ChatMessageDto(
        id = assistantMessageId,
        tenantId = tenantId,
        userId = userId,
        sessionId = sessionId,
        role = MessageRole.Assistant,
        content = chatResult.answer,
        scope = request.scope,
        documentProcessingId = documentId,
        citations = citations,
        chunksRetrieved = chatResult.chunksRetrieved,
        aiModel = aiConfig.models.chat,
        aiProvider = aiConfig.defaultProvider.name.lowercase(),
        generationTimeMs = generationTimeMs,
        promptTokens = null, // TODO: Track from LLM response
        completionTokens = null,
        sequenceNumber = assistantSequence,
        createdAt = Clock.System.now().toLocalDateTime(TimeZone.UTC)
    )
    chatRepository.saveMessage(assistantMessage)

    logger.info(
        "Chat completed: session=$sessionId, chunks=${chatResult.chunksRetrieved}, " +
                "citations=${citations?.size ?: 0}, time=${generationTimeMs}ms"
    )

    // Build response metadata
    val metadata = ChatResponseMetadata(
        chunksRetrieved = chatResult.chunksRetrieved,
        chunksUsed = chatResult.citations.size,
        contextTokens = null,
        generationTimeMs = generationTimeMs,
        model = aiConfig.models.chat,
        provider = aiConfig.defaultProvider.name.lowercase(),
        promptTokens = null,
        completionTokens = null,
        totalTokens = null
    )

    return ChatResponse(
        userMessage = userMessage,
        assistantMessage = assistantMessage,
        sessionId = sessionId,
        isNewSession = isNewSession,
        metadata = metadata
    )
}
