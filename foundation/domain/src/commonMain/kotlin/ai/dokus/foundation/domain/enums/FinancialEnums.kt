package ai.dokus.foundation.domain.enums

import ai.dokus.foundation.domain.database.DbEnum
import kotlinx.serialization.Serializable

// ============================================================================
// TENANT & USER ENUMS
// ============================================================================

@Serializable
enum class TenantPlan(override val dbValue: String) : DbEnum {
    Free("FREE"),
    Starter("STARTER"),
    Professional("PROFESSIONAL"),
    Enterprise("ENTERPRISE")
}

@Serializable
enum class TenantStatus(override val dbValue: String) : DbEnum {
    Active("ACTIVE"),
    Suspended("SUSPENDED"),
    Cancelled("CANCELLED"),
    Trial("TRIAL")
}

@Serializable
enum class Language(override val dbValue: String) : DbEnum {
    En("EN"),
    Fr("FR"),
    Nl("NL"),
    De("DE"),
    Es("ES"),
    It("IT")
}

@Serializable
enum class UserRole(override val dbValue: String) : DbEnum {
    Owner("OWNER"),
    Admin("ADMIN"),
    Accountant("ACCOUNTANT"),
    Editor("EDITOR"),
    Viewer("VIEWER");

    companion object {
        val all = listOf(Owner, Admin, Accountant, Editor, Viewer)
    }
}

@Serializable
enum class UserStatus(override val dbValue: String) : DbEnum {
    Active("ACTIVE"),
    PendingVerification("PENDING_VERIFICATION"),
    Suspended("SUSPENDED"),
    Locked("LOCKED"),
    Deleted("DELETED")
}

@Serializable
enum class Permission(override val dbValue: String) : DbEnum {
    // Invoices
    InvoicesRead("INVOICES_READ"),
    InvoicesCreate("INVOICES_CREATE"),
    InvoicesEdit("INVOICES_EDIT"),
    InvoicesDelete("INVOICES_DELETE"),
    InvoicesSend("INVOICES_SEND"),

    // Clients
    ClientsRead("CLIENTS_READ"),
    ClientsManage("CLIENTS_MANAGE"),

    // Settings
    SettingsRead("SETTINGS_READ"),
    SettingsManage("SETTINGS_MANAGE"),

    // Users & permissions
    UsersRead("USERS_READ"),
    UsersManage("USERS_MANAGE"),

    // Financial data
    ReportsView("REPORTS_VIEW"),
    ExportsCreate("EXPORTS_CREATE")
}

@Serializable
enum class SubscriptionTier(override val dbValue: String) : DbEnum {
    SelfHosted("SELF_HOSTED"),
    CloudFree("CLOUD_FREE"),
    CloudBasic("CLOUD_BASIC"),
    CloudPro("CLOUD_PRO")
}

// ============================================================================
// INVOICE ENUMS
// ============================================================================

@Serializable
enum class InvoiceStatus(override val dbValue: String) : DbEnum {
    Draft("DRAFT"),
    Sent("SENT"),
    Viewed("VIEWED"),
    PartiallyPaid("PARTIALLY_PAID"),
    Paid("PAID"),
    Overdue("OVERDUE"),
    Cancelled("CANCELLED"),
    Refunded("REFUNDED")
}

@Serializable
enum class Currency(override val dbValue: String) : DbEnum {
    Eur("EUR"),
    Usd("USD"),
    Gbp("GBP"),
    Chf("CHF"),
    Cad("CAD"),
    Aud("AUD")
}

@Serializable
enum class PeppolStatus(override val dbValue: String) : DbEnum {
    Pending("PENDING"),
    Sent("SENT"),
    Delivered("DELIVERED"),
    Failed("FAILED"),
    Rejected("REJECTED")
}

// ============================================================================
// EXPENSE ENUMS
// ============================================================================

