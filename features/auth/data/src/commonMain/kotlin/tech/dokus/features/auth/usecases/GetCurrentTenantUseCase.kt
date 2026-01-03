package tech.dokus.features.auth.usecases

import tech.dokus.domain.asbtractions.TokenManager
import tech.dokus.domain.model.Tenant
import tech.dokus.features.auth.datasource.TenantRemoteDataSource
import tech.dokus.foundation.platform.Logger

/**
 * Returns the tenant that is currently selected in the JWT claims.
 * Falls back to null if no tenant is set.
 */
class GetCurrentTenantUseCaseImpl(
    private val tokenManager: TokenManager,
    private val tenantDataSource: TenantRemoteDataSource
) : GetCurrentTenantUseCase {
    private val logger = Logger.forClass<GetCurrentTenantUseCaseImpl>()

    /**
     * Retrieves the currently scoped tenant from the user's JWT claims.
     *
     * Extracts the tenant scope from the current JWT token via [TokenManager], then
     * fetches the full tenant details from the [TenantRemoteDataSource]. If no tenant
     * is present in the claims (user hasn't selected a tenant yet), returns `null`
     * without making a network request.
     *
     * @return [Result.success] containing:
     *         - [Tenant] with full tenant details if a tenant is scoped in the session
     *         - `null` if no tenant is present in the JWT claims (tenant not yet selected)
     *
     *         [Result.failure] if retrieval failed, which may occur if:
     *         - Network error when fetching tenant details from the remote data source
     *         - Server error from the tenant API
     *         - The tenant ID in the claims no longer exists or is inaccessible
     * @see GetCurrentTenantUseCase.invoke for the interface contract
     */
    override suspend operator fun invoke(): Result<Tenant?> {
        val claims = tokenManager.getCurrentClaims()
        val tenantScope = claims?.tenant
        if (tenantScope == null) {
            logger.d { "No tenant present in JWT claims" }
            return Result.success(null)
        }

        return tenantDataSource.getTenant(tenantScope.tenantId)
            .onSuccess { tenant ->
                logger.d { "Loaded current tenant ${tenant.legalName.value}" }
            }
            .onFailure { error ->
                logger.e(error) { "Failed to load current tenant from claims" }
            }
            .map { it }
    }
}
