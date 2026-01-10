package tech.dokus.domain.enums

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import tech.dokus.domain.database.DbEnum

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
    @SerialName("FREELANCER")
    Freelancer(
        dbValue = "FREELANCER",
        requiresDisplayName = false,
        legalNameFromUser = true
    ),

    @SerialName("COMPANY")
    Company(
        dbValue = "COMPANY",
        requiresDisplayName = true,
        legalNameFromUser = false
    )
}

@Serializable
enum class TenantPlan(override val dbValue: String) : DbEnum {
    @SerialName("FREE")
    Free("FREE"),

    @SerialName("STARTER")
    Starter("STARTER"),

    @SerialName("PROFESSIONAL")
    Professional("PROFESSIONAL"),

    @SerialName("ENTERPRISE")
    Enterprise("ENTERPRISE")
}

@Serializable
enum class TenantStatus(override val dbValue: String) : DbEnum {
    @SerialName("ACTIVE")
    Active("ACTIVE"),

    @SerialName("SUSPENDED")
    Suspended("SUSPENDED"),

    @SerialName("CANCELLED")
    Cancelled("CANCELLED"),

    @SerialName("TRIAL")
    Trial("TRIAL")
}

@Serializable
enum class Language(override val dbValue: String) : DbEnum {
    @SerialName("EN")
    En("EN"),

    @SerialName("FR")
    Fr("FR"),

    @SerialName("NL")
    Nl("NL"),

    @SerialName("DE")
    De("DE"),

    @SerialName("ES")
    Es("ES"),

    @SerialName("IT")
    It("IT")
}

@Serializable
enum class Country(override val dbValue: String) : DbEnum {
    @SerialName("BE")
    Belgium("BE"),

    @SerialName("NL")
    Netherlands("NL"),

    @SerialName("FR")
    France("FR")
}

@Serializable
enum class UserRole(override val dbValue: String) : DbEnum {
    @SerialName("OWNER")
    Owner("OWNER"),

    @SerialName("ADMIN")
    Admin("ADMIN"),

    @SerialName("ACCOUNTANT")
    Accountant("ACCOUNTANT"),

    @SerialName("EDITOR")
    Editor("EDITOR"),

    @SerialName("VIEWER")
    Viewer("VIEWER");

    companion object {
        val all = listOf(Owner, Admin, Accountant, Editor, Viewer)

        /** Roles that can be assigned to new members (excludes Owner) */
        val assignable = listOf(Admin, Accountant, Editor, Viewer)
    }
}

@Serializable
enum class InvitationStatus(override val dbValue: String) : DbEnum {
    @SerialName("PENDING")
    Pending("PENDING"),

    @SerialName("ACCEPTED")
    Accepted("ACCEPTED"),

    @SerialName("EXPIRED")
    Expired("EXPIRED"),

    @SerialName("CANCELLED")
    Cancelled("CANCELLED")
}

@Serializable
enum class Permission(override val dbValue: String) : DbEnum {
    // Invoices
    @SerialName("INVOICES_READ")
    InvoicesRead("INVOICES_READ"),

    @SerialName("INVOICES_CREATE")
    InvoicesCreate("INVOICES_CREATE"),

    @SerialName("INVOICES_EDIT")
    InvoicesEdit("INVOICES_EDIT"),

    @SerialName("INVOICES_DELETE")
    InvoicesDelete("INVOICES_DELETE"),

    @SerialName("INVOICES_SEND")
    InvoicesSend("INVOICES_SEND"),

    // Clients
    @SerialName("CLIENTS_READ")
    ClientsRead("CLIENTS_READ"),

    @SerialName("CLIENTS_MANAGE")
    ClientsManage("CLIENTS_MANAGE"),

    // Settings
    @SerialName("SETTINGS_READ")
    SettingsRead("SETTINGS_READ"),

    @SerialName("SETTINGS_MANAGE")
    SettingsManage("SETTINGS_MANAGE"),

    // Users & permissions
    @SerialName("USERS_READ")
    UsersRead("USERS_READ"),

    @SerialName("USERS_MANAGE")
    UsersManage("USERS_MANAGE"),

    // Financial data
    @SerialName("REPORTS_VIEW")
    ReportsView("REPORTS_VIEW"),

