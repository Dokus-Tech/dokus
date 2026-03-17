@file:Suppress(
    "TooManyFunctions", // Container handles chat workflow
    "TooGenericExceptionCaught" // Network errors need catch-all
)

package tech.dokus.features.cashflow.presentation.chat

import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.api.Store
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.dsl.withState
import pro.respawn.flowmvi.plugins.reduce
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.UserId
import tech.dokus.domain.model.ai.ChatAttachedFile
import tech.dokus.domain.model.ai.ChatConfiguration
import tech.dokus.domain.model.ai.ChatMessageDto
import tech.dokus.domain.model.ai.ChatMessageId
import tech.dokus.domain.model.ai.ChatResponse
import tech.dokus.domain.model.ai.ChatScope
import tech.dokus.domain.model.ai.ChatSessionId
import tech.dokus.domain.model.ai.MessageRole
import tech.dokus.domain.utils.currentTimeMillis
import tech.dokus.features.cashflow.usecases.GetChatConfigurationUseCase
import tech.dokus.features.cashflow.usecases.GetChatSessionHistoryUseCase
import tech.dokus.features.cashflow.usecases.ListChatSessionsUseCase
import tech.dokus.features.cashflow.usecases.SendChatMessageUseCase
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.app.state.isSuccess
import tech.dokus.foundation.platform.Logger
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/** Maximum number of messages to load from session history */
private const val SessionHistoryLimit = 100

/** Maximum number of recent sessions to display in picker */
private const val RecentSessionsLimit = 10

