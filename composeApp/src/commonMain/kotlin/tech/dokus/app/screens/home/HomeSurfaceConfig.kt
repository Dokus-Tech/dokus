package tech.dokus.app.screens.home

import tech.dokus.app.navigation.HomeNavigationCommand
import tech.dokus.foundation.app.shell.UserAccessContext
import tech.dokus.foundation.aura.model.NavItem
import tech.dokus.navigation.destinations.HomeDestination
import tech.dokus.navigation.destinations.NavigationDestination

/**
 * Captures the behavioural differences between surfaces (CM / BC).
 * Everything else in the home shell is shared.
 */
internal data class HomeSurfaceConfig(
    val computeIsBCDrillDown: (currentRoute: String?) -> Boolean,
    val filterNavItems: (items: List<NavItem>, accessContext: UserAccessContext) -> List<NavItem>,
    val resolveStartDestination: (visible: List<NavItem>, all: List<NavItem>) -> NavigationDestination,
    val appendMoreTab: Boolean,
    val handleCommand: (command: HomeNavigationCommand, canBCAccess: Boolean, canCM: Boolean) -> SurfaceCommandAction,
    val interceptDestination: HomeDestination?,
    val shouldIntercept: (UserAccessContext) -> Boolean,
)

internal sealed interface SurfaceCommandAction {
    data object ExecuteLocally : SurfaceCommandAction
    data object SwitchSurface : SurfaceCommandAction
}
