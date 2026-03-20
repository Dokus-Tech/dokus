package tech.dokus.database.mapper

import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.ResultRow
import tech.dokus.database.repository.peppol.PeppolTransmissionInternal
import tech.dokus.database.tables.peppol.PeppolDirectoryCacheTable
import tech.dokus.database.tables.peppol.PeppolRegistrationTable
import tech.dokus.database.tables.peppol.PeppolSettingsTable
import tech.dokus.database.tables.peppol.PeppolTransmissionsTable
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.InvoiceId
import tech.dokus.domain.ids.PeppolId
import tech.dokus.domain.ids.PeppolRegistrationId
import tech.dokus.domain.ids.PeppolSettingsId
import tech.dokus.domain.ids.PeppolTransmissionId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.PeppolRegistrationDto
import tech.dokus.domain.model.PeppolResolution
import tech.dokus.domain.model.PeppolSettingsDto
import tech.dokus.domain.model.PeppolTransmissionDto

internal fun ResultRow.toPeppolTransmissionDto(): PeppolTransmissionDto = PeppolTransmissionDto(
    id = PeppolTransmissionId.parse(this[PeppolTransmissionsTable.id].value.toString()),
    tenantId = TenantId.parse(this[PeppolTransmissionsTable.tenantId].toString()),
    direction = this[PeppolTransmissionsTable.direction],
    documentType = this[PeppolTransmissionsTable.documentType],
    status = this[PeppolTransmissionsTable.status],
    invoiceId = this[PeppolTransmissionsTable.invoiceId]?.let { InvoiceId.parse(it.toString()) },
    externalDocumentId = this[PeppolTransmissionsTable.externalDocumentId],
    recipientPeppolId = this[PeppolTransmissionsTable.recipientPeppolId]?.let { PeppolId(it) },
    senderPeppolId = this[PeppolTransmissionsTable.senderPeppolId]?.let { PeppolId(it) },
    errorMessage = this[PeppolTransmissionsTable.errorMessage],
    attemptCount = this[PeppolTransmissionsTable.attemptCount],
    nextRetryAt = this[PeppolTransmissionsTable.nextRetryAt],
    lastAttemptAt = this[PeppolTransmissionsTable.lastAttemptAt],
    providerErrorCode = this[PeppolTransmissionsTable.providerErrorCode],
    providerErrorMessage = this[PeppolTransmissionsTable.providerErrorMessage],
    transmittedAt = this[PeppolTransmissionsTable.transmittedAt],
    createdAt = this[PeppolTransmissionsTable.createdAt],
    updatedAt = this[PeppolTransmissionsTable.updatedAt]
)

internal fun ResultRow.toPeppolTransmissionInternal(): PeppolTransmissionInternal = PeppolTransmissionInternal(
    id = PeppolTransmissionId.parse(this[PeppolTransmissionsTable.id].value.toString()),
    tenantId = TenantId.parse(this[PeppolTransmissionsTable.tenantId].toString()),
    direction = this[PeppolTransmissionsTable.direction],
    documentType = this[PeppolTransmissionsTable.documentType],
    status = this[PeppolTransmissionsTable.status],
    invoiceId = this[PeppolTransmissionsTable.invoiceId]?.let { InvoiceId.parse(it.toString()) },
    externalDocumentId = this[PeppolTransmissionsTable.externalDocumentId],
    idempotencyKey = this[PeppolTransmissionsTable.idempotencyKey],
    recipientPeppolId = this[PeppolTransmissionsTable.recipientPeppolId]?.let { PeppolId(it) },
    senderPeppolId = this[PeppolTransmissionsTable.senderPeppolId]?.let { PeppolId(it) },
    errorMessage = this[PeppolTransmissionsTable.errorMessage],
    providerErrorCode = this[PeppolTransmissionsTable.providerErrorCode],
    providerErrorMessage = this[PeppolTransmissionsTable.providerErrorMessage],
    attemptCount = this[PeppolTransmissionsTable.attemptCount],
    nextRetryAt = this[PeppolTransmissionsTable.nextRetryAt],
    lastAttemptAt = this[PeppolTransmissionsTable.lastAttemptAt],
    rawRequest = this[PeppolTransmissionsTable.rawRequest],
    rawResponse = this[PeppolTransmissionsTable.rawResponse],
    rawUblXmlKey = this[PeppolTransmissionsTable.rawUblXmlKey],
    transmittedAt = this[PeppolTransmissionsTable.transmittedAt],
    createdAt = this[PeppolTransmissionsTable.createdAt],
    updatedAt = this[PeppolTransmissionsTable.updatedAt]
)

