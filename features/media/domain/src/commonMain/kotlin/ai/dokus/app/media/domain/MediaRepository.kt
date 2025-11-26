package ai.dokus.app.media.domain

import ai.dokus.foundation.domain.enums.EntityType
import ai.dokus.foundation.domain.enums.MediaStatus
import ai.dokus.foundation.domain.ids.MediaId
import ai.dokus.foundation.domain.model.MediaDto
import ai.dokus.foundation.domain.model.MediaProcessingUpdateRequest
import ai.dokus.foundation.domain.model.MediaUploadRequest

interface MediaRepository {
    suspend fun upload(request: MediaUploadRequest): MediaDto
    suspend fun get(mediaId: MediaId): MediaDto
    suspend fun list(status: MediaStatus? = null, limit: Int = 50, offset: Int = 0): List<MediaDto>
    suspend fun listPending(limit: Int = 50, offset: Int = 0): List<MediaDto>
    suspend fun attach(mediaId: MediaId, entityType: EntityType, entityId: String): MediaDto
    suspend fun updateProcessing(request: MediaProcessingUpdateRequest): MediaDto
}
