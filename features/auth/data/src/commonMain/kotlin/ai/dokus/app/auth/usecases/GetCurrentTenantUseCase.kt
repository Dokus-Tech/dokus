package ai.dokus.app.auth.usecases

import ai.dokus.app.auth.domain.TenantRemoteService
import ai.dokus.app.auth.usecases.GetCurrentTenantUseCase
import ai.dokus.foundation.domain.asbtractions.TokenManager
import ai.dokus.foundation.domain.model.Tenant
import ai.dokus.foundation.platform.Logger

/**
 * Returns the tenant that is currently selected in the JWT claims.
 * Falls back to null if no tenant is set.
 */
class GetCurrentTenantUseCaseImpl(
    private val tokenManager: TokenManager,
    private val tenantRemoteService: TenantRemoteService
) : GetCurrentTenantUseCase {
    private val logger = Logger.forClass<GetCurrentTenantUseCaseImpl>()

    override suspend operator fun invoke(): Result<Tenant?> = runCatching {
        val claims = tokenManager.getCurrentClaims()
        val tenantScope = claims?.tenant
        if (tenantScope == null) {
            logger.d { "No tenant present in JWT claims" }
            return@runCatching null
        }

        tenantRemoteService.getTenant(tenantScope.tenantId).also { tenant ->
            logger.d { "Loaded current tenant ${tenant.legalName.value}" }
        }
    }.onFailure { error ->
        logger.e(error) { "Failed to load current tenant from claims" }
    }
}
