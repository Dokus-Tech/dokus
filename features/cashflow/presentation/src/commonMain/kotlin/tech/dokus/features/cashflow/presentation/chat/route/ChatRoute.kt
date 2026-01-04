package tech.dokus.features.cashflow.presentation.chat.route

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import pro.respawn.flowmvi.compose.dsl.DefaultLifecycle
import pro.respawn.flowmvi.compose.dsl.subscribe
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.DocumentId
import tech.dokus.features.cashflow.presentation.chat.ChatAction
import tech.dokus.features.cashflow.presentation.chat.ChatContainer
import tech.dokus.features.cashflow.presentation.chat.ChatIntent
import tech.dokus.features.cashflow.presentation.chat.screen.ChatScreen
import tech.dokus.foundation.app.mvi.container
import tech.dokus.foundation.aura.extensions.localized
import tech.dokus.foundation.aura.local.LocalScreenSize
import tech.dokus.foundation.aura.local.isLarge
import tech.dokus.navigation.local.LocalNavController

@Composable
internal fun ChatRoute(
    documentId: DocumentId? = null,
    container: ChatContainer = container(),
) {
    val navController = LocalNavController.current
    val isLargeScreen = LocalScreenSize.isLarge
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingError by remember { mutableStateOf<DokusException?>(null) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val errorMessage = pendingError?.localized

    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            snackbarHostState.showSnackbar(errorMessage)
            pendingError = null
        }
    }

    val state by container.store.subscribe(DefaultLifecycle) { action ->
        when (action) {
            is ChatAction.NavigateBack -> navController.popBackStack()
            is ChatAction.NavigateToDocumentReview -> {
                // TODO: Navigate to document review screen
            }
            is ChatAction.NavigateToDocumentPreview -> {
                // TODO: Navigate to document preview with page number
            }
            is ChatAction.ShowError -> pendingError = action.error
            is ChatAction.ShowSuccess -> {
                scope.launch { snackbarHostState.showSnackbar(action.message) }
            }
            is ChatAction.ShowInfo -> {
                scope.launch { snackbarHostState.showSnackbar(action.message) }
            }
            is ChatAction.ScrollToBottom -> {
                // Scroll is handled by message count effects in the screen.
            }
            is ChatAction.FocusInput -> {
                // Focus is handled by the input field.
            }
            is ChatAction.DismissKeyboard -> {
                // Keyboard dismissal is platform-specific.
            }
        }
    }

    LaunchedEffect(documentId) {
        if (documentId != null) {
            container.store.intent(ChatIntent.InitSingleDocChat(documentId))
        } else {
            container.store.intent(ChatIntent.InitCrossDocChat)
        }
    }

    ChatScreen(
        state = state,
        listState = listState,
        isLargeScreen = isLargeScreen,
        snackbarHostState = snackbarHostState,
        onIntent = { container.store.intent(it) },
        onBackClick = { navController.popBackStack() },
    )
}
