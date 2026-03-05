package tech.dokus.database.tables.documents

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.datetime
import tech.dokus.database.columns.EmbeddingDimensions
import tech.dokus.database.columns.vector
import tech.dokus.database.tables.auth.TenantTable
import tech.dokus.domain.enums.DocumentPurposeSource
import tech.dokus.domain.enums.DocumentType
import tech.dokus.foundation.backend.database.dbEnumeration

object DocumentPurposeExamplesTable : UUIDTable("document_purpose_examples") {
    val tenantId = uuid("tenant_id").references(
        TenantTable.id,
        onDelete = ReferenceOption.CASCADE
    )
    val documentId = uuid("document_id").references(
        DocumentsTable.id,
        onDelete = ReferenceOption.CASCADE
    )
    val documentType = dbEnumeration<DocumentType>("document_type")
    val counterpartyKey = varchar("counterparty_key", 255).nullable()
    val merchantToken = varchar("merchant_token", 120).nullable()
    val purposeBase = text("purpose_base")
    val purposeRendered = text("purpose_rendered").nullable()
    val purposeSource = dbEnumeration<DocumentPurposeSource>("purpose_source").nullable()
    val embedding = vector("embedding", EmbeddingDimensions.OLLAMA_NOMIC_EMBED_TEXT).nullable()
    val embeddingModel = varchar("embedding_model", 100).nullable()
    val indexedAt = datetime("indexed_at").defaultExpression(CurrentDateTime)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        uniqueIndex(tenantId, documentId)
        index(false, tenantId)
        index(false, tenantId, counterpartyKey, documentType)
        index(false, tenantId, merchantToken, documentType)
    }
}
