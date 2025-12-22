package ai.dokus.foundation.domain.enums

import ai.dokus.foundation.domain.database.DbEnum
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ============================================================================
// TENANT & USER ENUMS
// ============================================================================

@Serializable
enum class TenantType(
    override val dbValue: String,
    /** Whether this tenant type requires a separate display name (different from legal name) */
    val requiresDisplayName: Boolean,
    /** Whether the legal name should be derived from the user's name (and locked) */
    val legalNameFromUser: Boolean
) : DbEnum {
    Freelancer(
        dbValue = "FREELANCER",
        requiresDisplayName = false,
        legalNameFromUser = true
    ),
    Company(
        dbValue = "COMPANY",
        requiresDisplayName = true,
        legalNameFromUser = false
    )
}

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
enum class Country(override val dbValue: String) : DbEnum {
    Belgium("BE"),
    Netherlands("NL"),
    France("FR")
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
        /** Roles that can be assigned to new members (excludes Owner) */
        val assignable = listOf(Admin, Accountant, Editor, Viewer)
    }
}

@Serializable
enum class InvitationStatus(override val dbValue: String) : DbEnum {
    Pending("PENDING"),
    Accepted("ACCEPTED"),
    Expired("EXPIRED"),
    Cancelled("CANCELLED")
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
    CloudPro("CLOUD_PRO");

    companion object {
        val default = CloudFree
    }
}

// ============================================================================
// CLIENT ENUMS
// ============================================================================

@Serializable
enum class ClientType(override val dbValue: String) : DbEnum {
    Individual("INDIVIDUAL"),
    Business("BUSINESS"),
    Government("GOVERNMENT")
}

// ============================================================================
// CONTACT ENUMS
// ============================================================================

// ContactType removed - roles are now derived from cashflow items
// See Contact.kt for DerivedContactRoles

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
enum class BillStatus(override val dbValue: String) : DbEnum {
    Draft("DRAFT"),
    Pending("PENDING"),
    Scheduled("SCHEDULED"),
    Paid("PAID"),
    Overdue("OVERDUE"),
    Cancelled("CANCELLED")
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

@Serializable
enum class PeppolTransmissionDirection(override val dbValue: String) : DbEnum {
    Outbound("OUTBOUND"),  // Sending invoices to customers
    Inbound("INBOUND")     // Receiving bills from suppliers
}

@Serializable
enum class PeppolDocumentType(override val dbValue: String) : DbEnum {
    Invoice("INVOICE"),
    CreditNote("CREDIT_NOTE"),
    SelfBillingInvoice("SELF_BILLING_INVOICE"),
    SelfBillingCreditNote("SELF_BILLING_CREDIT_NOTE"),
    Xml("XML");  // Raw UBL XML

    companion object {
        /** Map from Recommand API values to enum */
        fun fromApiValue(value: String): PeppolDocumentType = when (value.lowercase()) {
            "invoice" -> Invoice
            "creditnote", "credit_note" -> CreditNote
            "selfbillinginvoice", "self_billing_invoice" -> SelfBillingInvoice
            "selfbillingcreditnote", "self_billing_credit_note" -> SelfBillingCreditNote
            "xml" -> Xml
            else -> Invoice // Default fallback
        }

        /** Convert to Recommand API value */
        fun PeppolDocumentType.toApiValue(): String = when (this) {
            Invoice -> "invoice"
            CreditNote -> "creditNote"
            SelfBillingInvoice -> "selfBillingInvoice"
            SelfBillingCreditNote -> "selfBillingCreditNote"
            Xml -> "xml"
        }
    }
}

@Serializable
enum class PeppolVatCategory(override val dbValue: String) : DbEnum {
    Standard("S"),           // Standard VAT rate
    ZeroRated("Z"),          // Zero rated
    Exempt("E"),             // VAT exempt
    ReverseCharge("AE"),     // Reverse charge (EU cross-border)
    IntraCommSupply("K"),    // Intra-community supply
    ExportOutsideEU("G"),    // Export outside EU
    NotSubject("O"),         // Not subject to VAT
    CanaryIslands("L"),      // Canary Islands
    Ceuta("M"),              // Ceuta and Melilla
    ServiceOutsideScope("B"); // Services outside scope of VAT

    companion object {
        fun fromCode(code: String): PeppolVatCategory = entries.find { it.dbValue == code } ?: Standard
    }
}

/**
 * Recommand API document direction.
 */
@Serializable
enum class RecommandDirection {
    @SerialName("incoming") Incoming,
    @SerialName("outgoing") Outgoing;

    companion object {
        fun fromString(value: String): RecommandDirection = when (value.lowercase()) {
            "incoming", "received", "inbound" -> Incoming
            "outgoing", "sent", "outbound" -> Outgoing
            else -> Incoming
        }
    }

    fun toPeppolDirection(): PeppolTransmissionDirection = when (this) {
        Incoming -> PeppolTransmissionDirection.Inbound
        Outgoing -> PeppolTransmissionDirection.Outbound
    }
}

/**
 * Recommand API document/transmission status.
 */
@Serializable
enum class RecommandDocumentStatus {
    @SerialName("pending") Pending,
    @SerialName("processing") Processing,
    @SerialName("delivered") Delivered,
    @SerialName("failed") Failed,
    @SerialName("rejected") Rejected;

