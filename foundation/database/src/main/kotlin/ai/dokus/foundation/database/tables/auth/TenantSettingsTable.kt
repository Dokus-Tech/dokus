package ai.dokus.foundation.database.tables.auth

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.datetime

/**
 * Tenant-specific preferences and defaults.
 * Per-tenant configuration.
 *
 * OWNER: auth service
 */
object TenantSettingsTable : UUIDTable("tenant_settings") {
    val tenantId = reference(
        name = "tenant_id",
        foreign = TenantTable,
        onDelete = ReferenceOption.CASCADE
    ).uniqueIndex()

    // Invoice defaults
    val invoicePrefix = varchar("invoice_prefix", 20).default("INV")
    val nextInvoiceNumber = integer("next_invoice_number").default(1)
    val defaultPaymentTerms = integer("default_payment_terms").default(30)
    val defaultVatRate = decimal("default_vat_rate", 5, 2)
        .default(java.math.BigDecimal("21.00"))

    // Company info (for invoices)
    val companyName = varchar("company_name", 255).nullable()
    val companyAddress = text("company_address").nullable()
    val companyVatNumber = varchar("company_vat_number", 50).nullable()
    val companyIban = varchar("company_iban", 34).nullable()
    val companyBic = varchar("company_bic", 11).nullable()
    val companyLogoUrl = varchar("company_logo_url", 500).nullable()

    // Notifications
    val emailInvoiceReminders = bool("email_invoice_reminders").default(true)
    val emailPaymentConfirmations = bool("email_payment_confirmations").default(true)
    val emailWeeklyReports = bool("email_weekly_reports").default(false)

    // Feature flags
    val enableBankSync = bool("enable_bank_sync").default(false)
    val enablePeppol = bool("enable_peppol").default(false)

    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)
}
