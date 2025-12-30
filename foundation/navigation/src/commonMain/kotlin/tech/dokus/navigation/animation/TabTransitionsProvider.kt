package tech.dokus.navigation.animation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.navigation.NavBackStackEntry

internal class TabTransitionsProvider : TransitionsProvider {
    override val AnimatedContentTransitionScope<NavBackStackEntry>.enterTransition: EnterTransition
        get() = fadeIn(animationSpec = tween(200, easing = LinearEasing))

    override val AnimatedContentTransitionScope<NavBackStackEntry>.exitTransition: ExitTransition
        get() = fadeOut(animationSpec = tween(200, easing = LinearEasing))

    override val AnimatedContentTransitionScope<NavBackStackEntry>.popEnterTransition: EnterTransition
        get() = fadeIn(animationSpec = tween(200, easing = LinearEasing))

    override val AnimatedContentTransitionScope<NavBackStackEntry>.popExitTransition: ExitTransition
        get() = fadeOut(animationSpec = tween(200, easing = LinearEasing))
}