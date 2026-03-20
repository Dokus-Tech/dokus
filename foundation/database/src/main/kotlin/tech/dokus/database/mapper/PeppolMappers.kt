package tech.dokus.database.mapper

import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.ResultRow
import tech.dokus.database.entity.PeppolRegistrationEntity
import tech.dokus.database.entity.PeppolSettingsEntity
import tech.dokus.database.repository.peppol.PeppolTransmissionEntity
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
import tech.dokus.database.entity.PeppolResolutionEntity
import tech.dokus.domain.model.PeppolResolution
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

internal fun PeppolTransmissionEntity.Companion.from(row: ResultRow): PeppolTransmissionEntity = PeppolTransmissionEntity(
    id = PeppolTransmissionId.parse(row[PeppolTransmissionsTable.id].value.toString()),
    tenantId = TenantId.parse(row[PeppolTransmissionsTable.tenantId].toString()),
    direction = row[PeppolTransmissionsTable.direction],
    documentType = row[PeppolTransmissionsTable.documentType],
    status = row[PeppolTransmissionsTable.status],
    invoiceId = row[PeppolTransmissionsTable.invoiceId]?.let { InvoiceId.parse(it.toString()) },
    externalDocumentId = row[PeppolTransmissionsTable.externalDocumentId],
    idempotencyKey = row[PeppolTransmissionsTable.idempotencyKey],
    recipientPeppolId = row[PeppolTransmissionsTable.recipientPeppolId]?.let { PeppolId(it) },
    senderPeppolId = row[PeppolTransmissionsTable.senderPeppolId]?.let { PeppolId(it) },
    errorMessage = row[PeppolTransmissionsTable.errorMessage],
    providerErrorCode = row[PeppolTransmissionsTable.providerErrorCode],
    providerErrorMessage = row[PeppolTransmissionsTable.providerErrorMessage],
    attemptCount = row[PeppolTransmissionsTable.attemptCount],
    nextRetryAt = row[PeppolTransmissionsTable.nextRetryAt],
    lastAttemptAt = row[PeppolTransmissionsTable.lastAttemptAt],
    rawRequest = row[PeppolTransmissionsTable.rawRequest],
    rawResponse = row[PeppolTransmissionsTable.rawResponse],
    rawUblXmlKey = row[PeppolTransmissionsTable.rawUblXmlKey],
    transmittedAt = row[PeppolTransmissionsTable.transmittedAt],
    createdAt = row[PeppolTransmissionsTable.createdAt],
    updatedAt = row[PeppolTransmissionsTable.updatedAt]
)

fun PeppolRegistrationEntity.Companion.from(row: ResultRow): PeppolRegistrationEntity = PeppolRegistrationEntity(
    id = PeppolRegistrationId.parse(row[PeppolRegistrationTable.id].value.toString()),
    tenantId = TenantId.parse(row[PeppolRegistrationTable.tenantId].toString()),
    peppolId = row[PeppolRegistrationTable.peppolId],
    recommandCompanyId = row[PeppolRegistrationTable.recommandCompanyId],
    status = row[PeppolRegistrationTable.status],
    canReceive = row[PeppolRegistrationTable.canReceive],
    canSend = row[PeppolRegistrationTable.canSend],
    testMode = row[PeppolRegistrationTable.testMode],
    waitingSince = row[PeppolRegistrationTable.waitingSince],
    lastPolledAt = row[PeppolRegistrationTable.lastPolledAt],
    errorMessage = row[PeppolRegistrationTable.errorMessage],
    createdAt = row[PeppolRegistrationTable.createdAt],
    updatedAt = row[PeppolRegistrationTable.updatedAt],
)

fun PeppolSettingsEntity.Companion.from(row: ResultRow): PeppolSettingsEntity = PeppolSettingsEntity(
    id = PeppolSettingsId.parse(row[PeppolSettingsTable.id].value.toString()),
    tenantId = TenantId.parse(row[PeppolSettingsTable.tenantId].toString()),
    companyId = row[PeppolSettingsTable.companyId],
    peppolId = PeppolId(row[PeppolSettingsTable.peppolId]),
    isEnabled = row[PeppolSettingsTable.isEnabled],
    testMode = row[PeppolSettingsTable.testMode],
    webhookToken = row[PeppolSettingsTable.webhookToken],
    lastFullSyncAt = row[PeppolSettingsTable.lastFullSyncAt],
    createdAt = row[PeppolSettingsTable.createdAt],
    updatedAt = row[PeppolSettingsTable.updatedAt],
)

internal fun PeppolResolutionEntity.Companion.from(row: ResultRow): PeppolResolutionEntity {
    val docTypesJson = row[PeppolDirectoryCacheTable.supportedDocTypes]
    val supportedDocTypes = if (docTypesJson.isNullOrBlank()) {
        emptyList()
    } else {
        runCatching { Json.decodeFromString<List<String>>(docTypesJson) }.getOrElse { emptyList() }
    }

    return PeppolResolutionEntity(
        contactId = ContactId.parse(row[PeppolDirectoryCacheTable.contactId].toString()),
        status = row[PeppolDirectoryCacheTable.status],
        participantId = row[PeppolDirectoryCacheTable.participantId],
        scheme = row[PeppolDirectoryCacheTable.scheme],
        supportedDocTypes = supportedDocTypes,
        source = row[PeppolDirectoryCacheTable.lookupSource],
        vatNumberSnapshot = row[PeppolDirectoryCacheTable.vatNumberSnapshot],
        companyNumberSnapshot = row[PeppolDirectoryCacheTable.companyNumberSnapshot],
        lastCheckedAt = row[PeppolDirectoryCacheTable.lastCheckedAt],
        expiresAt = row[PeppolDirectoryCacheTable.expiresAt],
        errorMessage = row[PeppolDirectoryCacheTable.errorMessage]
    )
}

fun PeppolResolution.Companion.from(entity: PeppolResolutionEntity): PeppolResolution = PeppolResolution(
    contactId = entity.contactId,
    status = entity.status,
    participantId = entity.participantId,
    scheme = entity.scheme,
    supportedDocTypes = entity.supportedDocTypes,
    source = entity.source,
    vatNumberSnapshot = entity.vatNumberSnapshot,
    companyNumberSnapshot = entity.companyNumberSnapshot,
    lastCheckedAt = entity.lastCheckedAt,
    expiresAt = entity.expiresAt,
    errorMessage = entity.errorMessage,
)
