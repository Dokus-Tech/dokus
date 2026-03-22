package tech.dokus.features.cashflow.presentation.chat.route

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import pro.respawn.flowmvi.compose.dsl.DefaultLifecycle
import pro.respawn.flowmvi.compose.dsl.subscribe
import androidx.compose.ui.platform.LocalUriHandler
import org.koin.compose.koinInject
import tech.dokus.domain.config.DynamicDokusEndpointProvider
import tech.dokus.domain.ids.DocumentId
import tech.dokus.features.cashflow.presentation.cashflow.components.rememberDocumentFilePicker
import tech.dokus.features.cashflow.presentation.chat.ChatAction
import tech.dokus.features.cashflow.presentation.chat.ChatContainer
import tech.dokus.features.cashflow.presentation.chat.ChatIntent
import tech.dokus.features.cashflow.presentation.chat.screen.IntelligenceScreen
import tech.dokus.foundation.app.mvi.container
import tech.dokus.navigation.destinations.CashFlowDestination
import tech.dokus.navigation.local.LocalNavController
import tech.dokus.navigation.navigateTo

@Composable
internal fun ChatRoute(
    documentId: String? = null,
    container: ChatContainer = container(),
    endpointProvider: DynamicDokusEndpointProvider = koinInject(),
) {
    val navController = LocalNavController.current
    val uriHandler = LocalUriHandler.current
    val endpoint = endpointProvider.currentEndpointSnapshot()
    val apiBaseUrl = "${endpoint.protocol}://${endpoint.host}:${endpoint.port}"

    val filePickerLauncher = rememberDocumentFilePicker { files ->
        files.forEach { file ->
            container.store.intent(ChatIntent.AttachFile(filename = file.name, bytes = file.bytes))
        }
    }
    val listState = rememberLazyListState()

    val state by container.store.subscribe(DefaultLifecycle) { action ->
        when (action) {
            is ChatAction.NavigateBack -> navController.popBackStack()
            is ChatAction.NavigateToDocumentDetail -> {
                navController.navigateTo(
                    CashFlowDestination.DocumentDetail(documentId = action.documentId.toString())
                )
            }
            is ChatAction.NavigateToDocumentPreview -> {
                navController.navigateTo(
                    CashFlowDestination.DocumentDetail(documentId = action.documentId.toString())
                )
            }
            is ChatAction.DownloadDocument -> {
                uriHandler.openUri("$apiBaseUrl/api/v1/documents/${action.documentId}/content")
            }
            is ChatAction.DownloadDocumentsZip -> {
                action.documentIds.forEach { docId ->
                    val baseUrl = "${endpoint.protocol}://${endpoint.host}:${endpoint.port}"
                    uriHandler.openUri("$baseUrl/api/v1/documents/$docId/content")
                }
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

    val parsedDocumentId = remember(documentId) { documentId?.let { DocumentId.parse(it) } }

    LaunchedEffect(parsedDocumentId) {
        if (parsedDocumentId != null) {
            container.store.intent(ChatIntent.InitSingleDocChat(parsedDocumentId))
        } else {
            container.store.intent(ChatIntent.InitCrossDocChat)
        }
    }

    IntelligenceScreen(
        state = state,
        listState = listState,
        onIntent = { container.store.intent(it) },
        onDownloadDocument = { docId ->
            val baseUrl = "${endpoint.protocol}://${endpoint.host}:${endpoint.port}"
            uriHandler.openUri("$baseUrl/api/v1/documents/$docId/content")
        },
        onDownloadZip = { docIds ->
            docIds.forEach { docId ->
                uriHandler.openUri("$apiBaseUrl/api/v1/documents/$docId/content")
            }
        },
        onNavigateToDocument = { docId ->
            navController.navigateTo(
                CashFlowDestination.DocumentDetail(documentId = docId)
            )
        },
        onUploadClick = { filePickerLauncher.launch() },
    )
}
