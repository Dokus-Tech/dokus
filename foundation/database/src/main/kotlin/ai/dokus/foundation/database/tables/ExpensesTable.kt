package ai.dokus.foundation.database.tables

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentDateTime
import org.jetbrains.exposed.sql.kotlin.datetime.date
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

/**
 * Business costs tracked for tax deductions
 * Track deductible business expenses with receipts
 */
object ExpensesTable : UUIDTable("expenses") {
    val tenantId = reference("tenant_id", TenantsTable, onDelete = ReferenceOption.CASCADE)

    val date = date("date")
    val merchant = varchar("merchant", 255)
    val amount = decimal("amount", 12, 2)
    val vatAmount = decimal("vat_amount", 12, 2).nullable()
    val vatRate = decimal("vat_rate", 5, 2).nullable()

    // Category: 'software', 'hardware', 'travel', 'office', 'meals', 'marketing', 'other'
    val category = varchar("category", 100)

    val description = text("description").nullable()

    // Receipt storage
    val receiptUrl = varchar("receipt_url", 500).nullable()      // S3 path
    val receiptFilename = varchar("receipt_filename", 255).nullable()

    // Tax deductibility
    val isDeductible = bool("is_deductible").default(true)
    val deductiblePercentage = decimal("deductible_percentage", 5, 2)
        .default(java.math.BigDecimal("100.00"))

    val paymentMethod = varchar("payment_method", 50).nullable()
    val isRecurring = bool("is_recurring").default(false)
    val notes = text("notes").nullable()

    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        index(false, tenantId)
        index(false, date)
        index(false, category)
        index(false, tenantId, date)      // Date range queries
        index(false, tenantId, category)  // Category filtering
    }
}