package ai.dokus.app.cashflow.presentation.chat

import ai.dokus.app.cashflow.repository.ChatRepositoryImpl
import ai.dokus.app.cashflow.usecase.SendChatMessageUseCase
import tech.dokus.domain.ids.DocumentProcessingId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.UserId
import tech.dokus.domain.model.ai.ChatConfiguration
import tech.dokus.domain.model.ai.ChatMessageDto
import tech.dokus.domain.model.ai.ChatMessageId
import tech.dokus.domain.model.ai.ChatResponse
import tech.dokus.domain.model.ai.ChatScope
import tech.dokus.domain.model.ai.ChatSessionId
import tech.dokus.domain.model.ai.MessageRole
import ai.dokus.foundation.platform.Logger
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.api.Store
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.dsl.withState
import pro.respawn.flowmvi.plugins.reduce
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

internal typealias ChatCtx = PipelineContext<ChatState, ChatIntent, ChatAction>

/**
 * Container for the Chat screen using FlowMVI.
 *
 * Manages RAG-powered document Q&A:
 * - Single-document chat (questions about a specific document)
 * - Cross-document chat (questions across all confirmed documents)
 * - Session management for conversation history
 * - Message sending with optimistic UI updates
 * - Citation display and navigation
 *
 * Message Flow:
 * 1. User sends message via [ChatIntent.SendMessage]
 * 2. Container creates optimistic user message
 * 3. API call to get AI response
 * 4. State updated with both user and assistant messages
 * 5. [ChatAction.ScrollToBottom] triggered
 *
 * Use with Koin's `container<>` DSL for automatic ViewModel wrapping and lifecycle management.
 */
