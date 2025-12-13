package ai.dokus.app.auth.datasource

import ai.dokus.foundation.domain.model.CreateTenantRequest
import ai.dokus.foundation.domain.ids.TenantId
import ai.dokus.foundation.domain.model.Tenant
import ai.dokus.foundation.domain.model.TenantSettings

/**
 * Remote data source for tenant management operations.
 * All methods require authentication and return Result to handle errors gracefully.
 */
interface TenantRemoteDataSource {
    /**
     * List all tenants the current user has access to.
     * @return Result containing a list of Tenant objects
     */
    suspend fun listMyTenants(): Result<List<Tenant>>

    /**
     * Create a new tenant.
     * @param request Tenant creation details
     * @return Result containing the created Tenant
     */
    suspend fun createTenant(request: CreateTenantRequest): Result<Tenant>

    /**
     * Get a specific tenant by ID.
     * @param id The tenant ID
     * @return Result containing the Tenant object
     */
    suspend fun getTenant(id: TenantId): Result<Tenant>

    /**
     * Get settings for the current tenant.
     * @return Result containing TenantSettings
     */
    suspend fun getTenantSettings(): Result<TenantSettings>

    /**
     * Update settings for the current tenant.
     * @param settings Updated tenant settings
     * @return Result indicating success or failure
     */
    suspend fun updateTenantSettings(settings: TenantSettings): Result<Unit>
}
