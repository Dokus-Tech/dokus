package tech.dokus.backend.services.documents.confirmation

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import tech.dokus.database.tables.documents.DocumentDraftsTable
import tech.dokus.domain.enums.DocumentStatus
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import java.util.UUID

/**
 * Shared draft validation and status update functions used by all confirmation services.
 * These run inside an existing Exposed transaction context (caller wraps in `dbQuery {}`).
 */
internal fun ensureDraftConfirmable(tenantId: TenantId, documentId: DocumentId) {
    val draft = DocumentDraftsTable.selectAll()
        .where {
            (DocumentDraftsTable.documentId eq UUID.fromString(documentId.toString())) and
                (DocumentDraftsTable.tenantId eq UUID.fromString(tenantId.toString()))
        }
        .singleOrNull() ?: throw DokusException.NotFound("Draft not found for document")

    val status = draft[DocumentDraftsTable.documentStatus]
    if (status != DocumentStatus.NeedsReview && status != DocumentStatus.Confirmed) {
        throw DokusException.BadRequest("Draft is not ready for confirmation: $status")
    }
}

internal fun markDraftConfirmed(tenantId: TenantId, documentId: DocumentId) {
    val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
    val updated = DocumentDraftsTable.update({
        (DocumentDraftsTable.documentId eq UUID.fromString(documentId.toString())) and
            (DocumentDraftsTable.tenantId eq UUID.fromString(tenantId.toString()))
    }) {
        it[documentStatus] = DocumentStatus.Confirmed
        it[rejectReason] = null
        it[updatedAt] = now
    }
    if (updated == 0) {
        throw DokusException.InternalError("Failed to update draft status to Confirmed")
    }
}
