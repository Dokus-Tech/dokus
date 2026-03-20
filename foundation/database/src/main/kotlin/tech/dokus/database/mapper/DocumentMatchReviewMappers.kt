package tech.dokus.database.mapper

import org.jetbrains.exposed.v1.core.ResultRow
import tech.dokus.database.repository.cashflow.DocumentMatchReviewEntity
import tech.dokus.database.tables.documents.DocumentMatchReviewsTable
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.DocumentMatchReviewId
import tech.dokus.domain.ids.DocumentSourceId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.UserId
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toKotlinUuid

@OptIn(ExperimentalUuidApi::class)
internal fun DocumentMatchReviewEntity.Companion.from(row: ResultRow): DocumentMatchReviewEntity {
    return DocumentMatchReviewEntity(
        id = DocumentMatchReviewId(row[DocumentMatchReviewsTable.id].value.toKotlinUuid()),
        tenantId = TenantId(row[DocumentMatchReviewsTable.tenantId].toKotlinUuid()),
        documentId = DocumentId.parse(row[DocumentMatchReviewsTable.documentId].toString()),
        incomingSourceId = DocumentSourceId(row[DocumentMatchReviewsTable.incomingSourceId].toKotlinUuid()),
        reasonType = row[DocumentMatchReviewsTable.reasonType],
        aiSummary = row[DocumentMatchReviewsTable.aiSummary],
        aiConfidence = row[DocumentMatchReviewsTable.aiConfidence]?.toDouble(),
        status = row[DocumentMatchReviewsTable.status],
        resolvedBy = row[DocumentMatchReviewsTable.resolvedBy]?.toKotlinUuid()?.let { UserId(it) },
        resolvedAt = row[DocumentMatchReviewsTable.resolvedAt],
        createdAt = row[DocumentMatchReviewsTable.createdAt],
        updatedAt = row[DocumentMatchReviewsTable.updatedAt]
    )
}
