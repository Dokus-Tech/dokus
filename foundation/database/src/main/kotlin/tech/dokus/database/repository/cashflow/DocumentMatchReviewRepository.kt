package tech.dokus.database.repository.cashflow
import kotlin.uuid.Uuid

import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.v1.jdbc.update
import tech.dokus.database.tables.documents.DocumentMatchReviewsTable
import tech.dokus.domain.enums.DocumentMatchReviewReasonType
import tech.dokus.domain.enums.DocumentMatchReviewStatus
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.DocumentMatchReviewId
import tech.dokus.domain.ids.DocumentSourceId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.UserId

data class DocumentMatchReviewSummary(
    val id: DocumentMatchReviewId,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val incomingSourceId: DocumentSourceId,
    val reasonType: DocumentMatchReviewReasonType,
    val aiSummary: String?,
    val aiConfidence: Double?,
    val status: DocumentMatchReviewStatus,
    val resolvedBy: UserId?,
    val resolvedAt: kotlinx.datetime.LocalDateTime?,
    val createdAt: kotlinx.datetime.LocalDateTime,
    val updatedAt: kotlinx.datetime.LocalDateTime
)

class DocumentMatchReviewRepository {

    suspend fun createPending(
        tenantId: TenantId,
        documentId: DocumentId,
        sourceId: DocumentSourceId,
        reasonType: DocumentMatchReviewReasonType,
        aiSummary: String? = null,
        aiConfidence: Double? = null
    ): DocumentMatchReviewId = newSuspendedTransaction {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        val id = DocumentMatchReviewId.generate()
        DocumentMatchReviewsTable.insert {
            it[DocumentMatchReviewsTable.id] = id.value
            it[DocumentMatchReviewsTable.tenantId] = tenantId.value
            it[DocumentMatchReviewsTable.documentId] = documentId.value
            it[incomingSourceId] = sourceId.value
            it[DocumentMatchReviewsTable.reasonType] = reasonType
            it[DocumentMatchReviewsTable.aiSummary] = aiSummary
            it[DocumentMatchReviewsTable.aiConfidence] = aiConfidence?.toBigDecimal()
            it[status] = DocumentMatchReviewStatus.Pending
            it[createdAt] = now
            it[updatedAt] = now
        }
        id
    }

    suspend fun getById(tenantId: TenantId, reviewId: DocumentMatchReviewId): DocumentMatchReviewSummary? =
        newSuspendedTransaction {
            DocumentMatchReviewsTable.selectAll()
                .where {
                    (DocumentMatchReviewsTable.id eq reviewId.value) and
                        (DocumentMatchReviewsTable.tenantId eq tenantId.value)
                }
                .map { it.toSummary() }
                .singleOrNull()
        }

    suspend fun listPendingByDocumentIds(
        tenantId: TenantId,
        documentIds: List<DocumentId>
    ): Map<DocumentId, DocumentMatchReviewSummary> = newSuspendedTransaction {
        if (documentIds.isEmpty()) return@newSuspendedTransaction emptyMap()
        val ids = documentIds.map { it.value }
        DocumentMatchReviewsTable.selectAll()
            .where {
                (DocumentMatchReviewsTable.tenantId eq tenantId.value) and
                    (DocumentMatchReviewsTable.documentId inList ids) and
                    (DocumentMatchReviewsTable.status eq DocumentMatchReviewStatus.Pending)
            }
            .map { it.toSummary() }
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
            (DocumentMatchReviewsTable.id eq reviewId.value) and
                (DocumentMatchReviewsTable.tenantId eq tenantId.value) and
                (DocumentMatchReviewsTable.status eq DocumentMatchReviewStatus.Pending)
        }) {
            it[DocumentMatchReviewsTable.status] = status
            it[DocumentMatchReviewsTable.resolvedBy] = resolvedBy.value
            it[resolvedAt] = now
            it[updatedAt] = now
        } > 0
    }

    private fun ResultRow.toSummary(): DocumentMatchReviewSummary {
        return DocumentMatchReviewSummary(
            id = DocumentMatchReviewId(this[DocumentMatchReviewsTable.id].value),
            tenantId = TenantId(this[DocumentMatchReviewsTable.tenantId]),
            documentId = DocumentId(this[DocumentMatchReviewsTable.documentId]),
            incomingSourceId = DocumentSourceId(this[DocumentMatchReviewsTable.incomingSourceId]),
            reasonType = this[DocumentMatchReviewsTable.reasonType],
            aiSummary = this[DocumentMatchReviewsTable.aiSummary],
            aiConfidence = this[DocumentMatchReviewsTable.aiConfidence]?.toDouble(),
            status = this[DocumentMatchReviewsTable.status],
            resolvedBy = this[DocumentMatchReviewsTable.resolvedBy]?.let { UserId(it) },
            resolvedAt = this[DocumentMatchReviewsTable.resolvedAt],
            createdAt = this[DocumentMatchReviewsTable.createdAt],
            updatedAt = this[DocumentMatchReviewsTable.updatedAt]
        )
    }
}
