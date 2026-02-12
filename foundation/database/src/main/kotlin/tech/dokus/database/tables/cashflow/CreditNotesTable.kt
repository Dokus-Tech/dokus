package tech.dokus.database.tables.cashflow

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.date
import org.jetbrains.exposed.v1.datetime.datetime
import tech.dokus.database.tables.auth.TenantTable
import tech.dokus.database.tables.contacts.ContactsTable
import tech.dokus.database.tables.documents.DocumentsTable
import tech.dokus.domain.enums.CreditNoteStatus
import tech.dokus.domain.enums.CreditNoteType
import tech.dokus.domain.enums.Currency
import tech.dokus.domain.enums.SettlementIntent
import tech.dokus.foundation.backend.database.dbEnumeration

private const val CreditNoteNumberMaxLength = 50

/**
 * Credit notes table - stores sales and purchase credit notes.
 *
 * Credit notes reduce receivables (Sales) or payables (Purchase).
 * They do NOT directly create cashflow entries.
 * Cashflow entries are created only when refunds are recorded.
 *
 * Original document tracking uses DocumentLinksTable with linkType=OriginalDocument.
 *
 * OWNER: cashflow service
 * CRITICAL: All queries MUST filter by tenant_id for tenant isolation.
 */
object CreditNotesTable : UUIDTable("credit_notes") {
    // Multi-tenancy (CRITICAL)
    val tenantId = uuid("tenant_id")
        .references(TenantTable.id, onDelete = ReferenceOption.CASCADE)
        .index()

    // Counterparty (customer for Sales, supplier for Purchase) - REQUIRED
    val contactId = uuid("contact_id")
        .references(ContactsTable.id, onDelete = ReferenceOption.RESTRICT)
        .index()

    // Document attachment (references DocumentsTable)
    val documentId = uuid("document_id")
        .references(DocumentsTable.id, onDelete = ReferenceOption.SET_NULL)
        .nullable()

    // Type: Sales (customer) or Purchase (supplier)
    val creditNoteType = dbEnumeration<CreditNoteType>("credit_note_type").index()

    // Identification
    val creditNoteNumber = varchar("credit_note_number", CreditNoteNumberMaxLength)

    // Dates
    val issueDate = date("issue_date").index()

    // Amounts (positive values, sign determined by type)
    // NUMERIC for exact arithmetic - NO FLOATS!
    val subtotalAmount = decimal("subtotal_amount", 12, 2)
    val vatAmount = decimal("vat_amount", 12, 2)
    val totalAmount = decimal("total_amount", 12, 2)
    val currency = dbEnumeration<Currency>("currency").default(Currency.Eur)

    // Settlement tracking
    val settlementIntent = dbEnumeration<SettlementIntent>("settlement_intent")
        .default(SettlementIntent.Unknown)
    val status = dbEnumeration<CreditNoteStatus>("status")
        .default(CreditNoteStatus.Draft)
        .index()

    // Content
    val reason = text("reason").nullable()
    val notes = text("notes").nullable()

    // Timestamps
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        // Composite indexes for common queries
        index(false, tenantId, status)
        index(false, tenantId, creditNoteType)
        index(false, tenantId, contactId)

        // Avoid duplicate credit note numbers per tenant
        uniqueIndex(tenantId, creditNoteNumber)

        // Idempotent document confirmation index managed by Flyway V3 migration
        // (partial unique index WHERE document_id IS NOT NULL)
    }
}
