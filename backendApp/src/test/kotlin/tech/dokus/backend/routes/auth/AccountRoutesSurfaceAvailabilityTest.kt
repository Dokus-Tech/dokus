package tech.dokus.backend.routes.auth

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.junit.jupiter.api.Test
import tech.dokus.domain.enums.UserRole
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.UserId
import tech.dokus.domain.model.TenantMembership
import tech.dokus.domain.model.auth.AppSurface
import kotlin.test.assertEquals
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
class AccountRoutesSurfaceAvailabilityTest {

    @Test
    fun `only non-accountant memberships enable workspace only`() {
        val result = resolveSurfaceAvailability(
            memberships = listOf(
                membership(role = UserRole.Owner),
                membership(role = UserRole.Admin)
            )
        )

        assertEquals(true, result.canWorkspace)
        assertEquals(false, result.canConsole)
        assertEquals(AppSurface.Workspace, result.defaultSurface)
    }

    @Test
    fun `only accountant memberships enable console only`() {
        val result = resolveSurfaceAvailability(
            memberships = listOf(
                membership(role = UserRole.Accountant),
                membership(role = UserRole.Accountant)
            )
        )

        assertEquals(false, result.canWorkspace)
        assertEquals(true, result.canConsole)
        assertEquals(AppSurface.Console, result.defaultSurface)
    }

    @Test
    fun `mixed memberships enable both and default to workspace`() {
        val result = resolveSurfaceAvailability(
            memberships = listOf(
                membership(role = UserRole.Accountant),
                membership(role = UserRole.Editor)
            )
        )

        assertEquals(true, result.canWorkspace)
        assertEquals(true, result.canConsole)
        assertEquals(AppSurface.Workspace, result.defaultSurface)
    }

    @Test
    fun `inactive memberships are ignored`() {
        val result = resolveSurfaceAvailability(
            memberships = listOf(
                membership(role = UserRole.Accountant, isActive = false),
                membership(role = UserRole.Editor, isActive = false)
            )
        )

        assertEquals(false, result.canWorkspace)
        assertEquals(false, result.canConsole)
        assertEquals(AppSurface.Workspace, result.defaultSurface)
    }

    @Test
    fun `empty memberships fallback to workspace default`() {
        val result = resolveSurfaceAvailability(emptyList())

        assertEquals(false, result.canWorkspace)
        assertEquals(false, result.canConsole)
        assertEquals(AppSurface.Workspace, result.defaultSurface)
    }

    private fun membership(
        role: UserRole,
        isActive: Boolean = true
    ): TenantMembership {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        return TenantMembership(
            userId = UserId("00000000-0000-0000-0000-000000000123"),
            tenantId = TenantId.generate(),
            role = role,
            isActive = isActive,
            createdAt = now,
            updatedAt = now
        )
    }
}
