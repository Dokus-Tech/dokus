package tech.dokus.features.auth.gateway

import tech.dokus.domain.model.Address
import tech.dokus.domain.model.AvatarUploadResponse
import tech.dokus.domain.model.TenantSettings

/**
 * Gateway for workspace settings and avatar operations.
 */
interface WorkspaceSettingsGateway {
    suspend fun getTenantSettings(): Result<TenantSettings>

    suspend fun getTenantAddress(): Result<Address?>

    suspend fun updateTenantSettings(settings: TenantSettings): Result<Unit>

    suspend fun uploadAvatar(
        imageBytes: ByteArray,
        filename: String,
        contentType: String,
        onProgress: (Float) -> Unit = {}
    ): Result<AvatarUploadResponse>

    suspend fun deleteAvatar(): Result<Unit>
}