@Serializable
enum class ExpenseCategory(override val dbValue: String) : DbEnum {
    OfficeSupplies("OFFICE_SUPPLIES"),
    Travel("TRAVEL"),
    Meals("MEALS"),
    Software("SOFTWARE"),
    Hardware("HARDWARE"),
    Utilities("UTILITIES"),
    Rent("RENT"),
    Insurance("INSURANCE"),
    Marketing("MARKETING"),
    ProfessionalServices("PROFESSIONAL_SERVICES"),
    Telecommunications("TELECOMMUNICATIONS"),
    Vehicle("VEHICLE"),
    Other("OTHER")
}

// ============================================================================
// PAYMENT ENUMS
// ============================================================================

@Serializable
enum class PaymentMethod(override val dbValue: String) : DbEnum {
    BankTransfer("BANK_TRANSFER"),
    CreditCard("CREDIT_CARD"),
    DebitCard("DEBIT_CARD"),
    PayPal("PAYPAL"),
    Stripe("STRIPE"),
    Cash("CASH"),
    Check("CHECK"),
    Other("OTHER")
}

// ============================================================================
// BANKING ENUMS
// ============================================================================

@Serializable
enum class BankProvider(override val dbValue: String) : DbEnum {
    Plaid("PLAID"),
    Yodlee("YODLEE"),
    Tink("TINK"),
    SaltEdge("SALT_EDGE"),
    Manual("MANUAL")
}

@Serializable
enum class BankAccountType(override val dbValue: String) : DbEnum {
    Checking("CHECKING"),
    Savings("SAVINGS"),
    CreditCard("CREDIT_CARD"),
    Business("BUSINESS"),
    Investment("INVESTMENT")
}

// ============================================================================
// VAT ENUMS
// ============================================================================

@Serializable
enum class VatReturnStatus(override val dbValue: String) : DbEnum {
    Draft("DRAFT"),
    Submitted("SUBMITTED"),
    Accepted("ACCEPTED"),
    Rejected("REJECTED"),
    Paid("PAID")
}

// ============================================================================
// AUDIT ENUMS
// ============================================================================

@Serializable
enum class AuditAction(override val dbValue: String) : DbEnum {
    // Invoice actions
    InvoiceCreated("INVOICE_CREATED"),
    InvoiceUpdated("INVOICE_UPDATED"),
    InvoiceDeleted("INVOICE_DELETED"),
    InvoiceSent("INVOICE_SENT"),
    InvoiceStatusChanged("INVOICE_STATUS_CHANGED"),

    // Payment actions
    PaymentRecorded("PAYMENT_RECORDED"),
    PaymentUpdated("PAYMENT_UPDATED"),
    PaymentDeleted("PAYMENT_DELETED"),

    // Expense actions
    ExpenseCreated("EXPENSE_CREATED"),
    ExpenseUpdated("EXPENSE_UPDATED"),
    ExpenseDeleted("EXPENSE_DELETED"),

    // Client actions
    ClientCreated("CLIENT_CREATED"),
    ClientUpdated("CLIENT_UPDATED"),
    ClientDeleted("CLIENT_DELETED"),

    // User actions
    UserLogin("USER_LOGIN"),
    UserLogout("USER_LOGOUT"),
    UserCreated("USER_CREATED"),
    UserUpdated("USER_UPDATED"),
    UserDeleted("USER_DELETED"),

    // Settings actions
    SettingsUpdated("SETTINGS_UPDATED"),

    // Banking actions
    BankConnected("BANK_CONNECTED"),
    BankDisconnected("BANK_DISCONNECTED"),
    BankSync("BANK_SYNC"),

    // VAT actions
    VatReturnCreated("VAT_RETURN_CREATED"),
    VatReturnSubmitted("VAT_RETURN_SUBMITTED"),
    VatReturnPaid("VAT_RETURN_PAID")
}

@Serializable
enum class EntityType(override val dbValue: String) : DbEnum {
    Invoice("INVOICE"),
    InvoiceItem("INVOICE_ITEM"),
    Client("CLIENT"),
    Expense("EXPENSE"),
    Payment("PAYMENT"),
    BankConnection("BANK_CONNECTION"),
    BankTransaction("BANK_TRANSACTION"),
    VatReturn("VAT_RETURN"),
    User("USER"),
    Tenant("TENANT"),
    Settings("SETTINGS"),
    Attachment("ATTACHMENT")
}