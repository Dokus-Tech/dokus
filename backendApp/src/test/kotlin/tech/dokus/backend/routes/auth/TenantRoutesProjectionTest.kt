package tech.dokus.backend.routes.auth

import kotlinx.datetime.LocalDateTime
import org.junit.jupiter.api.Test
import tech.dokus.domain.DisplayName
import tech.dokus.domain.LegalName
import tech.dokus.domain.enums.Language
import tech.dokus.domain.enums.SubscriptionTier
import tech.dokus.domain.enums.TenantStatus
import tech.dokus.domain.enums.TenantType
import tech.dokus.domain.enums.UserRole
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.model.Tenant
import tech.dokus.domain.model.common.Thumbnail
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TenantRoutesProjectionTest {

    @Test
    fun `projectTenantForMembership sets membership role on tenant projection`() {
        val tenant = Tenant(
            id = TenantId.generate(),
            type = TenantType.Company,
            legalName = LegalName("Invoid BV"),
            displayName = DisplayName("Invoid BV"),
            subscription = SubscriptionTier.Core,
            status = TenantStatus.Active,
            language = Language.En,
            vatNumber = VatNumber("BE0123456789"),
            createdAt = LocalDateTime(2026, 2, 1, 10, 0),
            updatedAt = LocalDateTime(2026, 2, 1, 10, 0)
        )
        assertNull(tenant.role)

        val avatar = Thumbnail(
            small = "https://example.com/small.png",
            medium = "https://example.com/medium.png",
            large = "https://example.com/large.png"
        )

        val projected = projectTenantForMembership(
            tenant = tenant,
            role = UserRole.Accountant,
            avatar = avatar
        )

        assertEquals(UserRole.Accountant, projected.role)
        assertEquals(avatar, projected.avatar)
        assertEquals(tenant.id, projected.id)
        assertEquals(tenant.displayName, projected.displayName)
    }
}
