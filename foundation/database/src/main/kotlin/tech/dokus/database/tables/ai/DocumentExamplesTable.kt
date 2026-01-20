package tech.dokus.database.tables.ai

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.datetime
import tech.dokus.database.tables.auth.TenantTable

/**
 * Document examples table - stores successful extractions for few-shot learning.
 *
 * When processing documents, the orchestrator looks up examples from the same vendor
 * (by VAT number or name) to guide extraction. This improves accuracy for repeat vendors.
 *
 * OWNER: ai-backend (write via orchestrator)
 * ACCESS: ai-backend (read for few-shot lookup)
 * CRITICAL: All queries MUST filter by tenant_id for multi-tenant security.
 *
 * Lookup priority:
 * 1. Exact match on vendor_vat (primary)
 * 2. Fuzzy match on vendor_name (fallback)
 */
object DocumentExamplesTable : UUIDTable("document_examples") {

    // Multi-tenancy (CRITICAL)
    val tenantId = uuid("tenant_id").references(
        TenantTable.id,
        onDelete = ReferenceOption.CASCADE
    )

    // Vendor identification (lookup keys)
    val vendorVat = varchar("vendor_vat", 20).nullable()
    val vendorName = varchar("vendor_name", 255)

    // Document classification
    val documentType = varchar("document_type", 50)

    // Extraction data as JSON (schema depends on documentType)
    val extraction = text("extraction")

    // Quality metrics
    val confidence = decimal("confidence", 4, 3)

    // Usage tracking
    val timesUsed = integer("times_used").default(0)

    // Timestamps
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        // CRITICAL: Index tenant_id for all queries (tenant isolation)
        index(false, tenantId)

        // Primary lookup: tenant + vendor VAT
        index(false, tenantId, vendorVat)

        // Fallback lookup: tenant + vendor name
        index(false, tenantId, vendorName)

        // Unique constraint: one example per vendor VAT per tenant (if VAT is present)
        uniqueIndex(tenantId, vendorVat)
    }
}
