package ai.dokus.app.cashflow.presentation.chat

import tech.dokus.domain.asbtractions.RetryHandler
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.DocumentProcessingId
import tech.dokus.domain.model.ai.ChatCitation
import tech.dokus.domain.model.ai.ChatConfiguration
import tech.dokus.domain.model.ai.ChatMessageDto
import tech.dokus.domain.model.ai.ChatScope
import tech.dokus.domain.model.ai.ChatSessionId
import tech.dokus.domain.model.ai.ChatSessionSummary
import tech.dokus.domain.model.ai.MessageRole
import androidx.compose.runtime.Immutable
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState
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

@Immutable
sealed interface ChatState : MVIState, DokusState<Nothing> {

    /**
     * Loading state - initializing chat configuration and session.
     */
    data object Loading : ChatState

    /**
     * Content state - chat is ready for interaction.
     *
     * @property scope Current chat scope (SINGLE_DOC or ALL_DOCS)
     * @property documentProcessingId Document ID for single-doc scope (null for cross-doc)
     * @property documentName Document name for display (null for cross-doc)
     * @property sessionId Current session ID (null if new conversation)
     * @property messages List of messages in the current conversation
     * @property inputText Current text in the input field
     * @property isSending Whether a message is being sent/processed
     * @property configuration Chat configuration from server
     * @property expandedCitationIds Set of citation IDs that are currently expanded
     * @property recentSessions Recent chat sessions for history display
     * @property showSessionPicker Whether to show session picker dialog
     */
    data class Content(
        val scope: ChatScope,
        val documentProcessingId: DocumentProcessingId? = null,
        val documentName: String? = null,
        val sessionId: ChatSessionId? = null,
        val messages: List<ChatMessageDto> = emptyList(),
        val inputText: String = "",
        val isSending: Boolean = false,
        val configuration: ChatConfiguration = ChatConfiguration(),
        val expandedCitationIds: Set<String> = emptySet(),
        val recentSessions: List<ChatSessionSummary> = emptyList(),
        val showSessionPicker: Boolean = false,
    ) : ChatState {

        /**
         * Whether the send button should be enabled.
         */
        val canSend: Boolean
            get() = inputText.isNotBlank() && !isSending

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
        val allCitations: List<ChatCitation>
            get() = messages
                .filter { it.role == MessageRole.Assistant }
                .flatMap { it.citations ?: emptyList() }

        /**
         * Maximum message length from configuration.
         */
        val maxMessageLength: Int
            get() = configuration.maxMessageLength

        /**
         * Whether the current input exceeds the maximum length.
         */
        val isInputTooLong: Boolean
            get() = inputText.length > maxMessageLength
    }

    /**
     * Error state - failed to initialize chat.
     *
     * @property exception The error that occurred
     * @property retryHandler Handler to retry initialization
     */
    data class Error(
        override val exception: DokusException,
        override val retryHandler: RetryHandler,
    ) : ChatState, DokusState.Error<Nothing>
}

// ============================================================================
// INTENTS (User Actions)
// ============================================================================

@Immutable
sealed interface ChatIntent : MVIIntent {

    // === Initialization ===

    /**
     * Initialize chat for single-document mode.
     * @param processingId The document to chat about
     */
    data class InitSingleDocChat(val processingId: DocumentProcessingId) : ChatIntent

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

    // === Scope Selection ===

    /**
     * Switch to single-document chat mode.
     * @param processingId The document to switch to
     */
    data class SwitchToSingleDoc(val processingId: DocumentProcessingId) : ChatIntent

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
     * @param processingId Document to view
     */
    data class NavigateToDocumentReview(val processingId: DocumentProcessingId) : ChatAction

    /**
     * Navigate to document preview at a specific page.
     * @param processingId Document to preview
     * @param pageNumber Optional page number
     */
    data class NavigateToDocumentPreview(
        val processingId: DocumentProcessingId,
        val pageNumber: Int? = null,
    ) : ChatAction

    // === Feedback ===

    /**
     * Show error message.
     * @param message Error message to display
     */
    data class ShowError(val message: String) : ChatAction

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

    /** Scroll to the bottom of the message list */
    data object ScrollToBottom : ChatAction

    /** Focus the input field */
    data object FocusInput : ChatAction

    /** Dismiss the keyboard */
    data object DismissKeyboard : ChatAction
}