internal fun ResultRow.toPeppolRegistrationDto(): PeppolRegistrationDto = PeppolRegistrationDto(
    id = PeppolRegistrationId.parse(this[PeppolRegistrationTable.id].value.toString()),
    tenantId = TenantId.parse(this[PeppolRegistrationTable.tenantId].toString()),
    peppolId = this[PeppolRegistrationTable.peppolId],
    recommandCompanyId = this[PeppolRegistrationTable.recommandCompanyId],
    status = this[PeppolRegistrationTable.status],
    canReceive = this[PeppolRegistrationTable.canReceive],
    canSend = this[PeppolRegistrationTable.canSend],
    testMode = this[PeppolRegistrationTable.testMode],
    waitingSince = this[PeppolRegistrationTable.waitingSince],
    lastPolledAt = this[PeppolRegistrationTable.lastPolledAt],
    errorMessage = this[PeppolRegistrationTable.errorMessage],
    createdAt = this[PeppolRegistrationTable.createdAt],
    updatedAt = this[PeppolRegistrationTable.updatedAt]
)

internal fun ResultRow.toPeppolSettingsDto(): PeppolSettingsDto = PeppolSettingsDto(
    id = PeppolSettingsId.parse(this[PeppolSettingsTable.id].value.toString()),
    tenantId = TenantId.parse(this[PeppolSettingsTable.tenantId].toString()),
    companyId = this[PeppolSettingsTable.companyId],
    peppolId = PeppolId(this[PeppolSettingsTable.peppolId]),
    isEnabled = this[PeppolSettingsTable.isEnabled],
    testMode = this[PeppolSettingsTable.testMode],
    webhookToken = this[PeppolSettingsTable.webhookToken],
    lastFullSyncAt = this[PeppolSettingsTable.lastFullSyncAt],
    createdAt = this[PeppolSettingsTable.createdAt],
    updatedAt = this[PeppolSettingsTable.updatedAt]
)

internal fun ResultRow.toPeppolResolution(): PeppolResolution {
    val docTypesJson = this[PeppolDirectoryCacheTable.supportedDocTypes]
    val supportedDocTypes = if (docTypesJson.isNullOrBlank()) {
        emptyList()
    } else {
        runCatching { Json.decodeFromString<List<String>>(docTypesJson) }.getOrElse { emptyList() }
    }

    return PeppolResolution(
        contactId = ContactId.parse(this[PeppolDirectoryCacheTable.contactId].toString()),
        status = this[PeppolDirectoryCacheTable.status],
        participantId = this[PeppolDirectoryCacheTable.participantId],
        scheme = this[PeppolDirectoryCacheTable.scheme],
        supportedDocTypes = supportedDocTypes,
        source = this[PeppolDirectoryCacheTable.lookupSource],
        vatNumberSnapshot = this[PeppolDirectoryCacheTable.vatNumberSnapshot],
        companyNumberSnapshot = this[PeppolDirectoryCacheTable.companyNumberSnapshot],
        lastCheckedAt = this[PeppolDirectoryCacheTable.lastCheckedAt],
        expiresAt = this[PeppolDirectoryCacheTable.expiresAt],
        errorMessage = this[PeppolDirectoryCacheTable.errorMessage]
    )
}
