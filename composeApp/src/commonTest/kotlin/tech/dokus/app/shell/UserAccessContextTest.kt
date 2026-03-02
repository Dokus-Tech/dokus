package tech.dokus.app.shell

import tech.dokus.domain.enums.UserRole
import tech.dokus.foundation.app.shell.UserAccessContext
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UserAccessContextTest {

    @Test
    fun `stage2 read-only is enabled for accountant role`() {
        val context = UserAccessContext(currentTenantRole = UserRole.Accountant)

        assertTrue(context.isStage2ReadOnly)
    }

    @Test
    fun `stage2 read-only is enabled for bookkeeper console drill-down while role is unresolved`() {
        val context = UserAccessContext(
            isBookkeeperConsoleDrillDown = true,
            currentTenantRole = null,
        )

        assertTrue(context.isStage2ReadOnly)
    }

    @Test
    fun `stage2 read-only stays disabled outside accountant drill-down context`() {
        val workspaceContext = UserAccessContext(
            isBookkeeperConsoleDrillDown = false,
            currentTenantRole = null,
        )
        val nonAccountantConsoleContext = UserAccessContext(
            isBookkeeperConsoleDrillDown = true,
            currentTenantRole = UserRole.Admin,
        )

        assertFalse(workspaceContext.isStage2ReadOnly)
        assertFalse(nonAccountantConsoleContext.isStage2ReadOnly)
    }
}
