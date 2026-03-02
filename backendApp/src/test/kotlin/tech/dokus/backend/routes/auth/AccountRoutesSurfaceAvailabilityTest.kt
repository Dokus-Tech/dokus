package tech.dokus.backend.routes.auth

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.junit.jupiter.api.Test
import tech.dokus.backend.services.auth.SurfaceResolver
import tech.dokus.domain.enums.FirmRole
import tech.dokus.domain.enums.UserRole
import tech.dokus.domain.ids.FirmId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.UserId
import tech.dokus.domain.model.FirmMembership
import tech.dokus.domain.model.TenantMembership
import tech.dokus.domain.model.auth.AppSurface
import kotlin.test.assertEquals

class AccountRoutesSurfaceAvailabilityTest {

    @Test
    fun `tenant memberships enable workspace surface`() {
        val result = SurfaceResolver.resolve(
            tenantMemberships = listOf(
                tenantMembership(role = UserRole.Owner),
                tenantMembership(role = UserRole.Admin)
            ),
            firmMemberships = emptyList(),
        )

        assertEquals(true, result.canCompanyManager)
        assertEquals(false, result.canBookkeeperConsole)
        assertEquals(AppSurface.CompanyManager, result.defaultSurface)
    }

    @Test
    fun `firm memberships enable console surface`() {
        val result = SurfaceResolver.resolve(
            tenantMemberships = emptyList(),
            firmMemberships = listOf(
                firmMembership(role = FirmRole.Owner),
                firmMembership(role = FirmRole.Staff)
            ),
        )

        assertEquals(false, result.canCompanyManager)
        assertEquals(true, result.canBookkeeperConsole)
        assertEquals(AppSurface.BookkeeperConsole, result.defaultSurface)
    }

    @Test
    fun `mixed memberships enable both and default to workspace`() {
        val result = SurfaceResolver.resolve(
            tenantMemberships = listOf(
                tenantMembership(role = UserRole.Editor),
            ),
            firmMemberships = listOf(
                firmMembership(role = FirmRole.Admin),
            ),
        )

        assertEquals(true, result.canCompanyManager)
        assertEquals(true, result.canBookkeeperConsole)
        assertEquals(AppSurface.CompanyManager, result.defaultSurface)
    }

    @Test
    fun `inactive memberships are ignored`() {
        val result = SurfaceResolver.resolve(
            tenantMemberships = listOf(
                tenantMembership(role = UserRole.Editor, isActive = false),
            ),
            firmMemberships = listOf(
                firmMembership(role = FirmRole.Staff, isActive = false),
            ),
        )

        assertEquals(false, result.canCompanyManager)
        assertEquals(false, result.canBookkeeperConsole)
        assertEquals(AppSurface.CompanyManager, result.defaultSurface)
    }

    @Test
    fun `empty memberships fallback to workspace default`() {
        val result = SurfaceResolver.resolve(
            tenantMemberships = emptyList(),
            firmMemberships = emptyList(),
        )

        assertEquals(false, result.canCompanyManager)
        assertEquals(false, result.canBookkeeperConsole)
        assertEquals(AppSurface.CompanyManager, result.defaultSurface)
    }

    private fun tenantMembership(
        role: UserRole,
        isActive: Boolean = true,
    ): TenantMembership {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        return TenantMembership(
            userId = UserId("00000000-0000-0000-0000-000000000123"),
            tenantId = TenantId.generate(),
            role = role,
            isActive = isActive,
            createdAt = now,
            updatedAt = now,
        )
    }

    private fun firmMembership(
        role: FirmRole,
        isActive: Boolean = true,
    ): FirmMembership {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        return FirmMembership(
            userId = UserId("00000000-0000-0000-0000-000000000123"),
            firmId = FirmId.generate(),
            role = role,
            isActive = isActive,
            createdAt = now,
            updatedAt = now,
        )
    }
}
