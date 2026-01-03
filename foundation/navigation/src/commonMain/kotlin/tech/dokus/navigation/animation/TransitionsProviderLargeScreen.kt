@file:Suppress("TopLevelPropertyNaming") // Using PascalCase for animation constants (Kotlin convention)

package tech.dokus.navigation.animation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.navigation.NavBackStackEntry

private const val AnimationDurationMs = 300
private const val ScaleIn = 0.95f
private const val ScaleOut = 1.05f

internal class TransitionsProviderLargeScreen : TransitionsProvider {
    override val AnimatedContentTransitionScope<NavBackStackEntry>.enterTransition: EnterTransition
        get() = fadeIn(animationSpec = tween(AnimationDurationMs)) +
            scaleIn(initialScale = ScaleIn, animationSpec = tween(AnimationDurationMs))

    override val AnimatedContentTransitionScope<NavBackStackEntry>.exitTransition: ExitTransition
        get() = fadeOut(animationSpec = tween(AnimationDurationMs)) +
            scaleOut(targetScale = ScaleOut, animationSpec = tween(AnimationDurationMs))

    override val AnimatedContentTransitionScope<NavBackStackEntry>.popEnterTransition: EnterTransition
        get() = fadeIn(animationSpec = tween(AnimationDurationMs)) +
            scaleIn(initialScale = ScaleOut, animationSpec = tween(AnimationDurationMs))

    override val AnimatedContentTransitionScope<NavBackStackEntry>.popExitTransition: ExitTransition
        get() = fadeOut(animationSpec = tween(AnimationDurationMs)) +
            scaleOut(targetScale = ScaleIn, animationSpec = tween(AnimationDurationMs))
}
