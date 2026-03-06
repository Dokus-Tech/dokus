package tech.dokus.app.screens.console

import tech.dokus.foundation.app.shell.UserAccessContext
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConsoleAccessGuardsTest {

    @Test
    fun `console access is denied when surface is resolved and user has no console access`() {
        val accessContext = UserAccessContext(
            canBookkeeperConsole = false,
            isSurfaceAvailabilityResolved = true,
        )

        assertTrue(isConsoleAccessDenied(accessContext))
        assertFalse(canRenderConsoleContent(accessContext))
    }

    @Test
    fun `console content can render when surface is resolved and console access is granted`() {
        val accessContext = UserAccessContext(
            canBookkeeperConsole = true,
            isSurfaceAvailabilityResolved = true,
        )

        assertFalse(isConsoleAccessDenied(accessContext))
        assertTrue(canRenderConsoleContent(accessContext))
    }

    @Test
    fun `console content is blocked until surface availability is resolved`() {
        val accessContext = UserAccessContext(
            canBookkeeperConsole = true,
            isSurfaceAvailabilityResolved = false,
        )

        assertFalse(isConsoleAccessDenied(accessContext))
        assertFalse(canRenderConsoleContent(accessContext))
    }
}