    @SerialName("EXPORTS_CREATE")
    ExportsCreate("EXPORTS_CREATE")
}

@Serializable
enum class SubscriptionTier(override val dbValue: String) : DbEnum {
    @SerialName("CORE")
    Core("CORE"),

    @SerialName("ONE")
    One("ONE"),

    @SerialName("SELF_HOSTED")
    SelfHosted("SELF_HOSTED"),

    @SerialName("CORE_FOUNDER")
    CoreFounder("CORE_FOUNDER");

    companion object {
        val default = Core

        /** Tiers with access to Tomorrow (AI/Forecast) features */
        val premiumTiers = setOf(One)

        fun hasTomorrowAccess(tier: SubscriptionTier): Boolean = tier in premiumTiers
    }
}

// ============================================================================
// CLIENT ENUMS
// ============================================================================

@Serializable
enum class ClientType(override val dbValue: String) : DbEnum {
    @SerialName("INDIVIDUAL")
    Individual("INDIVIDUAL"),

    @SerialName("BUSINESS")
    Business("BUSINESS"),

    @SerialName("GOVERNMENT")
    Government("GOVERNMENT")
}

// ============================================================================
// CONTACT ENUMS
// ============================================================================

// ContactType removed - roles are now derived from cashflow items
// See Contact.kt for DerivedContactRoles

/**
 * Source of contact creation - tracks how a contact was added to the system.
 */
@Serializable
enum class ContactSource(override val dbValue: String) : DbEnum {
    @SerialName("manual")
    Manual("manual"),

    @SerialName("ai")
    AI("ai"),

    @SerialName("peppol")
    Peppol("peppol")
}

// ============================================================================
// INVOICE ENUMS
// ============================================================================

@Serializable
enum class InvoiceStatus(override val dbValue: String) : DbEnum {
    @SerialName("DRAFT")
    Draft("DRAFT"),

    @SerialName("SENT")
    Sent("SENT"),

    @SerialName("VIEWED")
    Viewed("VIEWED"),

    @SerialName("PARTIALLY_PAID")
    PartiallyPaid("PARTIALLY_PAID"),

    @SerialName("PAID")
    Paid("PAID"),

    @SerialName("OVERDUE")
    Overdue("OVERDUE"),

    @SerialName("CANCELLED")
    Cancelled("CANCELLED"),

    @SerialName("REFUNDED")
    Refunded("REFUNDED")
}

@Serializable
enum class BillStatus(override val dbValue: String) : DbEnum {
    @SerialName("DRAFT")
    Draft("DRAFT"),

    @SerialName("PENDING")
    Pending("PENDING"),

    @SerialName("SCHEDULED")
    Scheduled("SCHEDULED"),

    @SerialName("PAID")
    Paid("PAID"),

    @SerialName("OVERDUE")
    Overdue("OVERDUE"),

    @SerialName("CANCELLED")
    Cancelled("CANCELLED")
}

@Serializable
enum class Currency(
    override val dbValue: String,
    val displayName: String,
    val displaySign: String,
) : DbEnum {
    @SerialName("EUR")
    Eur("EUR", "eur", "€"),

    @SerialName("USD")
    Usd("USD", "usd", "$"),

    @SerialName("GBP")
    Gbp("GBP", "gbp", "£");

    companion object {
        val default = Eur

        fun fromDbValue(dbValue: String): Currency? = Currency.entries.find {
            it.dbValue == dbValue
        }

        fun fromDisplay(displayValue: String?): Currency? = Currency.entries.find {
            it.displayName == displayValue || it.displaySign == displayValue
        }

        fun fromDisplayOrDefault(displayValue: String?): Currency =
            fromDisplay(displayValue) ?: default
    }
}

@Serializable
enum class PeppolStatus(override val dbValue: String) : DbEnum {
    @SerialName("PENDING")
    Pending("PENDING"),

    @SerialName("SENT")
    Sent("SENT"),

    @SerialName("DELIVERED")
    Delivered("DELIVERED"),

    @SerialName("FAILED")
    Failed("FAILED"),

    @SerialName("REJECTED")
    Rejected("REJECTED")
}

