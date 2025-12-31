package ai.dokus.app.cashflow.navigation

import ai.dokus.app.cashflow.presentation.chat.route.ChatRoute
import ai.dokus.app.cashflow.screens.CashflowScreen
import tech.dokus.navigation.NavigationProvider
import tech.dokus.navigation.destinations.HomeDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

internal object CashflowHomeNavigationProvider : NavigationProvider {
    override fun NavGraphBuilder.registerGraph() {
        composable<HomeDestination.Cashflow> {
            CashflowScreen()
        }
        composable<HomeDestination.AiChat> {
            ChatRoute(documentId = null)
        }
    }
}
