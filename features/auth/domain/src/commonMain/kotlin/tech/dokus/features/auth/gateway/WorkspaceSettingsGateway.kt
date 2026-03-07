package tech.dokus.features.auth.gateway

import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.Address
import tech.dokus.domain.model.TenantSettings
import tech.dokus.domain.model.common.Thumbnail

/**
 * Gateway for workspace settings and avatar operations.
 */
interface WorkspaceSettingsGateway {
    suspend fun getTenantSettings(): Result<TenantSettings>

    suspend fun getTenantAddress(): Result<Address?>

    suspend fun updateTenantSettings(settings: TenantSettings): Result<Unit>

    suspend fun uploadAvatar(
        tenantId: TenantId,
        imageBytes: ByteArray,
        filename: String,
        contentType: String,
        onProgress: (Float) -> Unit = {}
    ): Result<Thumbnail>

    suspend fun deleteAvatar(tenantId: TenantId): Result<Unit>
}
