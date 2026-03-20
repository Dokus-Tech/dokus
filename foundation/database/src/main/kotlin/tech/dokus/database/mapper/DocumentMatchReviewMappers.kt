package tech.dokus.database.mapper

import org.jetbrains.exposed.v1.core.ResultRow
import tech.dokus.database.repository.cashflow.DocumentMatchReviewSummary
import tech.dokus.database.tables.documents.DocumentMatchReviewsTable
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.DocumentMatchReviewId
import tech.dokus.domain.ids.DocumentSourceId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.UserId
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toKotlinUuid

@OptIn(ExperimentalUuidApi::class)
internal fun ResultRow.toDocumentMatchReviewSummary(): DocumentMatchReviewSummary {
    return DocumentMatchReviewSummary(
        id = DocumentMatchReviewId(this[DocumentMatchReviewsTable.id].value.toKotlinUuid()),
        tenantId = TenantId(this[DocumentMatchReviewsTable.tenantId].toKotlinUuid()),
        documentId = DocumentId.parse(this[DocumentMatchReviewsTable.documentId].toString()),
        incomingSourceId = DocumentSourceId(this[DocumentMatchReviewsTable.incomingSourceId].toKotlinUuid()),
        reasonType = this[DocumentMatchReviewsTable.reasonType],
        aiSummary = this[DocumentMatchReviewsTable.aiSummary],
        aiConfidence = this[DocumentMatchReviewsTable.aiConfidence]?.toDouble(),
        status = this[DocumentMatchReviewsTable.status],
        resolvedBy = this[DocumentMatchReviewsTable.resolvedBy]?.toKotlinUuid()?.let { UserId(it) },
        resolvedAt = this[DocumentMatchReviewsTable.resolvedAt],
        createdAt = this[DocumentMatchReviewsTable.createdAt],
        updatedAt = this[DocumentMatchReviewsTable.updatedAt]
    )
}
