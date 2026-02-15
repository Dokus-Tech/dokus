package tech.dokus.database.repository.cashflow

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.v1.jdbc.update
import tech.dokus.database.entity.IngestionItemEntity
import tech.dokus.database.tables.documents.DocumentIngestionRunsTable
import tech.dokus.database.tables.documents.DocumentsTable
import tech.dokus.domain.enums.IngestionStatus
import tech.dokus.domain.enums.ProcessingOutcome
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.IngestionRunId
import tech.dokus.domain.ids.DocumentSourceId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.processing.DocumentProcessingConstants
import java.util.*
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid

/**
 * Data class for ingestion run summary.
 */
data class IngestionRunSummary(
    val id: IngestionRunId,
    val documentId: DocumentId,
    val tenantId: TenantId,
    val status: IngestionStatus,
    val provider: String?,
    val queuedAt: LocalDateTime,
    val startedAt: LocalDateTime?,
    val finishedAt: LocalDateTime?,
    val errorMessage: String?,
    val confidence: Double?,
    val processingOutcome: ProcessingOutcome?,
    val rawExtractionJson: String? = null,
    val processingTrace: String? = null
)

/**
 * Repository for document ingestion run operations.
 * CRITICAL: All queries filter by tenantId for security.
 */
@OptIn(ExperimentalUuidApi::class)
class DocumentIngestionRunRepository {

    /**
     * Create a new ingestion run with status=Queued.
     * CRITICAL: Must provide tenantId.
     *
     * @param documentId Document to process
     * @param tenantId Tenant owning the document
     * @param overrideMaxPages Optional override for max pages (null = use default)
     * @param overrideDpi Optional override for DPI (null = use default)
     */
    suspend fun createRun(
        documentId: DocumentId,
        tenantId: TenantId,
        sourceId: DocumentSourceId? = null,
        userFeedback: String? = null,
        overrideMaxPages: Int? = null,
        overrideDpi: Int? = null,
    ): IngestionRunId = newSuspendedTransaction {
        val sanitizedMaxPages = overrideMaxPages?.takeIf { it > 0 }
        val sanitizedDpi = overrideDpi?.takeIf { it > 0 }
        return@newSuspendedTransaction IngestionRunId.generate().also { id ->
            DocumentIngestionRunsTable.insert {
                it[DocumentIngestionRunsTable.id] = id.value.toJavaUuid()
                it[DocumentIngestionRunsTable.documentId] = UUID.fromString(documentId.toString())
                it[DocumentIngestionRunsTable.tenantId] = UUID.fromString(tenantId.toString())
                it[DocumentIngestionRunsTable.sourceId] = sourceId?.let { value -> UUID.fromString(value.toString()) }
                it[status] = IngestionStatus.Queued
                it[DocumentIngestionRunsTable.userFeedback] = userFeedback?.takeIf { fb -> fb.isNotBlank() }
                it[DocumentIngestionRunsTable.overrideMaxPages] = sanitizedMaxPages
                it[DocumentIngestionRunsTable.overrideDpi] = sanitizedDpi
            }
        }
    }

    /**
     * Get an ingestion run by ID.
     * CRITICAL: Must filter by tenantId.
     */
    suspend fun getById(
        runId: IngestionRunId,
        tenantId: TenantId
    ): IngestionRunSummary? = newSuspendedTransaction {
        DocumentIngestionRunsTable.selectAll()
            .where {
                (DocumentIngestionRunsTable.id eq UUID.fromString(runId.toString())) and
                        (DocumentIngestionRunsTable.tenantId eq UUID.fromString(tenantId.toString()))
            }
            .map { it.toIngestionRunSummary() }
            .singleOrNull()
    }

    /**
     * List all ingestion runs for a document.
     * CRITICAL: Must filter by tenantId.
     */
    suspend fun listByDocument(
        documentId: DocumentId,
        tenantId: TenantId
    ): List<IngestionRunSummary> = newSuspendedTransaction {
        val tenantIdUuid = UUID.fromString(tenantId.toString())
        recoverStaleProcessingRunsInternal(tenantIdUuid)

        DocumentIngestionRunsTable.selectAll()
            .where {
                (DocumentIngestionRunsTable.documentId eq UUID.fromString(documentId.toString())) and
                        (DocumentIngestionRunsTable.tenantId eq tenantIdUuid)
            }
            .orderBy(DocumentIngestionRunsTable.queuedAt, SortOrder.DESC)
            .map { it.toIngestionRunSummary() }
    }

