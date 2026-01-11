package tech.dokus.foundation.backend.security

import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import tech.dokus.domain.enums.Permission
import tech.dokus.domain.enums.UserRole
import tech.dokus.domain.exceptions.DokusException

/**
 * Authentication method constants
 */
object AuthMethod {
    const val JWT = "auth-jwt"
}

/**
 * Role-to-permission mapping.
 * Defines which permissions each role has access to.
 *
 * Accountant role is specifically read-only as documented.
 */
object RolePermissions {
    private val ownerPermissions = Permission.entries.toSet()

    private val adminPermissions = Permission.entries.toSet() - setOf(Permission.UsersManage)

    private val accountantPermissions = setOf(
        Permission.InvoicesRead,
        Permission.ClientsRead,
        Permission.SettingsRead,
        Permission.ReportsView
    )

    private val editorPermissions = setOf(
        Permission.InvoicesRead,
        Permission.InvoicesCreate,
        Permission.InvoicesEdit,
        Permission.ClientsRead,
        Permission.ClientsManage,
        Permission.SettingsRead,
        Permission.ReportsView,
        Permission.ExportsCreate
    )

    private val viewerPermissions = setOf(
        Permission.InvoicesRead,
        Permission.ClientsRead,
        Permission.SettingsRead,
        Permission.ReportsView
    )

    /**
     * Get permissions for a given role.
     */
    fun permissionsFor(role: UserRole): Set<Permission> = when (role) {
        UserRole.Owner -> ownerPermissions
        UserRole.Admin -> adminPermissions
        UserRole.Accountant -> accountantPermissions
        UserRole.Editor -> editorPermissions
        UserRole.Viewer -> viewerPermissions
    }

    /**
     * Check if a role has a specific permission.
     */
    fun hasPermission(role: UserRole, permission: Permission): Boolean =
        permissionsFor(role).contains(permission)

    /**
     * Get role from string, defaulting to Viewer for unknown roles.
     */
    fun roleFromString(roleString: String?): UserRole =
        roleString?.let { r ->
            UserRole.entries.find { it.dbValue == r || it.name.equals(r, ignoreCase = true) }
        } ?: UserRole.Viewer
}

/**
 * Wrap routes with JWT authentication.
 * Similar to Pulse's authenticateJwt helper.
 *
 * Usage:
 * ```
 * route("/api/v1/resource") {
 *     authenticateJwt {
 *         get {
 *             val principal = dokusPrincipal
 *             // ... use principal
 *         }
 *     }
 * }
 * ```
 */
fun Route.authenticateJwt(
    build: Route.() -> Unit
): Route {
    return authenticate(AuthMethod.JWT, optional = false) {
        build()
    }
}

/**
 * Extension property to get the DokusPrincipal from authenticated routes.
 * Throws if no principal is available (route not authenticated or token invalid).
 *
 * Usage:
 * ```
 * get("/me") {
 *     val userId = dokusPrincipal.userId
 *     val tenantId = dokusPrincipal.requireTenantId()
 *     // ...
 * }
 * ```
 */
val RoutingContext.dokusPrincipal: DokusPrincipal
    get() = call.principal<DokusPrincipal>()
        ?: throw DokusException.NotAuthenticated("Authentication required")

/**
 * Get the user's role from the principal.
 * Defaults to Viewer if no role is set.
 */
val RoutingContext.userRole: UserRole
    get() = dokusPrincipal.roles.firstOrNull()?.let { RolePermissions.roleFromString(it) }
        ?: UserRole.Viewer

/**
 * Check if the current user has a specific permission.
 * Uses role-to-permission mapping.
 */
fun RoutingContext.hasPermission(permission: Permission): Boolean =
    RolePermissions.hasPermission(userRole, permission)

/**
 * Require that the current user has a specific permission.
 * Throws Forbidden if the permission is not granted.
 *
 * Usage:
 * ```
 * post("/invoices") {
 *     requirePermission(Permission.InvoicesCreate)
 *     // ... create invoice
 * }
 * ```
 */
fun RoutingContext.requirePermission(permission: Permission) {
    if (!hasPermission(permission)) {
        throw DokusException.NotAuthorized(
            "Permission denied: ${permission.dbValue} is required"
        )
    }
}

/**
 * Require that the current user has all of the specified permissions.
 * Throws Forbidden if any permission is not granted.
 */
fun RoutingContext.requireAllPermissions(vararg permissions: Permission) {
    permissions.forEach { requirePermission(it) }
}

/**
 * Require that the current user has at least one of the specified permissions.
 * Throws Forbidden if none of the permissions are granted.
 */
fun RoutingContext.requireAnyPermission(vararg permissions: Permission) {
    if (permissions.none { hasPermission(it) }) {
        throw DokusException.NotAuthorized(
            "Permission denied: one of [${permissions.joinToString { it.dbValue }}] is required"
        )
    }
}
