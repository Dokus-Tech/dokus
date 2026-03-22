package tech.dokus.database.repository.cashflow

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.v1.jdbc.update
import tech.dokus.database.entity.IngestionRunSummaryEntity
import tech.dokus.database.mapper.from
import tech.dokus.database.tables.documents.DocumentIngestionRunsTable
import tech.dokus.domain.enums.IngestionStatus
import tech.dokus.domain.enums.ProcessingOutcome
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.DocumentSourceId
import tech.dokus.domain.ids.IngestionRunId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.Dpi
import tech.dokus.domain.processing.DocumentProcessingConstants
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid

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
        overrideDpi: Dpi? = null,
    ): IngestionRunId = newSuspendedTransaction {
        val sanitizedMaxPages = overrideMaxPages?.takeIf { it > 0 }
        return@newSuspendedTransaction IngestionRunId.generate().also { id ->
            DocumentIngestionRunsTable.insert {
                it[DocumentIngestionRunsTable.id] = id.value.toJavaUuid()
                it[DocumentIngestionRunsTable.documentId] = documentId.value.toJavaUuid()
                it[DocumentIngestionRunsTable.tenantId] = tenantId.value.toJavaUuid()
                it[DocumentIngestionRunsTable.sourceId] = sourceId?.value?.toJavaUuid()
                it[status] = IngestionStatus.Queued
                it[DocumentIngestionRunsTable.userFeedback] = userFeedback?.takeIf { fb -> fb.isNotBlank() }
                it[DocumentIngestionRunsTable.overrideMaxPages] = sanitizedMaxPages
                it[DocumentIngestionRunsTable.overrideDpi] = overrideDpi?.value
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
    ): IngestionRunSummaryEntity? = newSuspendedTransaction {
        DocumentIngestionRunsTable.selectAll()
            .where {
                (DocumentIngestionRunsTable.id eq UUID.fromString(runId.toString())) and
                        (DocumentIngestionRunsTable.tenantId eq UUID.fromString(tenantId.toString()))
            }
            .map { IngestionRunSummaryEntity.from(it) }
            .singleOrNull()
    }

    /**
     * List all ingestion runs for a document.
     * CRITICAL: Must filter by tenantId.
     */
    suspend fun listByDocument(
        documentId: DocumentId,
        tenantId: TenantId
    ): List<IngestionRunSummaryEntity> = newSuspendedTransaction {
        val tenantIdUuid = UUID.fromString(tenantId.toString())
        recoverStaleProcessingRunsInternal(tenantIdUuid)

        DocumentIngestionRunsTable.selectAll()
            .where {
                (DocumentIngestionRunsTable.documentId eq UUID.fromString(documentId.toString())) and
                        (DocumentIngestionRunsTable.tenantId eq tenantIdUuid)
            }
            .orderBy(DocumentIngestionRunsTable.queuedAt, SortOrder.DESC)
            .map { IngestionRunSummaryEntity.from(it) }
    }

    /**
     * Get the latest ingestion run for a document.
     * Priority: Processing > latest Succeeded/Failed (by finishedAt) > latest Queued (by queuedAt)
     * CRITICAL: Must filter by tenantId.
     */
    suspend fun getLatestForDocument(
        documentId: DocumentId,
        tenantId: TenantId
    ): IngestionRunSummaryEntity? = newSuspendedTransaction {
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
            .map { IngestionRunSummaryEntity.from(it) }
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
            .map { IngestionRunSummaryEntity.from(it) }
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
            .map { IngestionRunSummaryEntity.from(it) }
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
    ): IngestionRunSummaryEntity? = newSuspendedTransaction {
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
            .map { IngestionRunSummaryEntity.from(it) }
            .firstOrNull()
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
                ProcessingOutcome.HighConfidence
            } else {
                ProcessingOutcome.LowConfidence
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

    suspend fun recoverStaleProcessingRunsForTenant(tenantId: TenantId): Int = newSuspendedTransaction {
        recoverStaleProcessingRunsInternal(UUID.fromString(tenantId.toString()))
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
            it[errorMessage] = DocumentProcessingConstants.INGESTION_TIMEOUT_ERROR_MESSAGE
        }
    }
}
