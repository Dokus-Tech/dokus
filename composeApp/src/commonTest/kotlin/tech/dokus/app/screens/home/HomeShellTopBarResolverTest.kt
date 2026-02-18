package tech.dokus.app.screens.home

import tech.dokus.foundation.app.shell.HomeShellTopBarConfig
import tech.dokus.foundation.app.shell.HomeShellTopBarMode
import tech.dokus.foundation.aura.model.NavItem
import tech.dokus.foundation.aura.model.ShellTopBarDefault
import tech.dokus.navigation.destinations.HomeDestination
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.chart_bar_trend_up
import tech.dokus.aura.resources.cashflow
import tech.dokus.aura.resources.cashflow_title
import tech.dokus.aura.resources.file_text
import tech.dokus.aura.resources.home_today
import tech.dokus.aura.resources.nav_contacts
import tech.dokus.aura.resources.nav_documents
import tech.dokus.aura.resources.nav_team
import tech.dokus.aura.resources.users
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HomeShellTopBarResolverTest {

    private val testNavItems = listOf(
        NavItem("today", Res.string.home_today, Res.drawable.chart_bar_trend_up,
            HomeDestination.Today, shellTopBar = ShellTopBarDefault.Search),
        NavItem("documents", Res.string.nav_documents, Res.drawable.file_text,
            HomeDestination.Documents, shellTopBar = ShellTopBarDefault.Search),
        NavItem("cashflow", Res.string.cashflow_title, Res.drawable.cashflow,
            HomeDestination.Cashflow, shellTopBar = ShellTopBarDefault.Title),
        NavItem("contacts", Res.string.nav_contacts, Res.drawable.users,
            HomeDestination.Contacts),
        NavItem("team", Res.string.nav_team, Res.drawable.users,
            HomeDestination.Team),
    )

    @Test
    fun `shell visibility is driven by NavItem shellTopBar field`() {
        // Items with shellTopBar set should resolve
        val todayResult = resolveHomeShellTopBarConfig(
            "today", testNavItems, emptyMap(), ::fallbackConfig
        )
        assertEquals("Search fallback", (todayResult?.mode as? HomeShellTopBarMode.Search)?.placeholder)

        val cashflowResult = resolveHomeShellTopBarConfig(
            "cashflow", testNavItems, emptyMap(), ::fallbackConfig
        )
        assertEquals("Default Cashflow", (cashflowResult?.mode as? HomeShellTopBarMode.Title)?.title)

        // Items without shellTopBar should return null
        val contactsResult = resolveHomeShellTopBarConfig(
            "contacts", testNavItems, emptyMap(), ::fallbackConfig
        )
        assertNull(contactsResult)

        val nullResult = resolveHomeShellTopBarConfig(
            null, testNavItems, emptyMap(), ::fallbackConfig
        )
        assertNull(nullResult)
    }

    @Test
    fun `registered config overrides default config`() {
        val registered = mapOf(
            "cashflow" to HomeShellTopBarConfig(
                mode = HomeShellTopBarMode.Title("Custom Cashflow")
            )
        )

        val resolved = resolveHomeShellTopBarConfig(
            route = "cashflow",
            allNavItems = testNavItems,
            registeredConfigs = registered,
            fallback = ::fallbackConfig
        )

        assertEquals(
            expected = "Custom Cashflow",
            actual = (resolved?.mode as HomeShellTopBarMode.Title).title
        )
    }

    @Test
    fun `clearing route registration restores default config`() {
        val registered = mutableMapOf(
            "cashflow" to HomeShellTopBarConfig(
                mode = HomeShellTopBarMode.Title("Custom Cashflow")
            )
        )

        registered.remove("cashflow")

        val resolved = resolveHomeShellTopBarConfig(
            route = "cashflow",
            allNavItems = testNavItems,
            registeredConfigs = registered,
            fallback = ::fallbackConfig
        )

        assertEquals(
            expected = "Default Cashflow",
            actual = (resolved?.mode as HomeShellTopBarMode.Title).title
        )
    }

    @Test
    fun `disabled route registration suppresses fallback top bar`() {
        val registered = mapOf(
            "documents" to HomeShellTopBarConfig(
                enabled = false,
                mode = HomeShellTopBarMode.Search(
                    query = "ignored",
                    placeholder = "Search",
                    onQueryChange = {}
                )
            )
        )

        val resolved = resolveHomeShellTopBarConfig(
            route = "documents",
            allNavItems = testNavItems,
            registeredConfigs = registered,
            fallback = ::fallbackConfig
        )

        assertNull(resolved)
    }

    private fun fallbackConfig(normalizedRoute: String, default: ShellTopBarDefault): HomeShellTopBarConfig? {
        return when (default) {
            ShellTopBarDefault.Search -> HomeShellTopBarConfig(
                mode = HomeShellTopBarMode.Search(
                    query = "",
                    placeholder = "Search fallback",
                    onQueryChange = {}
                )
            )
            ShellTopBarDefault.Title -> HomeShellTopBarConfig(
                mode = HomeShellTopBarMode.Title("Default Cashflow")
            )
        }
    }
}
