package ai.dokus.app.auth.usecases

import ai.dokus.foundation.domain.ids.TenantId

/**
 * Scopes the user's session to the provided tenant.
 */
interface SelectTenantUseCase {
    suspend operator fun invoke(tenantId: TenantId): Result<Unit>
}
