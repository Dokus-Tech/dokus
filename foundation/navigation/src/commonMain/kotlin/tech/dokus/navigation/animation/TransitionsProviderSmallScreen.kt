@file:Suppress("TopLevelPropertyNaming") // Using PascalCase for animation constants (Kotlin convention)

package tech.dokus.navigation.animation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.navigation.NavBackStackEntry

private const val AnimationDurationMs = 350
private const val FadeAlpha = 0.92f

internal class TransitionsProviderSmallScreen : TransitionsProvider {
    override val AnimatedContentTransitionScope<NavBackStackEntry>.enterTransition: EnterTransition
        get() = slideIntoContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Start,
            animationSpec = tween(AnimationDurationMs, easing = FastOutSlowInEasing)
        ) + fadeIn(
            animationSpec = tween(AnimationDurationMs, easing = FastOutSlowInEasing),
            initialAlpha = FadeAlpha
        )

    override val AnimatedContentTransitionScope<NavBackStackEntry>.exitTransition: ExitTransition
        get() = slideOutOfContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Start,
            animationSpec = tween(AnimationDurationMs, easing = FastOutSlowInEasing)
        ) + fadeOut(
            animationSpec = tween(AnimationDurationMs, easing = FastOutSlowInEasing),
            targetAlpha = FadeAlpha
        )

    override val AnimatedContentTransitionScope<NavBackStackEntry>.popEnterTransition: EnterTransition
        get() = slideIntoContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.End,
            animationSpec = tween(AnimationDurationMs, easing = FastOutSlowInEasing)
        ) + fadeIn(
            animationSpec = tween(AnimationDurationMs, easing = FastOutSlowInEasing),
            initialAlpha = FadeAlpha
        )

    override val AnimatedContentTransitionScope<NavBackStackEntry>.popExitTransition: ExitTransition
        get() = slideOutOfContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.End,
            animationSpec = tween(AnimationDurationMs, easing = FastOutSlowInEasing)
        ) + fadeOut(
            animationSpec = tween(AnimationDurationMs, easing = FastOutSlowInEasing),
            targetAlpha = FadeAlpha
        )
}
