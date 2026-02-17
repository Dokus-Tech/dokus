package tech.dokus.database.repository.documents
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.v1.jdbc.update
import tech.dokus.database.tables.documents.DocumentLineItemsTable
import tech.dokus.domain.enums.Currency
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.DocumentLineItemId
import tech.dokus.domain.ids.TenantId
import java.math.BigDecimal
import kotlin.time.Clock

/**
 * Data class representing a document line item.
 */
data class DocumentLineItemDto(
    val id: DocumentLineItemId,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val position: Int,
    val description: String,
    val quantity: BigDecimal?,
    val unitPrice: BigDecimal?,
    val netAmount: BigDecimal,
    val vatRate: BigDecimal?,
    val vatAmount: BigDecimal?,
    val grossAmount: BigDecimal,
    val currency: Currency,
    val isSynthetic: Boolean
)

/**
 * Payload for creating a document line item.
 */
data class CreateLineItemPayload(
    val position: Int,
    val description: String,
    val quantity: BigDecimal? = null,
    val unitPrice: BigDecimal? = null,
    val netAmount: BigDecimal,
    val vatRate: BigDecimal? = null,
    val vatAmount: BigDecimal? = null,
    val grossAmount: BigDecimal,
    val currency: Currency,
    val isSynthetic: Boolean = false
)

/**
 * Repository for document line items.
 *
 * Line items are generic across all document types (Invoice, Receipt, ProForma, CreditNote, etc.).
 * They are keyed by document_id + tenant_id.
 *
 * CRITICAL: All queries MUST filter by tenantId for tenant isolation.
 */
class DocumentLineItemRepository {

    /**
     * Create a single line item for a document.
     * CRITICAL: Must filter by tenantId.
     */
    suspend fun create(
        tenantId: TenantId,
        documentId: DocumentId,
        payload: CreateLineItemPayload
    ): DocumentLineItemId = newSuspendedTransaction {
        val id = DocumentLineItemId.generate()
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)

