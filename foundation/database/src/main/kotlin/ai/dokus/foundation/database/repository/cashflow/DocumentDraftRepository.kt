package ai.dokus.foundation.database.repository.cashflow

import ai.dokus.foundation.database.tables.cashflow.DocumentDraftsTable
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.enums.DraftStatus
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.IngestionRunId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.UserId
import tech.dokus.domain.model.ExtractedDocumentData
import tech.dokus.domain.model.TrackedCorrection
import tech.dokus.domain.repository.DraftStatusChecker
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
import org.jetbrains.exposed.v1.jdbc.update
import org.jetbrains.exposed.v1.jdbc.upsert
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toKotlinUuid

/**
 * Data class for draft summary.
 */
data class DraftSummary(
    val documentId: DocumentId,
    val tenantId: TenantId,
    val draftStatus: DraftStatus,
    val documentType: DocumentType?,
    val extractedData: ExtractedDocumentData?,
    val aiDraftData: ExtractedDocumentData?,
    val aiDraftSourceRunId: IngestionRunId?,
    val draftVersion: Int,
    val draftEditedAt: LocalDateTime?,
    val draftEditedBy: UserId?,
    val suggestedContactId: ContactId?,
    val contactSuggestionConfidence: Float?,
    val contactSuggestionReason: String?,
    val lastSuccessfulRunId: IngestionRunId?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

/**
 * Repository for document draft operations.
 * CRITICAL: All queries filter by tenantId for security.
 *
 * Implements DraftStatusChecker for chat confirmation checks.
 */
@OptIn(ExperimentalUuidApi::class)
class DocumentDraftRepository : DraftStatusChecker {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /**
     * Create or update a draft from an ingestion run result.
     *
     * Rules:
     * - ai_draft_data + ai_draft_source_run_id: Set ONLY if null (from first successful run, immutable)
     * - extracted_data: Set only if force=true OR draftVersion == 0 (not user-edited)
     *
     * CRITICAL: Must provide tenantId.
     */
    suspend fun createOrUpdateFromIngestion(
        documentId: DocumentId,
        tenantId: TenantId,
        runId: IngestionRunId,
        extractedData: ExtractedDocumentData,
        documentType: DocumentType,
        force: Boolean = false
    ): Boolean = newSuspendedTransaction {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        val docIdUuid = java.util.UUID.fromString(documentId.toString())
        val tenantIdUuid = java.util.UUID.fromString(tenantId.toString())
        val runIdUuid = java.util.UUID.fromString(runId.toString())

        // Check if draft exists and get current state
        val existing = DocumentDraftsTable.selectAll()
            .where {
                (DocumentDraftsTable.documentId eq docIdUuid) and
                (DocumentDraftsTable.tenantId eq tenantIdUuid)
            }
            .singleOrNull()

        if (existing == null) {
            // Create new draft
            DocumentDraftsTable.insert {
                it[DocumentDraftsTable.documentId] = docIdUuid
                it[DocumentDraftsTable.tenantId] = tenantIdUuid
                it[draftStatus] = DraftStatus.NeedsReview
                it[DocumentDraftsTable.documentType] = documentType
                it[aiDraftData] = json.encodeToString(extractedData)
                it[aiDraftSourceRunId] = runIdUuid
                it[DocumentDraftsTable.extractedData] = json.encodeToString(extractedData)
                it[lastSuccessfulRunId] = runIdUuid
                it[createdAt] = now
                it[updatedAt] = now
            }
            true
        } else {
            // Update existing draft
            val currentVersion = existing[DocumentDraftsTable.draftVersion]
            val hasAiDraft = existing[DocumentDraftsTable.aiDraftData] != null
            val shouldUpdateExtracted = force || currentVersion == 0

            DocumentDraftsTable.update({
                (DocumentDraftsTable.documentId eq docIdUuid) and
                (DocumentDraftsTable.tenantId eq tenantIdUuid)
            }) {
                // ai_draft_data: set only if null (immutable)
                if (!hasAiDraft) {
                    it[aiDraftData] = json.encodeToString(extractedData)
                    it[aiDraftSourceRunId] = runIdUuid
                }

                // extracted_data: update only if force or not user-edited
                if (shouldUpdateExtracted) {
                    it[DocumentDraftsTable.extractedData] = json.encodeToString(extractedData)
                    it[DocumentDraftsTable.documentType] = documentType
                    it[draftStatus] = DraftStatus.NeedsReview
                }

                it[lastSuccessfulRunId] = runIdUuid
                it[updatedAt] = now
            } > 0
        }
    }

    /**
     * Get a draft by document ID.
     * CRITICAL: Must filter by tenantId.
     */
    suspend fun getByDocumentId(
        documentId: DocumentId,
        tenantId: TenantId
    ): DraftSummary? = newSuspendedTransaction {
        DocumentDraftsTable.selectAll()
            .where {
                (DocumentDraftsTable.documentId eq java.util.UUID.fromString(documentId.toString())) and
                (DocumentDraftsTable.tenantId eq java.util.UUID.fromString(tenantId.toString()))
            }
            .map { it.toDraftSummary() }
            .singleOrNull()
    }

    /**
     * Update a draft with user corrections.
     * Increments version, tracks corrections, updates edited timestamp.
     * CRITICAL: Must filter by tenantId.
     *
     * @return The new draft version number, or null if not found
     */
    suspend fun updateDraft(
        documentId: DocumentId,
        tenantId: TenantId,
        userId: UserId,
        updatedData: ExtractedDocumentData,
        corrections: List<TrackedCorrection>
    ): Int? = newSuspendedTransaction {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        val docIdUuid = java.util.UUID.fromString(documentId.toString())
        val tenantIdUuid = java.util.UUID.fromString(tenantId.toString())

        // Get current draft to check version
        val current = DocumentDraftsTable.selectAll()
            .where {
                (DocumentDraftsTable.documentId eq docIdUuid) and
                (DocumentDraftsTable.tenantId eq tenantIdUuid)
            }
            .singleOrNull() ?: return@newSuspendedTransaction null

        val currentVersion = current[DocumentDraftsTable.draftVersion]
        val newVersion = currentVersion + 1

        // Merge corrections with existing
        val existingCorrections = current[DocumentDraftsTable.userCorrections]?.let {
            json.decodeFromString<List<TrackedCorrection>>(it)
        } ?: emptyList()
        val allCorrections = existingCorrections + corrections

        DocumentDraftsTable.update({
            (DocumentDraftsTable.documentId eq docIdUuid) and
            (DocumentDraftsTable.tenantId eq tenantIdUuid)
        }) {
            it[extractedData] = json.encodeToString(updatedData)
            it[userCorrections] = json.encodeToString(allCorrections)
            it[draftVersion] = newVersion
            it[draftEditedAt] = now
            it[draftEditedBy] = java.util.UUID.fromString(userId.toString())
            it[updatedAt] = now
        }

        newVersion
    }

    /**
     * Update draft status.
     * CRITICAL: Must filter by tenantId.
     */
    suspend fun updateDraftStatus(
        documentId: DocumentId,
        tenantId: TenantId,
        status: DraftStatus
    ): Boolean = newSuspendedTransaction {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        DocumentDraftsTable.update({
            (DocumentDraftsTable.documentId eq java.util.UUID.fromString(documentId.toString())) and
            (DocumentDraftsTable.tenantId eq java.util.UUID.fromString(tenantId.toString()))
        }) {
            it[draftStatus] = status
            it[updatedAt] = now
        } > 0
    }

    /**
     * List drafts by status with optional document type filter.
     * CRITICAL: Must filter by tenantId.
     *
     * @return Pair of (drafts, totalCount)
     */
    suspend fun listByStatus(
        tenantId: TenantId,
        statuses: List<DraftStatus>,
        documentType: DocumentType? = null,
        page: Int = 0,
        limit: Int = 20
    ): Pair<List<DraftSummary>, Long> = newSuspendedTransaction {
        val tenantIdUuid = java.util.UUID.fromString(tenantId.toString())

        val baseQuery = DocumentDraftsTable.selectAll()
            .where {
                val conditions = (DocumentDraftsTable.tenantId eq tenantIdUuid) and
                    (DocumentDraftsTable.draftStatus inList statuses)
                if (documentType != null) {
                    conditions and (DocumentDraftsTable.documentType eq documentType)
                } else {
                    conditions
                }
            }

        val total = baseQuery.count()

        val drafts = baseQuery
            .orderBy(DocumentDraftsTable.updatedAt, SortOrder.DESC)
            .limit(limit)
            .offset((page * limit).toLong())
            .map { it.toDraftSummary() }

        drafts to total
    }

    /**
     * Update contact suggestion.
     * CRITICAL: Must filter by tenantId.
     */
    suspend fun updateContactSuggestion(
        documentId: DocumentId,
        tenantId: TenantId,
        contactId: ContactId?,
        confidence: Float?,
        reason: String?
    ): Boolean = newSuspendedTransaction {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        DocumentDraftsTable.update({
            (DocumentDraftsTable.documentId eq java.util.UUID.fromString(documentId.toString())) and
            (DocumentDraftsTable.tenantId eq java.util.UUID.fromString(tenantId.toString()))
        }) {
            it[suggestedContactId] = contactId?.let { id -> java.util.UUID.fromString(id.toString()) }
            it[contactSuggestionConfidence] = confidence
            it[contactSuggestionReason] = reason
            it[updatedAt] = now
        } > 0
    }

    /**
     * Delete a draft.
     * CRITICAL: Must filter by tenantId.
     */
    suspend fun delete(
        documentId: DocumentId,
        tenantId: TenantId
    ): Boolean = newSuspendedTransaction {
        DocumentDraftsTable.deleteWhere {
            (DocumentDraftsTable.documentId eq java.util.UUID.fromString(documentId.toString())) and
            (DocumentDraftsTable.tenantId eq java.util.UUID.fromString(tenantId.toString()))
        } > 0
    }

    // =========================================================================
    // DraftStatusChecker Implementation
    // =========================================================================

    /**
     * Check if a document has been confirmed by the user.
     * Used by ChatAgent to enforce chat-only-for-confirmed policy.
     *
     * CRITICAL: Must filter by tenantId for multi-tenant security.
     */
    override suspend fun isConfirmed(
        tenantId: TenantId,
        documentId: DocumentId
    ): Boolean = newSuspendedTransaction {
        val draft = DocumentDraftsTable.selectAll()
            .where {
                (DocumentDraftsTable.documentId eq java.util.UUID.fromString(documentId.toString())) and
                (DocumentDraftsTable.tenantId eq java.util.UUID.fromString(tenantId.toString()))
            }
            .singleOrNull()

        draft?.get(DocumentDraftsTable.draftStatus) == DraftStatus.Confirmed
    }

    private fun ResultRow.toDraftSummary(): DraftSummary {
        return DraftSummary(
            documentId = DocumentId.parse(this[DocumentDraftsTable.documentId].toString()),
            tenantId = TenantId(this[DocumentDraftsTable.tenantId].toKotlinUuid()),
            draftStatus = this[DocumentDraftsTable.draftStatus],
            documentType = this[DocumentDraftsTable.documentType],
            extractedData = this[DocumentDraftsTable.extractedData]?.let { json.decodeFromString(it) },
            aiDraftData = this[DocumentDraftsTable.aiDraftData]?.let { json.decodeFromString(it) },
            aiDraftSourceRunId = this[DocumentDraftsTable.aiDraftSourceRunId]?.let { IngestionRunId.parse(it.toString()) },
            draftVersion = this[DocumentDraftsTable.draftVersion],
            draftEditedAt = this[DocumentDraftsTable.draftEditedAt],
            draftEditedBy = this[DocumentDraftsTable.draftEditedBy]?.let { UserId(it.toKotlinUuid()) },
            suggestedContactId = this[DocumentDraftsTable.suggestedContactId]?.let { ContactId(it.toKotlinUuid()) },
            contactSuggestionConfidence = this[DocumentDraftsTable.contactSuggestionConfidence],
            contactSuggestionReason = this[DocumentDraftsTable.contactSuggestionReason],
            lastSuccessfulRunId = this[DocumentDraftsTable.lastSuccessfulRunId]?.let { IngestionRunId.parse(it.toString()) },
            createdAt = this[DocumentDraftsTable.createdAt],
            updatedAt = this[DocumentDraftsTable.updatedAt]
        )
    }
}
