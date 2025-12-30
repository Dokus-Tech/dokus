package tech.dokus.database.tables.cashflow

import tech.dokus.database.tables.auth.TenantTable
import tech.dokus.database.tables.contacts.ContactsTable
import tech.dokus.domain.enums.BillStatus
import tech.dokus.domain.enums.Currency
import tech.dokus.domain.enums.ExpenseCategory
import tech.dokus.domain.enums.PaymentMethod
import tech.dokus.foundation.backend.database.dbEnumeration
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.date
import org.jetbrains.exposed.v1.datetime.datetime

/**
 * Bills table - stores incoming supplier invoices (Cash-Out).
 *
 * Bills represent invoices received from suppliers/vendors that need to be paid.
 * This is different from Expenses which are direct purchases/receipts.
 *
 * OWNER: cashflow service
 * CRITICAL: All queries MUST filter by tenant_id for tenant isolation.
 */
object BillsTable : UUIDTable("bills") {
    // Multi-tenancy (CRITICAL)
    val tenantId = uuid("tenant_id").references(
        TenantTable.id,
        onDelete = ReferenceOption.CASCADE
    ).index()

    // Supplier information
    val supplierName = varchar("supplier_name", 255).index()
    val supplierVatNumber = varchar("supplier_vat_number", 50).nullable()

    // Invoice details
    val invoiceNumber = varchar("invoice_number", 100).nullable()
    val issueDate = date("issue_date")
    val dueDate = date("due_date").index()

    // Amounts (NUMERIC for exact arithmetic - NO FLOATS!)
    val amount = decimal("amount", 12, 2)
    val vatAmount = decimal("vat_amount", 12, 2).nullable()
    val vatRate = decimal("vat_rate", 5, 4).nullable() // e.g., 0.2100 for 21%

    // Status & Currency
    val status = dbEnumeration<BillStatus>("status").default(BillStatus.Pending).index()
    val currency = dbEnumeration<Currency>("currency").default(Currency.Eur)

    // Categorization
    val category = dbEnumeration<ExpenseCategory>("category")
    val description = text("description").nullable()

    // Document attachment (references DocumentsTable)
    val documentId = uuid("document_id").references(DocumentsTable.id).nullable()

    // Contact (vendor) reference - RESTRICT prevents deleting contacts with linked bills
    val contactId = uuid("contact_id")
        .references(ContactsTable.id, onDelete = ReferenceOption.RESTRICT)
        .nullable()
        .index()

    // Payment tracking
    val paidAt = datetime("paid_at").nullable()
    val paidAmount = decimal("paid_amount", 12, 2).nullable()
    val paymentMethod = dbEnumeration<PaymentMethod>("payment_method").nullable()
    val paymentReference = varchar("payment_reference", 255).nullable()

    // Notes
    val notes = text("notes").nullable()

    // Timestamps
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        // Composite indexes for common queries
        index(false, tenantId, status)
        index(false, tenantId, dueDate)
        index(false, tenantId, category)

        // For contact activity queries: find all bills for a contact
        index(false, tenantId, contactId)

        // Avoid duplicate supplier invoice numbers per tenant when provided
        uniqueIndex(tenantId, invoiceNumber)
    }
}
