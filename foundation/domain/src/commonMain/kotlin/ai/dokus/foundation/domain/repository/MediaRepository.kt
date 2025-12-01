package ai.dokus.foundation.domain.repository

import ai.dokus.foundation.domain.enums.EntityType
import ai.dokus.foundation.domain.enums.MediaStatus
import ai.dokus.foundation.domain.ids.MediaId
import ai.dokus.foundation.domain.model.MediaDto
import ai.dokus.foundation.domain.model.MediaProcessingUpdateRequest
import ai.dokus.foundation.domain.model.MediaUploadRequest

/**
 * Repository interface for media operations.
 *
 * This interface is defined in foundation/domain so any module can
 * depend on it and inject the implementation via DI.
 *
 * The implementation lives in features/media/data module.
 */
interface MediaRepository {
    /**
     * Upload a new media file.
     */
    suspend fun upload(request: MediaUploadRequest): MediaDto

    /**
     * Get media by ID.
     */
    suspend fun get(mediaId: MediaId): MediaDto

    /**
     * List media files with optional status filter.
     */
    suspend fun list(status: MediaStatus? = null, limit: Int = 50, offset: Int = 0): List<MediaDto>

    /**
     * List media files pending processing.
     */
    suspend fun listPending(limit: Int = 50, offset: Int = 0): List<MediaDto>

    /**
     * Attach media to an entity (invoice, expense, bill, etc.)
     */
    suspend fun attach(mediaId: MediaId, entityType: EntityType, entityId: String): MediaDto

    /**
     * Update processing result for media.
     */
    suspend fun updateProcessing(request: MediaProcessingUpdateRequest): MediaDto
}
