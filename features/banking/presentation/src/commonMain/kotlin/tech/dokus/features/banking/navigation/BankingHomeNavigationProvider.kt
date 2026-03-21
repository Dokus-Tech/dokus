package tech.dokus.features.banking.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.toRoute
import tech.dokus.features.banking.presentation.balances.route.BalancesRoute
import tech.dokus.features.banking.presentation.payments.components.IgnoreReasonDialogRoute
import tech.dokus.features.banking.presentation.payments.route.PaymentsRoute
import tech.dokus.navigation.NavigationProvider
import tech.dokus.navigation.destinations.BankingDestination
import tech.dokus.navigation.destinations.HomeDestination

internal object BankingHomeNavigationProvider : NavigationProvider {
    override fun NavGraphBuilder.registerGraph() {
        composable<HomeDestination.Payments> {
            PaymentsRoute()
        }
        composable<HomeDestination.Balances> {
            BalancesRoute()
        }
        dialog<BankingDestination.IgnoreReasonDialog> { backStackEntry ->
            val route = backStackEntry.toRoute<BankingDestination.IgnoreReasonDialog>()
            IgnoreReasonDialogRoute(transactionId = route.transactionId)
        }
    }
}
