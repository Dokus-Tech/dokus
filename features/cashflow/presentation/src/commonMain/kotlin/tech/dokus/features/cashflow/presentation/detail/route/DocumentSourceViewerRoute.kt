package tech.dokus.features.cashflow.presentation.detail.route

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.koin.core.parameter.parametersOf
import pro.respawn.flowmvi.compose.dsl.DefaultLifecycle
import pro.respawn.flowmvi.compose.dsl.subscribe
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.DocumentSourceId
import tech.dokus.features.cashflow.presentation.detail.DocumentDetailAction
import tech.dokus.features.cashflow.presentation.detail.DocumentDetailContainer
import tech.dokus.features.cashflow.presentation.detail.DocumentDetailIntent
import tech.dokus.features.cashflow.presentation.detail.mvi.preview.DocumentPreviewIntent
import tech.dokus.features.cashflow.presentation.detail.components.mobile.MobileSourceViewerScreen
import tech.dokus.foundation.app.mvi.container
import tech.dokus.foundation.app.state.isError
import tech.dokus.foundation.aura.components.common.DokusErrorContent
import tech.dokus.foundation.aura.components.common.DokusLoader
import tech.dokus.navigation.destinations.CashFlowDestination
import tech.dokus.navigation.local.LocalNavController

@Composable
internal fun DocumentSourceViewerRoute(
    route: CashFlowDestination.DocumentSourceViewer,
    container: DocumentDetailContainer = container {
        parametersOf(
            DocumentId.parse(route.documentId),
            null,
        )
    },
) {
    val navController = LocalNavController.current
    val sourceId = remember(route.sourceId) { DocumentSourceId.parse(route.sourceId) }
    var hasOpenedSource by remember(route.documentId, route.sourceId) { mutableStateOf(false) }

    val state by container.store.subscribe(DefaultLifecycle) { action ->
        when (action) {
            is DocumentDetailAction.NavigateBack -> navController.popBackStack()
            is DocumentDetailAction.NavigateToEntity -> Unit
            is DocumentDetailAction.NavigateToCashflowEntry -> Unit
            is DocumentDetailAction.DownloadDocument -> Unit
        }
    }

    LaunchedEffect(state, hasOpenedSource) {
        if (hasOpenedSource) return@LaunchedEffect
        if (!state.hasContent) return@LaunchedEffect
        if (state.documentRecord?.sources.orEmpty().none { it.id == sourceId }) {
            hasOpenedSource = true
            navController.popBackStack()
            return@LaunchedEffect
        }
        hasOpenedSource = true
        container.store.intent(DocumentDetailIntent.Preview(DocumentPreviewIntent.OpenSourceModal(sourceId)))
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            state.hasContent -> {
                val viewerState = state.sourceViewerState
                if (viewerState == null) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        DokusLoader()
                    }
                } else {
                    MobileSourceViewerScreen(
                        contentState = state,
                        viewerState = viewerState,
                        onBack = { navController.popBackStack() },
                        onToggleTechnicalDetails = {
                            container.store.intent(DocumentDetailIntent.Preview(DocumentPreviewIntent.ToggleSourceTechnicalDetails))
                        },
                        onRetry = {
                            container.store.intent(DocumentDetailIntent.Preview(DocumentPreviewIntent.OpenSourceModal(viewerState.sourceId)))
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            state.document.isError() -> {
                val errorState = state.document as? tech.dokus.foundation.app.state.DokusState.Error
                if (errorState != null) {
                    DokusErrorContent(
                        exception = errorState.exception,
                        retryHandler = errorState.retryHandler,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            else -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    DokusLoader()
                }
            }
        }
    }
}
