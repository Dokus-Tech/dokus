package tech.dokus.app.screens

import kotlin.test.Test
import kotlin.test.assertEquals

class HomeRouteProfileNavigationTest {

    @Test
    fun `mobile profile dispatches to root callback`() {
        var homeCallCount = 0
        var rootCallCount = 0

        dispatchProfileNavigation(
            isLargeScreen = false,
            onNavigateHomeProfile = { homeCallCount++ },
            onNavigateRootProfile = { rootCallCount++ }
        )

        assertEquals(0, homeCallCount)
        assertEquals(1, rootCallCount)
    }

    @Test
    fun `large-screen profile dispatches to home callback`() {
        var homeCallCount = 0
        var rootCallCount = 0

        dispatchProfileNavigation(
            isLargeScreen = true,
            onNavigateHomeProfile = { homeCallCount++ },
            onNavigateRootProfile = { rootCallCount++ }
        )

        assertEquals(1, homeCallCount)
        assertEquals(0, rootCallCount)
    }
}
