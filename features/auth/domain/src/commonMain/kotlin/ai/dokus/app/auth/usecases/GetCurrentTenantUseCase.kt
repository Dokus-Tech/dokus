package ai.dokus.app.auth.usecases

import ai.dokus.foundation.domain.model.Tenant

/**
 * Returns the tenant currently scoped in the user's JWT claims.
 */
interface GetCurrentTenantUseCase {
    suspend operator fun invoke(): Result<Tenant?>
}