internal class ChatContainer(
    private val sendChatMessageUseCase: SendChatMessageUseCase,
    private val chatRepository: ChatRepositoryImpl,
) : Container<ChatState, ChatIntent, ChatAction> {

    private val logger = Logger.forClass<ChatContainer>()

    // Job for cancellable message sending
    private var sendMessageJob: Job? = null

    // Job for loading sessions
    private var loadSessionsJob: Job? = null

    override val store: Store<ChatState, ChatIntent, ChatAction> =
        store(ChatState.Loading) {
            reduce { intent ->
                when (intent) {
                    // === Initialization ===
                    is ChatIntent.InitSingleDocChat -> handleInitSingleDocChat(intent.processingId)
                    is ChatIntent.InitCrossDocChat -> handleInitCrossDocChat()
                    is ChatIntent.LoadSession -> handleLoadSession(intent.sessionId)
                    is ChatIntent.Refresh -> handleRefresh()

                    // === Message Input ===
                    is ChatIntent.UpdateInputText -> handleUpdateInputText(intent.text)
                    is ChatIntent.ClearInput -> handleClearInput()
                    is ChatIntent.SendMessage -> handleSendMessage()

                    // === Scope Selection ===
                    is ChatIntent.SwitchToSingleDoc -> handleSwitchToSingleDoc(intent.processingId)
                    is ChatIntent.SwitchToCrossDoc -> handleSwitchToCrossDoc()

                    // === Citation Interaction ===
                    is ChatIntent.ToggleCitation -> handleToggleCitation(intent.citationId)
                    is ChatIntent.ExpandAllCitations -> handleExpandAllCitations()
                    is ChatIntent.CollapseAllCitations -> handleCollapseAllCitations()

                    // === Session Management ===
                    is ChatIntent.ShowSessionPicker -> handleShowSessionPicker()
                    is ChatIntent.HideSessionPicker -> handleHideSessionPicker()
                    is ChatIntent.StartNewConversation -> handleStartNewConversation()
                    is ChatIntent.LoadRecentSessions -> handleLoadRecentSessions()

                    // === Navigation ===
                    is ChatIntent.ViewCitationSource -> handleViewCitationSource(
                        intent.documentId,
                        intent.pageNumber
                    )
                }
            }
        }

    // =========================================================================
    // INITIALIZATION
    // =========================================================================

    private suspend fun ChatCtx.handleInitSingleDocChat(processingId: DocumentProcessingId) {
        logger.d { "Initializing single-doc chat for document: $processingId" }
        updateState { ChatState.Loading }

        // Load configuration
        val config = loadConfiguration()

        updateState {
            ChatState.Content(
                scope = ChatScope.SingleDoc,
                documentProcessingId = processingId,
                documentName = null, // TODO: Fetch document name if needed
                configuration = config
            )
        }
    }

    private suspend fun ChatCtx.handleInitCrossDocChat() {
        logger.d { "Initializing cross-doc chat" }
        updateState { ChatState.Loading }

        // Load configuration
        val config = loadConfiguration()

        updateState {
            ChatState.Content(
                scope = ChatScope.AllDocs,
                configuration = config
            )
        }
    }

    private suspend fun ChatCtx.handleLoadSession(sessionId: ChatSessionId) {
        logger.d { "Loading session: $sessionId" }

        withState<ChatState.Content, _> {
            updateState { copy(isSending = true) }

            chatRepository.getSessionHistory(
                sessionId = sessionId,
                page = 0,
                limit = 100,
                descending = false
            ).fold(
                onSuccess = { history ->
                    logger.i { "Loaded session with ${history.messages.size} messages" }
                    updateState {
                        copy(
                            sessionId = sessionId,
                            messages = history.messages,
                            scope = history.session.scope,
                            documentProcessingId = history.session.documentProcessingId,
                            documentName = history.session.documentName,
                            isSending = false,
                            showSessionPicker = false
                        )
                    }
                    action(ChatAction.ScrollToBottom)
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to load session: $sessionId" }
                    updateState { copy(isSending = false) }
                    action(ChatAction.ShowError("Failed to load conversation: ${error.message}"))
                }
            )
        }
    }

    private suspend fun ChatCtx.handleRefresh() {
        withState<ChatState.Content, _> {
            if (sessionId != null) {
                handleLoadSession(sessionId)
            }
        }
    }

    // =========================================================================
    // MESSAGE INPUT
    // =========================================================================

    private suspend fun ChatCtx.handleUpdateInputText(text: String) {
        withState<ChatState.Content, _> {
            updateState { copy(inputText = text) }
        }
    }

    private suspend fun ChatCtx.handleClearInput() {
        withState<ChatState.Content, _> {
            updateState { copy(inputText = "") }
        }
    }

    private suspend fun ChatCtx.handleSendMessage() {
        withState<ChatState.Content, _> {
            val message = inputText.trim()
            if (message.isBlank()) return@withState
            if (isSending) return@withState

            logger.d { "Sending message: ${message.take(50)}..." }

            // Create optimistic user message
            val optimisticUserMessage = createOptimisticUserMessage(message)

            // Update state with optimistic user message and clear input
            updateState {
                copy(
                    messages = messages + optimisticUserMessage,
                    inputText = "",
                    isSending = true
                )
            }

            // Scroll to show the user message
            action(ChatAction.ScrollToBottom)

            // Cancel any previous send job
            sendMessageJob?.cancel()

            // Send message to API
            sendMessageJob = launch {
                val result = if (sessionId != null) {
                    // Continue existing session
                    sendChatMessageUseCase.continueChat(
                        sessionId = sessionId,
                        message = message,
                        scope = scope,
                        documentId = documentProcessingId
                    )
                } else {
                    // Start new session
                    when (scope) {
                        ChatScope.SingleDoc -> {
                            val docId = documentProcessingId
                            if (docId == null) {
                                action(ChatAction.ShowError("No document selected"))
                                withState<ChatState.Content, _> {
                                    updateState {
                                        copy(
                                            messages = messages.dropLast(1),
                                            isSending = false
                                        )
                                    }
                                }
                                return@launch
                            }
                            sendChatMessageUseCase.startDocumentChat(
                                documentId = docId,
                                initialMessage = message
                            )
                        }
                        ChatScope.AllDocs -> {
                            sendChatMessageUseCase.startCrossDocumentChat(
                                initialMessage = message
                            )
                        }
                    }
                }

                handleSendMessageResult(result, optimisticUserMessage)
            }
        }
    }

    private suspend fun ChatCtx.handleSendMessageResult(
        result: Result<ChatResponse>,
        optimisticUserMessage: ChatMessageDto
    ) {
        result.fold(
            onSuccess = { response ->
                logger.i {
                    "Message sent successfully. " +
                        "Session: ${response.sessionId}, " +
                        "Citations: ${response.assistantMessage.citations?.size ?: 0}"
                }

                withState<ChatState.Content, _> {
                    // Replace optimistic message with real user message
                    // and add assistant message
                    val updatedMessages = messages
                        .dropLast(1) // Remove optimistic message
                        .plus(response.userMessage)
                        .plus(response.assistantMessage)

                    updateState {
                        copy(
                            sessionId = response.sessionId,
                            messages = updatedMessages,
                            isSending = false
                        )
                    }
                    action(ChatAction.ScrollToBottom)
                }
            },
            onFailure = { error ->
                logger.e(error) { "Failed to send message" }

                withState<ChatState.Content, _> {
                    // Remove the optimistic message on failure
                    updateState {
                        copy(
                            messages = messages.dropLast(1),
                            inputText = optimisticUserMessage.content, // Restore message
                            isSending = false
                        )
                    }
                    action(ChatAction.ShowError("Failed to send message: ${error.message}"))
                }
            }
        )
    }

    // =========================================================================
    // SCOPE SELECTION
    // =========================================================================

    private suspend fun ChatCtx.handleSwitchToSingleDoc(processingId: DocumentProcessingId) {
        logger.d { "Switching to single-doc mode: $processingId" }

        withState<ChatState.Content, _> {
            // Clear current session and start fresh
            updateState {
                copy(
                    scope = ChatScope.SingleDoc,
                    documentProcessingId = processingId,
                    sessionId = null,
                    messages = emptyList()
                )
            }
        }
    }

    private suspend fun ChatCtx.handleSwitchToCrossDoc() {
        logger.d { "Switching to cross-doc mode" }

        withState<ChatState.Content, _> {
            // Clear current session and start fresh
            updateState {
                copy(
                    scope = ChatScope.AllDocs,
                    documentProcessingId = null,
                    documentName = null,
                    sessionId = null,
                    messages = emptyList()
                )
            }
        }
    }

    // =========================================================================
    // CITATION INTERACTION
    // =========================================================================

    private suspend fun ChatCtx.handleToggleCitation(citationId: String) {
        withState<ChatState.Content, _> {
            val updated = if (citationId in expandedCitationIds) {
                expandedCitationIds - citationId
            } else {
                expandedCitationIds + citationId
            }
            updateState { copy(expandedCitationIds = updated) }
        }
    }

    private suspend fun ChatCtx.handleExpandAllCitations() {
        withState<ChatState.Content, _> {
            val allCitationIds = allCitations.map { it.chunkId }.toSet()
            updateState { copy(expandedCitationIds = allCitationIds) }
        }
    }

    private suspend fun ChatCtx.handleCollapseAllCitations() {
        withState<ChatState.Content, _> {
            updateState { copy(expandedCitationIds = emptySet()) }
        }
    }

    // =========================================================================
    // SESSION MANAGEMENT
    // =========================================================================

    private suspend fun ChatCtx.handleShowSessionPicker() {
        withState<ChatState.Content, _> {
            updateState { copy(showSessionPicker = true) }
            handleLoadRecentSessions()
        }
    }

    private suspend fun ChatCtx.handleHideSessionPicker() {
        withState<ChatState.Content, _> {
            updateState { copy(showSessionPicker = false) }
        }
    }

    private suspend fun ChatCtx.handleStartNewConversation() {
        logger.d { "Starting new conversation" }

        withState<ChatState.Content, _> {
            updateState {
                copy(
                    sessionId = null,
                    messages = emptyList(),
                    expandedCitationIds = emptySet(),
                    showSessionPicker = false
                )
            }
            action(ChatAction.FocusInput)
        }
    }

    private suspend fun ChatCtx.handleLoadRecentSessions() {
        withState<ChatState.Content, _> {
            loadSessionsJob?.cancel()

            loadSessionsJob = launch {
                chatRepository.listSessions(
                    scope = scope.takeIf { it == ChatScope.AllDocs },
                    documentId = documentProcessingId,
                    page = 0,
                    limit = 10
                ).fold(
                    onSuccess = { response ->
                        logger.d { "Loaded ${response.items.size} recent sessions" }
                        withState<ChatState.Content, _> {
                            updateState { copy(recentSessions = response.items) }
                        }
                    },
                    onFailure = { error ->
                        logger.e(error) { "Failed to load recent sessions" }
                        // Silently fail - user can still use chat
                    }
                )
            }
        }
    }

    // =========================================================================
    // NAVIGATION
    // =========================================================================

    private suspend fun ChatCtx.handleViewCitationSource(documentId: String, pageNumber: Int?) {
        logger.d { "Navigating to citation source: document=$documentId, page=$pageNumber" }

        // Parse document ID
        val processingId = try {
            DocumentProcessingId.parse(documentId)
        } catch (e: Exception) {
            logger.e(e) { "Invalid document ID: $documentId" }
            action(ChatAction.ShowError("Invalid document reference"))
            return
        }

        action(ChatAction.NavigateToDocumentPreview(processingId, pageNumber))
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    private suspend fun loadConfiguration(): ChatConfiguration {
        return chatRepository.getConfiguration().getOrNull()
            ?: ChatConfiguration() // Use defaults if fetch fails
    }

    @OptIn(kotlin.uuid.ExperimentalUuidApi::class, ExperimentalTime::class)
    private fun ChatState.Content.createOptimisticUserMessage(content: String): ChatMessageDto {
        val nowInstant = Instant.fromEpochMilliseconds(Clock.System.now().toEpochMilliseconds())
        val now = nowInstant.toLocalDateTime(TimeZone.currentSystemDefault())

        return ChatMessageDto(
            id = ChatMessageId.generate(),
            tenantId = TenantId.generate(), // Placeholder
            userId = UserId.generate(), // Placeholder
            sessionId = sessionId ?: ChatSessionId.generate(),
            role = MessageRole.User,
            content = content,
            scope = scope,
            documentProcessingId = documentProcessingId,
            citations = null,
            sequenceNumber = messages.size + 1,
            createdAt = now
        )
    }
}
