package ai.dokus.app.auth.usecases

import tech.dokus.domain.ids.TenantId

/**
 * Scopes the user's session to a specific tenant.
 *
 * In multi-tenant environments, users may have access to multiple tenants (organizations).
 * After authentication, users must select which tenant to work with. This use case handles
 * that selection by refreshing the user's JWT tokens with the selected tenant scope,
 * ensuring all subsequent API requests are authorized for that tenant's data.
 *
 * This is typically called after login when a user has access to multiple tenants,
 * or when switching between tenants in a tenant selector UI.
 */
interface SelectTenantUseCase {
    /**
     * Selects the specified tenant for the current user session.
     *
     * @param tenantId The unique identifier of the tenant to scope the session to.
     *                 The user must have access to this tenant.
     * @return [Result.success] with [Unit] if tenant selection succeeded and tokens
     *         were refreshed with the new tenant scope.
     *         [Result.failure] if the selection failed, which may occur if:
     *         - The user does not have access to the specified tenant
     *         - The token refresh request failed (network error, server error)
     *         - The user is not authenticated
     */
    suspend operator fun invoke(tenantId: TenantId): Result<Unit>
}
