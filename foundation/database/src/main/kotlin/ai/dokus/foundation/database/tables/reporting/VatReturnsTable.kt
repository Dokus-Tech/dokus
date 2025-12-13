package ai.dokus.foundation.database.tables.reporting

import ai.dokus.foundation.ktor.database.dbEnumeration
import ai.dokus.foundation.domain.enums.VatReturnStatus
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.date
import org.jetbrains.exposed.v1.datetime.datetime

/**
 * VAT return submissions for tax compliance
 * Belgian quarterly VAT returns
 */
object VatReturnsTable : UUIDTable("vat_returns") {
    val tenantId = reference(
        name = "tenant_id",
        foreign = ai.dokus.foundation.database.tables.auth.TenantTable,
        onDelete = org.jetbrains.exposed.v1.core.ReferenceOption.CASCADE
    )

    val periodStart = date("period_start")
    val periodEnd = date("period_end")
    val quarterYear = integer("quarter_year")
    val quarter = integer("quarter")

    val totalSales = decimal("total_sales", 12, 2)
    val vatCollected = decimal("vat_collected", 12, 2)
    val totalPurchases = decimal("total_purchases", 12, 2)
    val vatPaid = decimal("vat_paid", 12, 2)
    val vatDue = decimal("vat_due", 12, 2)

    val status = dbEnumeration<VatReturnStatus>("status")
    val submittedAt = datetime("submitted_at").nullable()
    val referenceNumber = varchar("reference_number", 100).nullable()

    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        index(false, tenantId)
        index(false, tenantId, quarterYear, quarter)
        uniqueIndex(tenantId, quarterYear, quarter)
    }
}
