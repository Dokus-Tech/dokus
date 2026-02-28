package tech.dokus.domain.model.auth

import kotlinx.serialization.Serializable
import tech.dokus.domain.enums.UserRole
import tech.dokus.domain.ids.TenantId

/**
 * Tenant-scoped access context resolved server-side per request.
 */
@Serializable
data class TenantAccess(
    val tenantId: TenantId,
    val roles: Set<UserRole>
)