    companion object {
        fun fromString(value: String): RecommandDocumentStatus = when (value.lowercase()) {
            "pending" -> Pending
            "processing" -> Processing
            "delivered", "sent" -> Delivered
            "failed", "error" -> Failed
            "rejected" -> Rejected
            else -> Pending
        }
    }

    fun toPeppolStatus(): PeppolStatus = when (this) {
        Pending -> PeppolStatus.Pending
        Processing -> PeppolStatus.Pending
        Delivered -> PeppolStatus.Delivered
        Failed -> PeppolStatus.Failed
        Rejected -> PeppolStatus.Rejected
    }
}

/**
 * UNCL4461 Payment Means Codes for Peppol.
 */
@Serializable
enum class PaymentMeansCode(val code: String, val description: String) {
    NotDefined("1", "Not defined"),
    Cash("10", "Cash"),
    Cheque("20", "Cheque"),
    CreditTransfer("30", "Credit transfer"),
    DebitTransfer("31", "Debit transfer"),
    CashOnDelivery("42", "Payment to bank account"),
    BankCard("48", "Bank card"),
    DirectDebit("49", "Direct debit"),
    StandingAgreement("57", "Standing agreement"),
    SEPACreditTransfer("58", "SEPA credit transfer"),
    SEPADirectDebit("59", "SEPA direct debit"),
    OnlinePayment("68", "Online payment service");

    companion object {
        fun fromCode(code: String): PaymentMeansCode = entries.find { it.code == code } ?: CreditTransfer
    }
}

/**
 * ISO 4217 Currency codes commonly used in Peppol.
 */
@Serializable
enum class PeppolCurrency(val code: String, val displayName: String) {
    EUR("EUR", "Euro"),
    USD("USD", "US Dollar"),
    GBP("GBP", "British Pound"),
    CHF("CHF", "Swiss Franc"),
    SEK("SEK", "Swedish Krona"),
    NOK("NOK", "Norwegian Krone"),
    DKK("DKK", "Danish Krone"),
    PLN("PLN", "Polish Zloty"),
    CZK("CZK", "Czech Koruna"),
    HUF("HUF", "Hungarian Forint"),
    RON("RON", "Romanian Leu"),
    BGN("BGN", "Bulgarian Lev"),
    HRK("HRK", "Croatian Kuna"),
    ISK("ISK", "Icelandic Krona"),
    JPY("JPY", "Japanese Yen"),
    AUD("AUD", "Australian Dollar"),
    CAD("CAD", "Canadian Dollar"),
    NZD("NZD", "New Zealand Dollar"),
    SGD("SGD", "Singapore Dollar");

    companion object {
        fun fromCode(code: String): PeppolCurrency? = entries.find { it.code == code.uppercase() }
        fun fromCodeOrDefault(code: String): PeppolCurrency = fromCode(code) ?: EUR
    }
}

/**
 * UNCL5305 Unit codes commonly used in Peppol invoices.
 */
@Serializable
enum class UnitCode(val code: String, val description: String) {
    Each("C62", "One/Unit"),
    Hour("HUR", "Hour"),
    Day("DAY", "Day"),
    Week("WEE", "Week"),
    Month("MON", "Month"),
    Year("ANN", "Year"),
    Kilogram("KGM", "Kilogram"),
    Gram("GRM", "Gram"),
    Litre("LTR", "Litre"),
    Metre("MTR", "Metre"),
    SquareMetre("MTK", "Square metre"),
    CubicMetre("MTQ", "Cubic metre"),
    Piece("PCE", "Piece"),
    Set("SET", "Set"),
    Pair("PR", "Pair"),
    Dozen("DZN", "Dozen"),
    Package("PK", "Package"),
    Box("BX", "Box");

    companion object {
        fun fromCode(code: String): UnitCode = entries.find { it.code == code } ?: Each
    }
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

    // Bill actions
    BillCreated("BILL_CREATED"),
    BillUpdated("BILL_UPDATED"),
    BillDeleted("BILL_DELETED"),
    BillPaid("BILL_PAID"),
    BillStatusChanged("BILL_STATUS_CHANGED"),

    // Client actions (legacy)
    ClientCreated("CLIENT_CREATED"),
    ClientUpdated("CLIENT_UPDATED"),
    ClientDeleted("CLIENT_DELETED"),

    // Contact actions
    ContactCreated("CONTACT_CREATED"),
    ContactUpdated("CONTACT_UPDATED"),
    ContactDeleted("CONTACT_DELETED"),
    ContactNoteCreated("CONTACT_NOTE_CREATED"),
    ContactNoteUpdated("CONTACT_NOTE_UPDATED"),
    ContactNoteDeleted("CONTACT_NOTE_DELETED"),

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
    Contact("CONTACT"),
    ContactNote("CONTACT_NOTE"),
    Expense("EXPENSE"),
    Bill("BILL"),
    Payment("PAYMENT"),
    BankConnection("BANK_CONNECTION"),
    BankTransaction("BANK_TRANSACTION"),
    VatReturn("VAT_RETURN"),
    User("USER"),
    Tenant("TENANT"),
    Settings("SETTINGS"),
    Attachment("ATTACHMENT"),
    Media("MEDIA")
}
