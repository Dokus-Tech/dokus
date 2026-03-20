package tech.dokus.database.repository.documents

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import tech.dokus.database.tables.documents.DocumentLinksTable
import tech.dokus.domain.enums.DocumentLinkType
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.DocumentLinkId
import tech.dokus.domain.ids.TenantId
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toKotlinUuid

/**
 * Data class representing a document link.
 */
data class DocumentLinkDto(
    val id: DocumentLinkId,
    val tenantId: TenantId,
    val sourceDocumentId: DocumentId,
    val targetDocumentId: DocumentId?,
    val externalReference: String?,
    val linkType: DocumentLinkType,
    val createdAt: LocalDateTime
) {
    companion object
}

/**
 * Payload for creating a document link.
 */
data class CreateDocumentLinkPayload(
    val sourceDocumentId: DocumentId,
    val targetDocumentId: DocumentId? = null,
    val externalReference: String? = null,
    val linkType: DocumentLinkType
) {
    init {
        // ConvertedTo MUST have targetDocumentId
        if (linkType == DocumentLinkType.ConvertedTo) {
            require(targetDocumentId != null) {
                "ConvertedTo link type requires a targetDocumentId"
            }
        }
        // OriginalDocument must have either targetDocumentId OR externalReference
        if (linkType == DocumentLinkType.OriginalDocument) {
            require(targetDocumentId != null || !externalReference.isNullOrBlank()) {
                "OriginalDocument link type requires either targetDocumentId or externalReference"
            }
        }
    }
}

/**
 * Repository for document links.
 *
 * Links track document-to-document relationships:
 * - ConvertedTo: ProForma → Invoice conversion
 * - OriginalDocument: CreditNote → Original Invoice reference
 * - RelatedTo: Generic document relationship
 *
 * CRITICAL: All queries MUST filter by tenantId for tenant isolation.
 */
@OptIn(ExperimentalUuidApi::class)
class DocumentLinkRepository {

    /**
     * Create a document link.
     * CRITICAL: Must filter by tenantId.
     */
    suspend fun create(
        tenantId: TenantId,
        payload: CreateDocumentLinkPayload
    ): DocumentLinkId = newSuspendedTransaction {
        val id = DocumentLinkId.generate()
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)

