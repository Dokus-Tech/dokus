package ai.dokus.app.auth.usecases

import ai.dokus.app.auth.repository.AuthRepository
import ai.dokus.app.auth.usecases.SelectTenantUseCase
import ai.dokus.foundation.domain.ids.TenantId
import ai.dokus.foundation.platform.Logger

/**
 * Scopes the session to the provided tenant by refreshing tokens.
 */
class SelectTenantUseCaseImpl(
    private val authRepository: AuthRepository
) : SelectTenantUseCase {
    private val logger = Logger.forClass<SelectTenantUseCaseImpl>()

    override suspend operator fun invoke(tenantId: TenantId): Result<Unit> {
        logger.d { "Selecting tenant $tenantId" }
        return authRepository.selectTenant(tenantId)
    }
}
