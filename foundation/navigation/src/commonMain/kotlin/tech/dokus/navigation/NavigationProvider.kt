package tech.dokus.navigation

import androidx.navigation.NavGraphBuilder

fun interface NavigationProvider {
    fun NavGraphBuilder.registerGraph()
}