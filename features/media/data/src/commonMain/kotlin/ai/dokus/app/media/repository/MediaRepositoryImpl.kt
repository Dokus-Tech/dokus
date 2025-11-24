package ai.dokus.app.media.repository

import ai.dokus.app.media.domain.MediaRepository
import ai.dokus.foundation.domain.enums.EntityType
import ai.dokus.foundation.domain.enums.MediaStatus
import ai.dokus.foundation.domain.ids.MediaId
import ai.dokus.foundation.domain.model.MediaDto
import ai.dokus.foundation.domain.model.MediaProcessingUpdateRequest
import ai.dokus.foundation.domain.model.MediaUploadRequest
import ai.dokus.foundation.domain.rpc.MediaRemoteService

class MediaRepositoryImpl(
    private val remoteService: MediaRemoteService
) : MediaRepository {
    override suspend fun upload(request: MediaUploadRequest): MediaDto =
        remoteService.uploadMedia(request)

    override suspend fun get(mediaId: MediaId): MediaDto =
        remoteService.getMedia(mediaId)

    override suspend fun list(status: MediaStatus?, limit: Int, offset: Int): List<MediaDto> =
        remoteService.listMedia(status, limit, offset)

    override suspend fun listPending(limit: Int, offset: Int): List<MediaDto> =
        remoteService.listPendingMedia(limit, offset)

    override suspend fun attach(mediaId: MediaId, entityType: EntityType, entityId: String): MediaDto =
        remoteService.attachMedia(mediaId, entityType, entityId)

    override suspend fun updateProcessing(request: MediaProcessingUpdateRequest): MediaDto =
        remoteService.updateProcessingResult(request)
}
