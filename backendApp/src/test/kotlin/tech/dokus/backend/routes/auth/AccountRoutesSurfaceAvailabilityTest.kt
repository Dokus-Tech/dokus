package tech.dokus.backend.routes.auth

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.junit.jupiter.api.Test
import tech.dokus.backend.services.auth.SurfaceResolver
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
    fun `only non-accountant memberships enable company manager only`() {
        val result = SurfaceResolver.resolve(
            memberships = listOf(
                membership(role = UserRole.Owner),
                membership(role = UserRole.Admin)
            )
        )

        assertEquals(true, result.canCompanyManager)
        assertEquals(false, result.canBookkeeperConsole)
        assertEquals(AppSurface.CompanyManager, result.defaultSurface)
    }

    @Test
    fun `only accountant memberships enable bookkeeper console only`() {
        val result = SurfaceResolver.resolve(
            memberships = listOf(
                membership(role = UserRole.Accountant),
                membership(role = UserRole.Accountant)
            )
        )

        assertEquals(false, result.canCompanyManager)
        assertEquals(true, result.canBookkeeperConsole)
        assertEquals(AppSurface.BookkeeperConsole, result.defaultSurface)
    }

    @Test
    fun `mixed memberships enable both and default to company manager`() {
        val result = SurfaceResolver.resolve(
            memberships = listOf(
                membership(role = UserRole.Accountant),
                membership(role = UserRole.Editor)
            )
        )

        assertEquals(true, result.canCompanyManager)
        assertEquals(true, result.canBookkeeperConsole)
        assertEquals(AppSurface.CompanyManager, result.defaultSurface)
    }

    @Test
    fun `inactive memberships are ignored`() {
        val result = SurfaceResolver.resolve(
            memberships = listOf(
                membership(role = UserRole.Accountant, isActive = false),
                membership(role = UserRole.Editor, isActive = false)
            )
        )

        assertEquals(false, result.canCompanyManager)
        assertEquals(false, result.canBookkeeperConsole)
        assertEquals(AppSurface.CompanyManager, result.defaultSurface)
    }

    @Test
    fun `empty memberships fallback to company manager default`() {
        val result = SurfaceResolver.resolve(emptyList())

        assertEquals(false, result.canCompanyManager)
        assertEquals(false, result.canBookkeeperConsole)
        assertEquals(AppSurface.CompanyManager, result.defaultSurface)
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
