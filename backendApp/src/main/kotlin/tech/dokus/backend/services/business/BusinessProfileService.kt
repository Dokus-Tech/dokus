@file:OptIn(ExperimentalUuidApi::class)

package tech.dokus.backend.services.business

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import tech.dokus.database.repository.auth.TenantRepository
import tech.dokus.database.repository.business.BusinessProfileEnrichmentJobRepository
import tech.dokus.database.repository.business.BusinessProfileRecord
import tech.dokus.database.repository.business.BusinessProfileRepository
import tech.dokus.domain.enums.BusinessProfileSubjectType
import tech.dokus.domain.enums.BusinessProfileVerificationState
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.BusinessProfileUpdateResponse
import tech.dokus.domain.model.PinBusinessProfileFieldsRequest
import tech.dokus.domain.model.Tenant
import tech.dokus.domain.model.UpdateBusinessProfileRequest
import tech.dokus.domain.model.common.Thumbnail
import tech.dokus.domain.model.contact.ContactDto
import tech.dokus.domain.utils.json
import tech.dokus.foundation.backend.utils.loggerFor
import tech.dokus.foundation.backend.utils.runSuspendCatching
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

data class BusinessProfileProjection(
    val websiteUrl: String? = null,
    val businessSummary: String? = null,
    val businessActivities: List<String> = emptyList(),
    val verified: Boolean = false,
    val avatar: Thumbnail? = null,
    val logoStorageKey: String? = null,
    val websitePinned: Boolean = false,
    val summaryPinned: Boolean = false,
    val activitiesPinned: Boolean = false,
    val logoPinned: Boolean = false,
)

