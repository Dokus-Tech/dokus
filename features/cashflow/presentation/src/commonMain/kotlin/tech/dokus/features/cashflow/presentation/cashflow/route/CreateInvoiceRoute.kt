package tech.dokus.features.cashflow.presentation.cashflow.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import pro.respawn.flowmvi.compose.dsl.DefaultLifecycle
import pro.respawn.flowmvi.compose.dsl.subscribe
import tech.dokus.features.cashflow.mvi.CreateInvoiceAction
import tech.dokus.features.cashflow.mvi.CreateInvoiceContainer
import tech.dokus.features.cashflow.presentation.cashflow.screen.CreateInvoiceScreen
import tech.dokus.foundation.app.mvi.container
import tech.dokus.navigation.destinations.ContactsDestination
import tech.dokus.navigation.local.LocalNavController
import tech.dokus.navigation.navigateTo

@Composable
internal fun CreateInvoiceRoute(
    container: CreateInvoiceContainer = container(),
) {
    val navController = LocalNavController.current

    val state by container.store.subscribe(DefaultLifecycle) { action ->
        when (action) {
            is CreateInvoiceAction.NavigateBack -> navController.popBackStack()
            is CreateInvoiceAction.NavigateToCreateContact -> {
                navController.navigateTo(ContactsDestination.CreateContact())
            }
            is CreateInvoiceAction.NavigateToInvoice -> {
                navController.popBackStack()
            }
            is CreateInvoiceAction.ShowValidationError -> {
                // Could show a snackbar, for now handled via form state errors
            }
            CreateInvoiceAction.ShowSuccess -> {
                // Could show a success snackbar
            }
            is CreateInvoiceAction.ShowError -> {
                // Could show an error snackbar
            }
        }
    }

    CreateInvoiceScreen(
        state = state,
        onIntent = { container.store.intent(it) },
        onNavigateToCreateContact = { navController.navigateTo(ContactsDestination.CreateContact()) }
    )
}