@Serializable
enum class PeppolTransmissionDirection(override val dbValue: String) : DbEnum {
    @SerialName("OUTBOUND")
    Outbound("OUTBOUND"), // Sending invoices to customers

    @SerialName("INBOUND")
    Inbound("INBOUND") // Receiving bills from suppliers
}

@Serializable
enum class PeppolDocumentType(override val dbValue: String) : DbEnum {
    @SerialName("INVOICE")
    Invoice("INVOICE"),

    @SerialName("CREDIT_NOTE")
    CreditNote("CREDIT_NOTE"),

    @SerialName("SELF_BILLING_INVOICE")
    SelfBillingInvoice("SELF_BILLING_INVOICE"),

    @SerialName("SELF_BILLING_CREDIT_NOTE")
    SelfBillingCreditNote("SELF_BILLING_CREDIT_NOTE"),

    @SerialName("XML")
    Xml("XML"); // Raw UBL XML

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
    @SerialName("S")
    Standard("S"), // Standard VAT rate

    @SerialName("Z")
    ZeroRated("Z"), // Zero rated

    @SerialName("E")
    Exempt("E"), // VAT exempt

    @SerialName("AE")
    ReverseCharge("AE"), // Reverse charge (EU cross-border)

    @SerialName("K")
    IntraCommSupply("K"), // Intra-community supply

    @SerialName("G")
    ExportOutsideEu("G"), // Export outside EU

    @SerialName("O")
    NotSubject("O"), // Not subject to VAT

    @SerialName("L")
    CanaryIslands("L"), // Canary Islands

    @SerialName("M")
    Ceuta("M"), // Ceuta and Melilla

    @SerialName("B")
    ServiceOutsideScope("B"); // Services outside scope of VAT

    companion object {
        fun fromCode(code: String): PeppolVatCategory =
            entries.find { it.dbValue == code } ?: Standard
    }
}

/**
 * Recommand API document direction.
 */
@Serializable
enum class RecommandDirection {
    @SerialName("incoming")
    Incoming,

    @SerialName("outgoing")
    Outgoing;

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
    @SerialName("pending")
    Pending,

    @SerialName("processing")
    Processing,

    @SerialName("delivered")
    Delivered,

    @SerialName("failed")
    Failed,

    @SerialName("rejected")
    Rejected;

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
    @SerialName("1")
    NotDefined("1", "Not defined"),

    @SerialName("10")
    Cash("10", "Cash"),

    @SerialName("20")
    Cheque("20", "Cheque"),

    @SerialName("30")
    CreditTransfer("30", "Credit transfer"),

    @SerialName("31")
    DebitTransfer("31", "Debit transfer"),

    @SerialName("42")
    CashOnDelivery("42", "Payment to bank account"),

    @SerialName("48")
    BankCard("48", "Bank card"),

    @SerialName("49")
    DirectDebit("49", "Direct debit"),

    @SerialName("57")
    StandingAgreement("57", "Standing agreement"),

    @SerialName("58")
    SepaCreditTransfer("58", "SEPA credit transfer"),

    @SerialName("59")
    SepaDirectDebit("59", "SEPA direct debit"),

    @SerialName("68")
    OnlinePayment("68", "Online payment service");

    companion object {
        fun fromCode(code: String): PaymentMeansCode =
            entries.find { it.code == code } ?: CreditTransfer
    }
}

/**
 * ISO 4217 Currency codes commonly used in Peppol.
 */
@Serializable
enum class PeppolCurrency(val code: String, val displayName: String) {
    @SerialName("EUR")
    Eur("EUR", "Euro"),

    @SerialName("USD")
    Usd("USD", "US Dollar"),

    @SerialName("GBP")
    Gbp("GBP", "British Pound"),

    @SerialName("CHF")
    Chf("CHF", "Swiss Franc"),

    @SerialName("SEK")
    Sek("SEK", "Swedish Krona"),

    @SerialName("NOK")
    Nok("NOK", "Norwegian Krone"),

    @SerialName("DKK")
    Dkk("DKK", "Danish Krone"),

    @SerialName("PLN")
    Pln("PLN", "Polish Zloty"),

    @SerialName("CZK")
    Czk("CZK", "Czech Koruna"),

    @SerialName("HUF")
    Huf("HUF", "Hungarian Forint"),

