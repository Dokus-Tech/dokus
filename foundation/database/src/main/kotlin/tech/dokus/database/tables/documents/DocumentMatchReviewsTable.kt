package tech.dokus.database.tables.documents

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.datetime
import tech.dokus.database.tables.auth.TenantTable
import tech.dokus.database.tables.auth.UsersTable
import tech.dokus.domain.enums.DocumentMatchReviewReasonType
import tech.dokus.domain.enums.DocumentMatchReviewStatus
import tech.dokus.foundation.backend.database.dbEnumeration

/**
 * Manual match-review workflow items for non-silent document/source matches.
 */
object DocumentMatchReviewsTable : UUIDTable("document_match_reviews") {
    val tenantId = uuid("tenant_id").references(
        TenantTable.id,
        onDelete = ReferenceOption.CASCADE
    )

    val documentId = uuid("document_id").references(
        DocumentsTable.id,
        onDelete = ReferenceOption.CASCADE
    )

    val incomingSourceId = uuid("incoming_source_id").references(
        DocumentSourcesTable.id,
        onDelete = ReferenceOption.CASCADE
    )

    val reasonType = dbEnumeration<DocumentMatchReviewReasonType>("reason_type")
    val aiSummary = text("ai_summary").nullable()
    val aiConfidence = decimal("ai_confidence", 5, 4).nullable()
    val status = dbEnumeration<DocumentMatchReviewStatus>("status").default(DocumentMatchReviewStatus.Pending)

    val resolvedBy = uuid("resolved_by").references(
        UsersTable.id,
        onDelete = ReferenceOption.SET_NULL
    ).nullable()
    val resolvedAt = datetime("resolved_at").nullable()

    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        index(false, tenantId, documentId, status)
        index(false, tenantId, incomingSourceId, status)
    }
}
