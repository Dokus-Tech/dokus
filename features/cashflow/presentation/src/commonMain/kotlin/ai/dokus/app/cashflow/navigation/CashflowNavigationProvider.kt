package ai.dokus.app.cashflow.navigation

import ai.dokus.app.cashflow.screens.CashflowScreen
import ai.dokus.app.cashflow.screens.AddDocumentScreen
import ai.dokus.foundation.navigation.NavigationProvider
import ai.dokus.foundation.navigation.destinations.HomeDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

internal object CashflowNavigationProvider : NavigationProvider {
    override fun NavGraphBuilder.registerGraph() {
        composable<HomeDestination.Cashflow> {
            CashflowScreen()
        }
        composable<HomeDestination.CashflowUpload> {
            AddDocumentScreen()
        }
    }
}
