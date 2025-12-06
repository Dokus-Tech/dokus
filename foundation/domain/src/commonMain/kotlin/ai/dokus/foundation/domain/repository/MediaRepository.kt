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
     * Supports filtering by multiple statuses.
     *
     * @param statuses Optional list of statuses to filter by (if null or empty, returns all)
     * @param limit Maximum number of items to return
     * @param offset Number of items to skip
     */
    suspend fun list(statuses: List<MediaStatus>? = null, limit: Int = 50, offset: Int = 0): List<MediaDto>

    /**
     * Attach media to an entity (invoice, expense, bill, etc.)
     */
    suspend fun attach(mediaId: MediaId, entityType: EntityType, entityId: String): MediaDto

    /**
     * Update processing result for media.
     */
    suspend fun updateProcessing(request: MediaProcessingUpdateRequest): MediaDto
}
