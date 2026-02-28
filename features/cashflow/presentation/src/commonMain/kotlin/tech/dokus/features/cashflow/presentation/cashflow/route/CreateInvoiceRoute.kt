package tech.dokus.features.cashflow.presentation.cashflow.route

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalUriHandler
import kotlinx.coroutines.launch
import pro.respawn.flowmvi.compose.dsl.DefaultLifecycle
import pro.respawn.flowmvi.compose.dsl.subscribe
import tech.dokus.features.cashflow.mvi.CreateInvoiceAction
import tech.dokus.features.cashflow.mvi.CreateInvoiceContainer
import tech.dokus.features.cashflow.presentation.cashflow.screen.CreateInvoiceScreen
import tech.dokus.foundation.app.mvi.container
import tech.dokus.foundation.app.network.ConnectionSnackbarEffect
import tech.dokus.navigation.local.LocalNavController
import tech.dokus.navigation.navigateTo

@Composable
internal fun CreateInvoiceRoute(
    container: CreateInvoiceContainer = container(),
) {
    val navController = LocalNavController.current
    val uriHandler = LocalUriHandler.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val state by container.store.subscribe(DefaultLifecycle) { action ->
        when (action) {
            is CreateInvoiceAction.NavigateBack -> navController.popBackStack()
            is CreateInvoiceAction.NavigateToCreateContact -> {
                navController.navigateTo(action.toCreateContactDestination())
            }
            is CreateInvoiceAction.NavigateToInvoice -> {
                navController.popBackStack()
            }
            is CreateInvoiceAction.OpenExternalUrl -> uriHandler.openUri(action.url)
            is CreateInvoiceAction.ShowError -> scope.launch { snackbarHostState.showSnackbar(action.message) }
            is CreateInvoiceAction.ShowSuccess -> scope.launch { snackbarHostState.showSnackbar(action.message) }
            is CreateInvoiceAction.ShowValidationError -> scope.launch { snackbarHostState.showSnackbar(action.message) }
        }
    }

    ConnectionSnackbarEffect(snackbarHostState)

    CreateInvoiceScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onIntent = { container.store.intent(it) }
    )
}