/** Maximum characters to include in log previews of user messages */
private const val MessagePreviewLength = 50

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
    private val getChatConfigurationUseCase: GetChatConfigurationUseCase,
    private val listChatSessionsUseCase: ListChatSessionsUseCase,
    private val getChatSessionHistoryUseCase: GetChatSessionHistoryUseCase,
) : Container<ChatState, ChatIntent, ChatAction> {

    private val logger = Logger.forClass<ChatContainer>()

    // Job for cancellable message sending
    private var sendMessageJob: Job? = null

    // Job for loading sessions
    private var loadSessionsJob: Job? = null

    override val store: Store<ChatState, ChatIntent, ChatAction> =
        store(ChatState.initial) {
            reduce { intent ->
                when (intent) {
                    // === Initialization ===
                    is ChatIntent.InitSingleDocChat -> handleInitSingleDocChat(intent.documentId)
                    is ChatIntent.InitCrossDocChat -> handleInitCrossDocChat()
                    is ChatIntent.LoadSession -> handleLoadSession(intent.sessionId)
                    is ChatIntent.Refresh -> handleRefresh()

                    // === Message Input ===
                    is ChatIntent.UpdateInputText -> handleUpdateInputText(intent.text)
                    is ChatIntent.ClearInput -> handleClearInput()
                    is ChatIntent.SendMessage -> handleSendMessage()

                    // === Scope Selection ===
                    is ChatIntent.SwitchToSingleDoc -> handleSwitchToSingleDoc(intent.documentId)
                    is ChatIntent.SwitchToCrossDoc -> handleSwitchToCrossDoc()

                    // === Citation Interaction ===
                    is ChatIntent.ToggleCitation -> handleToggleCitation(intent.citationId)
                    is ChatIntent.ExpandAllCitations -> handleExpandAllCitations()
                    is ChatIntent.CollapseAllCitations -> handleCollapseAllCitations()

                    // === File Attachments ===
                    is ChatIntent.AttachFile -> handleAttachFile(intent.filename, intent.bytes)
                    is ChatIntent.RemoveAttachedFile -> updateState {
                        copy(attachedFiles = attachedFiles.filter { it.refId != intent.refId })
                    }

                    // === Session Management ===
                    is ChatIntent.ToggleSessionsPanel -> updateState { copy(isSessionsPanelOpen = !isSessionsPanelOpen) }
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

    private suspend fun ChatCtx.handleInitSingleDocChat(docId: DocumentId) {
        logger.d { "Initializing single-doc chat for document: $docId" }
        updateState { copy(session = DokusState.loading()) }

        // Load configuration
        val config = loadConfiguration()

        updateState {
            copy(
                session = DokusState.success(
                    ChatSessionData(
                        scope = ChatScope.SingleDoc,
                        documentId = docId,
                        documentName = null, // TODO: Fetch document name if needed
                        configuration = config
                    )
                )
            )
        }
    }

    private suspend fun ChatCtx.handleInitCrossDocChat() {
        // Don't reset if we already have a cross-doc session with messages
        withState {
            val existing = (session as? DokusState.Success<ChatSessionData>)?.data
            if (existing != null && existing.scope == ChatScope.AllDocs && existing.sessionId != null) {
                logger.d { "Cross-doc session already active: ${existing.sessionId}" }
                // Refresh the session list
                intent(ChatIntent.LoadRecentSessions)
                return@withState
            }
        }

        logger.d { "Initializing cross-doc chat" }
        updateState { copy(session = DokusState.loading()) }

        val config = loadConfiguration()

        // Try to load the most recent cross-doc session
        val recentSessions = listChatSessionsUseCase(
            scope = ChatScope.AllDocs,
            limit = 1,
        ).getOrNull()
        val lastSession = recentSessions?.items?.firstOrNull()

        updateState {
            copy(
                session = DokusState.success(
                    ChatSessionData(
                        scope = ChatScope.AllDocs,
                        configuration = config,
                        recentSessions = recentSessions?.items ?: emptyList(),
                    )
                )
            )
        }

        // If there's a recent session, load its history
        if (lastSession != null) {
            intent(ChatIntent.LoadSession(lastSession.sessionId))
        }
    }

    private suspend fun ChatCtx.handleLoadSession(sessionId: ChatSessionId) {
        logger.d { "Loading session: $sessionId" }

        withState {
            val data = (session as? DokusState.Success)?.data ?: return@withState
            updateState { copy(isSending = true) }

            getChatSessionHistoryUseCase(
                sessionId = sessionId,
                page = 0,
                limit = SessionHistoryLimit,
                descending = false
            ).fold(
                onSuccess = { history ->
                    logger.i { "Loaded session with ${history.messages.size} messages" }
                    updateState {
                        copy(
                            session = DokusState.success(
                                data.copy(
                                    sessionId = sessionId,
                                    messages = history.messages,
                                    scope = history.session.scope,
                                    documentId = history.session.documentId,
                                    documentName = history.session.documentName,
                                )
                            ),
                            isSending = false,
                            showSessionPicker = false
                        )
                    }
                    action(ChatAction.ScrollToBottom)
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to load session: $sessionId" }
                    updateState { copy(isSending = false) }
                    val reason = error.message?.takeIf { it.isNotBlank() }
                    action(ChatAction.ShowError(DokusException.ChatLoadConversationFailed(reason)))
                }
            )
        }
    }

    private suspend fun ChatCtx.handleRefresh() {
        withState {
            val data = (session as? DokusState.Success)?.data ?: return@withState
            if (data.sessionId != null) {
                handleLoadSession(data.sessionId)
            }
        }
    }

    // =========================================================================
    // MESSAGE INPUT
    // =========================================================================

    private suspend fun ChatCtx.handleUpdateInputText(text: String) {
        updateState { copy(inputText = text) }
    }

    private suspend fun ChatCtx.handleClearInput() {
        updateState { copy(inputText = "") }
    }

    private suspend fun ChatCtx.handleSendMessage() {
        withState {
            val data = (session as? DokusState.Success)?.data ?: return@withState
            val message = inputText.trim()
            if (message.isBlank()) return@withState
            if (isSending) return@withState

            logger.d { "Sending message: ${message.take(MessagePreviewLength)}..." }

            // Create optimistic user message
            val optimisticUserMessage = data.createOptimisticUserMessage(message)

            // Update state with optimistic user message and clear input
            updateState {
                copy(
                    session = DokusState.success(
                        data.copy(messages = data.messages + optimisticUserMessage)
                    ),
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
                val docId = if (data.scope == ChatScope.SingleDoc) data.documentId else null
                if (data.scope == ChatScope.SingleDoc && docId == null) {
                    action(ChatAction.ShowError(DokusException.ChatNoDocumentSelected))
                    withState {
                        val currentData = (session as? DokusState.Success)?.data ?: return@withState
                        updateState {
                            copy(
                                session = DokusState.success(
                                    currentData.copy(messages = currentData.messages.dropLast(1))
                                ),
                                isSending = false
                            )
                        }
                    }
                    return@launch
                }

                val result = sendChatMessageUseCase(
                    message = message,
                    scope = data.scope,
                    documentId = docId,
                    sessionId = data.sessionId
                )

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

                withState {
                    val data = (session as? DokusState.Success)?.data ?: return@withState
                    // Replace optimistic message with real user message
                    // and add assistant message
                    val updatedMessages = data.messages
                        .dropLast(1) // Remove optimistic message
                        .plus(response.userMessage)
                        .plus(response.assistantMessage)

                    updateState {
                        copy(
                            session = DokusState.success(
                                data.copy(
                                    sessionId = response.sessionId,
                                    messages = updatedMessages,
                                )
                            ),
                            isSending = false
                        )
                    }
                    action(ChatAction.ScrollToBottom)
                }
            },
            onFailure = { error ->
                logger.e(error) { "Failed to send message" }

                withState {
                    val data = (session as? DokusState.Success)?.data ?: return@withState
                    // Remove the optimistic message on failure
                    updateState {
                        copy(
                            session = DokusState.success(
                                data.copy(messages = data.messages.dropLast(1))
                            ),
                            inputText = optimisticUserMessage.content, // Restore message
                            isSending = false
                        )
                    }
                    val reason = error.message?.takeIf { it.isNotBlank() }
                    action(ChatAction.ShowError(DokusException.ChatSendMessageFailed(reason)))
                }
            }
        )
    }

    // =========================================================================
    // SCOPE SELECTION
    // =========================================================================

    private suspend fun ChatCtx.handleSwitchToSingleDoc(docId: DocumentId) {
        logger.d { "Switching to single-doc mode: $docId" }

        withState {
            val data = (session as? DokusState.Success)?.data ?: return@withState
            // Clear current session and start fresh
            updateState {
                copy(
                    session = DokusState.success(
                        data.copy(
                            scope = ChatScope.SingleDoc,
                            documentId = data.documentId,
                            sessionId = null,
                            messages = emptyList()
                        )
                    )
                )
            }
        }
    }

    private suspend fun ChatCtx.handleSwitchToCrossDoc() {
        logger.d { "Switching to cross-doc mode" }

        withState {
            val data = (session as? DokusState.Success)?.data ?: return@withState
            // Clear current session and start fresh
            updateState {
                copy(
                    session = DokusState.success(
                        data.copy(
                            scope = ChatScope.AllDocs,
                            documentId = null,
                            documentName = null,
                            sessionId = null,
                            messages = emptyList()
                        )
                    )
                )
            }
        }
    }

    // =========================================================================
    // CITATION INTERACTION
    // =========================================================================

    private suspend fun ChatCtx.handleToggleCitation(citationId: String) {
        withState {
            val updated = if (citationId in expandedCitationIds) {
                expandedCitationIds - citationId
            } else {
                expandedCitationIds + citationId
            }
            updateState { copy(expandedCitationIds = updated) }
        }
    }

    private suspend fun ChatCtx.handleExpandAllCitations() {
        withState {
            val data = (session as? DokusState.Success)?.data ?: return@withState
            val allCitationIds = data.allCitations.map { it.chunkId }.toSet()
            updateState { copy(expandedCitationIds = allCitationIds) }
        }
    }

    private suspend fun ChatCtx.handleCollapseAllCitations() {
        updateState { copy(expandedCitationIds = emptySet()) }
    }

    // =========================================================================
    // SESSION MANAGEMENT
    // =========================================================================

    private suspend fun ChatCtx.handleShowSessionPicker() {
        updateState { copy(showSessionPicker = true) }
        handleLoadRecentSessions()
    }

    private suspend fun ChatCtx.handleHideSessionPicker() {
        updateState { copy(showSessionPicker = false) }
    }

    private suspend fun ChatCtx.handleStartNewConversation() {
        logger.d { "Starting new conversation" }

        withState {
            val data = (session as? DokusState.Success)?.data ?: return@withState
            updateState {
                copy(
                    session = DokusState.success(
                        data.copy(
                            sessionId = null,
                            messages = emptyList(),
                        )
                    ),
                    expandedCitationIds = emptySet(),
                    showSessionPicker = false
                )
            }
            action(ChatAction.FocusInput)
        }
    }

    private suspend fun ChatCtx.handleLoadRecentSessions() {
        withState {
            val data = (session as? DokusState.Success)?.data ?: return@withState
            loadSessionsJob?.cancel()

            loadSessionsJob = launch {
                listChatSessionsUseCase(
                    scope = data.scope.takeIf { it == ChatScope.AllDocs },
                    documentId = data.documentId,
                    page = 0,
                    limit = RecentSessionsLimit
                ).fold(
                    onSuccess = { response ->
                        logger.d { "Loaded ${response.items.size} recent sessions" }
                        withState {
                            val currentData = (session as? DokusState.Success)?.data ?: return@withState
                            updateState {
                                copy(
                                    session = DokusState.success(
                                        currentData.copy(recentSessions = response.items)
                                    )
                                )
                            }
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

    private suspend fun ChatCtx.handleViewCitationSource(documentIdStr: String, pageNumber: Int?) {
        logger.d { "Navigating to citation source: document=$documentIdStr, page=$pageNumber" }

        // Parse document ID
        val docId = try {
            DocumentId.parse(documentIdStr)
        } catch (e: Exception) {
            logger.e(e) { "Invalid document ID: $documentIdStr" }
            action(ChatAction.ShowError(DokusException.ChatInvalidDocumentReference))
            return
        }

        action(ChatAction.NavigateToDocumentPreview(docId, pageNumber))
    }

    private suspend fun ChatCtx.handleAttachFile(filename: String, bytes: ByteArray) {
        // Add file as "uploading" placeholder immediately
        val tempRefId = "temp-${kotlin.time.Clock.System.now().toEpochMilliseconds()}"
        val pendingFile = ChatAttachedFile(
            refId = tempRefId,
            filename = filename,
            isUploading = true,
        )
        updateState { copy(attachedFiles = attachedFiles + pendingFile) }

        // TODO: Call POST /api/v1/chat/upload to upload to temp storage + RAG index
        // For now, mark as uploaded immediately (backend endpoint not yet implemented)
        updateState {
            copy(
                attachedFiles = attachedFiles.map { file ->
                    if (file.refId == tempRefId) file.copy(isUploading = false) else file
                }
            )
        }
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    private suspend fun loadConfiguration(): ChatConfiguration {
        return getChatConfigurationUseCase().getOrNull()
            ?: ChatConfiguration() // Use defaults if fetch fails
    }

    @OptIn(kotlin.uuid.ExperimentalUuidApi::class, ExperimentalTime::class)
    private fun ChatSessionData.createOptimisticUserMessage(content: String): ChatMessageDto {
        val nowInstant = Instant.fromEpochMilliseconds(currentTimeMillis)
        val now = nowInstant.toLocalDateTime(TimeZone.currentSystemDefault())

        return ChatMessageDto(
            id = ChatMessageId.generate(),
            tenantId = TenantId.generate(), // Placeholder
            userId = UserId.generate(), // Placeholder
            sessionId = sessionId ?: ChatSessionId.generate(),
            role = MessageRole.User,
            content = content,
            scope = scope,
            documentId = documentId,
            citations = null,
            sequenceNumber = messages.size + 1,
            createdAt = now
        )
    }
}
