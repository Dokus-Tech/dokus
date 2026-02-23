package tech.dokus.features.cashflow.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import tech.dokus.features.cashflow.presentation.cashflow.route.AddDocumentRoute
import tech.dokus.features.cashflow.presentation.cashflow.route.CreateInvoiceRoute
import tech.dokus.features.cashflow.presentation.chat.route.ChatRoute
import tech.dokus.features.cashflow.presentation.ledger.route.CashflowLedgerRoute
import tech.dokus.features.cashflow.presentation.peppol.route.PeppolRegistrationRoute
import tech.dokus.features.cashflow.presentation.review.route.DocumentReviewRoute
import tech.dokus.features.cashflow.presentation.review.route.DocumentSourceViewerRoute
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
        composable<CashFlowDestination.DocumentSourceViewer>(
            enterTransition = { sourceViewerEnterTransition() },
            popExitTransition = { sourceViewerPopExitTransition() },
        ) { backStackEntry ->
            val route = backStackEntry.toRoute<CashFlowDestination.DocumentSourceViewer>()
            DocumentSourceViewerRoute(route = route)
        }
        composable<CashFlowDestination.DocumentChat> { backStackEntry ->
            val route = backStackEntry.toRoute<CashFlowDestination.DocumentChat>()
            ChatRoute(documentId = route.documentId)
        }
        composable<CashFlowDestination.CashflowLedger> { backStackEntry ->
            val route = backStackEntry.toRoute<CashFlowDestination.CashflowLedger>()
            CashflowLedgerRoute(highlightEntryId = route.highlightEntryId)
        }
        composable<SettingsDestination.PeppolRegistration> {
            PeppolRegistrationRoute()
        }
    }
}

private const val SourceViewerTransitionDurationMs = 260
private const val SourceViewerInitialAlpha = 0.96f

private fun AnimatedContentTransitionScope<NavBackStackEntry>.sourceViewerEnterTransition(): EnterTransition {
    return slideIntoContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.Start,
        animationSpec = tween(SourceViewerTransitionDurationMs),
    ) + fadeIn(
        animationSpec = tween(SourceViewerTransitionDurationMs),
        initialAlpha = SourceViewerInitialAlpha,
    )
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.sourceViewerPopExitTransition(): ExitTransition {
    return slideOutOfContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.End,
        animationSpec = tween(SourceViewerTransitionDurationMs),
    ) + fadeOut(
        animationSpec = tween(SourceViewerTransitionDurationMs),
        targetAlpha = SourceViewerInitialAlpha,
    )
}
