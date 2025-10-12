package ai.dokus.foundation.database.tables

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentDateTime
import org.jetbrains.exposed.sql.kotlin.datetime.date
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

/**
 * Core financial documents sent to clients
 * Billable documents with Peppol e-invoicing support
 */
object InvoicesTable : UUIDTable("invoices") {
    val tenantId = reference("tenant_id", TenantsTable, onDelete = ReferenceOption.CASCADE)
    val clientId = reference("client_id", ClientsTable, onDelete = ReferenceOption.RESTRICT)

    // Identification
    val invoiceNumber = varchar("invoice_number", 50)
    val issueDate = date("issue_date")
    val dueDate = date("due_date")

    // Amounts - CRITICAL: Use NUMERIC(12, 2) for financial precision
    val subtotalAmount = decimal("subtotal_amount", 12, 2)
    val vatAmount = decimal("vat_amount", 12, 2)
    val totalAmount = decimal("total_amount", 12, 2)
    val paidAmount = decimal("paid_amount", 12, 2).default(java.math.BigDecimal.ZERO)

    // Status: 'draft', 'sent', 'viewed', 'paid', 'overdue', 'cancelled'
    val status = varchar("status", 50)

    // Peppol e-invoicing (Belgium 2026 requirement)
    val peppolId = varchar("peppol_id", 255).nullable()
    val peppolSentAt = datetime("peppol_sent_at").nullable()
    val peppolStatus = varchar("peppol_status", 50).nullable()

    // Payment integration
    val paymentLink = varchar("payment_link", 500).nullable()
    val paymentLinkExpiresAt = datetime("payment_link_expires_at").nullable()
    val paidAt = datetime("paid_at").nullable()
    val paymentMethod = varchar("payment_method", 50).nullable()

    // Additional
    val currency = varchar("currency", 3).default("EUR")
    val notes = text("notes").nullable()
    val termsAndConditions = text("terms_and_conditions").nullable()

    // Timestamps
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        uniqueIndex(tenantId, invoiceNumber) // Unique per tenant
        index(false, tenantId)
        index(false, clientId)
        index(false, status)
        index(false, issueDate)
        index(false, dueDate)
        index(false, tenantId, status, dueDate) // Dashboard query
    }
}