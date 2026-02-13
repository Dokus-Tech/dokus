package tech.dokus.app.screens.home

import tech.dokus.app.navigation.NavDefinition
import tech.dokus.foundation.app.shell.HomeShellTopBarConfig
import tech.dokus.foundation.app.shell.HomeShellTopBarMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HomeShellTopBarResolverTest {

    @Test
    fun `shell visibility is limited to today documents and cashflow`() {
        assertTrue(NavDefinition.shouldShowShellTopBar(NavDefinition.Routes.TODAY))
        assertTrue(NavDefinition.shouldShowShellTopBar(NavDefinition.Routes.DOCUMENTS))
        assertTrue(NavDefinition.shouldShowShellTopBar(NavDefinition.Routes.CASHFLOW))

        assertFalse(NavDefinition.shouldShowShellTopBar(NavDefinition.Routes.CONTACTS))
        assertFalse(NavDefinition.shouldShowShellTopBar(NavDefinition.Routes.TEAM))
        assertFalse(NavDefinition.shouldShowShellTopBar(NavDefinition.Routes.AI_CHAT))
        assertFalse(NavDefinition.shouldShowShellTopBar(NavDefinition.Routes.MORE))
        assertFalse(NavDefinition.shouldShowShellTopBar(NavDefinition.Routes.WORKSPACE_SETTINGS))
        assertFalse(NavDefinition.shouldShowShellTopBar(null))
    }

    @Test
    fun `registered config overrides default config`() {
        val route = NavDefinition.Routes.CASHFLOW
        val registered = mapOf(
            route to HomeShellTopBarConfig(
                mode = HomeShellTopBarMode.Title("Custom Cashflow")
            )
        )

        val resolved = resolveHomeShellTopBarConfig(
            route = route,
            registeredConfigs = registered,
            fallback = ::fallbackConfigForRoute
        )

        assertEquals(
            expected = "Custom Cashflow",
            actual = (resolved?.mode as HomeShellTopBarMode.Title).title
        )
    }

    @Test
    fun `clearing route registration restores default config`() {
        val route = NavDefinition.Routes.CASHFLOW
        val registered = mutableMapOf(
            route to HomeShellTopBarConfig(
                mode = HomeShellTopBarMode.Title("Custom Cashflow")
            )
        )

        registered.remove(route)

        val resolved = resolveHomeShellTopBarConfig(
            route = route,
            registeredConfigs = registered,
            fallback = ::fallbackConfigForRoute
        )

        assertEquals(
            expected = "Default Cashflow",
            actual = (resolved?.mode as HomeShellTopBarMode.Title).title
        )
    }

    private fun fallbackConfigForRoute(normalizedRoute: String): HomeShellTopBarConfig? {
        return when (NavDefinition.resolveShellTopBarDefault(normalizedRoute)?.mode) {
            NavDefinition.ShellTopBarDefaultMode.Search -> HomeShellTopBarConfig(
                mode = HomeShellTopBarMode.Search(
                    query = "",
                    placeholder = "Search",
                    onQueryChange = {}
                )
            )

            NavDefinition.ShellTopBarDefaultMode.Title -> HomeShellTopBarConfig(
                mode = HomeShellTopBarMode.Title("Default Cashflow")
            )

            null -> null
        }
    }
}
