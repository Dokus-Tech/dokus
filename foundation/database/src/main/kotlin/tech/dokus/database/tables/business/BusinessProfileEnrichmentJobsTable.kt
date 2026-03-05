package tech.dokus.database.tables.business

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.datetime
import tech.dokus.database.tables.auth.TenantTable
import tech.dokus.domain.enums.BusinessProfileEnrichmentJobStatus
import tech.dokus.domain.enums.BusinessProfileSubjectType
import tech.dokus.foundation.backend.database.dbEnumeration

object BusinessProfileEnrichmentJobsTable : UUIDTable("business_profile_enrichment_jobs") {
    val tenantId = uuid("tenant_id").references(TenantTable.id, onDelete = ReferenceOption.CASCADE).index()
    val subjectType = dbEnumeration<BusinessProfileSubjectType>("subject_type")
    val subjectId = uuid("subject_id")

    val status = dbEnumeration<BusinessProfileEnrichmentJobStatus>("status").index()
    val triggerReason = varchar("trigger_reason", 64)
    val scheduledAt = datetime("scheduled_at")
    val nextAttemptAt = datetime("next_attempt_at")
    val attemptCount = integer("attempt_count").default(0)
    val lastError = text("last_error").nullable()
    val processingStartedAt = datetime("processing_started_at").nullable()

    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        uniqueIndex(tenantId, subjectType, subjectId)
        index(false, status, nextAttemptAt)
    }
}
