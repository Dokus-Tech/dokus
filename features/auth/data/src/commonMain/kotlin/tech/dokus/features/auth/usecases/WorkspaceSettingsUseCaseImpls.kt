package tech.dokus.features.auth.usecases

import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.Address
import tech.dokus.domain.model.TenantSettings
import tech.dokus.domain.model.common.Thumbnail
import tech.dokus.features.auth.gateway.WorkspaceSettingsGateway

internal class GetTenantSettingsUseCaseImpl(
    private val workspaceSettingsGateway: WorkspaceSettingsGateway
) : GetTenantSettingsUseCase {
    override suspend fun invoke(): Result<TenantSettings> {
        return workspaceSettingsGateway.getTenantSettings()
    }
}

internal class GetTenantAddressUseCaseImpl(
    private val workspaceSettingsGateway: WorkspaceSettingsGateway
) : GetTenantAddressUseCase {
    override suspend fun invoke(): Result<Address?> {
        return workspaceSettingsGateway.getTenantAddress()
    }
}

internal class UpdateTenantSettingsUseCaseImpl(
    private val workspaceSettingsGateway: WorkspaceSettingsGateway
) : UpdateTenantSettingsUseCase {
    override suspend fun invoke(settings: TenantSettings): Result<Unit> {
        return workspaceSettingsGateway.updateTenantSettings(settings)
    }
}

internal class UploadWorkspaceAvatarUseCaseImpl(
    private val workspaceSettingsGateway: WorkspaceSettingsGateway
) : UploadWorkspaceAvatarUseCase {
    override suspend fun invoke(
        tenantId: TenantId,
        imageBytes: ByteArray,
        filename: String,
        contentType: String,
        onProgress: (Float) -> Unit
    ): Result<Thumbnail> {
        return workspaceSettingsGateway.uploadAvatar(
            tenantId = tenantId,
            imageBytes = imageBytes,
            filename = filename,
            contentType = contentType,
            onProgress = onProgress
        )
    }
}

internal class DeleteWorkspaceAvatarUseCaseImpl(
    private val workspaceSettingsGateway: WorkspaceSettingsGateway
) : DeleteWorkspaceAvatarUseCase {
    override suspend fun invoke(tenantId: TenantId): Result<Unit> {
        return workspaceSettingsGateway.deleteAvatar(tenantId)
    }
}
