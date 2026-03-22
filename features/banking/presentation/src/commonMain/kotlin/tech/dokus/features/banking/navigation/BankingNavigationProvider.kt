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

/**
 * Root-level navigation provider for banking screens.
 * Used when navigating from the "More" menu on mobile.
 */
internal object BankingNavigationProvider : NavigationProvider {
    override fun NavGraphBuilder.registerGraph() {
        composable<BankingDestination.Balances> {
            BalancesRoute()
        }
        composable<BankingDestination.Payments> {
            PaymentsRoute()
        }
        dialog<BankingDestination.IgnoreReasonDialog> { backStackEntry ->
            val route = backStackEntry.toRoute<BankingDestination.IgnoreReasonDialog>()
            IgnoreReasonDialogRoute(transactionId = route.transactionId)
        }
    }
}
