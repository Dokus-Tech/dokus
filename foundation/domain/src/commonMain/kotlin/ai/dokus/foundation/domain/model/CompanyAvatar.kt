package ai.dokus.foundation.domain.model

import ai.dokus.foundation.domain.ids.TenantId
import ai.dokus.foundation.domain.model.common.Thumbnail
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
