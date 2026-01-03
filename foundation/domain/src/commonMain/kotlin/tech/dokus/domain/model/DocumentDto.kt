package tech.dokus.domain.model

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId

/**
 * Document DTO - represents a document stored in object storage (MinIO).
 *
 * Documents are pure file metadata. Entity linkage is handled by the
 * financial entity tables (Invoice, Bill, Expense) which have a documentId FK.
 * The download URL is generated fresh on each fetch to avoid storing
 * expiring presigned URLs.
 */
@Serializable
data class DocumentDto(
    val id: DocumentId,
    val tenantId: TenantId,
    val filename: String,
    val contentType: String,
    val sizeBytes: Long,
    val storageKey: String,
    val uploadedAt: LocalDateTime,
    val downloadUrl: String? = null
)
