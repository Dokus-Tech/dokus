package tech.dokus.database.mapper

import org.jetbrains.exposed.v1.core.ResultRow
import tech.dokus.database.repository.business.BusinessProfileEnrichmentJob
import tech.dokus.database.repository.business.BusinessProfileRecord
import tech.dokus.database.tables.business.BusinessProfileEnrichmentJobsTable
import tech.dokus.database.tables.business.BusinessProfilesTable
import tech.dokus.domain.ids.TenantId
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toKotlinUuid

@OptIn(ExperimentalUuidApi::class)
internal fun ResultRow.toBusinessProfileRecord(tenantId: TenantId): BusinessProfileRecord = BusinessProfileRecord(
    tenantId = tenantId,
    subjectType = this[BusinessProfilesTable.subjectType],
    subjectId = this[BusinessProfilesTable.subjectId].toKotlinUuid(),
    websiteUrl = this[BusinessProfilesTable.websiteUrl],
    businessSummary = this[BusinessProfilesTable.businessSummary],
    businessActivitiesJson = this[BusinessProfilesTable.businessActivitiesJson],
    verificationState = this[BusinessProfilesTable.verificationState],
    evidenceScore = this[BusinessProfilesTable.evidenceScore],
    evidenceChecksJson = this[BusinessProfilesTable.evidenceChecksJson],
    logoStorageKey = this[BusinessProfilesTable.logoStorageKey],
    websitePinned = this[BusinessProfilesTable.websitePinned],
    summaryPinned = this[BusinessProfilesTable.summaryPinned],
    activitiesPinned = this[BusinessProfilesTable.activitiesPinned],
    logoPinned = this[BusinessProfilesTable.logoPinned],
    lastRunAt = this[BusinessProfilesTable.lastRunAt],
    lastErrorCode = this[BusinessProfilesTable.lastErrorCode],
    lastErrorMessage = this[BusinessProfilesTable.lastErrorMessage],
    createdAt = this[BusinessProfilesTable.createdAt],
    updatedAt = this[BusinessProfilesTable.updatedAt],
)

@OptIn(ExperimentalUuidApi::class)
internal fun ResultRow.toBusinessProfileEnrichmentJob(): BusinessProfileEnrichmentJob = BusinessProfileEnrichmentJob(
    id = this[BusinessProfileEnrichmentJobsTable.id].value.toKotlinUuid(),
    tenantId = TenantId(this[BusinessProfileEnrichmentJobsTable.tenantId].toKotlinUuid()),
    subjectType = this[BusinessProfileEnrichmentJobsTable.subjectType],
    subjectId = this[BusinessProfileEnrichmentJobsTable.subjectId].toKotlinUuid(),
    status = this[BusinessProfileEnrichmentJobsTable.status],
    triggerReason = this[BusinessProfileEnrichmentJobsTable.triggerReason],
    scheduledAt = this[BusinessProfileEnrichmentJobsTable.scheduledAt],
    nextAttemptAt = this[BusinessProfileEnrichmentJobsTable.nextAttemptAt],
    attemptCount = this[BusinessProfileEnrichmentJobsTable.attemptCount],
    lastError = this[BusinessProfileEnrichmentJobsTable.lastError],
    processingStartedAt = this[BusinessProfileEnrichmentJobsTable.processingStartedAt],
    createdAt = this[BusinessProfileEnrichmentJobsTable.createdAt],
    updatedAt = this[BusinessProfileEnrichmentJobsTable.updatedAt],
)
