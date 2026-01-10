package tech.dokus.database.tables.cashflow

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.date
import org.jetbrains.exposed.v1.datetime.datetime
import tech.dokus.database.tables.auth.TenantTable
import tech.dokus.database.tables.contacts.ContactsTable
import tech.dokus.database.tables.documents.DocumentsTable
import tech.dokus.domain.enums.CashflowDirection
import tech.dokus.domain.enums.CashflowEntryStatus
import tech.dokus.domain.enums.CashflowSourceType
import tech.dokus.domain.enums.Currency
import tech.dokus.foundation.backend.database.dbEnumeration

/**
 * Cashflow entries table - projection layer for all financial flows.
 *
 * This is the normalized source of truth for cashflow data. Entries are created
 * when financial facts (Invoice, Bill, Expense) are confirmed from documents.
 *
 * Cashflow is NEVER driven directly by documents or integrations.
 * Financial facts are created ONLY after document confirmation.
 *
 * OWNER: cashflow service
 * CRITICAL: All queries MUST filter by tenant_id
 */
object CashflowEntriesTable : UUIDTable("cashflow_entries") {
    // Multi-tenancy (CRITICAL)
    val tenantId = uuid("tenant_id").references(
        TenantTable.id,
        onDelete = ReferenceOption.CASCADE
    ).index()

    // Source tracking - links back to the financial fact
    val sourceType = dbEnumeration<CashflowSourceType>("source_type")
    val sourceId = uuid("source_id") // Invoice/Bill/Expense ID

    // Document linkage (optional - for traceability)
    val documentId = uuid("document_id")
        .references(DocumentsTable.id, onDelete = ReferenceOption.SET_NULL)
        .nullable()

    // Flow direction
    val direction = dbEnumeration<CashflowDirection>("direction") // IN / OUT

    // Event date (due date for invoices/bills, expense date for expenses)
    val eventDate = date("event_date").index()

    // Amounts (NUMERIC for exact decimal arithmetic - NEVER Float!)
    val amountGross = decimal("amount_gross", 12, 2)
    val amountVat = decimal("amount_vat", 12, 2)
    val remainingAmount = decimal("remaining_amount", 12, 2)

    // Currency
    val currency = dbEnumeration<Currency>("currency").default(Currency.Eur)

    // Status
    val status = dbEnumeration<CashflowEntryStatus>("status").default(CashflowEntryStatus.Open).index()

    // Counterparty (customer for invoices, vendor for bills/expenses)
    val counterpartyId = uuid("counterparty_id")
        .references(ContactsTable.id, onDelete = ReferenceOption.SET_NULL)
        .nullable()

    // Timestamps
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        // Unique constraint: one entry per source
        uniqueIndex(tenantId, sourceType, sourceId)
        // Common query indexes
        index(false, tenantId, eventDate)
        index(false, tenantId, status)
        index(false, tenantId, direction)
        index(false, tenantId, direction, status)
    }
}
