package ai.dokus.foundation.domain.enums

import kotlinx.serialization.Serializable

// ============================================================================
// TENANT & USER ENUMS
// ============================================================================

@Serializable
enum class TenantPlan {
    FREE,
    STARTER,
    PROFESSIONAL,
    ENTERPRISE
}

@Serializable
enum class TenantStatus {
    ACTIVE,
    SUSPENDED,
    CANCELLED,
    TRIAL
}

@Serializable
enum class Language {
    EN,
    FR,
    NL,
    DE,
    ES,
    IT
}

@Serializable
enum class UserRole {
    OWNER,
    ADMIN,
    ACCOUNTANT,
    VIEWER
}

// ============================================================================
// INVOICE ENUMS
// ============================================================================

@Serializable
enum class InvoiceStatus {
    DRAFT,
    SENT,
    VIEWED,
    PARTIALLY_PAID,
    PAID,
    OVERDUE,
    CANCELLED,
    REFUNDED
}

@Serializable
enum class Currency {
    EUR,
    USD,
    GBP,
    CHF,
    CAD,
    AUD
}

@Serializable
enum class PeppolStatus {
    PENDING,
    SENT,
    DELIVERED,
    FAILED,
    REJECTED
}

// ============================================================================
// EXPENSE ENUMS
// ============================================================================

@Serializable
enum class ExpenseCategory {
    OFFICE_SUPPLIES,
    TRAVEL,
    MEALS,
    SOFTWARE,
    HARDWARE,
    UTILITIES,
    RENT,
    INSURANCE,
    MARKETING,
    PROFESSIONAL_SERVICES,
    TELECOMMUNICATIONS,
    VEHICLE,
    OTHER
}

// ============================================================================
// PAYMENT ENUMS
// ============================================================================

@Serializable
enum class PaymentMethod {
    BANK_TRANSFER,
    CREDIT_CARD,
    DEBIT_CARD,
    PAYPAL,
    STRIPE,
    CASH,
    CHECK,
    OTHER
}

// ============================================================================
// BANKING ENUMS
// ============================================================================

@Serializable
enum class BankProvider {
    PLAID,
    YODLEE,
    TINK,
    SALT_EDGE,
    MANUAL
}

@Serializable
enum class BankAccountType {
    CHECKING,
    SAVINGS,
    CREDIT_CARD,
    BUSINESS,
    INVESTMENT
}

// ============================================================================
// VAT ENUMS
// ============================================================================

@Serializable
enum class VatReturnStatus {
    DRAFT,
    SUBMITTED,
    ACCEPTED,
    REJECTED,
    PAID
}

// ============================================================================
// AUDIT ENUMS
// ============================================================================

@Serializable
enum class AuditAction {
    // Invoice actions
    INVOICE_CREATED,
    INVOICE_UPDATED,
    INVOICE_DELETED,
    INVOICE_SENT,
    INVOICE_STATUS_CHANGED,

    // Payment actions
    PAYMENT_RECORDED,
    PAYMENT_UPDATED,
    PAYMENT_DELETED,

    // Expense actions
    EXPENSE_CREATED,
    EXPENSE_UPDATED,
    EXPENSE_DELETED,

    // Client actions
    CLIENT_CREATED,
    CLIENT_UPDATED,
    CLIENT_DELETED,

    // User actions
    USER_LOGIN,
    USER_LOGOUT,
    USER_CREATED,
    USER_UPDATED,
    USER_DELETED,

    // Settings actions
    SETTINGS_UPDATED,

    // Banking actions
    BANK_CONNECTED,
    BANK_DISCONNECTED,
    BANK_SYNC,

    // VAT actions
    VAT_RETURN_CREATED,
    VAT_RETURN_SUBMITTED,
    VAT_RETURN_PAID
}

@Serializable
enum class EntityType {
    INVOICE,
    INVOICE_ITEM,
    CLIENT,
    EXPENSE,
    PAYMENT,
    BANK_CONNECTION,
    BANK_TRANSACTION,
    VAT_RETURN,
    USER,
    TENANT,
    SETTINGS,
    ATTACHMENT
}