package tech.dokus.database.mapper

import org.jetbrains.exposed.v1.core.ResultRow
import tech.dokus.database.repository.business.BusinessProfileEnrichmentJobEntity
import tech.dokus.database.repository.business.BusinessProfileEntity
import tech.dokus.database.tables.business.BusinessProfileEnrichmentJobsTable
import tech.dokus.database.tables.business.BusinessProfilesTable
import tech.dokus.domain.ids.TenantId
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toKotlinUuid

@OptIn(ExperimentalUuidApi::class)
internal fun BusinessProfileEntity.Companion.from(row: ResultRow, tenantId: TenantId): BusinessProfileEntity = BusinessProfileEntity(
    tenantId = tenantId,
    subjectType = row[BusinessProfilesTable.subjectType],
    subjectId = row[BusinessProfilesTable.subjectId].toKotlinUuid(),
    websiteUrl = row[BusinessProfilesTable.websiteUrl],
    businessSummary = row[BusinessProfilesTable.businessSummary],
    businessActivitiesJson = row[BusinessProfilesTable.businessActivitiesJson],
    verificationState = row[BusinessProfilesTable.verificationState],
    evidenceScore = row[BusinessProfilesTable.evidenceScore],
    evidenceChecksJson = row[BusinessProfilesTable.evidenceChecksJson],
    logoStorageKey = row[BusinessProfilesTable.logoStorageKey],
    websitePinned = row[BusinessProfilesTable.websitePinned],
    summaryPinned = row[BusinessProfilesTable.summaryPinned],
    activitiesPinned = row[BusinessProfilesTable.activitiesPinned],
    logoPinned = row[BusinessProfilesTable.logoPinned],
    lastRunAt = row[BusinessProfilesTable.lastRunAt],
    lastErrorCode = row[BusinessProfilesTable.lastErrorCode],
    lastErrorMessage = row[BusinessProfilesTable.lastErrorMessage],
    createdAt = row[BusinessProfilesTable.createdAt],
    updatedAt = row[BusinessProfilesTable.updatedAt],
)

@OptIn(ExperimentalUuidApi::class)
internal fun BusinessProfileEnrichmentJobEntity.Companion.from(row: ResultRow): BusinessProfileEnrichmentJobEntity = BusinessProfileEnrichmentJobEntity(
    id = row[BusinessProfileEnrichmentJobsTable.id].value.toKotlinUuid(),
    tenantId = TenantId(row[BusinessProfileEnrichmentJobsTable.tenantId].toKotlinUuid()),
    subjectType = row[BusinessProfileEnrichmentJobsTable.subjectType],
    subjectId = row[BusinessProfileEnrichmentJobsTable.subjectId].toKotlinUuid(),
    status = row[BusinessProfileEnrichmentJobsTable.status],
    triggerReason = row[BusinessProfileEnrichmentJobsTable.triggerReason],
    scheduledAt = row[BusinessProfileEnrichmentJobsTable.scheduledAt],
    nextAttemptAt = row[BusinessProfileEnrichmentJobsTable.nextAttemptAt],
    attemptCount = row[BusinessProfileEnrichmentJobsTable.attemptCount],
    lastError = row[BusinessProfileEnrichmentJobsTable.lastError],
    processingStartedAt = row[BusinessProfileEnrichmentJobsTable.processingStartedAt],
    createdAt = row[BusinessProfileEnrichmentJobsTable.createdAt],
    updatedAt = row[BusinessProfileEnrichmentJobsTable.updatedAt],
)
