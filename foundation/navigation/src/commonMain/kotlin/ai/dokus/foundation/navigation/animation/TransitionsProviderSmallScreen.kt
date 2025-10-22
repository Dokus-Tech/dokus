package ai.dokus.foundation.navigation.animation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.navigation.NavBackStackEntry

internal class TransitionsProviderSmallScreen : TransitionsProvider {
    override val AnimatedContentTransitionScope<NavBackStackEntry>.enterTransition: EnterTransition
        get() = slideIntoContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Start,
            animationSpec = tween(350, easing = FastOutSlowInEasing)
        ) + fadeIn(
            animationSpec = tween(350, easing = FastOutSlowInEasing),
            initialAlpha = 0.92f
        )

    override val AnimatedContentTransitionScope<NavBackStackEntry>.exitTransition: ExitTransition
        get() = slideOutOfContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Start,
            animationSpec = tween(350, easing = FastOutSlowInEasing)
        ) + fadeOut(
            animationSpec = tween(350, easing = FastOutSlowInEasing),
            targetAlpha = 0.92f
        )

    override val AnimatedContentTransitionScope<NavBackStackEntry>.popEnterTransition: EnterTransition
        get() = slideIntoContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.End,
            animationSpec = tween(350, easing = FastOutSlowInEasing)
        ) + fadeIn(
            animationSpec = tween(350, easing = FastOutSlowInEasing),
            initialAlpha = 0.92f
        )

    override val AnimatedContentTransitionScope<NavBackStackEntry>.popExitTransition: ExitTransition
        get() = slideOutOfContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.End,
            animationSpec = tween(350, easing = FastOutSlowInEasing)
        ) + fadeOut(
            animationSpec = tween(350, easing = FastOutSlowInEasing),
            targetAlpha = 0.92f
        )
}