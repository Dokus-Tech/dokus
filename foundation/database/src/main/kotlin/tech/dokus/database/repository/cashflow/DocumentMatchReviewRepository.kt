package tech.dokus.database.repository.cashflow

import kotlinx.datetime.Clock
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
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toKotlinUuid

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

@OptIn(ExperimentalUuidApi::class)
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

    suspend fun getById(tenantId: TenantId, reviewId: DocumentMatchReviewId): DocumentMatchReviewSummary? =
        newSuspendedTransaction {
            DocumentMatchReviewsTable.selectAll()
                .where {
                    (DocumentMatchReviewsTable.id eq UUID.fromString(reviewId.toString())) and
                        (DocumentMatchReviewsTable.tenantId eq UUID.fromString(tenantId.toString()))
                }
                .map { it.toSummary() }
                .singleOrNull()
        }

    suspend fun listPendingByDocumentIds(
        tenantId: TenantId,
        documentIds: List<DocumentId>
    ): Map<DocumentId, DocumentMatchReviewSummary> = newSuspendedTransaction {
        if (documentIds.isEmpty()) return@newSuspendedTransaction emptyMap()
        val ids = documentIds.map { UUID.fromString(it.toString()) }
        DocumentMatchReviewsTable.selectAll()
            .where {
                (DocumentMatchReviewsTable.tenantId eq UUID.fromString(tenantId.toString())) and
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

    private fun ResultRow.toSummary(): DocumentMatchReviewSummary {
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
}
