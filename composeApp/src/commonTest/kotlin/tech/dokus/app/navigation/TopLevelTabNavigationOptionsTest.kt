package tech.dokus.app.navigation

import tech.dokus.navigation.defaultTopLevelTabNavigationPolicy
import kotlin.test.Test
import kotlin.test.assertTrue

class TopLevelTabNavigationOptionsTest {

    @Test
    fun `default top-level tab policy enables restore and dedupe behavior`() {
        assertTrue(defaultTopLevelTabNavigationPolicy.saveState)
        assertTrue(defaultTopLevelTabNavigationPolicy.restoreState)
        assertTrue(defaultTopLevelTabNavigationPolicy.launchSingleTop)
    }
}
