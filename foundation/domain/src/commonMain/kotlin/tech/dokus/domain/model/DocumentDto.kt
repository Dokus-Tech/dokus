package tech.dokus.domain.model

import tech.dokus.domain.enums.EntityType
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

/**
 * Document DTO - represents a document stored in object storage (MinIO).
 *
 * Documents are uploaded first, then optionally linked to an entity
 * (Invoice, Bill, Expense). The download URL is generated fresh on
 * each fetch to avoid storing expiring presigned URLs.
 */
@Serializable
data class DocumentDto(
    val id: DocumentId,
    val tenantId: TenantId,
    val filename: String,
    val contentType: String,
    val sizeBytes: Long,
    val storageKey: String,
    val entityType: EntityType? = null,
    val entityId: String? = null,
    val uploadedAt: LocalDateTime,
    val downloadUrl: String? = null
)
