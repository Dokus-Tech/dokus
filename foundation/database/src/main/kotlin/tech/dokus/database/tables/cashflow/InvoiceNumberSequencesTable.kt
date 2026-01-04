package tech.dokus.database.tables.cashflow

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.datetime
import tech.dokus.database.tables.auth.TenantTable

/**
 * Invoice number sequences table - tracks gap-less sequential invoice numbers per tenant per year.
 *
 * This table is used to implement Belgian tax law compliant gap-less invoice numbering.
 * Each row tracks the current sequence number for a specific tenant and year combination.
 * The sequence is atomically incremented using SELECT...FOR UPDATE locking to prevent
 * race conditions during concurrent invoice creation.
 *
 * OWNER: cashflow service
 * CRITICAL: All queries MUST filter by tenant_id
 * CRITICAL: Use SELECT...FOR UPDATE when incrementing to ensure atomicity
 */
object InvoiceNumberSequencesTable : UUIDTable("invoice_number_sequences") {
    // Multi-tenancy (CRITICAL)
    val tenantId = uuid("tenant_id").references(
        TenantTable.id,
        onDelete = ReferenceOption.CASCADE
    ).index()

    // Year for yearly reset support
    val year = integer("year")

    // Current sequence number (incremented atomically)
    val currentNumber = integer("current_number").default(0)

    // Timestamps
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        // Unique composite index: one sequence row per tenant per year
        uniqueIndex(tenantId, year)
    }
}
