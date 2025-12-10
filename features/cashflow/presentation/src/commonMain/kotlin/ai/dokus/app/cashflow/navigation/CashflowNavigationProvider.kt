package ai.dokus.app.cashflow.navigation

import ai.dokus.app.cashflow.screens.AddDocumentScreen
import ai.dokus.app.cashflow.screens.settings.PeppolSettingsScreen
import ai.dokus.foundation.navigation.NavigationProvider
import ai.dokus.foundation.navigation.destinations.CashFlowDestination
import ai.dokus.foundation.navigation.destinations.SettingsDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

internal object CashflowNavigationProvider : NavigationProvider {
    override fun NavGraphBuilder.registerGraph() {
        composable<CashFlowDestination.AddDocument> {
            AddDocumentScreen()
        }
        composable<SettingsDestination.PeppolSettings> {
            PeppolSettingsScreen()
        }
    }
}
