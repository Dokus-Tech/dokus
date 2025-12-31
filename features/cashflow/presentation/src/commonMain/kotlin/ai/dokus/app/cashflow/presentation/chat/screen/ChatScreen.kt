package ai.dokus.app.cashflow.presentation.chat.screen

import ai.dokus.app.cashflow.presentation.chat.ChatIntent
import ai.dokus.app.cashflow.presentation.chat.ChatState
import ai.dokus.app.cashflow.presentation.chat.components.ChatContent
import ai.dokus.app.cashflow.presentation.chat.components.ChatTopBar
import ai.dokus.app.cashflow.presentation.chat.components.ErrorContent
import ai.dokus.app.cashflow.presentation.chat.components.LoadingContent
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

@Composable
internal fun ChatScreen(
    state: ChatState,
    listState: LazyListState,
    isLargeScreen: Boolean,
    snackbarHostState: SnackbarHostState,
    onIntent: (ChatIntent) -> Unit,
    onBackClick: () -> Unit,
) {
    val contentState = state as? ChatState.Content
    val messageCount = contentState?.messages?.size ?: 0
    LaunchedEffect(messageCount) {
        if (messageCount > 0) {
            listState.animateScrollToItem(messageCount - 1)
        }
    }

    Scaffold(
        topBar = {
            ChatTopBar(
                state = state,
                onBackClick = onBackClick,
                onNewChat = { onIntent(ChatIntent.StartNewConversation) },
                onShowHistory = { onIntent(ChatIntent.ShowSessionPicker) },
                onSwitchScope = { scope ->
                    when (scope) {
                        tech.dokus.domain.model.ai.ChatScope.SingleDoc -> {
                            // For single-doc, we need a document ID.
                        }
                        tech.dokus.domain.model.ai.ChatScope.AllDocs -> {
                            onIntent(ChatIntent.SwitchToCrossDoc)
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { contentPadding ->
        when (state) {
            is ChatState.Loading -> LoadingContent(contentPadding)
            is ChatState.Content -> ChatContent(
                state = state,
                contentPadding = contentPadding,
                listState = listState,
                isLargeScreen = isLargeScreen,
                onIntent = onIntent,
            )
            is ChatState.Error -> ErrorContent(
                error = state,
                contentPadding = contentPadding
            )
        }
    }
}
