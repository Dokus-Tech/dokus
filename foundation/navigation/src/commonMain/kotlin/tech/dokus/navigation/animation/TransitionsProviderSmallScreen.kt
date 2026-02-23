@file:Suppress("TopLevelPropertyNaming") // Using PascalCase for animation constants (Kotlin convention)

package tech.dokus.navigation.animation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute
import tech.dokus.navigation.destinations.CoreDestination

private const val AnimationDurationMs = 350
private const val FadeAlpha = 0.92f
private const val SplashHandoffExitDurationMs = 280
private const val SplashHandoffEnterDurationMs = 420
private const val SplashHandoffEnterDelayMs = 70
private const val SplashHandoffRiseDivisor = 24
private const val SplashHandoffExitScale = 0.985f
private const val SplashHandoffEnterAlpha = 0.84f

private fun AnimatedContentTransitionScope<NavBackStackEntry>.isSplashToMain(): Boolean {
    return initialState.destination.hasRoute<CoreDestination.Splash>() &&
        targetState.destination.hasRoute<CoreDestination.Home>()
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.isMainToSplash(): Boolean {
    return initialState.destination.hasRoute<CoreDestination.Home>() &&
        targetState.destination.hasRoute<CoreDestination.Splash>()
}

internal class TransitionsProviderSmallScreen : TransitionsProvider {
    override val AnimatedContentTransitionScope<NavBackStackEntry>.enterTransition: EnterTransition
        get() {
            if (isSplashToMain()) {
                return fadeIn(
                    animationSpec = tween(
                        durationMillis = SplashHandoffEnterDurationMs,
                        delayMillis = SplashHandoffEnterDelayMs,
                        easing = FastOutSlowInEasing
                    ),
                    initialAlpha = SplashHandoffEnterAlpha
                ) + slideInVertically(
                    animationSpec = tween(
                        durationMillis = SplashHandoffEnterDurationMs,
                        delayMillis = SplashHandoffEnterDelayMs,
                        easing = FastOutSlowInEasing
                    )
                ) { it / SplashHandoffRiseDivisor }
            }

            return slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(AnimationDurationMs, easing = FastOutSlowInEasing)
            ) + fadeIn(
                animationSpec = tween(AnimationDurationMs, easing = FastOutSlowInEasing),
                initialAlpha = FadeAlpha
            )
        }

    override val AnimatedContentTransitionScope<NavBackStackEntry>.exitTransition: ExitTransition
        get() {
            if (isSplashToMain()) {
                return fadeOut(
                    animationSpec = tween(SplashHandoffExitDurationMs, easing = FastOutSlowInEasing),
                    targetAlpha = 0f
                ) + scaleOut(
                    targetScale = SplashHandoffExitScale,
                    animationSpec = tween(SplashHandoffExitDurationMs, easing = FastOutSlowInEasing)
                )
            }

            return slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(AnimationDurationMs, easing = FastOutSlowInEasing)
            ) + fadeOut(
                animationSpec = tween(AnimationDurationMs, easing = FastOutSlowInEasing),
                targetAlpha = FadeAlpha
            )
        }

    override val AnimatedContentTransitionScope<NavBackStackEntry>.popEnterTransition: EnterTransition
        get() {
            if (isMainToSplash()) {
                return fadeIn(
                    animationSpec = tween(SplashHandoffExitDurationMs, easing = FastOutSlowInEasing),
                    initialAlpha = SplashHandoffEnterAlpha
                )
            }

            return slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(AnimationDurationMs, easing = FastOutSlowInEasing)
            ) + fadeIn(
                animationSpec = tween(AnimationDurationMs, easing = FastOutSlowInEasing),
                initialAlpha = FadeAlpha
            )
        }

    override val AnimatedContentTransitionScope<NavBackStackEntry>.popExitTransition: ExitTransition
        get() {
            if (isMainToSplash()) {
                return fadeOut(
                    animationSpec = tween(SplashHandoffExitDurationMs, easing = FastOutSlowInEasing),
                    targetAlpha = 0f
                )
            }

            return slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(AnimationDurationMs, easing = FastOutSlowInEasing)
            ) + fadeOut(
                animationSpec = tween(AnimationDurationMs, easing = FastOutSlowInEasing),
                targetAlpha = FadeAlpha
            )
        }
}
