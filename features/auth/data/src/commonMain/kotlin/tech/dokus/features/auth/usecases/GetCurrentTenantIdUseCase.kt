package tech.dokus.features.auth.usecases

import tech.dokus.domain.asbtractions.TokenManager
import tech.dokus.domain.ids.TenantId

/**
 * Implementation that reads tenant ID directly from JWT claims.
 * No network call is made - this is safe for offline use.
 */
class GetCurrentTenantIdUseCaseImpl(
    private val tokenManager: TokenManager
) : GetCurrentTenantIdUseCase {

    override suspend operator fun invoke(): TenantId? {
        return tokenManager.getSelectedTenantId()
    }
}
