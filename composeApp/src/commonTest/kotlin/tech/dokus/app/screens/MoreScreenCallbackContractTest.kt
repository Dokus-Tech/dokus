package tech.dokus.app.screens

import tech.dokus.navigation.destinations.AuthDestination
import tech.dokus.navigation.destinations.BankingDestination
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
            onNavigateRoot = { destination ->
                rootCallCount++
                rootDestination = destination as? SettingsDestination
            }
        )

        assertEquals(1, rootCallCount)
        assertEquals(SettingsDestination.TeamSettings, rootDestination)
    }

    @Test
    fun `banking balances dispatches to root banking balances`() {
        var rootCallCount = 0
        var rootDestination: NavigationDestination? = null

        dispatchMoreNavigation(
            destination = HomeDestination.Balances,
            onNavigateRoot = { destination ->
                rootCallCount++
                rootDestination = destination
            }
        )

        assertEquals(1, rootCallCount)
        assertEquals(BankingDestination.Balances, rootDestination)
    }

    @Test
    fun `banking payments dispatches to root banking payments`() {
        var rootCallCount = 0
        var rootDestination: NavigationDestination? = null

        dispatchMoreNavigation(
            destination = HomeDestination.Payments,
            onNavigateRoot = { destination ->
                rootCallCount++
                rootDestination = destination
            }
        )

        assertEquals(1, rootCallCount)
        assertEquals(BankingDestination.Payments, rootDestination)
    }

    @Test
    fun `console clients dispatches to root callback`() {
        var rootCallCount = 0

        dispatchMoreNavigation(
            destination = HomeDestination.ConsoleClients,
            onNavigateRoot = { rootCallCount++ }
        )

        assertEquals(1, rootCallCount)
    }

    @Test
    fun `auth destination dispatches to root callback`() {
        var rootCallCount = 0

        dispatchMoreNavigation(
            destination = AuthDestination.ProfileSettings,
            onNavigateRoot = { rootCallCount++ }
        )

        assertEquals(1, rootCallCount)
    }
}
