package tech.dokus.app.shell

import tech.dokus.foundation.app.shell.UserAccessContext
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UserAccessContextTest {

    @Test
    fun `bookkeeper console only when can console but not company manager`() {
        val context = UserAccessContext(canCompanyManager = false, canBookkeeperConsole = true)

        assertTrue(context.isBookkeeperConsoleOnly)
    }

    @Test
    fun `not bookkeeper console only when both surfaces available`() {
        val context = UserAccessContext(canCompanyManager = true, canBookkeeperConsole = true)

        assertFalse(context.isBookkeeperConsoleOnly)
    }
}