        DocumentLineItemsTable.insert {
            it[DocumentLineItemsTable.id] = id.value
            it[DocumentLineItemsTable.tenantId] = tenantId.value
            it[DocumentLineItemsTable.documentId] = documentId.value
            it[position] = payload.position
            it[description] = payload.description
            it[quantity] = payload.quantity
            it[unitPrice] = payload.unitPrice
            it[netAmount] = payload.netAmount
            it[vatRate] = payload.vatRate
            it[vatAmount] = payload.vatAmount
            it[grossAmount] = payload.grossAmount
            it[currency] = payload.currency
            it[isSynthetic] = payload.isSynthetic
            it[createdAt] = now
            it[updatedAt] = now
        }
        id
    }

    /**
     * Batch create line items for a document.
     * CRITICAL: Must filter by tenantId.
     */
    suspend fun batchCreate(
        tenantId: TenantId,
        documentId: DocumentId,
        items: List<CreateLineItemPayload>
    ): List<DocumentLineItemId> = newSuspendedTransaction {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        val tenantIdUuid = tenantId.value
        val documentIdUuid = documentId.value

        val ids = items.map { DocumentLineItemId.generate() }

        DocumentLineItemsTable.batchInsert(items.zip(ids)) { (payload, id) ->
            this[DocumentLineItemsTable.id] = id.value
            this[DocumentLineItemsTable.tenantId] = tenantIdUuid
            this[DocumentLineItemsTable.documentId] = documentIdUuid
            this[DocumentLineItemsTable.position] = payload.position
            this[DocumentLineItemsTable.description] = payload.description
            this[DocumentLineItemsTable.quantity] = payload.quantity
            this[DocumentLineItemsTable.unitPrice] = payload.unitPrice
            this[DocumentLineItemsTable.netAmount] = payload.netAmount
            this[DocumentLineItemsTable.vatRate] = payload.vatRate
            this[DocumentLineItemsTable.vatAmount] = payload.vatAmount
            this[DocumentLineItemsTable.grossAmount] = payload.grossAmount
            this[DocumentLineItemsTable.currency] = payload.currency
            this[DocumentLineItemsTable.isSynthetic] = payload.isSynthetic
            this[DocumentLineItemsTable.createdAt] = now
            this[DocumentLineItemsTable.updatedAt] = now
        }
        ids
    }

    /**
     * Get all line items for a document, ordered by position.
     * CRITICAL: Must filter by tenantId.
     */
    suspend fun getByDocumentId(
        tenantId: TenantId,
        documentId: DocumentId
    ): List<DocumentLineItemDto> = newSuspendedTransaction {
        DocumentLineItemsTable.selectAll()
            .where {
                (DocumentLineItemsTable.tenantId eq tenantId.value) and
                    (DocumentLineItemsTable.documentId eq documentId.value)
            }
            .orderBy(DocumentLineItemsTable.position, SortOrder.ASC)
            .map { it.toDto() }
    }

    /**
     * Get a single line item by ID.
     * CRITICAL: Must filter by tenantId.
     */
    suspend fun getById(
        tenantId: TenantId,
        lineItemId: DocumentLineItemId
    ): DocumentLineItemDto? = newSuspendedTransaction {
        DocumentLineItemsTable.selectAll()
            .where {
                (DocumentLineItemsTable.tenantId eq tenantId.value) and
                    (DocumentLineItemsTable.id eq lineItemId.value)
            }
            .map { it.toDto() }
            .singleOrNull()
    }

    /**
     * Delete all line items for a document.
     * CRITICAL: Must filter by tenantId.
     *
     * @return Number of deleted items
     */
    suspend fun deleteByDocumentId(
        tenantId: TenantId,
        documentId: DocumentId
    ): Int = newSuspendedTransaction {
        DocumentLineItemsTable.deleteWhere {
            (DocumentLineItemsTable.tenantId eq tenantId.value) and
                (DocumentLineItemsTable.documentId eq documentId.value)
        }
    }

    /**
     * Delete a single line item.
     * CRITICAL: Must filter by tenantId.
     */
    suspend fun delete(
        tenantId: TenantId,
        lineItemId: DocumentLineItemId
    ): Boolean = newSuspendedTransaction {
        DocumentLineItemsTable.deleteWhere {
            (DocumentLineItemsTable.tenantId eq tenantId.value) and
                (DocumentLineItemsTable.id eq lineItemId.value)
        } > 0
    }

    /**
     * Count line items for a document.
     * CRITICAL: Must filter by tenantId.
     */
    suspend fun countByDocumentId(
        tenantId: TenantId,
        documentId: DocumentId
    ): Long = newSuspendedTransaction {
        DocumentLineItemsTable.selectAll()
            .where {
                (DocumentLineItemsTable.tenantId eq tenantId.value) and
                    (DocumentLineItemsTable.documentId eq documentId.value)
            }
            .count()
    }

    /**
     * Update the position of a line item (for reordering).
     * CRITICAL: Must filter by tenantId.
     */
    suspend fun updatePosition(
        tenantId: TenantId,
        lineItemId: DocumentLineItemId,
        newPosition: Int
    ): Boolean = newSuspendedTransaction {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)

        DocumentLineItemsTable.update({
            (DocumentLineItemsTable.tenantId eq tenantId.value) and
                (DocumentLineItemsTable.id eq lineItemId.value)
        }) {
            it[position] = newPosition
            it[updatedAt] = now
        } > 0
    }

    private fun ResultRow.toDto(): DocumentLineItemDto {
        return DocumentLineItemDto(
            id = DocumentLineItemId(this[DocumentLineItemsTable.id].value),
            tenantId = TenantId(this[DocumentLineItemsTable.tenantId]),
            documentId = DocumentId(this[DocumentLineItemsTable.documentId]),
            position = this[DocumentLineItemsTable.position],
            description = this[DocumentLineItemsTable.description],
            quantity = this[DocumentLineItemsTable.quantity],
            unitPrice = this[DocumentLineItemsTable.unitPrice],
            netAmount = this[DocumentLineItemsTable.netAmount],
            vatRate = this[DocumentLineItemsTable.vatRate],
            vatAmount = this[DocumentLineItemsTable.vatAmount],
            grossAmount = this[DocumentLineItemsTable.grossAmount],
            currency = this[DocumentLineItemsTable.currency],
            isSynthetic = this[DocumentLineItemsTable.isSynthetic]
        )
    }
}
