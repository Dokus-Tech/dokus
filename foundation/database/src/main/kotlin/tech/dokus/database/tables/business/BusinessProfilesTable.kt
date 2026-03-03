package tech.dokus.database.tables.business

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.datetime
import tech.dokus.database.tables.auth.TenantTable
import tech.dokus.domain.enums.BusinessProfileSubjectType
import tech.dokus.domain.enums.BusinessProfileVerificationState
import tech.dokus.foundation.backend.database.dbEnumeration

/**
 * Enriched business profile per subject (tenant or contact).
 */
object BusinessProfilesTable : UUIDTable("business_profiles") {
    val tenantId = uuid("tenant_id").references(TenantTable.id, onDelete = ReferenceOption.CASCADE).index()
    val subjectType = dbEnumeration<BusinessProfileSubjectType>("subject_type")
    val subjectId = uuid("subject_id")

    val websiteUrl = varchar("website_url", 500).nullable()
    val businessSummary = text("business_summary").nullable()
    val businessActivitiesJson = text("business_activities_json").nullable()

    val verificationState = dbEnumeration<BusinessProfileVerificationState>("verification_state")
        .default(BusinessProfileVerificationState.Unset)
    val evidenceScore = integer("evidence_score").default(0)
    val evidenceChecksJson = text("evidence_checks_json").nullable()

    val logoStorageKey = varchar("logo_storage_key", 500).nullable()

    val websitePinned = bool("website_pinned").default(false)
    val summaryPinned = bool("summary_pinned").default(false)
    val activitiesPinned = bool("activities_pinned").default(false)
    val logoPinned = bool("logo_pinned").default(false)

    val lastRunAt = datetime("last_run_at").nullable()
    val lastErrorCode = varchar("last_error_code", 80).nullable()
    val lastErrorMessage = text("last_error_message").nullable()

    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        uniqueIndex(tenantId, subjectType, subjectId)
        index(false, tenantId, subjectType)
    }
}
