package tech.dokus.features.auth.usecases

import tech.dokus.domain.ids.TenantId

/**
 * Retrieves the tenant ID currently scoped in the user's JWT claims.
 *
 * Unlike [GetCurrentTenantUseCase], this does NOT make a network call.
 * It only reads from the locally stored JWT token, making it safe for offline use.
 *
 * Use this when you only need the tenant ID (e.g., for local cache lookups)
 * and don't need full tenant details.
 */
interface GetCurrentTenantIdUseCase {
    /**
     * Retrieves the tenant ID from the locally stored JWT claims.
     *
     * @return The [TenantId] if a tenant is scoped in the session, or `null` if:
     *         - The user is not authenticated
     *         - No tenant has been selected yet
     */
    suspend operator fun invoke(): TenantId?
}
