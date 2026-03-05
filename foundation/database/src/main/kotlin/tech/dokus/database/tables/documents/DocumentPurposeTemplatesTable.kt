package tech.dokus.database.tables.documents

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.datetime
import tech.dokus.database.tables.auth.TenantTable
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.enums.PurposePeriodMode
import tech.dokus.foundation.backend.database.dbEnumeration

object DocumentPurposeTemplatesTable : UUIDTable("document_purpose_templates") {
    val tenantId = uuid("tenant_id").references(
        TenantTable.id,
        onDelete = ReferenceOption.CASCADE
    )
    val counterpartyKey = varchar("counterparty_key", 255)
    val documentType = dbEnumeration<DocumentType>("document_type")
    val purposeBase = text("purpose_base")
    val periodMode = dbEnumeration<PurposePeriodMode>("period_mode")
    val confidence = decimal("confidence", 5, 4).default("0.0".toBigDecimal())
    val usageCount = integer("usage_count").default(0)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        uniqueIndex(tenantId, counterpartyKey, documentType)
        index(false, tenantId, counterpartyKey, documentType)
    }
}
