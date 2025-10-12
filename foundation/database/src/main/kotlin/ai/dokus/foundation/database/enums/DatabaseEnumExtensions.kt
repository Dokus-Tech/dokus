package ai.dokus.foundation.database.enums

import ai.dokus.foundation.domain.enums.*

/**
 * Extension properties to provide database values for domain enums
 * These extensions allow domain enums to work with Exposed's database layer
 */

// ============================================================================
// TENANT & USER ENUMS
// ============================================================================

val TenantPlan.dbValue: String
    get() = when (this) {
        TenantPlan.FREE -> "free"
        TenantPlan.STARTER -> "starter"
        TenantPlan.PROFESSIONAL -> "professional"
        TenantPlan.ENTERPRISE -> "enterprise"
    }

val TenantStatus.dbValue: String
    get() = when (this) {
        TenantStatus.ACTIVE -> "active"
        TenantStatus.SUSPENDED -> "suspended"
        TenantStatus.CANCELLED -> "cancelled"
        TenantStatus.TRIAL -> "trial"
    }

val Language.dbValue: String
    get() = when (this) {
        Language.EN -> "en"
        Language.FR -> "fr"
        Language.NL -> "nl"
        Language.DE -> "de"
        Language.ES -> "es"
        Language.IT -> "it"
    }

val UserRole.dbValue: String
    get() = when (this) {
        UserRole.OWNER -> "owner"
        UserRole.ADMIN -> "admin"
        UserRole.ACCOUNTANT -> "accountant"
        UserRole.VIEWER -> "viewer"
    }

// ============================================================================
// INVOICE ENUMS
// ============================================================================

val InvoiceStatus.dbValue: String
    get() = when (this) {
        InvoiceStatus.DRAFT -> "draft"
        InvoiceStatus.SENT -> "sent"
        InvoiceStatus.VIEWED -> "viewed"
        InvoiceStatus.PARTIALLY_PAID -> "partially_paid"
        InvoiceStatus.PAID -> "paid"
        InvoiceStatus.OVERDUE -> "overdue"
        InvoiceStatus.CANCELLED -> "cancelled"
        InvoiceStatus.REFUNDED -> "refunded"
    }

val Currency.dbValue: String
    get() = when (this) {
        Currency.EUR -> "EUR"
        Currency.USD -> "USD"
        Currency.GBP -> "GBP"
        Currency.CHF -> "CHF"
        Currency.CAD -> "CAD"
        Currency.AUD -> "AUD"
    }

val PeppolStatus.dbValue: String
    get() = when (this) {
        PeppolStatus.PENDING -> "pending"
        PeppolStatus.SENT -> "sent"
        PeppolStatus.DELIVERED -> "delivered"
        PeppolStatus.FAILED -> "failed"
        PeppolStatus.REJECTED -> "rejected"
    }

// ============================================================================
// EXPENSE ENUMS
// ============================================================================

val ExpenseCategory.dbValue: String
    get() = when (this) {
        ExpenseCategory.OFFICE_SUPPLIES -> "office_supplies"
        ExpenseCategory.TRAVEL -> "travel"
        ExpenseCategory.MEALS -> "meals"
        ExpenseCategory.SOFTWARE -> "software"
        ExpenseCategory.HARDWARE -> "hardware"
        ExpenseCategory.UTILITIES -> "utilities"
        ExpenseCategory.RENT -> "rent"
        ExpenseCategory.INSURANCE -> "insurance"
        ExpenseCategory.MARKETING -> "marketing"
        ExpenseCategory.PROFESSIONAL_SERVICES -> "professional_services"
        ExpenseCategory.TELECOMMUNICATIONS -> "telecommunications"
        ExpenseCategory.VEHICLE -> "vehicle"
        ExpenseCategory.OTHER -> "other"
    }

// ============================================================================
// PAYMENT ENUMS
// ============================================================================

