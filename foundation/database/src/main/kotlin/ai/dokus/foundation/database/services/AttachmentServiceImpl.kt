package ai.dokus.foundation.database.services

import ai.dokus.foundation.database.mappers.AttachmentMapper.toAttachment
import ai.dokus.foundation.database.storage.FileStorage
import ai.dokus.foundation.database.tables.AttachmentsTable
import ai.dokus.foundation.database.utils.dbQuery
import ai.dokus.foundation.domain.AttachmentId
import ai.dokus.foundation.domain.TenantId
import ai.dokus.foundation.domain.enums.EntityType
import ai.dokus.foundation.domain.model.Attachment
import ai.dokus.foundation.domain.model.UploadInfo
import ai.dokus.foundation.ktor.services.AttachmentService
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.slf4j.LoggerFactory
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

@OptIn(ExperimentalUuidApi::class)
class AttachmentServiceImpl(
    private val fileStorage: FileStorage
) : AttachmentService {
    private val logger = LoggerFactory.getLogger(AttachmentServiceImpl::class.java)

    // Allowed MIME types for security
    private val allowedMimeTypes = setOf(
        // Images
        "image/jpeg",
        "image/jpg",
        "image/png",
        "image/gif",
        "image/webp",
        // Documents
        "application/pdf",
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.ms-excel",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "text/plain",
        "text/csv"
    )

    override suspend fun upload(
        tenantId: TenantId,
        entityType: EntityType,
        entityId: Uuid,
        filename: String,
        mimeType: String,
        fileContent: ByteArray
    ): Attachment {
        // Validate file
        validateFile(mimeType, fileContent.size.toLong())

        // Sanitize filename
        val sanitizedFilename = filename.replace(Regex("[^a-zA-Z0-9._-]"), "_")

        // Generate unique S3 key
        val s3Key = "attachments/$tenantId/$entityType/$entityId/${Uuid.random()}_$sanitizedFilename"
        val s3Bucket = "dokus-storage" // Default bucket name

        // Store file
        val storageUrl = fileStorage.store(s3Key, fileContent, mimeType)

        // Create metadata record
        val attachment = dbQuery {
            val attachmentId = AttachmentsTable.insertAndGetId {
                it[AttachmentsTable.tenantId] = tenantId.value.toJavaUuid()
                it[AttachmentsTable.entityType] = entityType
                it[AttachmentsTable.entityId] = entityId.toJavaUuid()
                it[AttachmentsTable.filename] = sanitizedFilename
                it[AttachmentsTable.mimeType] = mimeType
                it[AttachmentsTable.sizeBytes] = fileContent.size.toLong()
                it[AttachmentsTable.s3Key] = s3Key
                it[AttachmentsTable.s3Bucket] = s3Bucket
            }.value

            AttachmentsTable.selectAll()
                .where { AttachmentsTable.id eq attachmentId }
                .single()
                .toAttachment()
        }

        logger.info("Uploaded attachment ${attachment.id} for $entityType $entityId: $sanitizedFilename (${fileContent.size} bytes)")

        return attachment
    }

    override suspend fun download(attachmentId: AttachmentId): ByteArray {
        val attachment = findById(attachmentId)
            ?: throw IllegalArgumentException("Attachment not found: $attachmentId")

        return try {
            fileStorage.retrieve(attachment.s3Key)
        } catch (e: Exception) {
            logger.error("Failed to download attachment $attachmentId", e)
            throw IllegalArgumentException("Failed to download attachment: ${e.message}", e)
        }
    }

    override suspend fun delete(attachmentId: AttachmentId) {
        val attachment = findById(attachmentId)
            ?: throw IllegalArgumentException("Attachment not found: $attachmentId")

        // Delete file from storage
        try {
            fileStorage.delete(attachment.s3Key)
        } catch (e: Exception) {
            logger.warn("Failed to delete file from storage for attachment $attachmentId: ${e.message}")
            // Continue with metadata deletion even if file deletion fails
        }

        // Delete metadata
        dbQuery {
            AttachmentsTable.deleteWhere { id eq attachmentId.value.toJavaUuid() }
        }

        logger.info("Deleted attachment $attachmentId")
    }

    override suspend fun listByEntity(entityType: EntityType, entityId: Uuid): List<Attachment> = dbQuery {
        AttachmentsTable.selectAll()
            .where { AttachmentsTable.entityType eq entityType }
            .andWhere { AttachmentsTable.entityId eq entityId.toJavaUuid() }
            .orderBy(AttachmentsTable.uploadedAt to SortOrder.DESC)
            .map { it.toAttachment() }
    }

    override suspend fun listByTenant(
        tenantId: TenantId,
        entityType: EntityType?,
        limit: Int?,
        offset: Int?
    ): List<Attachment> = dbQuery {
        var query = AttachmentsTable.selectAll()
            .where { AttachmentsTable.tenantId eq tenantId.value.toJavaUuid() }

        if (entityType != null) {
            query = query.andWhere { AttachmentsTable.entityType eq entityType }
        }

        if (limit != null) query = query.limit(limit)
        if (offset != null) query = query.offset(offset.toLong())

        query.orderBy(AttachmentsTable.uploadedAt to SortOrder.DESC)
            .map { it.toAttachment() }
    }

    override suspend fun findById(id: AttachmentId): Attachment? = dbQuery {
        AttachmentsTable.selectAll()
            .where { AttachmentsTable.id eq id.value.toJavaUuid() }
            .singleOrNull()
            ?.toAttachment()
    }

    override suspend fun getPresignedUrl(attachmentId: AttachmentId, expiresInSeconds: Int): String {
        val attachment = findById(attachmentId)
            ?: throw IllegalArgumentException("Attachment not found: $attachmentId")

        return fileStorage.getPresignedDownloadUrl(attachment.s3Key, expiresInSeconds)
    }

    override suspend fun getPresignedUploadUrl(
        tenantId: TenantId,
        entityType: EntityType,
        entityId: Uuid,
        filename: String,
        mimeType: String,
        expiresInSeconds: Int
    ): UploadInfo {
        // Validate MIME type
        validateFile(mimeType, 0) // Size will be validated on actual upload

        // Sanitize filename
        val sanitizedFilename = filename.replace(Regex("[^a-zA-Z0-9._-]"), "_")

        // Generate unique S3 key
        val s3Key = "attachments/$tenantId/$entityType/$entityId/${Uuid.random()}_$sanitizedFilename"

        // Get presigned upload URL
        val uploadUrl = fileStorage.getPresignedUploadUrl(s3Key, mimeType, expiresInSeconds)

        return UploadInfo(uploadUrl = uploadUrl, s3Key = s3Key)
    }

    override suspend fun createMetadata(
        tenantId: TenantId,
        entityType: EntityType,
        entityId: Uuid,
        filename: String,
        mimeType: String,
        sizeBytes: Long,
        s3Key: String,
        s3Bucket: String
    ): Attachment {
        // Validate file metadata
        validateFile(mimeType, sizeBytes)

        return dbQuery {
            val attachmentId = AttachmentsTable.insertAndGetId {
                it[AttachmentsTable.tenantId] = tenantId.value.toJavaUuid()
                it[AttachmentsTable.entityType] = entityType
                it[AttachmentsTable.entityId] = entityId.toJavaUuid()
                it[AttachmentsTable.filename] = filename
                it[AttachmentsTable.mimeType] = mimeType
                it[AttachmentsTable.sizeBytes] = sizeBytes
                it[AttachmentsTable.s3Key] = s3Key
                it[AttachmentsTable.s3Bucket] = s3Bucket
            }.value

            logger.info("Created attachment metadata $attachmentId for $entityType $entityId")

            AttachmentsTable.selectAll()
                .where { AttachmentsTable.id eq attachmentId }
                .single()
                .toAttachment()
        }
    }

    override suspend fun getTotalStorageUsed(tenantId: TenantId): Long = dbQuery {
        AttachmentsTable
            .select(AttachmentsTable.sizeBytes.sum())
            .where { AttachmentsTable.tenantId eq tenantId.value.toJavaUuid() }
            .singleOrNull()
            ?.get(AttachmentsTable.sizeBytes.sum())
            ?: 0L
    }

    override suspend fun validateFile(mimeType: String, sizeBytes: Long, maxSizeBytes: Long): Boolean {
        if (!allowedMimeTypes.contains(mimeType)) {
            throw IllegalArgumentException("File type not allowed: $mimeType. Allowed types: ${allowedMimeTypes.joinToString()}")
        }

        if (sizeBytes > maxSizeBytes) {
            throw IllegalArgumentException("File too large: ${sizeBytes / 1_048_576}MB. Maximum: ${maxSizeBytes / 1_048_576}MB")
        }

        return true
    }

    override suspend fun getAllowedMimeTypes(): List<String> {
        return allowedMimeTypes.toList()
    }

    override suspend fun deleteAllForEntity(entityType: EntityType, entityId: Uuid): Int {
        val attachments = listByEntity(entityType, entityId)

        // Delete files from storage
        attachments.forEach { attachment ->
            try {
                fileStorage.delete(attachment.s3Key)
            } catch (e: Exception) {
                logger.warn("Failed to delete file ${attachment.s3Key}: ${e.message}")
            }
        }

        // Delete metadata
        val deletedCount = dbQuery {
            AttachmentsTable.deleteWhere {
                (AttachmentsTable.entityType eq entityType) and (AttachmentsTable.entityId eq entityId.toJavaUuid())
            }
        }

        logger.info("Deleted $deletedCount attachments for $entityType $entityId")

        return deletedCount
    }
}