    /**
     * Get the latest ingestion run for a document.
     * Priority: Processing > latest Succeeded/Failed (by finishedAt) > latest Queued (by queuedAt)
     * CRITICAL: Must filter by tenantId.
     */
    suspend fun getLatestForDocument(
        documentId: DocumentId,
        tenantId: TenantId
    ): IngestionRunSummary? = newSuspendedTransaction {
        val docIdUuid = UUID.fromString(documentId.toString())
        val tenantIdUuid = UUID.fromString(tenantId.toString())
        recoverStaleProcessingRunsInternal(tenantIdUuid)

        // First, check for Processing status
        val processing = DocumentIngestionRunsTable.selectAll()
            .where {
                (DocumentIngestionRunsTable.documentId eq docIdUuid) and
                        (DocumentIngestionRunsTable.tenantId eq tenantIdUuid) and
                        (DocumentIngestionRunsTable.status eq IngestionStatus.Processing)
            }
            .orderBy(
                DocumentIngestionRunsTable.startedAt to SortOrder.DESC_NULLS_LAST,
                DocumentIngestionRunsTable.id to SortOrder.DESC
            )
            .map { it.toIngestionRunSummary() }
            .firstOrNull()

        if (processing != null) return@newSuspendedTransaction processing

        // Then, check for latest Succeeded/Failed by finishedAt
        val finished = DocumentIngestionRunsTable.selectAll()
            .where {
                (DocumentIngestionRunsTable.documentId eq docIdUuid) and
                        (DocumentIngestionRunsTable.tenantId eq tenantIdUuid) and
                        (DocumentIngestionRunsTable.status inList listOf(
                            IngestionStatus.Succeeded,
                            IngestionStatus.Failed
                        ))
            }
            .orderBy(
                DocumentIngestionRunsTable.finishedAt to SortOrder.DESC_NULLS_LAST,
                DocumentIngestionRunsTable.id to SortOrder.DESC
            )
            .map { it.toIngestionRunSummary() }
            .firstOrNull()

        if (finished != null) return@newSuspendedTransaction finished

        // Finally, check for latest Queued by queuedAt
        DocumentIngestionRunsTable.selectAll()
            .where {
                (DocumentIngestionRunsTable.documentId eq docIdUuid) and
                        (DocumentIngestionRunsTable.tenantId eq tenantIdUuid) and
                        (DocumentIngestionRunsTable.status eq IngestionStatus.Queued)
            }
            .orderBy(
                DocumentIngestionRunsTable.queuedAt to SortOrder.DESC,
                DocumentIngestionRunsTable.id to SortOrder.DESC
            )
            .map { it.toIngestionRunSummary() }
            .firstOrNull()
    }

    /**
     * Find an active (Queued or Processing) run for a document.
     * Used for idempotent reprocessing.
     * CRITICAL: Must filter by tenantId.
     */
    suspend fun findActiveRun(
        documentId: DocumentId,
        tenantId: TenantId
    ): IngestionRunSummary? = newSuspendedTransaction {
        val tenantIdUuid = UUID.fromString(tenantId.toString())
        recoverStaleProcessingRunsInternal(tenantIdUuid)

        val pendingStatuses = listOf(IngestionStatus.Queued, IngestionStatus.Processing)
        DocumentIngestionRunsTable.selectAll()
            .where {
                (DocumentIngestionRunsTable.documentId eq UUID.fromString(documentId.toString())) and
                        (DocumentIngestionRunsTable.tenantId eq tenantIdUuid) and
                        (DocumentIngestionRunsTable.status inList pendingStatuses)
            }
            .orderBy(DocumentIngestionRunsTable.queuedAt, SortOrder.DESC)
            .map { it.toIngestionRunSummary() }
            .firstOrNull()
    }

    /**
     * Find pending runs for processing (Queued status).
     * Note: This is typically called by the processor without tenant filtering
     * since the processor processes all tenants.
     */
    suspend fun findPendingForProcessing(
        limit: Int = 10
    ): List<IngestionItemEntity> = newSuspendedTransaction {
        (DocumentIngestionRunsTable innerJoin DocumentsTable)
            .selectAll()
            .where {
                DocumentIngestionRunsTable.status eq IngestionStatus.Queued
            }
            .orderBy(DocumentIngestionRunsTable.queuedAt to SortOrder.ASC)
            .limit(limit)
            .map { row ->
                IngestionItemEntity(
                    runId = IngestionRunId(row[DocumentIngestionRunsTable.id].value.toKotlinUuid()),
                    documentId = DocumentId(row[DocumentIngestionRunsTable.documentId].toKotlinUuid()),
                    tenantId = TenantId(row[DocumentIngestionRunsTable.tenantId].toKotlinUuid()),
                    sourceId = row[DocumentIngestionRunsTable.sourceId]?.toKotlinUuid()?.let { DocumentSourceId(it) },
                    storageKey = row[DocumentsTable.storageKey],
                    filename = row[DocumentsTable.filename],
                    contentType = row[DocumentsTable.contentType],
                    userFeedback = row[DocumentIngestionRunsTable.userFeedback],
                    overrideMaxPages = row[DocumentIngestionRunsTable.overrideMaxPages],
                    overrideDpi = row[DocumentIngestionRunsTable.overrideDpi],
                )
            }
    }

