package tech.dokus.app.screens

import tech.dokus.navigation.destinations.HomeDestination
import tech.dokus.navigation.destinations.SettingsDestination
import kotlin.test.Test
import kotlin.test.assertEquals

class MoreScreenNavigationResolverTest {

    @Test
    fun `team destination resolves to home controller`() {
        val target = resolveMoreNavigationTarget(HomeDestination.Team)
        assertEquals(MoreNavigationTarget.Home, target)
    }

    @Test
    fun `contacts destination resolves to home controller`() {
        val target = resolveMoreNavigationTarget(HomeDestination.Contacts)
        assertEquals(MoreNavigationTarget.Home, target)
    }

    @Test
    fun `workspace settings destination resolves to root controller`() {
        val target = resolveMoreNavigationTarget(SettingsDestination.WorkspaceSettings)
        assertEquals(MoreNavigationTarget.Root, target)
    }
}
