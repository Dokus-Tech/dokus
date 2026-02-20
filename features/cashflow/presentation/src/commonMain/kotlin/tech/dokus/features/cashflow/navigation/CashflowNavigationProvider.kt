package tech.dokus.features.cashflow.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import tech.dokus.domain.ids.CashflowEntryId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.features.cashflow.presentation.cashflow.route.AddDocumentRoute
import tech.dokus.features.cashflow.presentation.cashflow.route.CreateInvoiceRoute
import tech.dokus.features.cashflow.presentation.chat.route.ChatRoute
import tech.dokus.features.cashflow.presentation.ledger.route.CashflowLedgerRoute
import tech.dokus.features.cashflow.presentation.peppol.route.PeppolRegistrationRoute
import tech.dokus.features.cashflow.presentation.review.route.DocumentReviewRoute
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
            DocumentReviewRoute(route = route)
        }
        composable<CashFlowDestination.DocumentChat> { backStackEntry ->
            val route = backStackEntry.toRoute<CashFlowDestination.DocumentChat>()
            val documentId = DocumentId.parse(route.documentId)
            ChatRoute(documentId = documentId)
        }
        composable<CashFlowDestination.CashflowLedger> { backStackEntry ->
            val route = backStackEntry.toRoute<CashFlowDestination.CashflowLedger>()
            val highlightEntryId = route.highlightEntryId
            val parsedEntryId = highlightEntryId?.let {
                runCatching { CashflowEntryId.parse(it) }.getOrNull()
            }
            CashflowLedgerRoute(highlightEntryId = parsedEntryId)
        }
        composable<SettingsDestination.PeppolRegistration> {
            PeppolRegistrationRoute()
        }
    }
}
