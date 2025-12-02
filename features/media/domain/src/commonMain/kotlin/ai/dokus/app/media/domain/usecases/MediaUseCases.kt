package ai.dokus.app.media.domain.usecases

import ai.dokus.foundation.domain.enums.EntityType
import ai.dokus.foundation.domain.enums.MediaStatus
import ai.dokus.foundation.domain.ids.MediaId
import ai.dokus.foundation.domain.model.MediaDto
import ai.dokus.foundation.domain.model.MediaProcessingUpdateRequest
import ai.dokus.foundation.domain.model.MediaUploadRequest
import ai.dokus.foundation.domain.repository.MediaRepository
import ai.dokus.foundation.domain.usecases.AttachMediaUseCase
import ai.dokus.foundation.domain.usecases.GetMediaUseCase
import ai.dokus.foundation.domain.usecases.ListMediaUseCase
import ai.dokus.foundation.domain.usecases.ListPendingMediaUseCase
import ai.dokus.foundation.domain.usecases.UpdateMediaProcessingUseCase
import ai.dokus.foundation.domain.usecases.UploadMediaUseCase

/**
 * Upload media use case implementation.
 */
class UploadMediaUseCaseImpl(
    private val repository: MediaRepository
) : UploadMediaUseCase {
    override suspend operator fun invoke(request: MediaUploadRequest): MediaDto =
        repository.upload(request)
}

/**
 * List media use case implementation.
 */
class ListMediaUseCaseImpl(
    private val repository: MediaRepository
) : ListMediaUseCase {
    override suspend operator fun invoke(
        status: MediaStatus?,
        limit: Int,
        offset: Int
    ): List<MediaDto> = repository.list(status, limit, offset)
}

/**
 * List pending media use case implementation.
 */
class ListPendingMediaUseCaseImpl(
    private val repository: MediaRepository
) : ListPendingMediaUseCase {
    override suspend operator fun invoke(
        limit: Int,
        offset: Int
    ): List<MediaDto> = repository.listPending(limit, offset)
}

/**
 * Get media use case implementation.
 */
class GetMediaUseCaseImpl(
    private val repository: MediaRepository
) : GetMediaUseCase {
    override suspend operator fun invoke(mediaId: MediaId): MediaDto =
        repository.get(mediaId)
}

/**
 * Attach media use case implementation.
 */
class AttachMediaUseCaseImpl(
    private val repository: MediaRepository
) : AttachMediaUseCase {
    override suspend operator fun invoke(
        mediaId: MediaId,
        entityType: EntityType,
        entityId: String
    ): MediaDto = repository.attach(mediaId, entityType, entityId)
}

/**
 * Update media processing use case implementation.
 */
class UpdateMediaProcessingUseCaseImpl(
    private val repository: MediaRepository
) : UpdateMediaProcessingUseCase {
    override suspend operator fun invoke(request: MediaProcessingUpdateRequest): MediaDto =
        repository.updateProcessing(request)
}
