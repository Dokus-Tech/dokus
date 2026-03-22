package tech.dokus.domain.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId

/**
 * Document DTO - represents a canonical document (a financial fact, not a file).
 *
 * `filename` is a display field resolved at runtime from the preferred source
 * via DocumentRecordLoader. It is NOT persisted on the canonical document.
 * The download URL is generated fresh on each fetch (presigned MinIO URL).
 */
@Serializable
data class DocumentDto(
    val id: DocumentId,
    val tenantId: TenantId,
    val filename: String = "",
    val uploadedAt: LocalDateTime,
    val sortDate: LocalDate,
    val downloadUrl: String? = null
) {
    companion object
}
