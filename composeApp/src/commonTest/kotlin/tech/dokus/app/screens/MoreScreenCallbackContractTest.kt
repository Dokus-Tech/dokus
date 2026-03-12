package tech.dokus.app.screens

import tech.dokus.navigation.destinations.HomeDestination
import tech.dokus.navigation.destinations.NavigationDestination
import tech.dokus.navigation.destinations.SettingsDestination
import kotlin.test.Test
import kotlin.test.assertEquals

class MoreScreenCallbackContractTest {

    @Test
    fun `mobile company details dispatches to root workspace settings`() {
        var rootCallCount = 0
        var rootDestination: SettingsDestination? = null

        dispatchMoreNavigation(
            destination = HomeDestination.WorkspaceDetails,
            onNavigateHome = { error("Should not navigate home") },
            onNavigateRoot = { destination ->
                rootCallCount++
                rootDestination = destination as? SettingsDestination
            }
        )

        assertEquals(1, rootCallCount)
        assertEquals(SettingsDestination.WorkspaceSettings, rootDestination)
    }

    @Test
    fun `mobile team dispatches to root team settings`() {
        var rootCallCount = 0
        var rootDestination: SettingsDestination? = null

        dispatchMoreNavigation(
            destination = HomeDestination.Team,
            onNavigateHome = { error("Should not navigate home") },
            onNavigateRoot = { destination ->
                rootCallCount++
                rootDestination = destination as? SettingsDestination
            }
        )

        assertEquals(1, rootCallCount)
        assertEquals(SettingsDestination.TeamSettings, rootDestination)
    }

    @Test
    fun `home destination dispatches to home callback`() {
        var homeCallCount = 0
        var homeDestination: NavigationDestination? = null

        dispatchMoreNavigation(
            destination = HomeDestination.Balances,
            onNavigateHome = { destination ->
                homeCallCount++
                homeDestination = destination
            },
            onNavigateRoot = { error("Should not navigate root") }
        )

        assertEquals(1, homeCallCount)
        assertEquals(HomeDestination.Balances, homeDestination)
    }

    @Test
    fun `payments destination dispatches to home callback`() {
        var homeCallCount = 0
        var homeDestination: NavigationDestination? = null

        dispatchMoreNavigation(
            destination = HomeDestination.Payments,
            onNavigateHome = { destination ->
                homeCallCount++
                homeDestination = destination
            },
            onNavigateRoot = { error("Should not navigate root") }
        )

        assertEquals(1, homeCallCount)
        assertEquals(HomeDestination.Payments, homeDestination)
    }
}
