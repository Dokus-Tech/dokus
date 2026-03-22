package tech.dokus.features.cashflow.presentation.cashflow.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalUriHandler
import pro.respawn.flowmvi.compose.dsl.DefaultLifecycle
import pro.respawn.flowmvi.compose.dsl.subscribe
import tech.dokus.features.cashflow.mvi.CreateInvoiceAction
import tech.dokus.features.cashflow.mvi.CreateInvoiceContainer
import tech.dokus.features.cashflow.presentation.cashflow.screen.CreateInvoiceScreen
import tech.dokus.foundation.app.mvi.container
import tech.dokus.navigation.local.LocalNavController
import tech.dokus.navigation.navigateTo

@Composable
internal fun CreateInvoiceRoute(
    container: CreateInvoiceContainer = container(),
) {
    val navController = LocalNavController.current
    val uriHandler = LocalUriHandler.current

    val state by container.store.subscribe(DefaultLifecycle) { action ->
        when (action) {
            is CreateInvoiceAction.NavigateBack -> navController.popBackStack()
            is CreateInvoiceAction.NavigateToCreateContact -> {
                navController.navigateTo(action.toCreateContactDestination())
            }
            is CreateInvoiceAction.OpenExternalUrl -> uriHandler.openUri(action.url)
        }
    }

    CreateInvoiceScreen(
        state = state,
        onIntent = { container.store.intent(it) }
    )
}
