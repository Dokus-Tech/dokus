package tech.dokus.features.cashflow.presentation.ledger.route

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.jetbrains.compose.resources.stringResource
import pro.respawn.flowmvi.compose.dsl.DefaultLifecycle
import pro.respawn.flowmvi.compose.dsl.subscribe
import org.koin.core.parameter.parametersOf
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.cashflow_create_invoice
import tech.dokus.aura.resources.cashflow_title
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.CashflowEntryId
import tech.dokus.features.cashflow.presentation.ledger.mvi.CashflowLedgerAction
import tech.dokus.features.cashflow.presentation.ledger.mvi.CashflowLedgerContainer
import tech.dokus.features.cashflow.presentation.ledger.mvi.CashflowLedgerIntent
import tech.dokus.features.cashflow.presentation.ledger.screen.CashflowLedgerScreen
import tech.dokus.foundation.app.mvi.container
import tech.dokus.foundation.app.network.ConnectionSnackbarEffect
import tech.dokus.foundation.app.shell.HomeShellTopBarAction
import tech.dokus.foundation.app.shell.HomeShellTopBarConfig
import tech.dokus.foundation.app.shell.HomeShellTopBarMode
import tech.dokus.foundation.app.shell.RegisterHomeShellTopBar
import tech.dokus.foundation.aura.extensions.localized
import tech.dokus.navigation.destinations.CashFlowDestination
import tech.dokus.navigation.local.LocalNavController
import tech.dokus.navigation.navigateTo

private const val HOME_ROUTE_CASHFLOW = "cashflow"

@Composable
internal fun CashflowLedgerRoute(
    highlightEntryId: CashflowEntryId? = null,
    container: CashflowLedgerContainer = container {
        parametersOf(highlightEntryId)
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
            is CashflowLedgerAction.NavigateToDocumentReview -> {
                navController.navigateTo(CashFlowDestination.DocumentReview(action.documentId))
            }
            is CashflowLedgerAction.NavigateToEntity -> {
                // TODO: Navigate to entity detail screen when available
                // For now, no-op or show toast
            }
            is CashflowLedgerAction.ShowError -> {
                pendingError = action.error
            }
            is CashflowLedgerAction.ShowPaymentSuccess -> {
                pendingSuccessMessage = "Payment recorded"
            }
            is CashflowLedgerAction.ShowPaymentError -> {
                pendingError = action.error
            }
        }
    }

    ConnectionSnackbarEffect(snackbarHostState)
    val cashflowTitle = stringResource(Res.string.cashflow_title)
    val createInvoiceLabel = stringResource(Res.string.cashflow_create_invoice)
    val onCreateInvoiceClick = remember(navController) {
        {
            navController.navigateTo(CashFlowDestination.CreateInvoice)
        }
    }
    val topBarConfig = remember(cashflowTitle, createInvoiceLabel, onCreateInvoiceClick) {
        HomeShellTopBarConfig(
            mode = HomeShellTopBarMode.Title(
                title = cashflowTitle
            ),
            actions = listOf(
                HomeShellTopBarAction.Text(
                    label = createInvoiceLabel,
                    onClick = onCreateInvoiceClick
                )
            )
        )
    }

    RegisterHomeShellTopBar(
        route = HOME_ROUTE_CASHFLOW,
        config = topBarConfig
    )

    CashflowLedgerScreen(
        state = state,
        onIntent = { container.store.intent(it) }
    )
}
