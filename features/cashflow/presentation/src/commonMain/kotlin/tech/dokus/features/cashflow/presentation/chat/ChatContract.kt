package tech.dokus.features.cashflow.presentation.chat

import androidx.compose.runtime.Immutable
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.model.ai.ChatAttachedFile
import tech.dokus.domain.model.ai.ChatCitationDto
import tech.dokus.domain.model.ai.ChatConfiguration
import tech.dokus.domain.model.ai.ChatMessageDto
import tech.dokus.domain.model.ai.ChatScope
import tech.dokus.domain.model.ai.ChatSessionId
import tech.dokus.domain.model.ai.ChatSessionSummary
import tech.dokus.domain.model.ai.MessageRole
import tech.dokus.foundation.app.state.DokusState

/**
 * Contract for the Chat screen.
 *
 * The Chat screen provides RAG-powered document Q&A:
 * - Single-document chat (ask questions about a specific document)
 * - Cross-document chat (ask questions across all confirmed documents)
 * - Message bubbles with source citations
 * - Expandable citations showing document excerpts
 * - Session management for conversation history
 *
 * Flow:
 * 1. Loading -> Initialize chat (load config, optional session history)
 * 2. Content -> Ready to send messages and display responses
 * 3. Error -> Failed to initialize with retry option
 *
 * Message Flow:
 * 1. User types message and sends
 * 2. Optimistic UI shows user message immediately
 * 3. Loading indicator while waiting for AI response
 * 4. AI response arrives with citations
 * 5. Citations can be expanded to see source excerpts
 */

// ============================================================================
// STATE
// ============================================================================

/**
 * Loaded chat session data from the server.
 *
 * @property scope Current chat scope (SINGLE_DOC or ALL_DOCS)
 * @property documentId Document ID for single-doc scope (null for cross-doc)
 * @property documentName Document name for display (null for cross-doc)
 * @property sessionId Current session ID (null if new conversation)
 * @property messages List of messages in the current conversation
 * @property configuration Chat configuration from server
 * @property recentSessions Recent chat sessions for history display
 */
@Immutable
data class ChatSessionData(
    val scope: ChatScope,
    val documentId: DocumentId? = null,
    val documentName: String? = null,
    val sessionId: ChatSessionId? = null,
    val messages: List<ChatMessageDto> = emptyList(),
    val configuration: ChatConfiguration = ChatConfiguration(),
    val recentSessions: List<ChatSessionSummary> = emptyList(),
) {

    /**
     * Whether the chat is in single-document mode.
     */
    val isSingleDocMode: Boolean
        get() = scope == ChatScope.SingleDoc

    /**
     * Whether the chat is in cross-document mode.
     */
    val isCrossDocMode: Boolean
        get() = scope == ChatScope.AllDocs

    /**
     * Whether this is a new conversation (no session ID yet).
     */
    val isNewConversation: Boolean
        get() = sessionId == null

    /**
     * Number of messages in the conversation.
     */
    val messageCount: Int
        get() = messages.size

    /**
     * User messages in the conversation.
     */
    val userMessages: List<ChatMessageDto>
        get() = messages.filter { it.role == MessageRole.User }

    /**
     * Assistant messages in the conversation.
     */
    val assistantMessages: List<ChatMessageDto>
        get() = messages.filter { it.role == MessageRole.Assistant }

    /**
     * Whether there are any citations to display.
     */
    val hasCitations: Boolean
        get() = messages.any { it.citations?.isNotEmpty() == true }

    /**
     * All citations from assistant messages.
     */
    val allCitations: List<ChatCitationDto>
        get() = messages
            .filter { it.role == MessageRole.Assistant }
            .flatMap { it.citations ?: emptyList() }

    /**
     * Maximum message length from configuration.
     */
    val maxMessageLength: Int
        get() = configuration.maxMessageLength
}

/**
 * Flat data class state for the Chat screen.
 *
 * Network/loaded data is wrapped in [DokusState], UI state is top-level.
 *
 * @property session Chat session data (loading / success / error)
 * @property inputText Current text in the input field
 * @property isSending Whether a message is being sent/processed
 * @property expandedCitationIds Set of citation IDs that are currently expanded
 * @property showSessionPicker Whether to show session picker dialog
 */
@Immutable
data class ChatState(
    val session: DokusState<ChatSessionData> = DokusState.loading(),
    val inputText: String = "",
    val isSending: Boolean = false,
    val expandedCitationIds: Set<String> = emptySet(),
    val showSessionPicker: Boolean = false,
    val isSessionsPanelOpen: Boolean = true,
    val attachedFiles: List<ChatAttachedFile> = emptyList(),
) : MVIState {

    companion object {
        val initial by lazy { ChatState() }
    }

    /**
     * Whether the send button should be enabled.
     */
    val canSend: Boolean
        get() = inputText.isNotBlank() && !isSending

    /**
     * Whether the current input exceeds the maximum length.
     */
    val isInputTooLong: Boolean
        get() {
            val data = (session as? DokusState.Success)?.data ?: return false
            return inputText.length > data.maxMessageLength
        }
}

