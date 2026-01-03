package tech.dokus.features.cashflow.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.model.PeppolProvider
import tech.dokus.features.cashflow.presentation.cashflow.route.AddDocumentRoute
import tech.dokus.features.cashflow.presentation.cashflow.route.CreateInvoiceRoute
import tech.dokus.features.cashflow.presentation.chat.route.ChatRoute
import tech.dokus.features.cashflow.presentation.review.route.DocumentReviewRoute
import tech.dokus.features.cashflow.presentation.settings.route.PeppolConnectRoute
import tech.dokus.features.cashflow.presentation.settings.route.PeppolSettingsRoute
import tech.dokus.navigation.NavigationProvider
import tech.dokus.navigation.destinations.CashFlowDestination
import tech.dokus.navigation.destinations.SettingsDestination

internal object CashflowNavigationProvider : NavigationProvider {
    override fun NavGraphBuilder.registerGraph() {
        composable<CashFlowDestination.AddDocument> {
            AddDocumentRoute()
        }
        composable<CashFlowDestination.CreateInvoice> {
            CreateInvoiceRoute()
        }
        composable<CashFlowDestination.DocumentReview> { backStackEntry ->
            val route = backStackEntry.toRoute<CashFlowDestination.DocumentReview>()
            val documentId = DocumentId.parse(route.documentId)
            DocumentReviewRoute(documentId = documentId)
        }
        composable<CashFlowDestination.DocumentChat> { backStackEntry ->
            val route = backStackEntry.toRoute<CashFlowDestination.DocumentChat>()
            val documentId = DocumentId.parse(route.documentId)
            ChatRoute(documentId = documentId)
        }
        composable<SettingsDestination.PeppolSettings> {
            PeppolSettingsRoute()
        }
        composable<SettingsDestination.PeppolConfiguration.Connect> { backStackEntry ->
            val route = backStackEntry.toRoute<SettingsDestination.PeppolConfiguration.Connect>()
            val provider = PeppolProvider.fromName(route.providerName) ?: PeppolProvider.Recommand
            PeppolConnectRoute(provider = provider)
        }
    }
}
