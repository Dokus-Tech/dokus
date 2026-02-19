package tech.dokus.app.screens.home

import tech.dokus.foundation.app.shell.HomeShellTopBarConfig
import tech.dokus.foundation.aura.model.NavItem
import tech.dokus.foundation.aura.model.ShellTopBarDefault
import tech.dokus.navigation.destinations.route

internal fun resolveHomeShellTopBarConfig(
    route: String?,
    allNavItems: List<NavItem>,
    sortedRoutes: List<String>,
    registeredConfigs: Map<String, HomeShellTopBarConfig>,
    fallback: (normalizedRoute: String, default: ShellTopBarDefault) -> HomeShellTopBarConfig?,
): HomeShellTopBarConfig? {
    val normalizedRoute = normalizeRoute(route, sortedRoutes) ?: return null
    val navItem = allNavItems.find { it.destination.route == normalizedRoute } ?: return null
    val default = navItem.shellTopBar ?: return null
    val registered = registeredConfigs[normalizedRoute]
    if (registered != null) return registered.takeIf { it.enabled }
    return fallback(normalizedRoute, default)
}

/** Pre-compute sorted known routes (longest first) for normalizeRoute. Call once via remember. */
internal fun buildSortedRoutes(allNavItems: List<NavItem>): List<String> =
    allNavItems.map { it.destination.route }.sortedByDescending { it.length }

/** Normalize a backstack route string to a known base route (strips query params and sub-paths). */
internal fun normalizeRoute(route: String?, sortedRoutes: List<String>): String? {
    val cleaned = route?.substringBefore("?") ?: return null
    return sortedRoutes.firstOrNull { cleaned == it || cleaned.startsWith("$it/") }
}