    @SerialName("RON")
    Ron("RON", "Romanian Leu"),

    @SerialName("BGN")
    Bgn("BGN", "Bulgarian Lev"),

    @SerialName("HRK")
    Hrk("HRK", "Croatian Kuna"),

    @SerialName("ISK")
    Isk("ISK", "Icelandic Krona"),

    @SerialName("JPY")
    Jpy("JPY", "Japanese Yen"),

    @SerialName("AUD")
    Aud("AUD", "Australian Dollar"),

    @SerialName("CAD")
    Cad("CAD", "Canadian Dollar"),

    @SerialName("NZD")
    Nzd("NZD", "New Zealand Dollar"),

    @SerialName("SGD")
    Sgd("SGD", "Singapore Dollar");

    companion object {
        fun fromCode(code: String): PeppolCurrency? = entries.find { it.code == code.uppercase() }
        fun fromCodeOrDefault(code: String): PeppolCurrency = fromCode(code) ?: Eur
    }
}

/**
 * UNCL5305 Unit codes commonly used in Peppol invoices.
 */
@Serializable
enum class UnitCode(val code: String, val description: String) {
    @SerialName("C62")
    Each("C62", "One/Unit"),

    @SerialName("HUR")
    Hour("HUR", "Hour"),

    @SerialName("DAY")
    Day("DAY", "Day"),

    @SerialName("WEE")
    Week("WEE", "Week"),

    @SerialName("MON")
    Month("MON", "Month"),

    @SerialName("ANN")
    Year("ANN", "Year"),

    @SerialName("KGM")
    Kilogram("KGM", "Kilogram"),

    @SerialName("GRM")
    Gram("GRM", "Gram"),

    @SerialName("LTR")
    Litre("LTR", "Litre"),

    @SerialName("MTR")
    Metre("MTR", "Metre"),

    @SerialName("MTK")
    SquareMetre("MTK", "Square metre"),

    @SerialName("MTQ")
    CubicMetre("MTQ", "Cubic metre"),

    @SerialName("PCE")
    Piece("PCE", "Piece"),

    @SerialName("SET")
    Set("SET", "Set"),

    @SerialName("PR")
    Pair("PR", "Pair"),

    @SerialName("DZN")
    Dozen("DZN", "Dozen"),

    @SerialName("PK")
    Package("PK", "Package"),

    @SerialName("BX")
    Box("BX", "Box");

    companion object {
        fun fromCode(code: String): UnitCode = entries.find { it.code == code } ?: Each
    }
}

// ============================================================================
// CASHFLOW ENTRIES ENUMS
// ============================================================================

@Serializable
enum class CashflowSourceType(override val dbValue: String) : DbEnum {
    @SerialName("INVOICE")
    Invoice("INVOICE"),

    @SerialName("BILL")
    Bill("BILL"),

    @SerialName("EXPENSE")
    Expense("EXPENSE"),

    @SerialName("MANUAL")
    Manual("MANUAL")
}

@Serializable
enum class CashflowDirection(override val dbValue: String) : DbEnum {
    @SerialName("IN")
    In("IN"),

    @SerialName("OUT")
    Out("OUT")
}

@Serializable
enum class CashflowEntryStatus(override val dbValue: String) : DbEnum {
    @SerialName("OPEN")
    Open("OPEN"),

    @SerialName("PAID")
    Paid("PAID"),

    @SerialName("OVERDUE")
    Overdue("OVERDUE"),

    @SerialName("CANCELLED")
    Cancelled("CANCELLED")
}

// ============================================================================
// EXPENSE ENUMS
// ============================================================================

@Serializable
enum class ExpenseCategory(override val dbValue: String) : DbEnum {
    @SerialName("OFFICE_SUPPLIES")
    OfficeSupplies("OFFICE_SUPPLIES"),

    @SerialName("TRAVEL")
    Travel("TRAVEL"),

    @SerialName("MEALS")
    Meals("MEALS"),

    @SerialName("SOFTWARE")
    Software("SOFTWARE"),

    @SerialName("HARDWARE")
    Hardware("HARDWARE"),

    @SerialName("UTILITIES")
    Utilities("UTILITIES"),

    @SerialName("RENT")
    Rent("RENT"),

