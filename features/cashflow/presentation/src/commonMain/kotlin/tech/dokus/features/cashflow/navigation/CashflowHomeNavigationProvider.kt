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
import tech.dokus.features.cashflow.presentation.chat.route.ChatRoute
import tech.dokus.features.cashflow.presentation.documents.route.DocumentsRoute
import tech.dokus.features.cashflow.presentation.ledger.route.CashflowLedgerRoute
import tech.dokus.features.cashflow.presentation.review.route.DocumentReviewRoute
import tech.dokus.features.cashflow.presentation.review.route.DocumentSourceViewerRoute
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
        composable<CashFlowDestination.DocumentSourceViewer>(
            enterTransition = { sourceViewerEnterTransition() },
            popExitTransition = { sourceViewerPopExitTransition() },
        ) { backStackEntry ->
            val route = backStackEntry.toRoute<CashFlowDestination.DocumentSourceViewer>()
            DocumentSourceViewerRoute(route = route)
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
