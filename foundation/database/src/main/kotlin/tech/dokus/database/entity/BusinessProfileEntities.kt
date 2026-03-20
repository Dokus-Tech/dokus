@file:OptIn(ExperimentalUuidApi::class)

package tech.dokus.database.entity

import kotlinx.datetime.LocalDateTime
import tech.dokus.domain.enums.BusinessProfileEnrichmentJobStatus
import tech.dokus.domain.enums.BusinessProfileSubjectType
import tech.dokus.domain.enums.BusinessProfileVerificationState
import tech.dokus.domain.ids.TenantId
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

data class BusinessProfileEntity(
    val tenantId: TenantId,
    val subjectType: BusinessProfileSubjectType,
    val subjectId: Uuid,
    val websiteUrl: String? = null,
    val businessSummary: String? = null,
    val businessActivitiesJson: String? = null,
    val verificationState: BusinessProfileVerificationState = BusinessProfileVerificationState.Unset,
    val evidenceScore: Int = 0,
    val evidenceChecksJson: String? = null,
    val logoStorageKey: String? = null,
    val websitePinned: Boolean = false,
    val summaryPinned: Boolean = false,
    val activitiesPinned: Boolean = false,
    val logoPinned: Boolean = false,
    val lastRunAt: LocalDateTime? = null,
    val lastErrorCode: String? = null,
    val lastErrorMessage: String? = null,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null,
) {
    companion object
}

data class BusinessProfileEnrichmentJobEntity(
    val id: Uuid,
    val tenantId: TenantId,
    val subjectType: BusinessProfileSubjectType,
    val subjectId: Uuid,
    val status: BusinessProfileEnrichmentJobStatus,
    val triggerReason: String,
    val scheduledAt: LocalDateTime,
    val nextAttemptAt: LocalDateTime,
    val attemptCount: Int,
    val lastError: String?,
    val processingStartedAt: LocalDateTime?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object
}
