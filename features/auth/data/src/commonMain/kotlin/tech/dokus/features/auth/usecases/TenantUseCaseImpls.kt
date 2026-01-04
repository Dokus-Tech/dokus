package tech.dokus.features.auth.usecases

import tech.dokus.domain.model.Address
import tech.dokus.domain.model.AvatarUploadResponse
import tech.dokus.domain.model.Tenant
import tech.dokus.domain.model.TenantSettings
import tech.dokus.features.auth.datasource.TenantRemoteDataSource

internal class ListMyTenantsUseCaseImpl(
    private val tenantRemoteDataSource: TenantRemoteDataSource
) : ListMyTenantsUseCase {
    override suspend fun invoke(): Result<List<Tenant>> {
        return tenantRemoteDataSource.listMyTenants()
    }
}

internal class GetInvoiceNumberPreviewUseCaseImpl(
    private val tenantRemoteDataSource: TenantRemoteDataSource
) : GetInvoiceNumberPreviewUseCase {
    override suspend fun invoke(): Result<String> {
        return tenantRemoteDataSource.getInvoiceNumberPreview()
    }
}

internal class WorkspaceSettingsUseCaseImpl(
    private val tenantRemoteDataSource: TenantRemoteDataSource
) : WorkspaceSettingsUseCase {
    override suspend fun getTenantSettings(): Result<TenantSettings> {
        return tenantRemoteDataSource.getTenantSettings()
    }

    override suspend fun getTenantAddress(): Result<Address?> {
        return tenantRemoteDataSource.getTenantAddress()
    }

    override suspend fun updateTenantSettings(settings: TenantSettings): Result<Unit> {
        return tenantRemoteDataSource.updateTenantSettings(settings)
    }

    override suspend fun uploadAvatar(
        imageBytes: ByteArray,
        filename: String,
        contentType: String,
        onProgress: (Float) -> Unit
    ): Result<AvatarUploadResponse> {
        return tenantRemoteDataSource.uploadAvatar(
            imageBytes = imageBytes,
            filename = filename,
            contentType = contentType,
            onProgress = onProgress
        )
    }

    override suspend fun deleteAvatar(): Result<Unit> {
        return tenantRemoteDataSource.deleteAvatar()
    }
}
