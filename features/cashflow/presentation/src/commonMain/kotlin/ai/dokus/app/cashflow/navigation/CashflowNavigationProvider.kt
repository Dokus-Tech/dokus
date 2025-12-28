package ai.dokus.app.cashflow.navigation

import ai.dokus.app.cashflow.presentation.chat.ChatScreen
import ai.dokus.app.cashflow.presentation.review.DocumentReviewScreen
import ai.dokus.app.cashflow.screens.AddDocumentScreen
import ai.dokus.app.cashflow.screens.CreateInvoiceScreen
import ai.dokus.app.cashflow.screens.settings.PeppolConnectScreen
import ai.dokus.app.cashflow.screens.settings.PeppolSettingsScreen
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.model.PeppolProvider
import ai.dokus.foundation.navigation.NavigationProvider
import ai.dokus.foundation.navigation.destinations.CashFlowDestination
import ai.dokus.foundation.navigation.destinations.SettingsDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute

internal object CashflowNavigationProvider : NavigationProvider {
    override fun NavGraphBuilder.registerGraph() {
        composable<CashFlowDestination.AddDocument> {
            AddDocumentScreen()
        }
        composable<CashFlowDestination.CreateInvoice> {
            CreateInvoiceScreen()
        }
        composable<CashFlowDestination.DocumentReview> { backStackEntry ->
            val route = backStackEntry.toRoute<CashFlowDestination.DocumentReview>()
            val documentId = DocumentId.parse(route.documentId)
            DocumentReviewScreen(documentId = documentId)
        }
        composable<CashFlowDestination.DocumentChat> { backStackEntry ->
            val route = backStackEntry.toRoute<CashFlowDestination.DocumentChat>()
            val documentId = DocumentId.parse(route.documentId)
            ChatScreen(documentId = documentId)
        }
        composable<SettingsDestination.PeppolSettings> {
            PeppolSettingsScreen()
        }
        composable<SettingsDestination.PeppolConfiguration.Connect> { backStackEntry ->
            val route = backStackEntry.toRoute<SettingsDestination.PeppolConfiguration.Connect>()
            val provider = PeppolProvider.fromName(route.providerName) ?: PeppolProvider.Recommand
            PeppolConnectScreen(provider = provider)
        }
    }
}
