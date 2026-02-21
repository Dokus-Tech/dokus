package tech.dokus.features.cashflow.presentation.chat.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import tech.dokus.features.cashflow.presentation.chat.ChatIntent
import tech.dokus.features.cashflow.presentation.chat.ChatState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import tech.dokus.domain.model.ai.ChatScope
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

@Composable
internal fun ChatContent(
    state: ChatState.Content,
    contentPadding: PaddingValues,
    listState: LazyListState,
    isLargeScreen: Boolean,
    onIntent: (ChatIntent) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .imePadding()
    ) {
        if (state.isCrossDocMode) {
            ScopeSelectorChips(
                currentScope = state.scope,
                onScopeChange = { scope ->
                    when (scope) {
                        tech.dokus.domain.model.ai.ChatScope.AllDocs -> onIntent(ChatIntent.SwitchToCrossDoc)
                        tech.dokus.domain.model.ai.ChatScope.SingleDoc -> {
                            // Would need document picker.
                        }
                    }
                },
                modifier = Modifier.padding(
                    horizontal = Constraints.Spacing.medium,
                    vertical = Constraints.Spacing.small
                )
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (state.messages.isEmpty()) {
                EmptyStateContent(
                    isSingleDocMode = state.isSingleDocMode,
                    documentName = state.documentName,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                MessagesList(
                    messages = state.messages,
                    expandedCitationIds = state.expandedCitationIds,
                    listState = listState,
                    isLargeScreen = isLargeScreen,
                    onToggleCitation = { citationId ->
                        onIntent(ChatIntent.ToggleCitation(citationId))
                    },
                    onDocumentClick = { documentId ->
                        onIntent(ChatIntent.ViewCitationSource(documentId))
                    }
                )
            }

            SendingIndicator(
                isSending = state.isSending,
                modifier = Modifier
                    .align(androidx.compose.ui.Alignment.BottomCenter)
                    .padding(bottom = Constraints.Spacing.medium)
            )
        }

        ChatInputSection(
            inputText = state.inputText,
            canSend = state.canSend,
            isSending = state.isSending,
            isInputTooLong = state.isInputTooLong,
            maxLength = state.maxMessageLength,
            onInputChange = { text -> onIntent(ChatIntent.UpdateInputText(text)) },
            onSend = { onIntent(ChatIntent.SendMessage) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(Constraints.Spacing.medium)
        )
    }

    if (state.showSessionPicker) {
        SessionPickerDialog(
            sessions = state.recentSessions,
            onSessionSelect = { sessionId ->
                onIntent(ChatIntent.LoadSession(sessionId))
            },
            onNewSession = { onIntent(ChatIntent.StartNewConversation) },
            onDismiss = { onIntent(ChatIntent.HideSessionPicker) }
        )
    }
}

@Preview
@Composable
private fun ChatContentPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        ChatContent(
            state = ChatState.Content(
                scope = ChatScope.AllDocs,
                messages = emptyList(),
            ),
            contentPadding = PaddingValues(0.dp),
            listState = rememberLazyListState(),
            isLargeScreen = false,
            onIntent = {},
        )
    }
}
