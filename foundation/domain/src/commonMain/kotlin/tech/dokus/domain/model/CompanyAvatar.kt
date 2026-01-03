package tech.dokus.domain.model

import kotlinx.serialization.Serializable
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.common.Thumbnail

/**
 * Response from avatar upload endpoint.
 */
@Serializable
data class AvatarUploadResponse(
    val avatar: Thumbnail,
    val storageKey: String,
    val tenantId: TenantId
)
