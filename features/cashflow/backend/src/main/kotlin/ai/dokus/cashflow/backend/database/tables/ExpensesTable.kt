package ai.dokus.cashflow.backend.database.tables

import ai.dokus.foundation.domain.enums.ExpenseCategory
import ai.dokus.foundation.domain.enums.PaymentMethod
import ai.dokus.foundation.ktor.database.dbEnumeration
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.date
import org.jetbrains.exposed.v1.datetime.datetime

/**
 * Expenses table - stores all business expenses
 * CRITICAL: All queries MUST filter by organization_id
 */
object ExpensesTable : UUIDTable("expenses") {
    // Multi-tenancy (CRITICAL)
    val organizationId = uuid("organization_id").index()

    // Expense details
    val date = date("date")
    val merchant = varchar("merchant", 255)
    val amount = decimal("amount", 12, 2)
    val vatAmount = decimal("vat_amount", 12, 2).nullable()
    val vatRate = decimal("vat_rate", 5, 4).nullable() // e.g., 0.2100 for 21%

    // Categorization
    val category = dbEnumeration<ExpenseCategory>("category")
    val description = text("description").nullable()

    // Receipt/document
    val receiptUrl = varchar("receipt_url", 500).nullable()
    val receiptFilename = varchar("receipt_filename", 255).nullable()

    // Tax deduction
    val isDeductible = bool("is_deductible").default(true)
    val deductiblePercentage = decimal("deductible_percentage", 5, 2).default(java.math.BigDecimal("100.00"))

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
        // CRITICAL: Index organization_id for security and performance
        index(false, organizationId)
        index(false, category)
        index(false, date)
        index(false, merchant)

        // Composite index for common queries
        index(false, organizationId, category)
        index(false, organizationId, date)
    }
}
