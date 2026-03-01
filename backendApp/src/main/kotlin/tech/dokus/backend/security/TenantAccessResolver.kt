package tech.dokus.backend.security

import io.ktor.server.routing.RoutingContext
import io.ktor.util.AttributeKey
import org.koin.ktor.ext.get
import tech.dokus.database.repository.auth.UserRepository
import tech.dokus.domain.enums.Permission
import tech.dokus.domain.enums.UserRole
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.auth.TenantAccess
import tech.dokus.foundation.backend.security.RolePermissions
import tech.dokus.foundation.backend.security.dokusPrincipal

const val TenantHeaderName = "X-Tenant-Id"

private val TenantAccessAttributeKey = AttributeKey<TenantAccess>("dokus.tenant.access")

val RoutingContext.tenantAccessOrNull: TenantAccess?
    get() = if (call.attributes.contains(TenantAccessAttributeKey)) {
        call.attributes[TenantAccessAttributeKey]
    } else {
        null
    }

suspend fun RoutingContext.requireTenantAccess(
    userRepository: UserRepository = call.application.get()
): TenantAccess {
    tenantAccessOrNull?.let { return it }

    val tenantId = resolveTenantIdFromRequest()
    val membership = userRepository.getMembership(dokusPrincipal.userId, tenantId)

    if (membership == null || !membership.isActive) {
        throw DokusException.NotAuthorized("You do not have access to tenant $tenantId")
    }

    return TenantAccess(
        tenantId = tenantId,
        role = membership.role
    ).also { resolved ->
        call.attributes.put(TenantAccessAttributeKey, resolved)
    }
}

fun TenantAccess.requireRole(required: UserRole): TenantAccess {
    if (role != required) {
        throw DokusException.NotAuthorized("Role ${required.dbValue} is required")
    }
    return this
}

fun TenantAccess.requireAnyRole(vararg requiredRoles: UserRole): TenantAccess {
    if (!requiredRoles.contains(role)) {
        val required = requiredRoles.joinToString(", ") { it.dbValue }
        throw DokusException.NotAuthorized("One of roles [$required] is required")
    }
    return this
}

fun TenantAccess.hasPermission(permission: Permission): Boolean =
    RolePermissions.hasPermission(role, permission)

suspend fun RoutingContext.requireTenantId(): TenantId = requireTenantAccess().tenantId

suspend fun RoutingContext.requirePermission(permission: Permission) {
    val access = requireTenantAccess()
    if (!access.hasPermission(permission)) {
        throw DokusException.NotAuthorized(
            "Permission denied: ${permission.dbValue} is required"
        )
    }
}

/**
 * Resolves tenant ID from request using fallback priority:
 * 1. X-Tenant-Id header (primary — set by client TenantHeaderPlugin)
 * 2. {tenantId} path parameter
 * 3. {tenant_id} query parameter
 */
private fun RoutingContext.resolveTenantIdFromRequest(): TenantId {
    val tenantRaw = call.request.headers[TenantHeaderName]
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?: call.parameters["tenantId"]
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
        ?: call.parameters["tenant_id"]
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
        ?: throw DokusException.BadRequest("Missing required header $TenantHeaderName")

    return runCatching { TenantId.parse(tenantRaw) }
        .getOrElse { throw DokusException.BadRequest("Invalid tenant id in $TenantHeaderName") }
}
