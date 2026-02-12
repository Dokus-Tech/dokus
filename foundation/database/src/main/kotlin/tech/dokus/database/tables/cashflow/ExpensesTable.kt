package tech.dokus.database.tables.cashflow

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.date
import org.jetbrains.exposed.v1.datetime.datetime
import tech.dokus.database.tables.auth.TenantTable
import tech.dokus.database.tables.contacts.ContactsTable
import tech.dokus.database.tables.documents.DocumentsTable
import tech.dokus.domain.enums.ExpenseCategory
import tech.dokus.domain.enums.PaymentMethod
import tech.dokus.foundation.backend.database.dbEnumeration
import java.math.BigDecimal

/**
 * Expenses table - stores all business expenses.
 *
 * OWNER: cashflow service
 * CRITICAL: All queries MUST filter by tenant_id
 */
object ExpensesTable : UUIDTable("expenses") {
    // Multi-tenancy (CRITICAL)
    val tenantId = uuid("tenant_id").references(
        TenantTable.id,
        onDelete = ReferenceOption.CASCADE
    ).index()

    // Expense details
    val date = date("date").index()
    val merchant = varchar("merchant", 255).index()
    val amount = decimal("amount", 12, 2)
    val vatAmount = decimal("vat_amount", 12, 2).nullable()
    val vatRate = decimal("vat_rate", 5, 4).nullable() // e.g., 0.2100 for 21%

    // Categorization
    val category = dbEnumeration<ExpenseCategory>("category").index()
    val description = text("description").nullable()

    // Document attachment (references DocumentsTable)
    val documentId = uuid("document_id").references(DocumentsTable.id).nullable()

    // Contact (vendor) reference - RESTRICT prevents deleting contacts with linked expenses
    val contactId = uuid("contact_id")
        .references(ContactsTable.id, onDelete = ReferenceOption.RESTRICT)
        .nullable()
        .index()

    // Tax deduction
    val isDeductible = bool("is_deductible").default(true)
    val deductiblePercentage = decimal("deductible_percentage", 5, 2).default(BigDecimal("100.00"))

    // Payment
    val paymentMethod = dbEnumeration<PaymentMethod>("payment_method").nullable()

    // Recurring
    val isRecurring = bool("is_recurring").default(false)

    // Notes
    val notes = text("notes").nullable()

    // Timestamps
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        // Composite index for common queries
        index(false, tenantId, category)
        index(false, tenantId, date)

        // For contact activity queries: find all expenses for a contact
        index(false, tenantId, contactId)

        // Idempotent document confirmation index managed by Flyway V3 migration
        // (partial unique index WHERE document_id IS NOT NULL)
    }
}
