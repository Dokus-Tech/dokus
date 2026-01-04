package tech.dokus.features.auth.gateway

import tech.dokus.domain.model.TenantSettings
import tech.dokus.features.auth.datasource.TenantRemoteDataSource

internal class WorkspaceSettingsGatewayImpl(
    private val tenantRemoteDataSource: TenantRemoteDataSource
) : WorkspaceSettingsGateway {
    override suspend fun getTenantSettings() = tenantRemoteDataSource.getTenantSettings()

    override suspend fun getTenantAddress() = tenantRemoteDataSource.getTenantAddress()

    override suspend fun updateTenantSettings(settings: TenantSettings): Result<Unit> {
        return tenantRemoteDataSource.updateTenantSettings(settings)
    }

    override suspend fun uploadAvatar(
        imageBytes: ByteArray,
        filename: String,
        contentType: String,
        onProgress: (Float) -> Unit
    ) = tenantRemoteDataSource.uploadAvatar(
        imageBytes = imageBytes,
        filename = filename,
        contentType = contentType,
        onProgress = onProgress
    )

    override suspend fun deleteAvatar() = tenantRemoteDataSource.deleteAvatar()
}
