package ai.dokus.app.media.network

import ai.dokus.foundation.domain.asbtractions.AuthManager
import ai.dokus.foundation.domain.asbtractions.TokenManager
import ai.dokus.foundation.domain.enums.EntityType
import ai.dokus.foundation.domain.enums.MediaStatus
import ai.dokus.foundation.domain.ids.MediaId
import ai.dokus.foundation.domain.model.MediaDto
import ai.dokus.foundation.domain.model.MediaProcessingUpdateRequest
import ai.dokus.foundation.domain.model.MediaUploadRequest
import ai.dokus.foundation.domain.rpc.MediaRemoteService
import ai.dokus.foundation.network.resilient.RemoteServiceDelegate
import ai.dokus.foundation.network.resilient.createRetryDelegate
import ai.dokus.foundation.network.resilient.invoke
import ai.dokus.foundation.network.resilient.withAuth

class ResilientMediaRemoteService(
    serviceProvider: () -> MediaRemoteService,
    tokenManager: TokenManager,
    authManager: AuthManager
) : MediaRemoteService {

    private val delegate: RemoteServiceDelegate<MediaRemoteService> =
        createRetryDelegate(serviceProvider).withAuth(tokenManager, authManager)

    override suspend fun uploadMedia(request: MediaUploadRequest): MediaDto =
        delegate { it.uploadMedia(request) }

    override suspend fun getMedia(mediaId: MediaId): MediaDto =
        delegate { it.getMedia(mediaId) }

    override suspend fun listMedia(status: MediaStatus?, limit: Int, offset: Int): List<MediaDto> =
        delegate { it.listMedia(status, limit, offset) }

    override suspend fun listPendingMedia(limit: Int, offset: Int): List<MediaDto> =
        delegate { it.listPendingMedia(limit, offset) }

    override suspend fun attachMedia(
        mediaId: MediaId,
        entityType: EntityType,
        entityId: String
    ): MediaDto = delegate { it.attachMedia(mediaId, entityType, entityId) }

    override suspend fun updateProcessingResult(request: MediaProcessingUpdateRequest): MediaDto =
        delegate { it.updateProcessingResult(request) }
}
