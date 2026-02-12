package tech.dokus.features.auth.usecases

import tech.dokus.domain.ids.TenantId

/**
 * Reads the last tenant ID that was successfully scoped in the user session.
 *
 * This value is persisted locally and can be used as a deterministic fallback
 * when the active tenant claim is missing.
 */
interface GetLastSelectedTenantIdUseCase {
    suspend operator fun invoke(): TenantId?
}
