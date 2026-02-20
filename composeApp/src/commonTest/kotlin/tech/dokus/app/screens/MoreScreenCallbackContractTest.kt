package tech.dokus.app.screens

import tech.dokus.navigation.destinations.AuthDestination
import tech.dokus.navigation.destinations.HomeDestination
import tech.dokus.navigation.destinations.SettingsDestination
import kotlin.test.Test
import kotlin.test.assertEquals

class MoreScreenCallbackContractTest {

    @Test
    fun `mobile company details dispatches to root workspace settings`() {
        var homeCallCount = 0
        var rootCallCount = 0
        var rootDestination: SettingsDestination? = null

        dispatchMoreNavigation(
            destination = HomeDestination.WorkspaceDetails,
            onNavigateHome = { homeCallCount++ },
            onNavigateRoot = { destination ->
                rootCallCount++
                rootDestination = destination as? SettingsDestination
            }
        )

        assertEquals(0, homeCallCount)
        assertEquals(1, rootCallCount)
        assertEquals(SettingsDestination.WorkspaceSettings, rootDestination)
    }

    @Test
    fun `mobile team dispatches to root team settings`() {
        var homeCallCount = 0
        var rootCallCount = 0
        var rootDestination: SettingsDestination? = null

        dispatchMoreNavigation(
            destination = HomeDestination.Team,
            onNavigateHome = { homeCallCount++ },
            onNavigateRoot = { destination ->
                rootCallCount++
                rootDestination = destination as? SettingsDestination
            }
        )

        assertEquals(0, homeCallCount)
        assertEquals(1, rootCallCount)
        assertEquals(SettingsDestination.TeamSettings, rootDestination)
    }

    @Test
    fun `accountant dispatches to root callback`() {
        var homeCallCount = 0
        var rootCallCount = 0

        dispatchMoreNavigation(
            destination = HomeDestination.Accountant,
            onNavigateHome = { homeCallCount++ },
            onNavigateRoot = { rootCallCount++ }
        )

        assertEquals(0, homeCallCount)
        assertEquals(1, rootCallCount)
    }

    @Test
    fun `auth destination dispatches to root callback`() {
        var homeCallCount = 0
        var rootCallCount = 0

        dispatchMoreNavigation(
            destination = AuthDestination.ProfileSettings,
            onNavigateHome = { homeCallCount++ },
            onNavigateRoot = { rootCallCount++ }
        )

        assertEquals(0, homeCallCount)
        assertEquals(1, rootCallCount)
    }
}