    /**
     * Mark a run as processing.
     */
    suspend fun markAsProcessing(
        runId: IngestionRunId,
        provider: String
    ): Boolean = newSuspendedTransaction {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        DocumentIngestionRunsTable.update({
            DocumentIngestionRunsTable.id eq UUID.fromString(runId.toString())
        }) {
            it[status] = IngestionStatus.Processing
            it[startedAt] = now
            it[DocumentIngestionRunsTable.provider] = provider
        } > 0
    }

    /**
     * Mark a run as succeeded with extraction results.
     */
    suspend fun markAsSucceeded(
        runId: IngestionRunId,
        rawText: String?,
        rawExtractionJson: String?,
        confidence: Double?
    ): Boolean = newSuspendedTransaction {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        val outcome = confidence?.let {
            if (it >= DocumentProcessingConstants.AUTO_CONFIRM_CONFIDENCE_THRESHOLD) {
                ProcessingOutcome.AutoConfirmEligible
            } else {
                ProcessingOutcome.ManualReviewRequired
            }
        }
        DocumentIngestionRunsTable.update({
            DocumentIngestionRunsTable.id eq UUID.fromString(runId.toString())
        }) {
            it[status] = IngestionStatus.Succeeded
            it[finishedAt] = now
            it[DocumentIngestionRunsTable.rawText] = rawText
            it[DocumentIngestionRunsTable.rawExtractionJson] = rawExtractionJson
            it[DocumentIngestionRunsTable.confidence] = confidence?.toBigDecimal()
            it[processingOutcome] = outcome
            it[fieldConfidences] = null
            it[errorMessage] = null
        } > 0
    }

    /**
     * Mark a run as failed with error message.
     */
    suspend fun markAsFailed(
        runId: IngestionRunId,
        error: String
    ): Boolean = newSuspendedTransaction {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        DocumentIngestionRunsTable.update({
            DocumentIngestionRunsTable.id eq UUID.fromString(runId.toString())
        }) {
            it[status] = IngestionStatus.Failed
            it[finishedAt] = now
            it[errorMessage] = error
        } > 0
    }

    suspend fun recoverStaleProcessingRunsForTenant(tenantId: TenantId): Int = newSuspendedTransaction {
        recoverStaleProcessingRunsInternal(UUID.fromString(tenantId.toString()))
    }

    suspend fun recoverStaleProcessingRuns(): Int = newSuspendedTransaction {
        recoverStaleProcessingRunsInternal(tenantId = null)
    }

    /**
     * Delete all runs for a document.
     * CRITICAL: Must filter by tenantId.
     */
    suspend fun deleteByDocument(
        documentId: DocumentId,
        tenantId: TenantId
    ): Int = newSuspendedTransaction {
        DocumentIngestionRunsTable.deleteWhere {
            (DocumentIngestionRunsTable.documentId eq UUID.fromString(documentId.toString())) and
                    (DocumentIngestionRunsTable.tenantId eq UUID.fromString(tenantId.toString()))
        }
    }

    private fun ResultRow.toIngestionRunSummary(): IngestionRunSummary {
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

    private fun recoverStaleProcessingRunsInternal(tenantId: UUID?): Int {
        val timeout = DocumentProcessingConstants.INGESTION_RUN_TIMEOUT
        val cutoff = (Clock.System.now() - timeout).toLocalDateTime(TimeZone.UTC)
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)

        val staleCondition = if (tenantId != null) {
            (DocumentIngestionRunsTable.status eq IngestionStatus.Processing) and
                (DocumentIngestionRunsTable.startedAt.isNull() or
                    (DocumentIngestionRunsTable.startedAt lessEq cutoff)) and
                (DocumentIngestionRunsTable.tenantId eq tenantId)
        } else {
            (DocumentIngestionRunsTable.status eq IngestionStatus.Processing) and
                (DocumentIngestionRunsTable.startedAt.isNull() or
                    (DocumentIngestionRunsTable.startedAt lessEq cutoff))
        }

        return DocumentIngestionRunsTable.update({ staleCondition }) {
            it[status] = IngestionStatus.Failed
            it[finishedAt] = now
            it[errorMessage] = DocumentProcessingConstants.ingestionTimeoutErrorMessage()
        }
    }
}
