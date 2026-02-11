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
)

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
            .map { it.toDto() }
            .singleOrNull()
    }

    /**
     * Get all links from a source document.
     * CRITICAL: Must filter by tenantId.
     */
    suspend fun getBySourceDocumentId(
        tenantId: TenantId,
        sourceDocumentId: DocumentId
    ): List<DocumentLinkDto> = newSuspendedTransaction {
        DocumentLinksTable.selectAll()
            .where {
                (DocumentLinksTable.tenantId eq UUID.fromString(tenantId.toString())) and
                    (DocumentLinksTable.sourceDocumentId eq UUID.fromString(sourceDocumentId.toString()))
            }
            .map { it.toDto() }
    }

    /**
     * Get all links to a target document.
     * CRITICAL: Must filter by tenantId.
     */
    suspend fun getByTargetDocumentId(
        tenantId: TenantId,
        targetDocumentId: DocumentId
    ): List<DocumentLinkDto> = newSuspendedTransaction {
        DocumentLinksTable.selectAll()
            .where {
                (DocumentLinksTable.tenantId eq UUID.fromString(tenantId.toString())) and
                    (DocumentLinksTable.targetDocumentId eq UUID.fromString(targetDocumentId.toString()))
            }
            .map { it.toDto() }
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
            .map { it.toDto() }
    }

    /**
     * Check if a ConvertedTo link exists from a document.
     * Use this to check if a ProForma has already been converted to an Invoice.
     * CRITICAL: Must filter by tenantId.
     */
    suspend fun hasConversionLink(
        tenantId: TenantId,
        sourceDocumentId: DocumentId
    ): Boolean = newSuspendedTransaction {
        DocumentLinksTable.selectAll()
            .where {
                (DocumentLinksTable.tenantId eq UUID.fromString(tenantId.toString())) and
                    (DocumentLinksTable.sourceDocumentId eq UUID.fromString(sourceDocumentId.toString())) and
                    (DocumentLinksTable.linkType eq DocumentLinkType.ConvertedTo)
            }
            .count() > 0
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

    /**
     * Delete all links from a source document.
     * CRITICAL: Must filter by tenantId.
     *
     * @return Number of deleted links
     */
    suspend fun deleteBySourceDocumentId(
        tenantId: TenantId,
        sourceDocumentId: DocumentId
    ): Int = newSuspendedTransaction {
        DocumentLinksTable.deleteWhere {
            (DocumentLinksTable.tenantId eq UUID.fromString(tenantId.toString())) and
                (DocumentLinksTable.sourceDocumentId eq UUID.fromString(sourceDocumentId.toString()))
        }
    }

    private fun ResultRow.toDto(): DocumentLinkDto {
        return DocumentLinkDto(
            id = DocumentLinkId.parse(this[DocumentLinksTable.id].toString()),
            tenantId = TenantId(this[DocumentLinksTable.tenantId].toKotlinUuid()),
            sourceDocumentId = DocumentId.parse(this[DocumentLinksTable.sourceDocumentId].toString()),
            targetDocumentId = this[DocumentLinksTable.targetDocumentId]?.let {
                DocumentId.parse(it.toString())
            },
            externalReference = this[DocumentLinksTable.externalReference],
            linkType = this[DocumentLinksTable.linkType],
            createdAt = this[DocumentLinksTable.createdAt]
        )
    }
}