    @SerialName("INSURANCE")
    Insurance("INSURANCE"),

    @SerialName("MARKETING")
    Marketing("MARKETING"),

    @SerialName("PROFESSIONAL_SERVICES")
    ProfessionalServices("PROFESSIONAL_SERVICES"),

    @SerialName("TELECOMMUNICATIONS")
    Telecommunications("TELECOMMUNICATIONS"),

    @SerialName("VEHICLE")
    Vehicle("VEHICLE"),

    @SerialName("OTHER")
    Other("OTHER")
}

// ============================================================================
// PAYMENT ENUMS
// ============================================================================

@Serializable
enum class PaymentMethod(override val dbValue: String) : DbEnum {
    @SerialName("BANK_TRANSFER")
    BankTransfer("BANK_TRANSFER"),

    @SerialName("CREDIT_CARD")
    CreditCard("CREDIT_CARD"),

    @SerialName("DEBIT_CARD")
    DebitCard("DEBIT_CARD"),

    @SerialName("PAYPAL")
    PayPal("PAYPAL"),

    @SerialName("STRIPE")
    Stripe("STRIPE"),

    @SerialName("CASH")
    Cash("CASH"),

    @SerialName("CHECK")
    Check("CHECK"),

    @SerialName("OTHER")
    Other("OTHER")
}

// ============================================================================
// BANKING ENUMS
// ============================================================================

@Serializable
enum class BankProvider(override val dbValue: String) : DbEnum {
    @SerialName("PLAID")
    Plaid("PLAID"),

    @SerialName("YODLEE")
    Yodlee("YODLEE"),

    @SerialName("TINK")
    Tink("TINK"),

    @SerialName("SALT_EDGE")
    SaltEdge("SALT_EDGE"),

    @SerialName("MANUAL")
    Manual("MANUAL")
}

@Serializable
enum class BankAccountType(override val dbValue: String) : DbEnum {
    @SerialName("CHECKING")
    Checking("CHECKING"),

    @SerialName("SAVINGS")
    Savings("SAVINGS"),

    @SerialName("CREDIT_CARD")
    CreditCard("CREDIT_CARD"),

    @SerialName("BUSINESS")
    Business("BUSINESS"),

    @SerialName("INVESTMENT")
    Investment("INVESTMENT")
}

// ============================================================================
// VAT ENUMS
// ============================================================================

@Serializable
enum class VatReturnStatus(override val dbValue: String) : DbEnum {
    @SerialName("DRAFT")
    Draft("DRAFT"),

    @SerialName("SUBMITTED")
    Submitted("SUBMITTED"),

    @SerialName("ACCEPTED")
    Accepted("ACCEPTED"),

    @SerialName("REJECTED")
    Rejected("REJECTED"),

    @SerialName("PAID")
    Paid("PAID")
}

// ============================================================================
// AUDIT ENUMS
// ============================================================================

@Serializable
enum class AuditAction(override val dbValue: String) : DbEnum {
    // Invoice actions
    @SerialName("INVOICE_CREATED")
    InvoiceCreated("INVOICE_CREATED"),

    @SerialName("INVOICE_UPDATED")
    InvoiceUpdated("INVOICE_UPDATED"),

    @SerialName("INVOICE_DELETED")
    InvoiceDeleted("INVOICE_DELETED"),

    @SerialName("INVOICE_SENT")
    InvoiceSent("INVOICE_SENT"),

    @SerialName("INVOICE_STATUS_CHANGED")
    InvoiceStatusChanged("INVOICE_STATUS_CHANGED"),

    // Payment actions
    @SerialName("PAYMENT_RECORDED")
    PaymentRecorded("PAYMENT_RECORDED"),

    @SerialName("PAYMENT_UPDATED")
    PaymentUpdated("PAYMENT_UPDATED"),

    @SerialName("PAYMENT_DELETED")
    PaymentDeleted("PAYMENT_DELETED"),

    // Expense actions
    @SerialName("EXPENSE_CREATED")
    ExpenseCreated("EXPENSE_CREATED"),

    @SerialName("EXPENSE_UPDATED")
    ExpenseUpdated("EXPENSE_UPDATED"),

    @SerialName("EXPENSE_DELETED")
    ExpenseDeleted("EXPENSE_DELETED"),

