package ai.dokus.foundation.navigation

import androidx.navigation.NavGraphBuilder

fun interface NavigationProvider {
    fun NavGraphBuilder.registerGraph()
}