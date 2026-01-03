package tech.dokus.domain.model.common

import kotlinx.serialization.Serializable

@Serializable
data class Thumbnail(
    val small: String, // 64x64 presigned URL
    val medium: String, // 128x128 presigned URL
    val large: String // 256x256 presigned URL
)
