package ai.dokus.foundation.database.repository.cashflow

import ai.dokus.foundation.database.entity.IngestionItemEntity
import ai.dokus.foundation.database.tables.cashflow.DocumentIngestionRunsTable
import ai.dokus.foundation.database.tables.cashflow.DocumentsTable
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.v1.jdbc.update
import tech.dokus.domain.enums.IngestionStatus
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.IngestionRunId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.ExtractedDocumentData
import kotlin.uuid.ExperimentalUuidApi
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
    val confidence: Double?
)

/**
 * Repository for document ingestion run operations.
 * CRITICAL: All queries filter by tenantId for security.
 */
@OptIn(ExperimentalUuidApi::class)
class DocumentIngestionRunRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /**
     * Create a new ingestion run with status=Queued.
     * CRITICAL: Must provide tenantId.
     *
     * @param documentId Document to process
     * @param tenantId Tenant owning the document
     * @param overrideMaxPages Optional override for max pages (null = use default)
     * @param overrideDpi Optional override for DPI (null = use default)
     * @param overrideTimeoutSeconds Optional override for timeout (null = use default)
     */
    suspend fun createRun(
        documentId: DocumentId,
        tenantId: TenantId,
        overrideMaxPages: Int? = null,
        overrideDpi: Int? = null,
        overrideTimeoutSeconds: Int? = null
    ): IngestionRunId = newSuspendedTransaction {
        val id = IngestionRunId.generate()
        DocumentIngestionRunsTable.insert {
            it[DocumentIngestionRunsTable.id] = java.util.UUID.fromString(id.toString())
            it[DocumentIngestionRunsTable.documentId] = java.util.UUID.fromString(documentId.toString())
            it[DocumentIngestionRunsTable.tenantId] = java.util.UUID.fromString(tenantId.toString())
            it[status] = IngestionStatus.Queued
            it[DocumentIngestionRunsTable.overrideMaxPages] = overrideMaxPages
            it[DocumentIngestionRunsTable.overrideDpi] = overrideDpi
            it[DocumentIngestionRunsTable.overrideTimeoutSeconds] = overrideTimeoutSeconds
        }
        id
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
                (DocumentIngestionRunsTable.id eq java.util.UUID.fromString(runId.toString())) and
                (DocumentIngestionRunsTable.tenantId eq java.util.UUID.fromString(tenantId.toString()))
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
        DocumentIngestionRunsTable.selectAll()
            .where {
                (DocumentIngestionRunsTable.documentId eq java.util.UUID.fromString(documentId.toString())) and
                (DocumentIngestionRunsTable.tenantId eq java.util.UUID.fromString(tenantId.toString()))
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
        val docIdUuid = java.util.UUID.fromString(documentId.toString())
        val tenantIdUuid = java.util.UUID.fromString(tenantId.toString())

        // First, check for Processing status
        val processing = DocumentIngestionRunsTable.selectAll()
            .where {
                (DocumentIngestionRunsTable.documentId eq docIdUuid) and
                (DocumentIngestionRunsTable.tenantId eq tenantIdUuid) and
                (DocumentIngestionRunsTable.status eq IngestionStatus.Processing)
            }
            .map { it.toIngestionRunSummary() }
            .firstOrNull()

        if (processing != null) return@newSuspendedTransaction processing

        // Then, check for latest Succeeded/Failed by finishedAt
        val finished = DocumentIngestionRunsTable.selectAll()
            .where {
                (DocumentIngestionRunsTable.documentId eq docIdUuid) and
                (DocumentIngestionRunsTable.tenantId eq tenantIdUuid) and
                (DocumentIngestionRunsTable.status inList listOf(IngestionStatus.Succeeded, IngestionStatus.Failed))
            }
            .orderBy(DocumentIngestionRunsTable.finishedAt, SortOrder.DESC_NULLS_LAST)
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
            .orderBy(DocumentIngestionRunsTable.queuedAt, SortOrder.DESC)
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
        DocumentIngestionRunsTable.selectAll()
            .where {
                (DocumentIngestionRunsTable.documentId eq java.util.UUID.fromString(documentId.toString())) and
                (DocumentIngestionRunsTable.tenantId eq java.util.UUID.fromString(tenantId.toString())) and
                (DocumentIngestionRunsTable.status inList listOf(IngestionStatus.Queued, IngestionStatus.Processing))
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
                    runId = row[DocumentIngestionRunsTable.id].value.toString(),
                    documentId = row[DocumentIngestionRunsTable.documentId].toString(),
                    tenantId = row[DocumentIngestionRunsTable.tenantId].toString(),
                    storageKey = row[DocumentsTable.storageKey],
                    filename = row[DocumentsTable.filename],
                    contentType = row[DocumentsTable.contentType],
                    overrideMaxPages = row[DocumentIngestionRunsTable.overrideMaxPages],
                    overrideDpi = row[DocumentIngestionRunsTable.overrideDpi],
                    overrideTimeoutSeconds = row[DocumentIngestionRunsTable.overrideTimeoutSeconds]
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
            DocumentIngestionRunsTable.id eq java.util.UUID.fromString(runId.toString())
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
        extractedData: ExtractedDocumentData?,
        confidence: Double?
    ): Boolean = newSuspendedTransaction {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        DocumentIngestionRunsTable.update({
            DocumentIngestionRunsTable.id eq java.util.UUID.fromString(runId.toString())
        }) {
            it[status] = IngestionStatus.Succeeded
            it[finishedAt] = now
            it[DocumentIngestionRunsTable.rawText] = rawText
            it[rawExtractionJson] = extractedData?.let { data -> json.encodeToString(data) }
            it[DocumentIngestionRunsTable.confidence] = confidence?.toBigDecimal()
            it[fieldConfidences] = extractedData?.fieldConfidences?.let { fc -> json.encodeToString(fc) }
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
            DocumentIngestionRunsTable.id eq java.util.UUID.fromString(runId.toString())
        }) {
            it[status] = IngestionStatus.Failed
            it[finishedAt] = now
            it[errorMessage] = error
        } > 0
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
            (DocumentIngestionRunsTable.documentId eq java.util.UUID.fromString(documentId.toString())) and
            (DocumentIngestionRunsTable.tenantId eq java.util.UUID.fromString(tenantId.toString()))
        }
    }

    /**
     * Get raw extraction JSON for a run.
     * Used by draft repository to populate draft data.
     */
    suspend fun getRawExtraction(
        runId: IngestionRunId,
        tenantId: TenantId
    ): ExtractedDocumentData? = newSuspendedTransaction {
        DocumentIngestionRunsTable.selectAll()
            .where {
                (DocumentIngestionRunsTable.id eq java.util.UUID.fromString(runId.toString())) and
                (DocumentIngestionRunsTable.tenantId eq java.util.UUID.fromString(tenantId.toString()))
            }
            .map { row ->
                row[DocumentIngestionRunsTable.rawExtractionJson]?.let { jsonStr ->
                    json.decodeFromString<ExtractedDocumentData>(jsonStr)
                }
            }
            .singleOrNull()
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
            confidence = this[DocumentIngestionRunsTable.confidence]?.toDouble()
        )
    }
}
