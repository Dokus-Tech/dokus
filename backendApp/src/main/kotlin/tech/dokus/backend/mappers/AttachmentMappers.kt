package tech.dokus.backend.mappers

import tech.dokus.database.entity.DocumentSourceEntity
import tech.dokus.domain.enums.EntityType
import tech.dokus.domain.ids.AttachmentId
import tech.dokus.domain.model.AttachmentDto
import tech.dokus.domain.model.DocumentDto

fun AttachmentDto.Companion.from(document: DocumentDto, source: DocumentSourceEntity) = AttachmentDto(
    id = AttachmentId.parse(document.id.toString()),
    tenantId = document.tenantId,
    entityType = EntityType.Attachment,
    entityId = "",
    filename = source.filename ?: document.filename,
    mimeType = source.contentType,
    sizeBytes = source.sizeBytes,
    s3Key = source.storageKey,
    s3Bucket = "minio",
    uploadedAt = document.uploadedAt
)
