package ai.dokus.app.cashflow.navigation

import ai.dokus.app.cashflow.screens.AddDocumentScreen
import ai.dokus.foundation.navigation.NavigationProvider
import ai.dokus.foundation.navigation.destinations.CashFlowDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

internal object CashflowNavigationProvider : NavigationProvider {
    override fun NavGraphBuilder.registerGraph() {
        composable<CashFlowDestination.AddDocument> {
            AddDocumentScreen()
        }
    }
}
