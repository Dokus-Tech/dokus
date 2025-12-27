package ai.dokus.app.auth.datasource

import ai.dokus.foundation.domain.ids.TenantId
import tech.dokus.domain.model.Address
import tech.dokus.domain.model.AvatarUploadResponse
import tech.dokus.domain.model.CreateTenantRequest
import tech.dokus.domain.model.Tenant
import tech.dokus.domain.model.TenantSettings
import tech.dokus.domain.model.UpsertTenantAddressRequest
import ai.dokus.foundation.domain.model.common.Thumbnail

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

    /**
     * Get company address for the current tenant.
     * @return Result containing Address or null if not configured.
     */
    suspend fun getTenantAddress(): Result<Address?>

    /**
     * Upsert company address for the current tenant.
     * @return Result containing the saved Address.
     */
    suspend fun upsertTenantAddress(request: UpsertTenantAddressRequest): Result<Address>

    // ===== Avatar Operations =====

    /**
     * Upload a company avatar image.
     * @param imageBytes The image data
     * @param filename Original filename
     * @param contentType MIME type of the image
     * @param onProgress Progress callback (0.0 to 1.0)
     * @return Result containing the upload response with avatar URLs
     */
    suspend fun uploadAvatar(
        imageBytes: ByteArray,
        filename: String,
        contentType: String,
        onProgress: (Float) -> Unit = {}
    ): Result<AvatarUploadResponse>

    /**
     * Get current avatar URLs for the tenant.
     * @return Result containing CompanyAvatar or null if no avatar is set
     */
    suspend fun getAvatar(): Result<Thumbnail?>

    /**
     * Delete the company avatar.
     * @return Result indicating success or failure
     */
    suspend fun deleteAvatar(): Result<Unit>

    /**
     * Preview the next invoice number without consuming it.
     * @return Result containing the preview invoice number string
     */
    suspend fun getInvoiceNumberPreview(): Result<String>
}