    // Bill actions
    @SerialName("BILL_CREATED")
    BillCreated("BILL_CREATED"),

    @SerialName("BILL_UPDATED")
    BillUpdated("BILL_UPDATED"),

    @SerialName("BILL_DELETED")
    BillDeleted("BILL_DELETED"),

    @SerialName("BILL_PAID")
    BillPaid("BILL_PAID"),

    @SerialName("BILL_STATUS_CHANGED")
    BillStatusChanged("BILL_STATUS_CHANGED"),

    // Client actions (legacy)
    @SerialName("CLIENT_CREATED")
    ClientCreated("CLIENT_CREATED"),

    @SerialName("CLIENT_UPDATED")
    ClientUpdated("CLIENT_UPDATED"),

    @SerialName("CLIENT_DELETED")
    ClientDeleted("CLIENT_DELETED"),

    // Contact actions
    @SerialName("CONTACT_CREATED")
    ContactCreated("CONTACT_CREATED"),

    @SerialName("CONTACT_UPDATED")
    ContactUpdated("CONTACT_UPDATED"),

    @SerialName("CONTACT_DELETED")
    ContactDeleted("CONTACT_DELETED"),

    @SerialName("CONTACT_NOTE_CREATED")
    ContactNoteCreated("CONTACT_NOTE_CREATED"),

    @SerialName("CONTACT_NOTE_UPDATED")
    ContactNoteUpdated("CONTACT_NOTE_UPDATED"),

    @SerialName("CONTACT_NOTE_DELETED")
    ContactNoteDeleted("CONTACT_NOTE_DELETED"),

    // User actions
    @SerialName("USER_LOGIN")
    UserLogin("USER_LOGIN"),

    @SerialName("USER_LOGOUT")
    UserLogout("USER_LOGOUT"),

    @SerialName("USER_CREATED")
    UserCreated("USER_CREATED"),

    @SerialName("USER_UPDATED")
    UserUpdated("USER_UPDATED"),

    @SerialName("USER_DELETED")
    UserDeleted("USER_DELETED"),

    // Settings actions
    @SerialName("SETTINGS_UPDATED")
    SettingsUpdated("SETTINGS_UPDATED"),

    // Banking actions
    @SerialName("BANK_CONNECTED")
    BankConnected("BANK_CONNECTED"),

    @SerialName("BANK_DISCONNECTED")
    BankDisconnected("BANK_DISCONNECTED"),

    @SerialName("BANK_SYNC")
    BankSync("BANK_SYNC"),

    // VAT actions
    @SerialName("VAT_RETURN_CREATED")
    VatReturnCreated("VAT_RETURN_CREATED"),

    @SerialName("VAT_RETURN_SUBMITTED")
    VatReturnSubmitted("VAT_RETURN_SUBMITTED"),

    @SerialName("VAT_RETURN_PAID")
    VatReturnPaid("VAT_RETURN_PAID")
}

@Serializable
enum class EntityType(override val dbValue: String) : DbEnum {
    @SerialName("INVOICE")
    Invoice("INVOICE"),

    @SerialName("INVOICE_ITEM")
    InvoiceItem("INVOICE_ITEM"),

    @SerialName("CLIENT")
    Client("CLIENT"),

    @SerialName("CONTACT")
    Contact("CONTACT"),

    @SerialName("CONTACT_NOTE")
    ContactNote("CONTACT_NOTE"),

    @SerialName("EXPENSE")
    Expense("EXPENSE"),

    @SerialName("BILL")
    Bill("BILL"),

    @SerialName("PAYMENT")
    Payment("PAYMENT"),

    @SerialName("BANK_CONNECTION")
    BankConnection("BANK_CONNECTION"),

    @SerialName("BANK_TRANSACTION")
    BankTransaction("BANK_TRANSACTION"),

    @SerialName("VAT_RETURN")
    VatReturn("VAT_RETURN"),

    @SerialName("USER")
    User("USER"),

    @SerialName("TENANT")
    Tenant("TENANT"),

    @SerialName("SETTINGS")
    Settings("SETTINGS"),

    @SerialName("ATTACHMENT")
    Attachment("ATTACHMENT"),

    @SerialName("MEDIA")
    Media("MEDIA")
}
