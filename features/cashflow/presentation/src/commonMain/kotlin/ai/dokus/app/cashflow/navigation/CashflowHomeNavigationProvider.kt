package ai.dokus.app.cashflow.navigation

import ai.dokus.app.cashflow.presentation.chat.ChatScreen
import ai.dokus.app.cashflow.screens.CashflowScreen
import ai.dokus.foundation.navigation.NavigationProvider
import ai.dokus.foundation.navigation.destinations.HomeDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

internal object CashflowHomeNavigationProvider : NavigationProvider {
    override fun NavGraphBuilder.registerGraph() {
        composable<HomeDestination.Cashflow> {
            CashflowScreen()
        }
        composable<HomeDestination.AiChat> {
            ChatScreen(documentId = null)
        }
    }
}
