package tech.dokus.database.mapper

import tech.dokus.database.entity.PeppolRegistrationEntity
import tech.dokus.database.entity.PeppolSettingsEntity
import tech.dokus.database.repository.peppol.PeppolTransmissionEntity
import tech.dokus.domain.model.PeppolRegistrationDto
import tech.dokus.domain.model.PeppolSettingsDto
import tech.dokus.domain.model.PeppolTransmissionDto

fun PeppolRegistrationDto.Companion.from(entity: PeppolRegistrationEntity) = PeppolRegistrationDto(
    id = entity.id,
    tenantId = entity.tenantId,
    peppolId = entity.peppolId,
    recommandCompanyId = entity.recommandCompanyId,
    status = entity.status,
    canReceive = entity.canReceive,
    canSend = entity.canSend,
    testMode = entity.testMode,
    waitingSince = entity.waitingSince,
    lastPolledAt = entity.lastPolledAt,
    errorMessage = entity.errorMessage,
    createdAt = entity.createdAt,
    updatedAt = entity.updatedAt,
)

fun PeppolSettingsDto.Companion.from(entity: PeppolSettingsEntity) = PeppolSettingsDto(
    id = entity.id,
    tenantId = entity.tenantId,
    companyId = entity.companyId,
    peppolId = entity.peppolId,
    isEnabled = entity.isEnabled,
    testMode = entity.testMode,
    webhookToken = entity.webhookToken,
    lastFullSyncAt = entity.lastFullSyncAt,
    createdAt = entity.createdAt,
    updatedAt = entity.updatedAt,
)

fun PeppolTransmissionDto.Companion.from(entity: PeppolTransmissionEntity) = PeppolTransmissionDto(
    id = entity.id,
    tenantId = entity.tenantId,
    direction = entity.direction,
    documentType = entity.documentType,
    status = entity.status,
    invoiceId = entity.invoiceId,
    externalDocumentId = entity.externalDocumentId,
    recipientPeppolId = entity.recipientPeppolId,
    senderPeppolId = entity.senderPeppolId,
    errorMessage = entity.errorMessage,
    rawRequest = entity.rawRequest,
    rawResponse = entity.rawResponse,
    attemptCount = entity.attemptCount,
    nextRetryAt = entity.nextRetryAt,
    lastAttemptAt = entity.lastAttemptAt,
    providerErrorCode = entity.providerErrorCode,
    providerErrorMessage = entity.providerErrorMessage,
    transmittedAt = entity.transmittedAt,
    createdAt = entity.createdAt,
    updatedAt = entity.updatedAt,
)
