package ai.dokus.app.media.datasource

import ai.dokus.foundation.domain.enums.EntityType
import ai.dokus.foundation.domain.enums.MediaStatus
import ai.dokus.foundation.domain.ids.MediaId
import ai.dokus.foundation.domain.model.MediaDto
import ai.dokus.foundation.domain.model.MediaProcessingUpdateRequest
import ai.dokus.foundation.domain.model.MediaUploadRequest
import io.ktor.client.HttpClient

/**
 * Remote data source for media operations
 * Provides HTTP-based access to media management endpoints
 */
interface MediaRemoteDataSource {

    /**
     * Upload a new media item (receipt, invoice, etc.)
     * Stores the file and enqueues it for AI processing
     * POST /api/v1/media
     */
    suspend fun uploadMedia(request: MediaUploadRequest): Result<MediaDto>

    /**
     * Fetch a single media entry for the authenticated tenant
     * GET /api/v1/media/{id}
     */
    suspend fun getMedia(mediaId: MediaId): Result<MediaDto>

    /**
     * List media items, optionally filtered by processing statuses.
     * Supports filtering by multiple statuses.
     * GET /api/v1/media?status={status1,status2}&limit={limit}&offset={offset}
     *
     * @param statuses Optional list of statuses to filter by (if null or empty, returns all)
     * @param limit Maximum number of items to return
     * @param offset Number of items to skip
     */
    suspend fun listMedia(
        statuses: List<MediaStatus>? = null,
        limit: Int = 50,
        offset: Int = 0
    ): Result<List<MediaDto>>

    /**
     * Attach a processed media item to a domain entity (e.g., invoice or expense)
     * POST /api/v1/media/{id}/attach
     */
    suspend fun attachMedia(
        mediaId: MediaId,
        entityType: EntityType,
        entityId: String
    ): Result<MediaDto>

    /**
     * Update processing status and extracted data (called by AI pipeline)
     * PUT /api/v1/media/{id}/processing-result
     */
    suspend fun updateProcessingResult(request: MediaProcessingUpdateRequest): Result<MediaDto>

    companion object {
        internal fun create(httpClient: HttpClient): MediaRemoteDataSource {
            return MediaRemoteDataSourceImpl(httpClient)
        }
    }
}
