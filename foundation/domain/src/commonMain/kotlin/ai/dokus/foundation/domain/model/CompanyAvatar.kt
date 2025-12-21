package ai.dokus.foundation.domain.model

import ai.dokus.foundation.domain.ids.TenantId
import kotlinx.serialization.Serializable

/**
 * Represents company avatar URLs at different sizes.
 * All URLs are presigned and time-limited.
 */
@Serializable
data class CompanyAvatar(
    val small: String,   // 64x64 presigned URL
    val medium: String,  // 128x128 presigned URL
    val large: String    // 256x256 presigned URL
)

/**
 * Response from avatar upload endpoint.
 */
@Serializable
data class AvatarUploadResponse(
    val avatar: CompanyAvatar,
    val storageKey: String,
    val tenantId: TenantId
)

/**
 * Request to upload a company avatar.
 * Used for multipart form data upload.
 */
@Serializable
data class AvatarUploadRequest(
    val tenantId: TenantId
)
