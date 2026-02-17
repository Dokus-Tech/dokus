package tech.dokus.database.tables.cashflow

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.date
import org.jetbrains.exposed.v1.datetime.datetime
import tech.dokus.database.tables.auth.TenantTable
import tech.dokus.database.tables.contacts.ContactsTable
import tech.dokus.database.tables.documents.DocumentsTable
import tech.dokus.domain.enums.Currency
import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.enums.InvoiceStatus
import tech.dokus.domain.enums.PaymentMethod
import tech.dokus.domain.enums.PeppolStatus
import tech.dokus.foundation.backend.database.dbEnumeration
import java.math.BigDecimal

/**
 * Invoices table - stores all customer invoices.
 *
 * OWNER: cashflow service
 * CRITICAL: All queries MUST filter by tenant_id
 */
object InvoicesTable : UuidTable("invoices") {
    // Multi-tenancy (CRITICAL)
    val tenantId = uuid("tenant_id").references(
        TenantTable.id,
        onDelete = ReferenceOption.CASCADE
    ).index()

    // Contact (customer) reference
    val contactId = uuid("contact_id").references(
        ContactsTable.id,
        onDelete = ReferenceOption.RESTRICT
    ).index()

    // Invoice identification
    val invoiceNumber = varchar("invoice_number", 50)

    // Dates
    val issueDate = date("issue_date").index()
    val dueDate = date("due_date").index()

    // Amounts (NUMERIC for exact decimal arithmetic - NEVER Float!)
    val subtotalAmount = decimal("subtotal_amount", 12, 2)
    val vatAmount = decimal("vat_amount", 12, 2)
    val totalAmount = decimal("total_amount", 12, 2)
    val paidAmount = decimal("paid_amount", 12, 2).default(BigDecimal.ZERO)

    // Status
    val status = dbEnumeration<InvoiceStatus>("status").default(InvoiceStatus.Draft).index()
    val direction = dbEnumeration<DocumentDirection>("direction").default(DocumentDirection.Outbound).index()
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
        // Composite index for common queries
        index(false, tenantId, status)
        index(false, tenantId, direction)
        index(false, tenantId, contactId)
        // Per-tenant uniqueness for invoice numbers
        uniqueIndex(tenantId, invoiceNumber)
        // Idempotent document confirmation: only one invoice per document per tenant
        uniqueIndex("ux_invoices_tenant_document_id", tenantId, documentId)
    }
}