val PaymentMethod.dbValue: String
    get() = when (this) {
        PaymentMethod.BANK_TRANSFER -> "bank_transfer"
        PaymentMethod.CREDIT_CARD -> "credit_card"
        PaymentMethod.DEBIT_CARD -> "debit_card"
        PaymentMethod.PAYPAL -> "paypal"
        PaymentMethod.STRIPE -> "stripe"
        PaymentMethod.CASH -> "cash"
        PaymentMethod.CHECK -> "check"
        PaymentMethod.OTHER -> "other"
    }

// ============================================================================
// BANKING ENUMS
// ============================================================================

val BankProvider.dbValue: String
    get() = when (this) {
        BankProvider.PLAID -> "plaid"
        BankProvider.YODLEE -> "yodlee"
        BankProvider.TINK -> "tink"
        BankProvider.SALT_EDGE -> "salt_edge"
        BankProvider.MANUAL -> "manual"
    }

val BankAccountType.dbValue: String
    get() = when (this) {
        BankAccountType.CHECKING -> "checking"
        BankAccountType.SAVINGS -> "savings"
        BankAccountType.CREDIT_CARD -> "credit_card"
        BankAccountType.BUSINESS -> "business"
        BankAccountType.INVESTMENT -> "investment"
    }

// ============================================================================
// VAT ENUMS
// ============================================================================

val VatReturnStatus.dbValue: String
    get() = when (this) {
        VatReturnStatus.DRAFT -> "draft"
        VatReturnStatus.SUBMITTED -> "submitted"
        VatReturnStatus.ACCEPTED -> "accepted"
        VatReturnStatus.REJECTED -> "rejected"
        VatReturnStatus.PAID -> "paid"
    }

// ============================================================================
// AUDIT ENUMS
// ============================================================================

val AuditAction.dbValue: String
    get() = when (this) {
        AuditAction.INVOICE_CREATED -> "invoice.created"
        AuditAction.INVOICE_UPDATED -> "invoice.updated"
        AuditAction.INVOICE_DELETED -> "invoice.deleted"
        AuditAction.INVOICE_SENT -> "invoice.sent"
        AuditAction.INVOICE_STATUS_CHANGED -> "invoice.status_changed"
        AuditAction.PAYMENT_RECORDED -> "payment.recorded"
        AuditAction.PAYMENT_UPDATED -> "payment.updated"
        AuditAction.PAYMENT_DELETED -> "payment.deleted"
        AuditAction.EXPENSE_CREATED -> "expense.created"
        AuditAction.EXPENSE_UPDATED -> "expense.updated"
        AuditAction.EXPENSE_DELETED -> "expense.deleted"
        AuditAction.CLIENT_CREATED -> "client.created"
        AuditAction.CLIENT_UPDATED -> "client.updated"
        AuditAction.CLIENT_DELETED -> "client.deleted"
        AuditAction.USER_LOGIN -> "user.login"
        AuditAction.USER_LOGOUT -> "user.logout"
        AuditAction.USER_CREATED -> "user.created"
        AuditAction.USER_UPDATED -> "user.updated"
        AuditAction.USER_DELETED -> "user.deleted"
        AuditAction.SETTINGS_UPDATED -> "settings.updated"
        AuditAction.BANK_CONNECTED -> "bank.connected"
        AuditAction.BANK_DISCONNECTED -> "bank.disconnected"
        AuditAction.BANK_SYNC -> "bank.sync"
        AuditAction.VAT_RETURN_CREATED -> "vat_return.created"
        AuditAction.VAT_RETURN_SUBMITTED -> "vat_return.submitted"
        AuditAction.VAT_RETURN_PAID -> "vat_return.paid"
    }

val EntityType.dbValue: String
    get() = when (this) {
        EntityType.INVOICE -> "invoice"
        EntityType.INVOICE_ITEM -> "invoice_item"
        EntityType.CLIENT -> "client"
        EntityType.EXPENSE -> "expense"
        EntityType.PAYMENT -> "payment"
        EntityType.BANK_CONNECTION -> "bank_connection"
        EntityType.BANK_TRANSACTION -> "bank_transaction"
        EntityType.VAT_RETURN -> "vat_return"
        EntityType.USER -> "user"
        EntityType.TENANT -> "tenant"
        EntityType.SETTINGS -> "settings"
        EntityType.ATTACHMENT -> "attachment"
    }