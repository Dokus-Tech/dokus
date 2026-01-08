package tech.dokus.features.cashflow.presentation.ledger.route

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import pro.respawn.flowmvi.compose.dsl.DefaultLifecycle
import pro.respawn.flowmvi.compose.dsl.subscribe
import org.koin.core.parameter.parametersOf
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.CashflowEntryId
import tech.dokus.features.cashflow.presentation.ledger.mvi.CashflowLedgerAction
import tech.dokus.features.cashflow.presentation.ledger.mvi.CashflowLedgerContainer
import tech.dokus.features.cashflow.presentation.ledger.mvi.CashflowLedgerIntent
import tech.dokus.features.cashflow.presentation.ledger.screen.CashflowLedgerScreen
import tech.dokus.foundation.app.mvi.container
import tech.dokus.foundation.app.network.ConnectionSnackbarEffect
import tech.dokus.foundation.aura.extensions.localized
import tech.dokus.navigation.destinations.CashFlowDestination
import tech.dokus.navigation.local.LocalNavController
import tech.dokus.navigation.navigateTo

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

    val errorMessage = pendingError?.localized

    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            snackbarHostState.showSnackbar(errorMessage)
            pendingError = null
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
        }
    }

    ConnectionSnackbarEffect(snackbarHostState)

    CashflowLedgerScreen(
        state = state,
        onIntent = { container.store.intent(it) }
    )
}
