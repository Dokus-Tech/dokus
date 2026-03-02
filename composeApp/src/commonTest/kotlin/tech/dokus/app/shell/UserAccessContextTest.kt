package tech.dokus.app.shell

import tech.dokus.foundation.app.shell.UserAccessContext
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UserAccessContextTest {

    @Test
    fun `stage2 read-only is enabled for bookkeeper console drill-down`() {
        val context = UserAccessContext(isBookkeeperConsoleDrillDown = true)

        assertTrue(context.isStage2ReadOnly)
    }

    @Test
    fun `stage2 read-only stays disabled outside console drill-down`() {
        val context = UserAccessContext(isBookkeeperConsoleDrillDown = false)

        assertFalse(context.isStage2ReadOnly)
    }
}
