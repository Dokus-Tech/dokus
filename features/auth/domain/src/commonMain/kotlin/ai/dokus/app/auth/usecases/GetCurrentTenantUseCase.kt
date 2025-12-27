package ai.dokus.app.auth.usecases

import tech.dokus.domain.model.Tenant

/**
 * Retrieves the tenant currently scoped in the user's JWT claims.
 *
 * In multi-tenant environments, users may have access to multiple tenants (organizations).
 * After authentication and tenant selection via [SelectTenantUseCase], the selected tenant
 * is embedded in the user's JWT token claims. This use case extracts that tenant information,
 * enabling the application to display tenant context and verify the current working scope.
 *
 * This is typically called to:
 * - Display the current tenant name in the UI (e.g., header, navigation)
 * - Verify a tenant is selected before performing tenant-scoped operations
 * - Determine if the user needs to be redirected to tenant selection
 *
 * @see SelectTenantUseCase for scoping a session to a specific tenant
 */
interface GetCurrentTenantUseCase {
    /**
     * Retrieves the currently scoped tenant from the user's session.
     *
     * @return [Result.success] containing:
     *         - [Tenant] if a tenant is currently scoped in the session
     *         - `null` if the user is authenticated but has not yet selected a tenant
     *
     *         [Result.failure] if retrieval failed, which may occur if:
     *         - The user is not authenticated (no valid JWT token)
     *         - The token parsing or validation failed
     *         - Network error when fetching tenant details
     */
    suspend operator fun invoke(): Result<Tenant?>
}
