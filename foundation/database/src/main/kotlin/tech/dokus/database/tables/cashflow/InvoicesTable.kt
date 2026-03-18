package tech.dokus.database.tables.cashflow

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.date
import org.jetbrains.exposed.v1.datetime.datetime
import tech.dokus.database.tables.auth.TenantTable
import tech.dokus.database.tables.contacts.ContactsTable
import tech.dokus.database.tables.documents.DocumentsTable
import tech.dokus.domain.enums.Currency
import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.enums.InvoiceDeliveryMethod
import tech.dokus.domain.enums.InvoiceDueDateMode
import tech.dokus.domain.enums.InvoiceStatus
import tech.dokus.domain.enums.PaymentMethod
import tech.dokus.domain.enums.PeppolStatus
import tech.dokus.database.tables.auth.UsersTable
import tech.dokus.foundation.backend.database.dbEnumeration
import java.math.BigDecimal

/**
 * Invoices table - stores all customer invoices.
 *
 * OWNER: cashflow service
 * CRITICAL: All queries MUST filter by tenant_id
 */
object InvoicesTable : UUIDTable("invoices") {
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
    val subtotalAmount = decimal("subtotal_amount", 19, 4)
    val vatAmount = decimal("vat_amount", 19, 4)
    val totalAmount = decimal("total_amount", 19, 4)
    val paidAmount = decimal("paid_amount", 19, 4).default(BigDecimal.ZERO)

    // Status
    val status = dbEnumeration<InvoiceStatus>("status").default(InvoiceStatus.Draft).index()
    val direction = dbEnumeration<DocumentDirection>("direction").default(DocumentDirection.Outbound).index()
    val currency = dbEnumeration<Currency>("currency").default(Currency.Eur)

    // Optional fields
    val notes = text("notes").nullable()
    val paymentTermsDays = integer("payment_terms_days").default(30)
    val dueDateMode = dbEnumeration<InvoiceDueDateMode>("due_date_mode").default(InvoiceDueDateMode.Terms)
    val structuredCommunication = varchar("structured_communication", 32).nullable()
    val senderIban = varchar("sender_iban", 34).nullable()
    val senderBic = varchar("sender_bic", 11).nullable()
    val deliveryMethod = dbEnumeration<InvoiceDeliveryMethod>("delivery_method").default(InvoiceDeliveryMethod.PdfExport)
    val termsAndConditions = text("terms_and_conditions").nullable()

    // Peppol e-invoicing (Belgium 2026 mandate)
    val peppolId = varchar("peppol_id", 255).nullable()
    val peppolSentAt = datetime("peppol_sent_at").nullable()
    val peppolStatus = dbEnumeration<PeppolStatus>("peppol_status").nullable()

    // Document attachment (references DocumentsTable)
    val documentId = uuid("document_id").references(DocumentsTable.id, onDelete = ReferenceOption.SET_NULL).nullable()

    // Payment
    val paymentLink = varchar("payment_link", 500).nullable()
    val paymentLinkExpiresAt = datetime("payment_link_expires_at").nullable()
    val paidAt = datetime("paid_at").nullable()
    val paymentMethod = dbEnumeration<PaymentMethod>("payment_method").nullable()

    // Audit: confirmation tracking
    val confirmedAt = datetime("confirmed_at").nullable()
    val confirmedBy = uuid("confirmed_by").references(UsersTable.id).nullable()

    // Timestamps
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        // Composite index for common queries
        index(false, tenantId, status)
        index(false, tenantId, direction)
        index(false, tenantId, contactId)
        index(false, tenantId, contactId, issueDate)
        // Per-tenant uniqueness for invoice numbers
        uniqueIndex(tenantId, invoiceNumber)
        // Idempotent document confirmation: only one invoice per document per tenant
        uniqueIndex("ux_invoices_tenant_document_id", tenantId, documentId)
    }
}
