package ai.dokus.app.media.repository

import ai.dokus.app.media.datasource.MediaRemoteDataSource
import ai.dokus.foundation.domain.enums.EntityType
import ai.dokus.foundation.domain.enums.MediaStatus
import ai.dokus.foundation.domain.ids.MediaId
import ai.dokus.foundation.domain.model.MediaDto
import ai.dokus.foundation.domain.model.MediaProcessingUpdateRequest
import ai.dokus.foundation.domain.model.MediaUploadRequest
import ai.dokus.foundation.domain.repository.MediaRepository

/**
 * Implementation of MediaRepository that delegates to remote data source.
 *
 * Implements interface from foundation/domain so it can be injected
 * anywhere via DI without direct module dependency.
 */
class MediaRepositoryImpl(
    private val remoteDataSource: MediaRemoteDataSource
) : MediaRepository {
    override suspend fun upload(request: MediaUploadRequest): MediaDto =
        remoteDataSource.uploadMedia(request).getOrThrow()

    override suspend fun get(mediaId: MediaId): MediaDto =
        remoteDataSource.getMedia(mediaId).getOrThrow()

    override suspend fun list(status: MediaStatus?, limit: Int, offset: Int): List<MediaDto> =
        remoteDataSource.listMedia(status, limit, offset).getOrThrow()

    override suspend fun listPending(limit: Int, offset: Int): List<MediaDto> =
        remoteDataSource.listPendingMedia(limit, offset).getOrThrow()

    override suspend fun attach(mediaId: MediaId, entityType: EntityType, entityId: String): MediaDto =
        remoteDataSource.attachMedia(mediaId, entityType, entityId).getOrThrow()

    override suspend fun updateProcessing(request: MediaProcessingUpdateRequest): MediaDto =
        remoteDataSource.updateProcessingResult(request).getOrThrow()
}
