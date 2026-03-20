package tech.dokus.database.mapper

import org.jetbrains.exposed.v1.core.ResultRow
import tech.dokus.database.repository.cashflow.DocumentBlobEntity
import tech.dokus.database.repository.cashflow.DocumentRepository
import tech.dokus.database.repository.cashflow.DraftSummaryEntity
import tech.dokus.database.repository.cashflow.IngestionRunSummaryEntity
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
internal fun DraftSummaryEntity.Companion.from(row: ResultRow, contactName: String? = null): DraftSummaryEntity {
    val counterpartyInfo = DocumentRepository.buildCounterpartyInfo(row)
    return DraftSummaryEntity(
        documentId = DocumentId.parse(row[DocumentsTable.id].toString()),
        tenantId = TenantId(row[DocumentsTable.tenantId].toKotlinUuid()),
        documentStatus = row[DocumentsTable.documentStatus] ?: DocumentStatus.NeedsReview,
        documentType = row[DocumentsTable.documentType],
        direction = row[DocumentsTable.direction] ?: DocumentDirection.Unknown,
        aiKeywords = row[DocumentsTable.aiKeywords]?.let { json.decodeFromString(it) } ?: emptyList(),
        purposeBase = row[DocumentsTable.purposeBase],
        purposePeriodYear = row[DocumentsTable.purposePeriodYear],
        purposePeriodMonth = row[DocumentsTable.purposePeriodMonth],
        purposeRendered = row[DocumentsTable.purposeRendered],
        purposeSource = row[DocumentsTable.purposeSource],
        purposeLocked = row[DocumentsTable.purposeLocked],
        purposePeriodMode = row[DocumentsTable.purposePeriodMode],
        counterpartyKey = row[DocumentsTable.counterpartyKey],
        merchantToken = row[DocumentsTable.merchantToken],
        aiDraftSourceRunId = row[DocumentsTable.aiDraftSourceRunId]
            ?.let { IngestionRunId.parse(it.toString()) },
        draftVersion = row[DocumentsTable.draftVersion],
        draftEditedAt = row[DocumentsTable.draftEditedAt],
        draftEditedBy = row[DocumentsTable.draftEditedBy]?.let { UserId(it.toKotlinUuid()) },
        counterparty = counterpartyInfo,
        counterpartyDisplayName = contactName
            ?: if (counterpartyInfo.isUnresolved()) counterpartyInfo.snapshot?.name else null,
        rejectReason = row[DocumentsTable.rejectReason],
        lastSuccessfulRunId = row[DocumentsTable.lastSuccessfulRunId]
            ?.let { IngestionRunId.parse(it.toString()) },
        createdAt = row[DocumentsTable.uploadedAt],
        updatedAt = row[DocumentsTable.updatedAt]
    )
}

@OptIn(ExperimentalUuidApi::class)
internal fun IngestionRunSummaryEntity.Companion.from(row: ResultRow): IngestionRunSummaryEntity {
    return IngestionRunSummaryEntity(
        id = IngestionRunId.parse(row[DocumentIngestionRunsTable.id].toString()),
        documentId = DocumentId.parse(row[DocumentIngestionRunsTable.documentId].toString()),
        tenantId = TenantId(row[DocumentIngestionRunsTable.tenantId].toKotlinUuid()),
        status = row[DocumentIngestionRunsTable.status],
        provider = row[DocumentIngestionRunsTable.provider],
        queuedAt = row[DocumentIngestionRunsTable.queuedAt],
        startedAt = row[DocumentIngestionRunsTable.startedAt],
        finishedAt = row[DocumentIngestionRunsTable.finishedAt],
        errorMessage = row[DocumentIngestionRunsTable.errorMessage],
        confidence = row[DocumentIngestionRunsTable.confidence]?.toDouble(),
        processingOutcome = row[DocumentIngestionRunsTable.processingOutcome],
        rawExtractionJson = row[DocumentIngestionRunsTable.rawExtractionJson],
        processingTrace = row[DocumentIngestionRunsTable.processingTrace]
    )
}

@OptIn(ExperimentalUuidApi::class)
internal fun DocumentBlobEntity.Companion.from(row: ResultRow): DocumentBlobEntity {
    return DocumentBlobEntity(
        id = DocumentBlobId(row[DocumentBlobsTable.id].value.toKotlinUuid()),
        tenantId = TenantId(row[DocumentBlobsTable.tenantId].toKotlinUuid()),
        inputHash = row[DocumentBlobsTable.inputHash],
        storageKey = row[DocumentBlobsTable.storageKey],
        contentType = row[DocumentBlobsTable.contentType],
        sizeBytes = row[DocumentBlobsTable.sizeBytes]
    )
}
