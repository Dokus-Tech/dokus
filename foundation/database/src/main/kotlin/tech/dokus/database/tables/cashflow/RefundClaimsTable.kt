package tech.dokus.database.tables.cashflow

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.date
import org.jetbrains.exposed.v1.datetime.datetime
import tech.dokus.database.tables.auth.TenantTable
import tech.dokus.database.tables.contacts.ContactsTable
import tech.dokus.domain.enums.Currency
import tech.dokus.domain.enums.RefundClaimStatus
import tech.dokus.foundation.backend.database.dbEnumeration

/**
 * Refund claims table - tracks expected refunds from credit notes.
 *
 * When a credit note has settlementIntent=RefundExpected, a RefundClaim is created.
 * This tracks the expected refund until it is received/paid.
 *
 * RefundClaims do NOT affect cashflow totals - they are a separate "Refunds expected" tracker.
 * Cashflow entries are created only when the refund is recorded as settled.
 *
 * Constraints:
 * - counterpartyId is NOT NULL (always know who owes/is owed refund)
 * - At most 1 open claim per credit note (unless partial refunds explicitly supported)
 *
 * OWNER: cashflow service
 * CRITICAL: All queries MUST filter by tenant_id for tenant isolation.
 */
object RefundClaimsTable : UuidTable("refund_claims") {
    // Multi-tenancy (CRITICAL)
    val tenantId = uuid("tenant_id")
        .references(TenantTable.id, onDelete = ReferenceOption.CASCADE)
        .index()

    // Reference to the credit note
    val creditNoteId = uuid("credit_note_id")
        .references(CreditNotesTable.id, onDelete = ReferenceOption.CASCADE)
        .index()

    // Counterparty (who owes/is owed the refund) - REQUIRED
    val counterpartyId = uuid("counterparty_id")
        .references(ContactsTable.id, onDelete = ReferenceOption.RESTRICT)
        .index()

    // Amount expected
    val amount = decimal("amount", 12, 2)
    val currency = dbEnumeration<Currency>("currency").default(Currency.Eur)

    // Expected date (optional)
    val expectedDate = date("expected_date").nullable()

    // Status
    val status = dbEnumeration<RefundClaimStatus>("status")
        .default(RefundClaimStatus.Open)
        .index()

    // Settlement tracking
    val settledAt = datetime("settled_at").nullable()
    val cashflowEntryId = uuid("cashflow_entry_id")
        .references(CashflowEntriesTable.id, onDelete = ReferenceOption.SET_NULL)
        .nullable()

    // Timestamps
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        // Composite indexes for common queries
        index(false, tenantId, status)
        index(false, tenantId, creditNoteId)
        index(false, tenantId, counterpartyId)

        // Prevent duplicate open claims for the same credit note
        // Note: This allows one settled + one new open claim if needed
        uniqueIndex("uq_refund_claims_open", tenantId, creditNoteId, status)
    }
}
