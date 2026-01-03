package tech.dokus.features.cashflow.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import tech.dokus.features.cashflow.presentation.cashflow.route.CashflowRoute
import tech.dokus.features.cashflow.presentation.chat.route.ChatRoute
import tech.dokus.navigation.NavigationProvider
import tech.dokus.navigation.destinations.HomeDestination

internal object CashflowHomeNavigationProvider : NavigationProvider {
    override fun NavGraphBuilder.registerGraph() {
        composable<HomeDestination.Cashflow> {
            CashflowRoute()
        }
        composable<HomeDestination.AiChat> {
            ChatRoute(documentId = null)
        }
    }
}
