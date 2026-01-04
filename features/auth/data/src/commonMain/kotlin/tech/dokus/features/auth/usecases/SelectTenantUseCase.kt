package tech.dokus.features.auth.usecases

import tech.dokus.domain.ids.TenantId
import tech.dokus.features.auth.gateway.AuthGateway
import tech.dokus.foundation.platform.Logger

/**
 * Scopes the session to the provided tenant by refreshing tokens.
 */
class SelectTenantUseCaseImpl(
    private val authGateway: AuthGateway
) : SelectTenantUseCase {
    private val logger = Logger.forClass<SelectTenantUseCaseImpl>()

    /**
     * Selects the specified tenant for the current user session.
     *
     * Delegates to the [AuthGateway] to refresh the user's JWT tokens with the
     * selected tenant scope. This ensures all subsequent API requests are authorized
     * for that tenant's data.
     *
     * @param tenantId The unique identifier of the tenant to scope the session to.
     *                 The user must have access to this tenant.
     * @return [Result.success] with [Unit] if tenant selection succeeded and tokens
     *         were refreshed with the new tenant scope.
     *         [Result.failure] if the selection failed, which may occur if:
     *         - The user does not have access to the specified tenant
     *         - The token refresh request failed (network error, server error)
     *         - The user is not authenticated
     * @see SelectTenantUseCase.invoke for the interface contract
     */
    override suspend operator fun invoke(tenantId: TenantId): Result<Unit> {
        logger.d { "Selecting tenant $tenantId" }
        return authGateway.selectTenant(tenantId)
    }
}
