package ai.dokus.foundation.domain.rpc

import ai.dokus.foundation.domain.enums.EntityType
import ai.dokus.foundation.domain.enums.MediaStatus
import ai.dokus.foundation.domain.ids.MediaId
import ai.dokus.foundation.domain.model.MediaDto
import ai.dokus.foundation.domain.model.MediaProcessingUpdateRequest
import ai.dokus.foundation.domain.model.MediaUploadRequest
import kotlinx.rpc.annotations.Rpc

@Rpc
interface MediaRemoteService {
    /**
    * Upload a new media item (receipt, invoice, etc.).
    * Stores the file and enqueues it for AI processing.
    */
    suspend fun uploadMedia(request: MediaUploadRequest): MediaDto

    /**
     * Fetch a single media entry for the authenticated tenant.
     */
    suspend fun getMedia(mediaId: MediaId): MediaDto

    /**
     * List media items, optionally filtered by processing status.
     */
    suspend fun listMedia(
        status: MediaStatus? = null,
        limit: Int = 50,
        offset: Int = 0
    ): List<MediaDto>

    /**
     * Convenience endpoint to list only pending/processing media items.
     */
    suspend fun listPendingMedia(
        limit: Int = 50,
        offset: Int = 0
    ): List<MediaDto>

    /**
     * Attach a processed media item to a domain entity (e.g., invoice or expense).
     */
    suspend fun attachMedia(
        mediaId: MediaId,
        entityType: EntityType,
        entityId: String
    ): MediaDto

    /**
     * Update processing status and extracted data (called by AI pipeline).
     */
    suspend fun updateProcessingResult(request: MediaProcessingUpdateRequest): MediaDto
}
