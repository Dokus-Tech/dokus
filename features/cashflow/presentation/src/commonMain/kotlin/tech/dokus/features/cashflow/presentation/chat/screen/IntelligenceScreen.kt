package tech.dokus.features.cashflow.presentation.chat.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tech.dokus.features.cashflow.presentation.chat.ChatIntent
import tech.dokus.features.cashflow.presentation.chat.ChatSessionData
import tech.dokus.features.cashflow.presentation.chat.ChatState
import tech.dokus.features.cashflow.presentation.chat.components.ChatSuggestedStarters
import tech.dokus.features.cashflow.presentation.chat.components.IntelligenceMessages
import tech.dokus.features.cashflow.presentation.chat.components.SessionsPanel
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.app.state.isError
import tech.dokus.foundation.app.state.isLoading
import tech.dokus.foundation.app.state.isSuccess
import tech.dokus.foundation.aura.components.common.DokusErrorContent
import tech.dokus.foundation.aura.components.chat.ChatGridBackground
import tech.dokus.foundation.aura.components.chat.ChatInputBar
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.style.textMuted

/**
 * Intelligence chat screen matching v29 design.
 *
 * Two-region layout:
 * - Left: Collapsible sessions panel (200dp)
 * - Right: Conversation area with grid background, messages, and input bar
 */
@Composable
internal fun IntelligenceScreen(
    state: ChatState,
    listState: LazyListState,
    snackbarHostState: SnackbarHostState,
    onIntent: (ChatIntent) -> Unit,
    onDownloadDocument: (String) -> Unit = {},
    onDownloadZip: (List<String>) -> Unit = {},
    onNavigateToDocument: (String) -> Unit = {},
) {
    val sessionState = state.session
    val sessionData = (sessionState as? DokusState.Success)?.data
    val messageCount = sessionData?.messages?.size ?: 0

    LaunchedEffect(messageCount) {
        if (messageCount > 0) {
            listState.animateScrollToItem(messageCount - 1)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Sessions panel (collapsible)
            AnimatedVisibility(
                visible = state.isSessionsPanelOpen,
                enter = expandHorizontally(expandFrom = Alignment.Start),
                exit = shrinkHorizontally(shrinkTowards = Alignment.Start),
            ) {
                SessionsPanel(
                    sessions = sessionData?.recentSessions ?: emptyList(),
                    activeSessionId = sessionData?.sessionId,
                    onSessionClick = { sessionId ->
                        onIntent(ChatIntent.LoadSession(sessionId))
                    },
                    onNewConversation = { onIntent(ChatIntent.StartNewConversation) },
                    onCollapse = { onIntent(ChatIntent.ToggleSessionsPanel) },
                )
            }

            // Conversation area
            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                // Grid background
                ChatGridBackground()

                // Expand button when panel is collapsed
                if (!state.isSessionsPanelOpen) {
                    Box(
                        modifier = Modifier
                            .padding(Constraints.Spacing.small)
                            .size(22.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                RoundedCornerShape(5.dp),
                            )
                            .border(
                                Constraints.Stroke.thin,
                                MaterialTheme.colorScheme.outlineVariant,
                                RoundedCornerShape(5.dp),
                            )
                            .clickable { onIntent(ChatIntent.ToggleSessionsPanel) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "\u25B8",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.textMuted,
                        )
                    }
                }

                // Content
                Column(modifier = Modifier.fillMaxSize()) {
                    when {
                        sessionState.isLoading() -> {
                            Box(
                                modifier = Modifier.weight(1f).fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "Loading...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.textMuted,
                                )
                            }
                        }
                        sessionState.isError() -> {
                            DokusErrorContent(
                                exception = sessionState.exception,
                                retryHandler = sessionState.retryHandler,
                                modifier = Modifier.weight(1f).fillMaxSize(),
                            )
                        }
                        sessionState.isSuccess() -> {
                            val data = sessionState.data
                            if (data.messages.isEmpty()) {
                                // Empty state with starters
                                ChatSuggestedStarters(
                                    onStarterClick = { starter ->
                                        onIntent(ChatIntent.UpdateInputText(starter))
                                        onIntent(ChatIntent.SendMessage)
                                    },
                                    modifier = Modifier.weight(1f),
                                )
                            } else {
                                // Messages
                                IntelligenceMessages(
                                    messages = data.messages,
                                    listState = listState,
                                    onDocumentDownload = { doc ->
                                        doc.documentId?.let { onDownloadDocument(it) }
                                    },
                                    onDocumentClick = { doc ->
                                        doc.documentId?.let { onNavigateToDocument(it) }
                                    },
                                    onDownloadAllZip = { docs ->
                                        val ids = docs.mapNotNull { it.documentId }
                                        if (ids.isNotEmpty()) onDownloadZip(ids)
                                    },
                                    onCitationClick = { citation ->
                                        onNavigateToDocument(citation.documentId)
                                    },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }

                    // Input bar (always visible when loaded)
                    if (sessionState.isSuccess()) {
                        ChatInputBar(
                            text = state.inputText,
                            onTextChange = { onIntent(ChatIntent.UpdateInputText(it)) },
                            onSend = { onIntent(ChatIntent.SendMessage) },
                            onUpload = { /* M4: wire to document upload */ },
                            isSending = state.isSending,
                            maxLength = sessionData?.maxMessageLength ?: 4000,
                        )
                    }
                }

                // Snackbar
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }
}