        DocumentLinksTable.insert {
            it[DocumentLinksTable.id] = UUID.fromString(id.toString())
            it[DocumentLinksTable.tenantId] = UUID.fromString(tenantId.toString())
            it[sourceDocumentId] = UUID.fromString(payload.sourceDocumentId.toString())
            it[targetDocumentId] = payload.targetDocumentId?.let { docId ->
                UUID.fromString(docId.toString())
            }
            it[externalReference] = payload.externalReference
            it[linkType] = payload.linkType
            it[createdAt] = now
        }
        id
    }

    /**
     * Upsert an OriginalDocument link for a source document.
     *
     * Guarantees one canonical OriginalDocument link per source document.
     * - If the same semantic link already exists, returns the existing link ID.
     * - Otherwise, replaces existing OriginalDocument links and creates one canonical link.
     */
    suspend fun upsertOriginalDocumentLink(
        tenantId: TenantId,
        sourceDocumentId: DocumentId,
        targetDocumentId: DocumentId?,
        externalReference: String?
    ): DocumentLinkId = newSuspendedTransaction {
        val normalizedExternalReference = externalReference?.trim()?.takeIf { it.isNotEmpty() }
        require(targetDocumentId != null || normalizedExternalReference != null) {
            "OriginalDocument link requires targetDocumentId or externalReference"
        }

        val tenantUuid = UUID.fromString(tenantId.toString())
        val sourceDocumentUuid = UUID.fromString(sourceDocumentId.toString())
        val targetDocumentUuid = targetDocumentId?.let { UUID.fromString(it.toString()) }

        val baseWhere = (DocumentLinksTable.tenantId eq tenantUuid) and
            (DocumentLinksTable.sourceDocumentId eq sourceDocumentUuid) and
            (DocumentLinksTable.linkType eq DocumentLinkType.OriginalDocument)

        val existing = DocumentLinksTable
            .selectAll()
            .where { baseWhere }
            .toList()

        val matching = existing.firstOrNull { row ->
            row[DocumentLinksTable.targetDocumentId] == targetDocumentUuid &&
                row[DocumentLinksTable.externalReference] == normalizedExternalReference
        }

        if (matching != null) {
            return@newSuspendedTransaction DocumentLinkId.parse(matching[DocumentLinksTable.id].toString())
        }

        if (existing.isNotEmpty()) {
            DocumentLinksTable.deleteWhere { baseWhere }
        }

        val linkId = DocumentLinkId.generate()
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)

        DocumentLinksTable.insert {
            it[DocumentLinksTable.id] = UUID.fromString(linkId.toString())
            it[DocumentLinksTable.tenantId] = tenantUuid
            it[DocumentLinksTable.sourceDocumentId] = sourceDocumentUuid
            it[DocumentLinksTable.targetDocumentId] = targetDocumentUuid
            it[DocumentLinksTable.externalReference] = normalizedExternalReference
            it[DocumentLinksTable.linkType] = DocumentLinkType.OriginalDocument
            it[DocumentLinksTable.createdAt] = now
        }

        linkId
    }

    /**
     * Get a link by ID.
     * CRITICAL: Must filter by tenantId.
     */
    suspend fun getById(
        tenantId: TenantId,
        linkId: DocumentLinkId
    ): DocumentLinkDto? = newSuspendedTransaction {
        DocumentLinksTable.selectAll()
            .where {
                (DocumentLinksTable.tenantId eq UUID.fromString(tenantId.toString())) and
                    (DocumentLinksTable.id eq UUID.fromString(linkId.toString()))
            }
            .map { DocumentLinkDto.from(it) }
            .singleOrNull()
    }

    /**
     * Get links by type from a source document.
     * CRITICAL: Must filter by tenantId.
     */
    suspend fun getBySourceAndType(
        tenantId: TenantId,
        sourceDocumentId: DocumentId,
        linkType: DocumentLinkType
    ): List<DocumentLinkDto> = newSuspendedTransaction {
        DocumentLinksTable.selectAll()
            .where {
                (DocumentLinksTable.tenantId eq UUID.fromString(tenantId.toString())) and
                    (DocumentLinksTable.sourceDocumentId eq UUID.fromString(sourceDocumentId.toString())) and
                    (DocumentLinksTable.linkType eq linkType)
            }
            .map { DocumentLinkDto.from(it) }
    }

    /**
     * Delete a link.
     * CRITICAL: Must filter by tenantId.
     */
    suspend fun delete(
        tenantId: TenantId,
        linkId: DocumentLinkId
    ): Boolean = newSuspendedTransaction {
        DocumentLinksTable.deleteWhere {
            (DocumentLinksTable.tenantId eq UUID.fromString(tenantId.toString())) and
                (DocumentLinksTable.id eq UUID.fromString(linkId.toString()))
        } > 0
    }

    private fun DocumentLinkDto.Companion.from(row: ResultRow): DocumentLinkDto {
        return DocumentLinkDto(
            id = DocumentLinkId.parse(row[DocumentLinksTable.id].toString()),
            tenantId = TenantId(row[DocumentLinksTable.tenantId].toKotlinUuid()),
            sourceDocumentId = DocumentId.parse(row[DocumentLinksTable.sourceDocumentId].toString()),
            targetDocumentId = row[DocumentLinksTable.targetDocumentId]?.let {
                DocumentId.parse(it.toString())
            },
            externalReference = row[DocumentLinksTable.externalReference],
            linkType = row[DocumentLinksTable.linkType],
            createdAt = row[DocumentLinksTable.createdAt]
        )
    }
}
