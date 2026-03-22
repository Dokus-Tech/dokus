package tech.dokus.database.repository.documents

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import tech.dokus.database.tables.documents.DocumentLineItemsTable
import tech.dokus.domain.enums.Currency
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.DocumentLineItemId
import tech.dokus.domain.ids.TenantId
import java.math.BigDecimal
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toKotlinUuid

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
) {
    companion object
}

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
@OptIn(ExperimentalUuidApi::class)
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
            it[DocumentLineItemsTable.id] = UUID.fromString(id.toString())
            it[DocumentLineItemsTable.tenantId] = UUID.fromString(tenantId.toString())
            it[DocumentLineItemsTable.documentId] = UUID.fromString(documentId.toString())
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
     * Get all line items for a document, ordered by position.
     * CRITICAL: Must filter by tenantId.
     */
    suspend fun getByDocumentId(
        tenantId: TenantId,
        documentId: DocumentId
    ): List<DocumentLineItemDto> = newSuspendedTransaction {
        DocumentLineItemsTable.selectAll()
            .where {
                (DocumentLineItemsTable.tenantId eq UUID.fromString(tenantId.toString())) and
                    (DocumentLineItemsTable.documentId eq UUID.fromString(documentId.toString()))
            }
            .orderBy(DocumentLineItemsTable.position, SortOrder.ASC)
            .map { DocumentLineItemDto.from(it) }
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
                (DocumentLineItemsTable.tenantId eq UUID.fromString(tenantId.toString())) and
                    (DocumentLineItemsTable.id eq UUID.fromString(lineItemId.toString()))
            }
            .map { DocumentLineItemDto.from(it) }
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
            (DocumentLineItemsTable.tenantId eq UUID.fromString(tenantId.toString())) and
                (DocumentLineItemsTable.documentId eq UUID.fromString(documentId.toString()))
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
            (DocumentLineItemsTable.tenantId eq UUID.fromString(tenantId.toString())) and
                (DocumentLineItemsTable.id eq UUID.fromString(lineItemId.toString()))
        } > 0
    }

    private fun DocumentLineItemDto.Companion.from(row: ResultRow): DocumentLineItemDto {
        return DocumentLineItemDto(
            id = DocumentLineItemId.parse(row[DocumentLineItemsTable.id].toString()),
            tenantId = TenantId(row[DocumentLineItemsTable.tenantId].toKotlinUuid()),
            documentId = DocumentId.parse(row[DocumentLineItemsTable.documentId].toString()),
            position = row[DocumentLineItemsTable.position],
            description = row[DocumentLineItemsTable.description],
            quantity = row[DocumentLineItemsTable.quantity],
            unitPrice = row[DocumentLineItemsTable.unitPrice],
            netAmount = row[DocumentLineItemsTable.netAmount],
            vatRate = row[DocumentLineItemsTable.vatRate],
            vatAmount = row[DocumentLineItemsTable.vatAmount],
            grossAmount = row[DocumentLineItemsTable.grossAmount],
            currency = row[DocumentLineItemsTable.currency],
            isSynthetic = row[DocumentLineItemsTable.isSynthetic]
        )
    }
}
