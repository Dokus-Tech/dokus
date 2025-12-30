package tech.dokus.navigation.animation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.navigation.NavBackStackEntry

interface TransitionsProvider {
    val AnimatedContentTransitionScope<NavBackStackEntry>.enterTransition: EnterTransition
    val AnimatedContentTransitionScope<NavBackStackEntry>.exitTransition: ExitTransition
    val AnimatedContentTransitionScope<NavBackStackEntry>.popEnterTransition: EnterTransition
    val AnimatedContentTransitionScope<NavBackStackEntry>.popExitTransition: ExitTransition

    companion object {
        fun forRoot(isLargeScreen: Boolean): TransitionsProvider {
            return if (isLargeScreen) {
                TransitionsProviderLargeScreen()
            } else {
                TransitionsProviderSmallScreen()
            }
        }

        fun forTabs(isLargeScreen: Boolean = false): TransitionsProvider {
            return TabTransitionsProvider()
        }
    }
}