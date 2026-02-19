@file:Suppress("TopLevelPropertyNaming") // Using PascalCase for animation constants (Kotlin convention)

package tech.dokus.navigation.animation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.navigation.NavBackStackEntry

// v2 film-frame vertical transition (desktop)
// Enter: fade in 300ms + slide from +30dp
// Exit: fade out 220ms + slide to -30dp
private const val EnterDurationMs = 300
private const val ExitDurationMs = 220
private const val SlideOffsetDp = 30

internal class TabTransitionsProvider : TransitionsProvider {
    override val AnimatedContentTransitionScope<NavBackStackEntry>.enterTransition: EnterTransition
        get() = fadeIn(tween(EnterDurationMs)) +
                slideInVertically(tween(EnterDurationMs)) { SlideOffsetDp }

    override val AnimatedContentTransitionScope<NavBackStackEntry>.exitTransition: ExitTransition
        get() = fadeOut(tween(ExitDurationMs)) +
                slideOutVertically(tween(ExitDurationMs)) { -SlideOffsetDp }

    override val AnimatedContentTransitionScope<NavBackStackEntry>.popEnterTransition: EnterTransition
        get() = fadeIn(tween(EnterDurationMs)) +
                slideInVertically(tween(EnterDurationMs)) { -SlideOffsetDp }

    override val AnimatedContentTransitionScope<NavBackStackEntry>.popExitTransition: ExitTransition
        get() = fadeOut(tween(ExitDurationMs)) +
                slideOutVertically(tween(ExitDurationMs)) { SlideOffsetDp }
}
