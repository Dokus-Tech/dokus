package ai.dokus.foundation.database.tables

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentDateTime
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

/**
 * Quarterly VAT filing records
 * Track VAT submissions to tax authorities
 */
object VatReturnsTable : UUIDTable("vat_returns") {
    val tenantId = reference("tenant_id", TenantsTable, onDelete = ReferenceOption.CASCADE)

    val quarter = integer("quarter")  // 1, 2, 3, 4
    val year = integer("year")

    // VAT calculations
    val salesVat = decimal("sales_vat", 12, 2)      // Collected from invoices
    val purchaseVat = decimal("purchase_vat", 12, 2) // Paid on expenses
    val netVat = decimal("net_vat", 12, 2)          // To pay or reclaim

    val status = varchar("status", 50)  // 'draft', 'filed', 'paid'
    val filedAt = datetime("filed_at").nullable()
    val paidAt = datetime("paid_at").nullable()

    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        uniqueIndex(tenantId, year, quarter)
        index(false, tenantId)
        index(false, year, quarter)
    }
}