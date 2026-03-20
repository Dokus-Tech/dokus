package tech.dokus.database.mapper

import org.jetbrains.exposed.v1.core.ResultRow
import tech.dokus.database.repository.cashflow.DocumentBlobSummary
import tech.dokus.database.repository.cashflow.DocumentRepository
import tech.dokus.database.repository.cashflow.DraftSummary
import tech.dokus.database.repository.cashflow.IngestionRunSummary
import tech.dokus.database.tables.documents.DocumentBlobsTable
import tech.dokus.database.tables.documents.DocumentIngestionRunsTable
import tech.dokus.database.tables.documents.DocumentsTable
import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.enums.DocumentStatus
import tech.dokus.domain.ids.DocumentBlobId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.IngestionRunId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.UserId
import tech.dokus.domain.model.contact.isUnresolved
import tech.dokus.domain.utils.json
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toKotlinUuid

@OptIn(ExperimentalUuidApi::class)
internal fun ResultRow.toDraftSummary(contactName: String? = null): DraftSummary {
    val counterpartyInfo = DocumentRepository.buildCounterpartyInfo(this)
    return DraftSummary(
        documentId = DocumentId.parse(this[DocumentsTable.id].toString()),
        tenantId = TenantId(this[DocumentsTable.tenantId].toKotlinUuid()),
        documentStatus = this[DocumentsTable.documentStatus] ?: DocumentStatus.NeedsReview,
        documentType = this[DocumentsTable.documentType],
        direction = this[DocumentsTable.direction] ?: DocumentDirection.Unknown,
        aiKeywords = this[DocumentsTable.aiKeywords]?.let { json.decodeFromString(it) } ?: emptyList(),
        purposeBase = this[DocumentsTable.purposeBase],
        purposePeriodYear = this[DocumentsTable.purposePeriodYear],
        purposePeriodMonth = this[DocumentsTable.purposePeriodMonth],
        purposeRendered = this[DocumentsTable.purposeRendered],
        purposeSource = this[DocumentsTable.purposeSource],
        purposeLocked = this[DocumentsTable.purposeLocked],
        purposePeriodMode = this[DocumentsTable.purposePeriodMode],
        counterpartyKey = this[DocumentsTable.counterpartyKey],
        merchantToken = this[DocumentsTable.merchantToken],
        aiDraftSourceRunId = this[DocumentsTable.aiDraftSourceRunId]
            ?.let { IngestionRunId.parse(it.toString()) },
        draftVersion = this[DocumentsTable.draftVersion],
        draftEditedAt = this[DocumentsTable.draftEditedAt],
        draftEditedBy = this[DocumentsTable.draftEditedBy]?.let { UserId(it.toKotlinUuid()) },
        counterparty = counterpartyInfo,
        counterpartyDisplayName = contactName
            ?: if (counterpartyInfo.isUnresolved()) counterpartyInfo.snapshot?.name else null,
        rejectReason = this[DocumentsTable.rejectReason],
        lastSuccessfulRunId = this[DocumentsTable.lastSuccessfulRunId]
            ?.let { IngestionRunId.parse(it.toString()) },
        createdAt = this[DocumentsTable.uploadedAt],
        updatedAt = this[DocumentsTable.updatedAt]
    )
}

@OptIn(ExperimentalUuidApi::class)
internal fun ResultRow.toIngestionRunSummary(): IngestionRunSummary {
    return IngestionRunSummary(
        id = IngestionRunId.parse(this[DocumentIngestionRunsTable.id].toString()),
        documentId = DocumentId.parse(this[DocumentIngestionRunsTable.documentId].toString()),
        tenantId = TenantId(this[DocumentIngestionRunsTable.tenantId].toKotlinUuid()),
        status = this[DocumentIngestionRunsTable.status],
        provider = this[DocumentIngestionRunsTable.provider],
        queuedAt = this[DocumentIngestionRunsTable.queuedAt],
        startedAt = this[DocumentIngestionRunsTable.startedAt],
        finishedAt = this[DocumentIngestionRunsTable.finishedAt],
        errorMessage = this[DocumentIngestionRunsTable.errorMessage],
        confidence = this[DocumentIngestionRunsTable.confidence]?.toDouble(),
        processingOutcome = this[DocumentIngestionRunsTable.processingOutcome],
        rawExtractionJson = this[DocumentIngestionRunsTable.rawExtractionJson],
        processingTrace = this[DocumentIngestionRunsTable.processingTrace]
    )
}

@OptIn(ExperimentalUuidApi::class)
internal fun ResultRow.toBlobSummary(): DocumentBlobSummary {
    return DocumentBlobSummary(
        id = DocumentBlobId(this[DocumentBlobsTable.id].value.toKotlinUuid()),
        tenantId = TenantId(this[DocumentBlobsTable.tenantId].toKotlinUuid()),
        inputHash = this[DocumentBlobsTable.inputHash],
        storageKey = this[DocumentBlobsTable.storageKey],
        contentType = this[DocumentBlobsTable.contentType],
        sizeBytes = this[DocumentBlobsTable.sizeBytes]
    )
}
