package ai.dokus.foundation.ktor.services

import ai.dokus.foundation.domain.AttachmentId
import ai.dokus.foundation.domain.TenantId
import ai.dokus.foundation.domain.enums.EntityType
import ai.dokus.foundation.domain.model.Attachment
import ai.dokus.foundation.domain.model.UploadInfo
import kotlinx.rpc.annotations.Rpc
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Rpc
interface AttachmentService {
    /**
     * Uploads a file and associates it with an entity
     * Stores the file in S3/MinIO and creates metadata record
     *
     * @param tenantId The tenant's unique identifier
     * @param entityType The type of entity this attachment belongs to
     * @param entityId The unique identifier of the entity
     * @param filename The original filename
     * @param mimeType The file MIME type (e.g., "image/jpeg", "application/pdf")
     * @param fileContent The file content as bytes
     * @return The created attachment record
     * @throws IllegalArgumentException if file validation fails or entity not found
     */
    suspend fun upload(
        tenantId: TenantId,
        entityType: EntityType,
        entityId: Uuid,
        filename: String,
        mimeType: String,
        fileContent: ByteArray
    ): Attachment

    /**
     * Downloads an attachment's file content
     *
     * @param attachmentId The attachment's unique identifier
     * @return The file content as ByteArray
     * @throws IllegalArgumentException if attachment not found
     */
    suspend fun download(attachmentId: AttachmentId): ByteArray

    /**
     * Deletes an attachment
     * Removes both the metadata record and the file from storage
     *
     * @param attachmentId The attachment's unique identifier
     * @throws IllegalArgumentException if attachment not found
     */
    suspend fun delete(attachmentId: AttachmentId)

    /**
     * Lists all attachments for an entity
     *
     * @param entityType The type of entity
     * @param entityId The unique identifier of the entity
     * @return List of attachments for the entity
     */
    suspend fun listByEntity(entityType: EntityType, entityId: Uuid): List<Attachment>

    /**
     * Lists all attachments for a tenant
     *
     * @param tenantId The tenant's unique identifier
     * @param entityType Filter by entity type (optional)
     * @param limit Maximum number of results (optional)
     * @param offset Pagination offset (optional)
     * @return List of attachments
     */
    suspend fun listByTenant(
        tenantId: TenantId,
        entityType: EntityType? = null,
        limit: Int? = null,
        offset: Int? = null
    ): List<Attachment>

    /**
     * Finds an attachment by its unique ID
     *
     * @param id The attachment's unique identifier
     * @return The attachment if found, null otherwise
     */
    suspend fun findById(id: AttachmentId): Attachment?

    /**
     * Gets a presigned URL for direct file download
     * Useful for browser downloads without proxying through backend
     *
     * @param attachmentId The attachment's unique identifier
     * @param expiresInSeconds URL expiration time in seconds (defaults to 3600 = 1 hour)
     * @return The presigned URL
     * @throws IllegalArgumentException if attachment not found
     */
    suspend fun getPresignedUrl(attachmentId: AttachmentId, expiresInSeconds: Int = 3600): String

    /**
     * Gets a presigned URL for direct file upload
     * Allows browser to upload directly to S3 without proxying through backend
     *
     * @param tenantId The tenant's unique identifier
     * @param entityType The type of entity this attachment will belong to
     * @param entityId The unique identifier of the entity
     * @param filename The filename for the upload
     * @param mimeType The file MIME type
     * @param expiresInSeconds URL expiration time in seconds (defaults to 3600 = 1 hour)
     * @return Upload information with uploadUrl and s3Key - use s3Key to create metadata after upload
     * @throws IllegalArgumentException if validation fails
     */
    suspend fun getPresignedUploadUrl(
        tenantId: TenantId,
        entityType: EntityType,
        entityId: Uuid,
        filename: String,
        mimeType: String,
        expiresInSeconds: Int = 3600
    ): UploadInfo

    /**
     * Creates attachment metadata after a presigned upload
     * Call this after successfully uploading via presigned URL
     *
     * @param tenantId The tenant's unique identifier
     * @param entityType The type of entity
     * @param entityId The unique identifier of the entity
     * @param filename The original filename
     * @param mimeType The file MIME type
     * @param sizeBytes The file size in bytes
     * @param s3Key The S3 key from presigned upload
     * @param s3Bucket The S3 bucket name
     * @return The created attachment record
     */
    suspend fun createMetadata(
        tenantId: TenantId,
        entityType: EntityType,
        entityId: Uuid,
        filename: String,
        mimeType: String,
        sizeBytes: Long,
        s3Key: String,
        s3Bucket: String
    ): Attachment

    /**
     * Gets total storage used by a tenant
     *
     * @param tenantId The tenant's unique identifier
     * @return Total bytes used
     */
    suspend fun getTotalStorageUsed(tenantId: TenantId): Long

    /**
     * Validates file type and size
     *
     * @param mimeType The file MIME type
     * @param sizeBytes The file size in bytes
     * @param maxSizeBytes Maximum allowed file size (defaults to 10MB)
     * @return True if valid, throws exception otherwise
     * @throws IllegalArgumentException if validation fails
     */
    suspend fun validateFile(
        mimeType: String,
        sizeBytes: Long,
        maxSizeBytes: Long = 10_485_760 // 10MB
    ): Boolean

    /**
     * Gets allowed MIME types for attachments
     *
     * @return List of allowed MIME types
     */
    suspend fun getAllowedMimeTypes(): List<String>

    /**
     * Deletes all attachments for an entity
     * Used when deleting an entity
     *
     * @param entityType The type of entity
     * @param entityId The unique identifier of the entity
     * @return Number of attachments deleted
     */
    suspend fun deleteAllForEntity(entityType: EntityType, entityId: Uuid): Int
}
