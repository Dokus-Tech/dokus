@file:Suppress("TopLevelPropertyNaming") // Using PascalCase for animation constants (Kotlin convention)

package tech.dokus.navigation.animation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute
import tech.dokus.navigation.destinations.CoreDestination

private const val AnimationDurationMs = 300
private const val ScaleIn = 0.95f
private const val ScaleOut = 1.05f
private const val SplashHandoffEnterDurationMs = 380
private const val SplashHandoffExitDurationMs = 260
private const val SplashHandoffEnterDelayMs = 70
private const val SplashHandoffEnterScale = 0.992f
private const val SplashHandoffExitScale = 1.01f
private const val SplashHandoffEnterAlpha = 0.86f

private fun AnimatedContentTransitionScope<NavBackStackEntry>.isSplashToMain(): Boolean {
    return initialState.destination.hasRoute<CoreDestination.Splash>() &&
        targetState.destination.hasRoute<CoreDestination.Home>()
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.isMainToSplash(): Boolean {
    return initialState.destination.hasRoute<CoreDestination.Home>() &&
        targetState.destination.hasRoute<CoreDestination.Splash>()
}

internal class TransitionsProviderLargeScreen : TransitionsProvider {
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
                ) + scaleIn(
                    initialScale = SplashHandoffEnterScale,
                    animationSpec = tween(
                        durationMillis = SplashHandoffEnterDurationMs,
                        delayMillis = SplashHandoffEnterDelayMs,
                        easing = FastOutSlowInEasing
                    )
                )
            }

            return fadeIn(animationSpec = tween(AnimationDurationMs)) +
                scaleIn(initialScale = ScaleIn, animationSpec = tween(AnimationDurationMs))
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

            return fadeOut(animationSpec = tween(AnimationDurationMs)) +
                scaleOut(targetScale = ScaleOut, animationSpec = tween(AnimationDurationMs))
        }

    override val AnimatedContentTransitionScope<NavBackStackEntry>.popEnterTransition: EnterTransition
        get() {
            if (isMainToSplash()) {
                return fadeIn(
                    animationSpec = tween(SplashHandoffExitDurationMs, easing = FastOutSlowInEasing),
                    initialAlpha = SplashHandoffEnterAlpha
                ) + scaleIn(
                    initialScale = SplashHandoffEnterScale,
                    animationSpec = tween(SplashHandoffExitDurationMs, easing = FastOutSlowInEasing)
                )
            }

            return fadeIn(animationSpec = tween(AnimationDurationMs)) +
                scaleIn(initialScale = ScaleOut, animationSpec = tween(AnimationDurationMs))
        }

    override val AnimatedContentTransitionScope<NavBackStackEntry>.popExitTransition: ExitTransition
        get() {
            if (isMainToSplash()) {
                return fadeOut(
                    animationSpec = tween(SplashHandoffExitDurationMs, easing = FastOutSlowInEasing),
                    targetAlpha = 0f
                ) + scaleOut(
                    targetScale = SplashHandoffExitScale,
                    animationSpec = tween(SplashHandoffExitDurationMs, easing = FastOutSlowInEasing)
                )
            }

            return fadeOut(animationSpec = tween(AnimationDurationMs)) +
                scaleOut(targetScale = ScaleIn, animationSpec = tween(AnimationDurationMs))
        }
}
