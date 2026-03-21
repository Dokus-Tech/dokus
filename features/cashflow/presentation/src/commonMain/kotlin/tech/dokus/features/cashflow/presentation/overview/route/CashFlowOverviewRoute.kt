package tech.dokus.features.cashflow.presentation.overview.route

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.jetbrains.compose.resources.stringResource
import org.koin.core.parameter.parametersOf
import pro.respawn.flowmvi.compose.dsl.DefaultLifecycle
import pro.respawn.flowmvi.compose.dsl.subscribe
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.cashflow_subtitle
import tech.dokus.aura.resources.cashflow_title
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.CashflowEntryId
import tech.dokus.features.cashflow.presentation.overview.mvi.CashFlowOverviewAction
import tech.dokus.features.cashflow.presentation.overview.mvi.CashFlowOverviewContainer
import tech.dokus.features.cashflow.presentation.overview.mvi.CashFlowOverviewIntent
import tech.dokus.features.cashflow.presentation.overview.screen.CashFlowOverviewScreen
import tech.dokus.foundation.app.mvi.container
import tech.dokus.foundation.app.network.ConnectionSnackbarEffect
import tech.dokus.foundation.app.shell.HomeShellTopBarConfig
import tech.dokus.foundation.app.shell.HomeShellTopBarMode
import tech.dokus.foundation.app.shell.RegisterHomeShellTopBar
import tech.dokus.foundation.aura.extensions.localized
import tech.dokus.navigation.destinations.CashFlowDestination
import tech.dokus.navigation.destinations.CashFlowDestination.DocumentDetailQueueContext
import tech.dokus.navigation.local.LocalNavController
import tech.dokus.navigation.navigateTo

private const val HOME_ROUTE_CASHFLOW = "cashflow"

@Composable
internal fun CashFlowOverviewRoute(
    highlightEntryId: String? = null,
    container: CashFlowOverviewContainer = container {
        val parsedId = highlightEntryId?.let {
            runCatching { CashflowEntryId.parse(it) }.getOrNull()
        }
        parametersOf(parsedId)
    },
) {
    val navController = LocalNavController.current
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingError by remember { mutableStateOf<DokusException?>(null) }
    var pendingSuccessMessage by remember { mutableStateOf<String?>(null) }

    val errorMessage = pendingError?.localized

    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            snackbarHostState.showSnackbar(errorMessage)
            pendingError = null
        }
    }

    LaunchedEffect(pendingSuccessMessage) {
        if (pendingSuccessMessage != null) {
            snackbarHostState.showSnackbar(pendingSuccessMessage!!)
            pendingSuccessMessage = null
        }
    }

    val state by container.store.subscribe(DefaultLifecycle) { action ->
        when (action) {
            is CashFlowOverviewAction.NavigateToDocumentDetail -> {
                navController.navigateTo(
                    CashFlowDestination.DocumentDetail.from(
                        documentId = action.documentId,
                        context = DocumentDetailQueueContext.DocumentList(),
                    )
                )
            }

            is CashFlowOverviewAction.NavigateToEntity -> {
                // TODO: Navigate to entity detail screen when available
                // For now, no-op or show toast
            }

            is CashFlowOverviewAction.ShowError -> {
                pendingError = action.error
            }

            is CashFlowOverviewAction.ShowPaymentSuccess -> {
                pendingSuccessMessage = "Payment recorded"
            }

            is CashFlowOverviewAction.ShowPaymentError -> {
                pendingError = action.error
            }
        }
    }

    ConnectionSnackbarEffect(snackbarHostState)
    val cashflowTitle = stringResource(Res.string.cashflow_title)
    val cashflowSubtitle = stringResource(Res.string.cashflow_subtitle)
    val onCreateInvoiceClick = remember(navController) {
        {
            navController.navigateTo(CashFlowDestination.CreateInvoice)
        }
    }
    val onIntent = remember(container) {
        { intent: CashFlowOverviewIntent ->
            container.store.intent(intent)
        }
    }
    val topBarConfig = remember(cashflowTitle, cashflowSubtitle) {
        HomeShellTopBarConfig(
            mode = HomeShellTopBarMode.Title(
                title = cashflowTitle,
                subtitle = cashflowSubtitle
            )
        )
    }

    RegisterHomeShellTopBar(
        route = HOME_ROUTE_CASHFLOW,
        config = topBarConfig
    )

    CashFlowOverviewScreen(
        state = state,
        onIntent = onIntent,
        onCreateInvoiceClick = onCreateInvoiceClick
    )
}
