package tech.dokus.database.repository.ai

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.v1.jdbc.update
import tech.dokus.database.tables.ai.DocumentExamplesTable
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.ids.ExampleId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.DocumentExample
import tech.dokus.domain.repository.ExampleRepository
import tech.dokus.foundation.backend.utils.loggerFor
import java.sql.Connection

/**
 * Repository for document examples used in few-shot learning.
 *
 * CRITICAL SECURITY: All queries MUST filter by tenantId for multi-tenant isolation.
 */
class DocumentExamplesRepository : ExampleRepository {

    private val logger = loggerFor()
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun findByVendorVat(
        tenantId: TenantId,
        vatNumber: String
    ): DocumentExample? = newSuspendedTransaction {
        val tenantUuid = Uuid.parse(tenantId.toString())

        logger.debug("Looking up example by VAT: tenant=$tenantId, vat=$vatNumber")

        DocumentExamplesTable
            .selectAll()
            .where {
                (DocumentExamplesTable.tenantId eq tenantUuid) and
                    (DocumentExamplesTable.vendorVat eq vatNumber)
            }
            .singleOrNull()
            ?.toDocumentExample()
    }

    override suspend fun findByVendorName(
        tenantId: TenantId,
        name: String,
        similarity: Float
    ): DocumentExample? = newSuspendedTransaction {
        val tenantUuid = Uuid.parse(tenantId.toString())

        logger.debug("Looking up example by name: tenant=$tenantId, name=$name, similarity=$similarity")

        // Use PostgreSQL trigram similarity for fuzzy matching
        // Requires pg_trgm extension: CREATE EXTENSION IF NOT EXISTS pg_trgm;
        val sql = """
            SELECT * FROM document_examples
            WHERE tenant_id = '$tenantUuid'
            AND similarity(vendor_name, '$name') >= $similarity
            ORDER BY similarity(vendor_name, '$name') DESC
            LIMIT 1
        """.trimIndent()

        val connection = this.connection.connection as Connection
        connection.createStatement().use { stmt ->
            stmt.executeQuery(sql).use { rs ->
                if (rs.next()) {
                    DocumentExample(
                        id = ExampleId.parse(rs.getString("id")),
                        tenantId = TenantId.parse(rs.getString("tenant_id")),
                        vendorVat = rs.getString("vendor_vat"),
                        vendorName = rs.getString("vendor_name"),
                        documentType = parseDocumentType(rs.getString("document_type")),
                        extraction = json.parseToJsonElement(rs.getString("extraction")),
                        confidence = rs.getBigDecimal("confidence").toDouble(),
                        timesUsed = rs.getInt("times_used"),
                        createdAt = Instant.parse(rs.getTimestamp("created_at").toInstant().toString()),
                        updatedAt = Instant.parse(rs.getTimestamp("updated_at").toInstant().toString())
                    )
                } else {
                    null
                }
            }
        }
    }

    override suspend fun save(example: DocumentExample): DocumentExample = newSuspendedTransaction {
        val tenantUuid = Uuid.parse(example.tenantId.toString())
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)

        logger.info(
            "Saving example: tenant=${example.tenantId}, vendor=${example.vendorName}, " +
                "vat=${example.vendorVat}, type=${example.documentType}"
        )

        // Check if example already exists for this vendor VAT
        val existingId = if (example.vendorVat != null) {
            DocumentExamplesTable
                .selectAll()
                .where {
                    (DocumentExamplesTable.tenantId eq tenantUuid) and
                        (DocumentExamplesTable.vendorVat eq example.vendorVat)
                }
                .singleOrNull()
                ?.get(DocumentExamplesTable.id)?.value
        } else {
            null
        }

