package ai.dokus.app.media.domain.usecases

import ai.dokus.app.media.domain.MediaRepository
import ai.dokus.foundation.domain.enums.EntityType
import ai.dokus.foundation.domain.enums.MediaStatus
import ai.dokus.foundation.domain.ids.MediaId
import ai.dokus.foundation.domain.model.MediaDto
import ai.dokus.foundation.domain.model.MediaProcessingUpdateRequest
import ai.dokus.foundation.domain.model.MediaUploadRequest

class UploadMediaUseCase(
    private val repository: MediaRepository
) {
    suspend operator fun invoke(request: MediaUploadRequest): MediaDto = repository.upload(request)
}

class ListMediaUseCase(
    private val repository: MediaRepository
) {
    suspend operator fun invoke(
        status: MediaStatus? = null,
        limit: Int = 50,
        offset: Int = 0
    ): List<MediaDto> = repository.list(status, limit, offset)
}

class ListPendingMediaUseCase(
    private val repository: MediaRepository
) {
    suspend operator fun invoke(
        limit: Int = 50,
        offset: Int = 0
    ): List<MediaDto> = repository.listPending(limit, offset)
}

class GetMediaUseCase(
    private val repository: MediaRepository
) {
    suspend operator fun invoke(mediaId: MediaId): MediaDto = repository.get(mediaId)
}

class AttachMediaUseCase(
    private val repository: MediaRepository
) {
    suspend operator fun invoke(
        mediaId: MediaId,
        entityType: EntityType,
        entityId: String
    ): MediaDto = repository.attach(mediaId, entityType, entityId)
}

class UpdateMediaProcessingUseCase(
    private val repository: MediaRepository
) {
    suspend operator fun invoke(request: MediaProcessingUpdateRequest): MediaDto =
        repository.updateProcessing(request)
}
