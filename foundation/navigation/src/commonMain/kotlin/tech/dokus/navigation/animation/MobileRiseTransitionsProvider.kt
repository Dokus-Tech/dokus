package tech.dokus.navigation.animation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.navigation.NavBackStackEntry

// v2 mobile rise transition: subtle rise (200ms, 5dp)
private const val RiseDurationMs = 200
private const val RiseOffsetDp = 5

internal class MobileRiseTransitionsProvider : TransitionsProvider {
    override val AnimatedContentTransitionScope<NavBackStackEntry>.enterTransition: EnterTransition
        get() = fadeIn(tween(RiseDurationMs)) +
                slideInVertically(tween(RiseDurationMs)) { RiseOffsetDp }

    override val AnimatedContentTransitionScope<NavBackStackEntry>.exitTransition: ExitTransition
        get() = fadeOut(tween(RiseDurationMs))

    override val AnimatedContentTransitionScope<NavBackStackEntry>.popEnterTransition: EnterTransition
        get() = fadeIn(tween(RiseDurationMs)) +
                slideInVertically(tween(RiseDurationMs)) { RiseOffsetDp }

    override val AnimatedContentTransitionScope<NavBackStackEntry>.popExitTransition: ExitTransition
        get() = fadeOut(tween(RiseDurationMs))
}
