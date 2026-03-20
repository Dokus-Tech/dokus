package tech.dokus.database.repository.cashflow

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.v1.jdbc.update
import tech.dokus.database.entity.DocumentMatchReviewEntity
import tech.dokus.database.mapper.from
import tech.dokus.database.tables.documents.DocumentMatchReviewsTable
import tech.dokus.domain.enums.ReviewReason
import tech.dokus.domain.enums.DocumentMatchReviewStatus
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.DocumentMatchReviewId
import tech.dokus.domain.ids.DocumentSourceId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.UserId
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toKotlinUuid

@OptIn(ExperimentalUuidApi::class)
class DocumentMatchReviewRepository {

    suspend fun createPending(
        tenantId: TenantId,
        documentId: DocumentId,
        sourceId: DocumentSourceId,
        reasonType: ReviewReason,
        aiSummary: String? = null,
        aiConfidence: Double? = null
    ): DocumentMatchReviewId = newSuspendedTransaction {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        val id = DocumentMatchReviewId.generate()
        DocumentMatchReviewsTable.insert {
            it[DocumentMatchReviewsTable.id] = UUID.fromString(id.toString())
            it[DocumentMatchReviewsTable.tenantId] = UUID.fromString(tenantId.toString())
            it[DocumentMatchReviewsTable.documentId] = UUID.fromString(documentId.toString())
            it[incomingSourceId] = UUID.fromString(sourceId.toString())
            it[DocumentMatchReviewsTable.reasonType] = reasonType
            it[DocumentMatchReviewsTable.aiSummary] = aiSummary
            it[DocumentMatchReviewsTable.aiConfidence] = aiConfidence?.toBigDecimal()
            it[status] = DocumentMatchReviewStatus.Pending
            it[createdAt] = now
            it[updatedAt] = now
        }
        id
    }

    suspend fun getById(tenantId: TenantId, reviewId: DocumentMatchReviewId): DocumentMatchReviewEntity? =
        newSuspendedTransaction {
            DocumentMatchReviewsTable.selectAll()
                .where {
                    (DocumentMatchReviewsTable.id eq UUID.fromString(reviewId.toString())) and
                        (DocumentMatchReviewsTable.tenantId eq UUID.fromString(tenantId.toString()))
                }
                .map { DocumentMatchReviewEntity.from(it) }
                .singleOrNull()
        }

    suspend fun listPendingByDocumentIds(
        tenantId: TenantId,
        documentIds: List<DocumentId>
    ): Map<DocumentId, DocumentMatchReviewEntity> = newSuspendedTransaction {
        if (documentIds.isEmpty()) return@newSuspendedTransaction emptyMap()
        val ids = documentIds.map { UUID.fromString(it.toString()) }
        DocumentMatchReviewsTable.selectAll()
            .where {
                (DocumentMatchReviewsTable.tenantId eq UUID.fromString(tenantId.toString())) and
                    (DocumentMatchReviewsTable.documentId inList ids) and
                    (DocumentMatchReviewsTable.status eq DocumentMatchReviewStatus.Pending)
            }
            .map { DocumentMatchReviewEntity.from(it) }
            .groupBy { it.documentId }
            .mapValues { (_, reviews) -> reviews.minBy { it.createdAt } }
    }

    suspend fun resolve(
        tenantId: TenantId,
        reviewId: DocumentMatchReviewId,
        status: DocumentMatchReviewStatus,
        resolvedBy: UserId
    ): Boolean = newSuspendedTransaction {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        DocumentMatchReviewsTable.update({
            (DocumentMatchReviewsTable.id eq UUID.fromString(reviewId.toString())) and
                (DocumentMatchReviewsTable.tenantId eq UUID.fromString(tenantId.toString())) and
                (DocumentMatchReviewsTable.status eq DocumentMatchReviewStatus.Pending)
        }) {
            it[DocumentMatchReviewsTable.status] = status
            it[DocumentMatchReviewsTable.resolvedBy] = UUID.fromString(resolvedBy.toString())
            it[resolvedAt] = now
            it[updatedAt] = now
        } > 0
    }

}
