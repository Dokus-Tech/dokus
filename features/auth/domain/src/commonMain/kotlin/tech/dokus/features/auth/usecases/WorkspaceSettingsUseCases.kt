package tech.dokus.features.auth.usecases

import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.Address
import tech.dokus.domain.model.TenantSettings
import tech.dokus.domain.model.common.Thumbnail

/**
 * Use case for loading tenant settings.
 */
interface GetTenantSettingsUseCase {
    suspend operator fun invoke(): Result<TenantSettings>
}

/**
 * Use case for loading tenant address.
 */
interface GetTenantAddressUseCase {
    suspend operator fun invoke(): Result<Address?>
}

/**
 * Use case for updating tenant settings.
 */
interface UpdateTenantSettingsUseCase {
    suspend operator fun invoke(settings: TenantSettings): Result<Unit>
}

/**
 * Use case for uploading the workspace avatar.
 */
interface UploadWorkspaceAvatarUseCase {
    suspend operator fun invoke(
        tenantId: TenantId,
        imageBytes: ByteArray,
        filename: String,
        contentType: String,
        onProgress: (Float) -> Unit = {}
    ): Result<Thumbnail>
}

/**
 * Use case for deleting the workspace avatar.
 */
interface DeleteWorkspaceAvatarUseCase {
    suspend operator fun invoke(tenantId: TenantId): Result<Unit>
}
