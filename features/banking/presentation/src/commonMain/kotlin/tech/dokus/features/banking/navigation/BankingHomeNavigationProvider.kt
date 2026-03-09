package tech.dokus.features.banking.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import tech.dokus.features.banking.presentation.payments.route.PaymentsRoute
import tech.dokus.navigation.NavigationProvider
import tech.dokus.navigation.destinations.HomeDestination

internal object BankingHomeNavigationProvider : NavigationProvider {
    override fun NavGraphBuilder.registerGraph() {
        composable<HomeDestination.Payments> {
            PaymentsRoute()
        }
        composable<HomeDestination.Balances> {
            // Balances screen — placeholder for Phase 6
            PaymentsRoute()
        }
    }
}
