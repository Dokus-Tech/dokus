package tech.dokus.app.screens.home

import tech.dokus.app.navigation.NavDefinition
import tech.dokus.foundation.app.shell.HomeShellTopBarConfig

internal fun resolveHomeShellTopBarConfig(
    route: String?,
    registeredConfigs: Map<String, HomeShellTopBarConfig>,
    fallback: (normalizedRoute: String) -> HomeShellTopBarConfig?,
): HomeShellTopBarConfig? {
    val normalizedRoute = NavDefinition.normalizeRoute(route) ?: return null
    if (!NavDefinition.shouldShowShellTopBar(normalizedRoute)) return null
    return registeredConfigs[normalizedRoute] ?: fallback(normalizedRoute)
}
