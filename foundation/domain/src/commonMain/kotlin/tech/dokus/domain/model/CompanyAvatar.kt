package tech.dokus.domain.model

import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.common.Thumbnail
import kotlinx.serialization.Serializable

/**
 * Response from avatar upload endpoint.
 */
@Serializable
data class AvatarUploadResponse(
    val avatar: Thumbnail,
    val storageKey: String,
    val tenantId: TenantId
)
