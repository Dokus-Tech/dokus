package ai.dokus.app.cashflow.navigation

import ai.dokus.app.cashflow.screens.AddDocumentScreen
import ai.dokus.app.cashflow.screens.CreateInvoiceScreen
import ai.dokus.app.cashflow.screens.settings.PeppolConnectScreen
import ai.dokus.app.cashflow.screens.settings.PeppolProvidersScreen
import ai.dokus.app.cashflow.screens.settings.PeppolSettingsScreen
import ai.dokus.foundation.navigation.NavigationProvider
import ai.dokus.foundation.navigation.destinations.CashFlowDestination
import ai.dokus.foundation.navigation.destinations.SettingsDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute

internal object CashflowNavigationProvider : NavigationProvider {
    override fun NavGraphBuilder.registerGraph() {
        composable<CashFlowDestination.AddDocument> {
            AddDocumentScreen()
        }
        composable<CashFlowDestination.CreateInvoice> {
            CreateInvoiceScreen()
        }
        composable<SettingsDestination.PeppolSettings> {
            PeppolSettingsScreen()
        }
        composable<SettingsDestination.PeppolProviders> {
            PeppolProvidersScreen()
        }
        composable<SettingsDestination.PeppolConnect> { backStackEntry ->
            val route = backStackEntry.toRoute<SettingsDestination.PeppolConnect>()
            PeppolConnectScreen(providerName = route.provider)
        }
    }
}
