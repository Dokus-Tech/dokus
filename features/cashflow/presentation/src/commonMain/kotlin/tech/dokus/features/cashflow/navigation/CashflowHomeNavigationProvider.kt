package tech.dokus.features.cashflow.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import tech.dokus.features.cashflow.presentation.chat.route.ChatRoute
import tech.dokus.features.cashflow.presentation.documents.route.DocumentsRoute
import tech.dokus.features.cashflow.presentation.ledger.route.CashflowLedgerRoute
import tech.dokus.features.cashflow.presentation.review.route.DocumentReviewRoute
import tech.dokus.navigation.NavigationProvider
import tech.dokus.navigation.destinations.CashFlowDestination
import tech.dokus.navigation.destinations.HomeDestination

internal object CashflowHomeNavigationProvider : NavigationProvider {
    override fun NavGraphBuilder.registerGraph() {
        composable<HomeDestination.Documents> {
            DocumentsRoute()
        }
        composable<HomeDestination.Cashflow> {
            CashflowLedgerRoute()
        }
        composable<HomeDestination.AiChat> {
            ChatRoute(documentId = null)
        }
        composable<CashFlowDestination.DocumentReview> { backStackEntry ->
            val route = backStackEntry.toRoute<CashFlowDestination.DocumentReview>()
            DocumentReviewRoute(route = route)
        }
    }
}