// ============================================================================
// INTENTS (User Actions)
// ============================================================================

@Immutable
sealed interface ChatIntent : MVIIntent {

    // === Initialization ===

    /**
     * Initialize chat for single-document mode.
     * @param documentId The document to chat about
     */
    data class InitSingleDocChat(val documentId: DocumentId) : ChatIntent

    /**
     * Initialize chat for cross-document mode.
     */
    data object InitCrossDocChat : ChatIntent

    /**
     * Load an existing session to continue the conversation.
     * @param sessionId The session to continue
     */
    data class LoadSession(val sessionId: ChatSessionId) : ChatIntent

    /** Refresh the current conversation */
    data object Refresh : ChatIntent

    // === Message Input ===

    /**
     * Update the input text field.
     * @param text New input text
     */
    data class UpdateInputText(val text: String) : ChatIntent

    /** Clear the input text field */
    data object ClearInput : ChatIntent

    /** Send the current message */
    data object SendMessage : ChatIntent

    // === File Attachments ===

    @Suppress("ArrayInDataClass") // ByteArray has no structural equality — intent is fire-and-forget, never compared
    data class AttachFile(val filename: String, val bytes: ByteArray) : ChatIntent

    /** Remove an attached file */
    data class RemoveAttachedFile(val refId: String) : ChatIntent

    // === Scope Selection ===

    /**
     * Switch to single-document chat mode.
     * @param documentId The document to switch to
     */
    data class SwitchToSingleDoc(val documentId: DocumentId) : ChatIntent

    /** Switch to cross-document chat mode */
    data object SwitchToCrossDoc : ChatIntent

    // === Citation Interaction ===

    /**
     * Toggle expansion state of a citation.
     * @param citationId The citation to toggle
     */
    data class ToggleCitation(val citationId: String) : ChatIntent

    /** Expand all citations in the conversation */
    data object ExpandAllCitations : ChatIntent

    /** Collapse all citations */
    data object CollapseAllCitations : ChatIntent

    // === Session Management ===

    /** Show the session picker dialog */
    data object ShowSessionPicker : ChatIntent

    /** Hide the session picker dialog */
    data object HideSessionPicker : ChatIntent

    /** Toggle the sessions side panel open/closed */
    data object ToggleSessionsPanel : ChatIntent

    /** Start a new conversation (clear current session) */
    data object StartNewConversation : ChatIntent

    /** Load recent sessions for the picker */
    data object LoadRecentSessions : ChatIntent

    // === Navigation ===

    /**
     * Navigate to source document from a citation.
     * @param documentId The document to navigate to
     * @param pageNumber Optional page number to scroll to
     */
    data class ViewCitationSource(
        val documentId: String,
        val pageNumber: Int? = null,
    ) : ChatIntent
}

// ============================================================================
// ACTIONS (Side Effects)
// ============================================================================

@Immutable
sealed interface ChatAction : MVIAction {

    // === Navigation ===

    /** Navigate back to previous screen */
    data object NavigateBack : ChatAction

    /**
     * Navigate to document review screen.
     * @param documentId Document to view
     */
    data class NavigateToDocumentReview(val documentId: DocumentId) : ChatAction

    /**
     * Navigate to document preview at a specific page.
     * @param documentId Document to preview
     * @param pageNumber Optional page number
     */
    data class NavigateToDocumentPreview(
        val documentId: DocumentId,
        val pageNumber: Int? = null,
    ) : ChatAction

    // === Feedback ===

    /**
     * Show error message.
     * @param message Error message to display
     */
    data class ShowError(val error: DokusException) : ChatAction

    /**
     * Show success message.
     * @param message Success message to display
     */
    data class ShowSuccess(val message: String) : ChatAction

    /**
     * Show info message.
     * @param message Info message to display
     */
    data class ShowInfo(val message: String) : ChatAction

    // === UI Effects ===

    /**
     * Download a single document PDF.
     * @param documentId Document to download
     */
    data class DownloadDocument(val documentId: String) : ChatAction

    /**
     * Download multiple documents as a ZIP archive.
     * @param documentIds Document IDs to bundle
     */
    data class DownloadDocumentsZip(val documentIds: List<String>) : ChatAction

    /** Scroll to the bottom of the message list */
    data object ScrollToBottom : ChatAction

    /** Focus the input field */
    data object FocusInput : ChatAction

    /** Dismiss the keyboard */
    data object DismissKeyboard : ChatAction
}
