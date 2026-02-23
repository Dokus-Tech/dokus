package tech.dokus.features.cashflow.presentation.review.route

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.DocumentSourceId
import tech.dokus.features.cashflow.presentation.review.DocumentReviewAction
import tech.dokus.features.cashflow.presentation.review.DocumentReviewContainer
import tech.dokus.features.cashflow.presentation.review.DocumentReviewIntent
import tech.dokus.features.cashflow.presentation.review.DocumentReviewState
import tech.dokus.features.cashflow.presentation.review.components.mobile.MobileSourceViewerScreen
import tech.dokus.foundation.app.mvi.container
import tech.dokus.foundation.aura.components.common.DokusErrorContent
import tech.dokus.foundation.aura.components.common.DokusLoader
import tech.dokus.foundation.aura.extensions.localized
import tech.dokus.navigation.destinations.CashFlowDestination
import tech.dokus.navigation.local.LocalNavController

@Composable
internal fun DocumentSourceViewerRoute(
    route: CashFlowDestination.DocumentSourceViewer,
    container: DocumentReviewContainer = container {
        parametersOf(
            DocumentId.parse(route.documentId),
            null,
        )
    },
) {
    val navController = LocalNavController.current
    val sourceId = remember(route.sourceId) { DocumentSourceId.parse(route.sourceId) }
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingError by remember { mutableStateOf<DokusException?>(null) }
    var hasOpenedSource by remember(route.documentId, route.sourceId) { mutableStateOf(false) }
    val errorMessage = pendingError?.localized

    val state by container.store.subscribe(DefaultLifecycle) { action ->
        when (action) {
            is DocumentReviewAction.NavigateBack -> navController.popBackStack()
            is DocumentReviewAction.ShowError -> pendingError = action.error
            else -> Unit
        }
    }

    LaunchedEffect(errorMessage) {
        val message = errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        pendingError = null
    }

    LaunchedEffect(state, hasOpenedSource) {
        if (hasOpenedSource) return@LaunchedEffect
        val contentState = state as? DocumentReviewState.Content ?: return@LaunchedEffect
        if (contentState.document.sources.none { it.id == sourceId }) {
            hasOpenedSource = true
            navController.popBackStack()
            return@LaunchedEffect
        }
        hasOpenedSource = true
        container.store.intent(DocumentReviewIntent.OpenSourceModal(sourceId))
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (val currentState = state) {
            is DocumentReviewState.Content -> {
                val viewerState = currentState.sourceViewerState
                if (viewerState == null) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        DokusLoader()
                    }
                } else {
                    MobileSourceViewerScreen(
                        contentState = currentState,
                        viewerState = viewerState,
                        onBack = { navController.popBackStack() },
                        onToggleTechnicalDetails = {
                            container.store.intent(DocumentReviewIntent.ToggleSourceTechnicalDetails)
                        },
                        onRetry = {
                            container.store.intent(DocumentReviewIntent.OpenSourceModal(viewerState.sourceId))
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            is DocumentReviewState.Error -> {
                DokusErrorContent(
                    exception = currentState.exception,
                    retryHandler = currentState.retryHandler,
                    modifier = Modifier.fillMaxSize(),
                )
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

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}
