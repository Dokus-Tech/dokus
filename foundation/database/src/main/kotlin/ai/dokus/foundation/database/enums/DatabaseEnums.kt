package ai.dokus.foundation.database.enums

import ai.dokus.foundation.domain.database.DbEnum

/**
 * Tenant subscription plans
 */
enum class TenantPlan(override val dbValue: String) : DbEnum {
    FREE("free"),
    PROFESSIONAL("professional"),
    BUSINESS("business"),
    ENTERPRISE("enterprise");
}

/**
 * Tenant account status
 */
enum class TenantStatus(override val dbValue: String) : DbEnum {
    ACTIVE("active"),
    SUSPENDED("suspended"),      // Payment failed
    CANCELLED("cancelled"),       // Subscription ended
    TRIAL("trial");              // In trial period
}

/**
 * User roles within a tenant
 */
enum class UserRole(override val dbValue: String) : DbEnum {
    OWNER("owner"),              // Full access, billing, settings
    MEMBER("member"),            // Create/edit invoices and expenses
    ACCOUNTANT("accountant"),    // View-only financial data
    VIEWER("viewer");            // Dashboard only
}

/**
 * Invoice status lifecycle
 */
enum class InvoiceStatus(override val dbValue: String) : DbEnum {
    DRAFT("draft"),              // Being created
    SENT("sent"),                // Sent to client
    VIEWED("viewed"),            // Client has viewed
    PAID("paid"),                // Fully paid
    PARTIALLY_PAID("partially_paid"), // Partial payment received
    OVERDUE("overdue"),          // Past due date
    CANCELLED("cancelled");      // Cancelled/void
}

/**
 * Payment methods
 */
enum class PaymentMethod(override val dbValue: String) : DbEnum {
    BANK_TRANSFER("bank_transfer"),
    STRIPE("stripe"),
    MOLLIE("mollie"),
    PAYPAL("paypal"),
    CASH("cash"),
    CHEQUE("cheque"),
    CREDIT_CARD("credit_card"),
    OTHER("other");
}

/**
 * Expense categories
 */
enum class ExpenseCategory(override val dbValue: String) : DbEnum {
    SOFTWARE("software"),
    HARDWARE("hardware"),
    TRAVEL("travel"),
    OFFICE("office"),
    MEALS("meals"),
    MARKETING("marketing"),
    PROFESSIONAL_SERVICES("professional_services"),
    UTILITIES("utilities"),
    RENT("rent"),
    INSURANCE("insurance"),
    TAXES("taxes"),
    OTHER("other");
}

/**
 * Bank connection providers
 */
enum class BankProvider(override val dbValue: String) : DbEnum {
    PLAID("plaid"),
    TINK("tink"),
    NORDIGEN("nordigen"),
    YAPILY("yapily"),
    MANUAL("manual");
}

/**
 * Bank account types
 */
enum class BankAccountType(override val dbValue: String) : DbEnum {
    CHECKING("checking"),
    SAVINGS("savings"),
    CREDIT_CARD("credit_card"),
    INVESTMENT("investment"),
    OTHER("other");
}

/**
 * VAT return status
 */
enum class VatReturnStatus(override val dbValue: String) : DbEnum {
    DRAFT("draft"),              // Being prepared
    FILED("filed"),              // Submitted to authorities
    PAID("paid"),                // Payment made/received
    AMENDED("amended");          // Correction filed
}

/**
 * Peppol e-invoice status
 */
enum class PeppolStatus(override val dbValue: String) : DbEnum {
    PENDING("pending"),
    SENT("sent"),
    DELIVERED("delivered"),
    ACCEPTED("accepted"),
    REJECTED("rejected"),
    ERROR("error");
}

/**
 * Entity types for attachments and audit logs
 */
enum class EntityType(override val dbValue: String) : DbEnum {
    INVOICE("invoice"),
    EXPENSE("expense"),
    CLIENT("client"),
    PAYMENT("payment"),
    VAT_RETURN("vat_return"),
    BANK_TRANSACTION("bank_transaction"),
    USER("user"),
    TENANT("tenant");
}

/**
 * Currency codes (ISO 4217)
 */
enum class Currency(override val dbValue: String) : DbEnum {
    EUR("EUR"),
    USD("USD"),
    GBP("GBP"),
    CHF("CHF"),
    SEK("SEK"),
    NOK("NOK"),
    DKK("DKK"),
    PLN("PLN"),
    CZK("CZK");
}

/**
 * Supported languages
 */
enum class Language(override val dbValue: String) : DbEnum {
    EN("en"),    // English
    NL("nl"),    // Dutch
    FR("fr"),    // French
    DE("de");    // German
}

/**
 * Audit log actions
 */
enum class AuditAction(override val dbValue: String) : DbEnum {
    // Invoice actions
    INVOICE_CREATED("invoice.created"),
    INVOICE_UPDATED("invoice.updated"),
    INVOICE_STATUS_CHANGED("invoice.status_changed"),
    INVOICE_SENT("invoice.sent"),
    INVOICE_CANCELLED("invoice.cancelled"),

    // Payment actions
    PAYMENT_RECORDED("payment.recorded"),
    PAYMENT_UPDATED("payment.updated"),
    PAYMENT_DELETED("payment.deleted"),

    // Expense actions
    EXPENSE_CREATED("expense.created"),
    EXPENSE_UPDATED("expense.updated"),
    EXPENSE_DELETED("expense.deleted"),

    // Client actions
    CLIENT_CREATED("client.created"),
    CLIENT_UPDATED("client.updated"),
    CLIENT_DEACTIVATED("client.deactivated"),

    // User actions
    USER_LOGIN("user.login"),
    USER_LOGOUT("user.logout"),
    USER_CREATED("user.created"),
    USER_UPDATED("user.updated"),
    USER_DEACTIVATED("user.deactivated"),

    // Bank actions
    BANK_CONNECTED("bank.connected"),
    BANK_SYNC("bank.sync"),
    BANK_DISCONNECTED("bank.disconnected"),
    TRANSACTION_RECONCILED("transaction.reconciled"),

    // VAT actions
    VAT_RETURN_CREATED("vat_return.created"),
    VAT_RETURN_FILED("vat_return.filed"),
    VAT_RETURN_PAID("vat_return.paid");
}