class BusinessProfileService(
    private val profileRepository: BusinessProfileRepository,
    private val jobRepository: BusinessProfileEnrichmentJobRepository,
    private val tenantRepository: TenantRepository,
) {
    private val logger = loggerFor()

    companion object {
        private const val AVATAR_SIZE_SMALL = "small"
        private const val AVATAR_SIZE_MEDIUM = "medium"
        private const val AVATAR_SIZE_LARGE = "large"
    }

    suspend fun enqueueTenant(tenantId: TenantId, triggerReason: String): Result<Unit> {
        return jobRepository.enqueueOrReset(
            tenantId = tenantId,
            subjectType = BusinessProfileSubjectType.Tenant,
            subjectId = tenantId.value,
            triggerReason = triggerReason
        )
    }

    suspend fun enqueueContact(tenantId: TenantId, contactId: ContactId, triggerReason: String): Result<Unit> {
        return jobRepository.enqueueOrReset(
            tenantId = tenantId,
            subjectType = BusinessProfileSubjectType.Contact,
            subjectId = contactId.value,
            triggerReason = triggerReason
        )
    }

    suspend fun projectTenant(tenant: Tenant): Tenant {
        val profile = getProjection(
            tenantId = tenant.id,
            subjectType = BusinessProfileSubjectType.Tenant,
            subjectId = tenant.id.value
        )
        return tenant.copy(
            websiteUrl = profile?.websiteUrl ?: tenant.websiteUrl,
            businessSummary = profile?.businessSummary,
            businessActivities = profile?.businessActivities ?: emptyList(),
            businessProfileVerified = profile?.verified ?: false
        )
    }

    suspend fun projectContacts(tenantId: TenantId, contacts: List<ContactDto>): List<ContactDto> {
        if (contacts.isEmpty()) return contacts

        val byId = profileRepository.getBySubjects(
            tenantId = tenantId,
            subjectType = BusinessProfileSubjectType.Contact,
            subjectIds = contacts.map { it.id.value }
        )
        return contacts.map { contact ->
            val profile = byId[contact.id.value]
            val activities = profile?.businessActivitiesJson
                ?.let { decodeActivities(it) }
                .orEmpty()
            val avatar = when {
                profile == null -> null
                profile.logoStorageKey.isNullOrBlank() -> {
                    logger.debug(
                        "No avatar in contact projection for tenant={}, contact={}, reason=no_logo_key",
                        tenantId,
                        contact.id
                    )
                    null
                }
                else -> buildContactAvatarThumbnail(contact.id.value)
            }
            contact.copy(
                websiteUrl = profile?.websiteUrl,
                businessSummary = profile?.businessSummary,
                businessActivities = activities,
                businessProfileVerified = profile?.verificationState == BusinessProfileVerificationState.Verified,
                avatar = avatar
            )
        }
    }

    suspend fun getLogoStorageKey(
        tenantId: TenantId,
        subjectType: BusinessProfileSubjectType,
        subjectId: Uuid
    ): String? {
        return profileRepository.getBySubject(tenantId, subjectType, subjectId)?.logoStorageKey
    }

    fun buildContactAvatarThumbnail(contactId: Uuid): Thumbnail = Thumbnail(
        small = "/api/v1/contacts/$contactId/avatar/$AVATAR_SIZE_SMALL.webp",
        medium = "/api/v1/contacts/$contactId/avatar/$AVATAR_SIZE_MEDIUM.webp",
        large = "/api/v1/contacts/$contactId/avatar/$AVATAR_SIZE_LARGE.webp"
    )

    fun buildTenantAvatarThumbnail(): Thumbnail = Thumbnail(
        small = "/api/v1/tenants/avatar/$AVATAR_SIZE_SMALL.webp",
        medium = "/api/v1/tenants/avatar/$AVATAR_SIZE_MEDIUM.webp",
        large = "/api/v1/tenants/avatar/$AVATAR_SIZE_LARGE.webp"
    )

    suspend fun updateTenantProfile(
        tenantId: TenantId,
        request: UpdateBusinessProfileRequest
    ): BusinessProfileUpdateResponse {
        val response = updateProfile(tenantId, BusinessProfileSubjectType.Tenant, tenantId.value, request)
        if (!request.websiteUrl.isNullOrBlank()) {
            tenantRepository.updateWebsiteUrl(tenantId, request.websiteUrl)
        }
        return response
    }

    suspend fun updateContactProfile(
        tenantId: TenantId,
        contactId: ContactId,
        request: UpdateBusinessProfileRequest
    ): BusinessProfileUpdateResponse =
        updateProfile(tenantId, BusinessProfileSubjectType.Contact, contactId.value, request)

    private suspend fun updateProfile(
        tenantId: TenantId,
        subjectType: BusinessProfileSubjectType,
        subjectId: Uuid,
        request: UpdateBusinessProfileRequest,
    ): BusinessProfileUpdateResponse {
        val existing = profileRepository.getBySubject(tenantId, subjectType, subjectId)
            ?: defaultRecord(tenantId, subjectType, subjectId)
        val activities = request.businessActivities?.let { normalizeActivities(it) }

        val updated = existing.copy(
            websiteUrl = request.websiteUrl ?: existing.websiteUrl,
            businessSummary = request.businessSummary ?: existing.businessSummary,
            businessActivitiesJson = activities?.let { encodeActivities(it) } ?: existing.businessActivitiesJson,
            verificationState = BusinessProfileVerificationState.Verified,
            websitePinned = request.websiteUrl?.let { true } ?: existing.websitePinned,
            summaryPinned = request.businessSummary?.let { true } ?: existing.summaryPinned,
            activitiesPinned = request.businessActivities?.let { true } ?: existing.activitiesPinned,
            lastRunAt = Clock.System.now().toLocalDateTime(TimeZone.UTC),
            lastErrorCode = null,
            lastErrorMessage = null,
        )

        profileRepository.upsert(updated)
        return updated.toResponse()
    }

    suspend fun updateTenantPins(
        tenantId: TenantId,
        request: PinBusinessProfileFieldsRequest
    ): BusinessProfileUpdateResponse {
        return updatePins(
            tenantId = tenantId,
            subjectType = BusinessProfileSubjectType.Tenant,
            subjectId = tenantId.value,
            request = request
        )
    }

    suspend fun updateContactPins(
        tenantId: TenantId,
        contactId: ContactId,
        request: PinBusinessProfileFieldsRequest
    ): BusinessProfileUpdateResponse {
        return updatePins(
            tenantId = tenantId,
            subjectType = BusinessProfileSubjectType.Contact,
            subjectId = contactId.value,
            request = request
        )
    }

    suspend fun setLogoPin(
        tenantId: TenantId,
        subjectType: BusinessProfileSubjectType,
        subjectId: Uuid,
        pinned: Boolean
    ) {
        val existing = profileRepository.getBySubject(tenantId, subjectType, subjectId)
            ?: defaultRecord(tenantId, subjectType, subjectId)
        profileRepository.upsert(existing.copy(logoPinned = pinned))
    }

    suspend fun markTenantAvatarUploaded(tenantId: TenantId, storageKey: String) {
        val subjectType = BusinessProfileSubjectType.Tenant
        val subjectId = tenantId.value
        val existing = profileRepository.getBySubject(tenantId, subjectType, subjectId)
            ?: defaultRecord(tenantId, subjectType, subjectId)
        profileRepository.upsert(
            existing.copy(
                logoStorageKey = storageKey,
                logoPinned = true
            )
        )
    }

    suspend fun markTenantAvatarDeleted(tenantId: TenantId) {
        val subjectType = BusinessProfileSubjectType.Tenant
        val subjectId = tenantId.value
        val existing = profileRepository.getBySubject(tenantId, subjectType, subjectId)
            ?: defaultRecord(tenantId, subjectType, subjectId)
        profileRepository.upsert(
            existing.copy(
                logoStorageKey = null,
                logoPinned = false
            )
        )
    }

    suspend fun applyEnrichment(
        tenantId: TenantId,
        subjectType: BusinessProfileSubjectType,
        subjectId: Uuid,
        verificationState: BusinessProfileVerificationState,
        evidenceScore: Int,
        evidenceChecksJson: String?,
        websiteUrl: String?,
        businessSummary: String?,
        businessActivities: List<String>?,
        logoStorageKey: String?,
        lastErrorCode: String?,
        lastErrorMessage: String?,
    ): BusinessProfileRecord {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        val existing = profileRepository.getBySubject(tenantId, subjectType, subjectId)
            ?: defaultRecord(tenantId, subjectType, subjectId)
        val normalizedActivities = businessActivities?.let { normalizeActivities(it) }
        val skip = verificationState == BusinessProfileVerificationState.Unset && lastErrorCode == "LOW_EVIDENCE"

        val mergedWebsite = when {
            existing.websitePinned -> existing.websiteUrl
            skip -> existing.websiteUrl
            else -> websiteUrl
        }
        val mergedSummary = when {
            existing.summaryPinned -> existing.businessSummary
            skip -> existing.businessSummary
            else -> businessSummary
        }
        val mergedActivitiesJson = when {
            existing.activitiesPinned -> existing.businessActivitiesJson
            skip -> existing.businessActivitiesJson
            else -> normalizedActivities?.let { encodeActivities(it) }
        }
        val mergedLogoStorageKey: String?
        val logoReason: String?
        when {
            existing.logoPinned -> {
                mergedLogoStorageKey = existing.logoStorageKey
                logoReason = if (subjectType == BusinessProfileSubjectType.Tenant) "tenant_logo_pinned" else "logo_pinned"
            }
            !existing.logoStorageKey.isNullOrBlank() -> {
                mergedLogoStorageKey = existing.logoStorageKey
                logoReason = "existing_logo_preserved"
            }
            skip -> {
                mergedLogoStorageKey = existing.logoStorageKey
                logoReason = "no_logo_key"
            }
            else -> {
                mergedLogoStorageKey = logoStorageKey
                logoReason = if (logoStorageKey == null) "no_logo_key" else null
            }
        }
        if (logoReason != null) {
            logger.debug(
                "Skipped logo update for tenant={}, subjectType={}, subjectId={}, reason={}",
                tenantId,
                subjectType,
                subjectId,
                logoReason
            )
        }

        val updated = existing.copy(
            websiteUrl = mergedWebsite,
            businessSummary = mergedSummary,
            businessActivitiesJson = mergedActivitiesJson,
            verificationState = if (skip) existing.verificationState else verificationState,
            evidenceScore = if (skip) existing.evidenceScore else evidenceScore,
            evidenceChecksJson = if (skip) existing.evidenceChecksJson else evidenceChecksJson,
            logoStorageKey = mergedLogoStorageKey,
            lastRunAt = now,
            lastErrorCode = lastErrorCode,
            lastErrorMessage = lastErrorMessage,
        )
        profileRepository.upsert(updated)

        if (subjectType == BusinessProfileSubjectType.Tenant &&
            verificationState == BusinessProfileVerificationState.Verified &&
            !existing.websitePinned &&
            !mergedWebsite.isNullOrBlank()
        ) {
            runSuspendCatching { tenantRepository.updateWebsiteUrl(tenantId, mergedWebsite) }
                .onFailure { logger.error("Failed to update canonical tenant website for {}", tenantId, it) }
        }

        return updated
    }

    private suspend fun getProjection(
        tenantId: TenantId,
        subjectType: BusinessProfileSubjectType,
        subjectId: Uuid,
    ): BusinessProfileProjection? {
        val profile = profileRepository.getBySubject(
            tenantId = tenantId,
            subjectType = subjectType,
            subjectId = subjectId
        ) ?: return null
        val avatar = if (profile.logoStorageKey.isNullOrBlank()) {
            logger.debug(
                "No avatar in business profile projection for tenant={}, subjectType={}, subjectId={}, reason=no_logo_key",
                tenantId,
                subjectType,
                subjectId
            )
            null
        } else {
            when (subjectType) {
                BusinessProfileSubjectType.Contact -> buildContactAvatarThumbnail(subjectId)
                BusinessProfileSubjectType.Tenant -> buildTenantAvatarThumbnail()
            }
        }
        return BusinessProfileProjection(
            websiteUrl = profile.websiteUrl,
            businessSummary = profile.businessSummary,
            businessActivities = profile.businessActivitiesJson?.let(::decodeActivities).orEmpty(),
            verified = profile.verificationState == BusinessProfileVerificationState.Verified,
            avatar = avatar,
            logoStorageKey = profile.logoStorageKey,
            websitePinned = profile.websitePinned,
            summaryPinned = profile.summaryPinned,
            activitiesPinned = profile.activitiesPinned,
            logoPinned = profile.logoPinned,
        )
    }

    private suspend fun updatePins(
        tenantId: TenantId,
        subjectType: BusinessProfileSubjectType,
        subjectId: Uuid,
        request: PinBusinessProfileFieldsRequest,
    ): BusinessProfileUpdateResponse {
        val existing = profileRepository.getBySubject(tenantId, subjectType, subjectId)
            ?: defaultRecord(tenantId, subjectType, subjectId)
        val updated = existing.copy(
            websitePinned = request.websitePinned ?: existing.websitePinned,
            summaryPinned = request.summaryPinned ?: existing.summaryPinned,
            activitiesPinned = request.activitiesPinned ?: existing.activitiesPinned,
            logoPinned = request.logoPinned ?: existing.logoPinned,
        )
        profileRepository.upsert(updated)
        return updated.toResponse()
    }

    private fun defaultRecord(
        tenantId: TenantId,
        subjectType: BusinessProfileSubjectType,
        subjectId: Uuid,
    ): BusinessProfileRecord = BusinessProfileRecord(
        tenantId = tenantId,
        subjectType = subjectType,
        subjectId = subjectId
    )

    private fun BusinessProfileRecord.toResponse(): BusinessProfileUpdateResponse = BusinessProfileUpdateResponse(
        subjectType = subjectType,
        subjectId = subjectId.toString(),
        websitePinned = websitePinned,
        summaryPinned = summaryPinned,
        activitiesPinned = activitiesPinned,
        logoPinned = logoPinned,
    )

    private fun normalizeActivities(values: List<String>): List<String> {
        return values
            .asSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .map { if (it.length > 80) it.take(80) else it }
            .distinct()
            .take(8)
            .toList()
    }

    private fun encodeActivities(values: List<String>): String = json.encodeToString(values)

    private fun decodeActivities(serialized: String): List<String> = runCatching {
        json.decodeFromString<List<String>>(serialized)
    }.getOrDefault(emptyList())
}
