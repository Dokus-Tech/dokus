package tech.dokus.database.entity

import kotlinx.datetime.LocalDateTime
import tech.dokus.domain.enums.PeppolLookupSource
import tech.dokus.domain.enums.PeppolLookupStatus
import tech.dokus.domain.enums.PeppolRegistrationStatus
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.PeppolId
import tech.dokus.domain.ids.PeppolRegistrationId
import tech.dokus.domain.ids.PeppolSettingsId
import tech.dokus.domain.ids.TenantId

data class PeppolRegistrationEntity(
    val id: PeppolRegistrationId,
    val tenantId: TenantId,
    val peppolId: String,
    val recommandCompanyId: String?,
    val status: PeppolRegistrationStatus,
    val canReceive: Boolean,
    val canSend: Boolean,
    val testMode: Boolean,
    val waitingSince: LocalDateTime?,
    val lastPolledAt: LocalDateTime?,
    val errorMessage: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object
}

/**
 * Database entity for PEPPOL directory cache resolution.
 */
data class PeppolResolutionEntity(
    val contactId: ContactId,
    val status: PeppolLookupStatus,
    val participantId: String? = null,
    val scheme: String? = null,
    val supportedDocTypes: List<String> = emptyList(),
    val source: PeppolLookupSource,
    val vatNumberSnapshot: String? = null,
    val companyNumberSnapshot: String? = null,
    val lastCheckedAt: LocalDateTime,
    val expiresAt: LocalDateTime? = null,
    val errorMessage: String? = null,
) {
    companion object
}

data class PeppolSettingsEntity(
    val id: PeppolSettingsId,
    val tenantId: TenantId,
    val companyId: String,
    val peppolId: PeppolId,
    val isEnabled: Boolean = false,
    val testMode: Boolean = true,
    val webhookToken: String? = null,
    val lastFullSyncAt: LocalDateTime? = null,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object
}