        if (existingId != null) {
            // Update existing example
            DocumentExamplesTable.update({
                DocumentExamplesTable.id eq existingId
            }) {
                it[vendorName] = example.vendorName
                it[documentType] = example.documentType.name
                it[extraction] = example.extraction.toString()
                it[confidence] = example.confidence.toBigDecimal()
                it[updatedAt] = now
            }

            logger.debug("Updated existing example: $existingId")
            example.copy(id = ExampleId.parse(existingId.toString()))
        } else {
            // Insert new example
            val id = Uuid.random()
            DocumentExamplesTable.insert {
                it[DocumentExamplesTable.id] = id
                it[DocumentExamplesTable.tenantId] = tenantUuid
                it[vendorVat] = example.vendorVat
                it[vendorName] = example.vendorName
                it[documentType] = example.documentType.name
                it[extraction] = example.extraction.toString()
                it[confidence] = example.confidence.toBigDecimal()
                it[timesUsed] = 0
                it[createdAt] = now
                it[updatedAt] = now
            }

            logger.debug("Created new example: $id")
            example.copy(id = ExampleId.parse(id.toString()))
        }
    }

    override suspend fun incrementUsage(exampleId: ExampleId): Unit = newSuspendedTransaction {
        val exampleUuid = Uuid.parse(exampleId.toString())
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)

        logger.debug("Incrementing usage for example: $exampleId")

        // Fetch current usage count and increment
        val currentUsage = DocumentExamplesTable
            .selectAll()
            .where { DocumentExamplesTable.id eq exampleUuid }
            .singleOrNull()
            ?.get(DocumentExamplesTable.timesUsed) ?: 0

        DocumentExamplesTable.update({
            DocumentExamplesTable.id eq exampleUuid
        }) {
            it[timesUsed] = currentUsage + 1
            it[updatedAt] = now
        }
    }

    override suspend fun delete(
        tenantId: TenantId,
        exampleId: ExampleId
    ): Boolean = newSuspendedTransaction {
        val tenantUuid = Uuid.parse(tenantId.toString())
        val exampleUuid = Uuid.parse(exampleId.toString())

        logger.info("Deleting example: $exampleId, tenant=$tenantId")

        val deleted = DocumentExamplesTable.deleteWhere {
            (DocumentExamplesTable.id eq exampleUuid) and
                (DocumentExamplesTable.tenantId eq tenantUuid)
        }

        deleted > 0
    }

    override suspend fun countForTenant(tenantId: TenantId): Long = newSuspendedTransaction {
        val tenantUuid = Uuid.parse(tenantId.toString())

        DocumentExamplesTable
            .selectAll()
            .where { DocumentExamplesTable.tenantId eq tenantUuid }
            .count()
    }

    private fun ResultRow.toDocumentExample(): DocumentExample {
        return DocumentExample(
            id = ExampleId.parse(this[DocumentExamplesTable.id].value.toString()),
            tenantId = TenantId.parse(this[DocumentExamplesTable.tenantId].toString()),
            vendorVat = this[DocumentExamplesTable.vendorVat],
            vendorName = this[DocumentExamplesTable.vendorName],
            documentType = parseDocumentType(this[DocumentExamplesTable.documentType]),
            extraction = json.parseToJsonElement(this[DocumentExamplesTable.extraction]),
            confidence = this[DocumentExamplesTable.confidence].toDouble(),
            timesUsed = this[DocumentExamplesTable.timesUsed],
            createdAt = this[DocumentExamplesTable.createdAt].let {
                Instant.parse(it.toString() + "Z")
            },
            updatedAt = this[DocumentExamplesTable.updatedAt].let {
                Instant.parse(it.toString() + "Z")
            }
        )
    }

    /**
     * Parse document type from database string.
     * Handles both old ClassifiedDocumentType format (INVOICE) and new DocumentType format (Invoice).
     */
    private fun parseDocumentType(value: String): DocumentType = when (value.uppercase()) {
        "INVOICE" -> DocumentType.Invoice
        "RECEIPT" -> DocumentType.Receipt
        "CREDIT_NOTE", "CREDITNOTE" -> DocumentType.CreditNote
        "PRO_FORMA", "PROFORMA" -> DocumentType.ProForma
        else -> DocumentType.Unknown
    }
}
