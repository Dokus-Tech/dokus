package ai.dokus.cashflow.backend.database.tables

import ai.dokus.foundation.domain.enums.Currency
import ai.dokus.foundation.domain.enums.InvoiceStatus
import ai.dokus.foundation.domain.enums.PaymentMethod
import ai.dokus.foundation.domain.enums.PeppolStatus
import ai.dokus.foundation.ktor.database.dbEnumeration
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.date
import org.jetbrains.exposed.v1.datetime.datetime

/**
 * Invoices table - stores all customer invoices
 * CRITICAL: All queries MUST filter by organization_id
 */
object InvoicesTable : UUIDTable("invoices") {
    // Multi-tenancy (CRITICAL)
    val tenantId = uuid("organization_id")

    // Client reference
    val clientId = uuid("client_id")

    // Invoice identification
    val invoiceNumber = varchar("invoice_number", 50).uniqueIndex()

    // Dates
    val issueDate = date("issue_date")
    val dueDate = date("due_date")

    // Amounts (NUMERIC for exact decimal arithmetic - NEVER Float!)
    val subtotalAmount = decimal("subtotal_amount", 12, 2)
    val vatAmount = decimal("vat_amount", 12, 2)
    val totalAmount = decimal("total_amount", 12, 2)
    val paidAmount = decimal("paid_amount", 12, 2).default(java.math.BigDecimal.ZERO)

    // Status
    val status = dbEnumeration<InvoiceStatus>("status").default(InvoiceStatus.Draft)
    val currency = dbEnumeration<Currency>("currency").default(Currency.Eur)

    // Optional fields
    val notes = text("notes").nullable()
    val termsAndConditions = text("terms_and_conditions").nullable()

    // Peppol e-invoicing (Belgium 2026 mandate)
    val peppolId = varchar("peppol_id", 255).nullable()
    val peppolSentAt = datetime("peppol_sent_at").nullable()
    val peppolStatus = dbEnumeration<PeppolStatus>("peppol_status").nullable()

    // Document attachment (references DocumentsTable)
    val documentId = uuid("document_id").references(DocumentsTable.id).nullable()

    // Payment
    val paymentLink = varchar("payment_link", 500).nullable()
    val paymentLinkExpiresAt = datetime("payment_link_expires_at").nullable()
    val paidAt = datetime("paid_at").nullable()
    val paymentMethod = dbEnumeration<PaymentMethod>("payment_method").nullable()

    // Timestamps
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        // CRITICAL: Index organization_id for security and performance
        index(false, tenantId)
        index(false, clientId)
        index(false, status)
        index(false, issueDate)
        index(false, dueDate)

        // Composite index for common queries
        index(false, tenantId, status)
        index(false, tenantId, clientId)
    }
}
