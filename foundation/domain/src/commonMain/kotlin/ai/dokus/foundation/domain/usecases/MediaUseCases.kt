package ai.dokus.foundation.domain.usecases

import ai.dokus.foundation.domain.enums.EntityType
import ai.dokus.foundation.domain.enums.MediaStatus
import ai.dokus.foundation.domain.ids.MediaId
import ai.dokus.foundation.domain.model.MediaDto
import ai.dokus.foundation.domain.model.MediaProcessingUpdateRequest
import ai.dokus.foundation.domain.model.MediaUploadRequest

/**
 * Use case interfaces for media operations.
 *
 * These interfaces are defined in foundation/domain so any module can
 * depend on them and inject implementations via DI.
 *
 * Implementations live in features/media/domain module.
 */

interface UploadMediaUseCase {
    suspend operator fun invoke(request: MediaUploadRequest): MediaDto
}

interface GetMediaUseCase {
    suspend operator fun invoke(mediaId: MediaId): MediaDto
}

interface ListMediaUseCase {
    suspend operator fun invoke(
        statuses: List<MediaStatus>? = null,
        limit: Int = 50,
        offset: Int = 0
    ): List<MediaDto>
}

interface AttachMediaUseCase {
    suspend operator fun invoke(
        mediaId: MediaId,
        entityType: EntityType,
        entityId: String
    ): MediaDto
}

interface UpdateMediaProcessingUseCase {
    suspend operator fun invoke(request: MediaProcessingUpdateRequest): MediaDto
}
