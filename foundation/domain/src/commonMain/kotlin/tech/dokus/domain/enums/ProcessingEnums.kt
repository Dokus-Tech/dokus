package tech.dokus.domain.enums

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import tech.dokus.domain.database.DbEnum

/**
 * Type of document detected during AI extraction.
 *
 * Note: This represents legal document types per the 4-axis classification system.
 * See CLASSIFICATION.md for full documentation.
 */
@Serializable
enum class DocumentType(
    override val dbValue: String,
    val supported: Boolean
) : DbEnum {

    // ═══════════════════════════════════════════════════════════════════
    // SALES (money coming in)
    // ═══════════════════════════════════════════════════════════════════

    @SerialName("INVOICE")
    Invoice("INVOICE", true),

    @SerialName("CREDIT_NOTE")
    CreditNote("CREDIT_NOTE", true),

    @SerialName("PRO_FORMA")
    ProForma("PRO_FORMA", false),

    @SerialName("QUOTE")
    Quote("QUOTE", false),

    @SerialName("ORDER_CONFIRMATION")
    OrderConfirmation("ORDER_CONFIRMATION", false),

    @SerialName("DELIVERY_NOTE")
    DeliveryNote("DELIVERY_NOTE", false),

    @SerialName("REMINDER")
    Reminder("REMINDER", false),

    @SerialName("STATEMENT_OF_ACCOUNT")
    StatementOfAccount("STATEMENT_OF_ACCOUNT", false),

    // ═══════════════════════════════════════════════════════════════════
    // PURCHASES (money going out)
    // ═══════════════════════════════════════════════════════════════════

    @SerialName("BILL")
    Bill("BILL", true),

    @SerialName("RECEIPT")
    Receipt("RECEIPT", true),

    @SerialName("PURCHASE_ORDER")
    PurchaseOrder("PURCHASE_ORDER", false),

    @SerialName("EXPENSE_CLAIM")
    ExpenseClaim("EXPENSE_CLAIM", false),

    // ═══════════════════════════════════════════════════════════════════
    // BANKING
    // ═══════════════════════════════════════════════════════════════════

    @SerialName("BANK_STATEMENT")
    BankStatement("BANK_STATEMENT", true),

    @SerialName("BANK_FEE")
    BankFee("BANK_FEE", false),

    @SerialName("INTEREST_STATEMENT")
    InterestStatement("INTEREST_STATEMENT", false),

    @SerialName("PAYMENT_CONFIRMATION")
    PaymentConfirmation("PAYMENT_CONFIRMATION", false),

    // ═══════════════════════════════════════════════════════════════════
    // VAT (Belgium)
    // ═══════════════════════════════════════════════════════════════════

    @SerialName("VAT_RETURN")
    VatReturn("VAT_RETURN", false),

    @SerialName("VAT_LISTING")
    VatListing("VAT_LISTING", false),

    @SerialName("VAT_ASSESSMENT")
    VatAssessment("VAT_ASSESSMENT", false),

    @SerialName("IC_LISTING")
    IcListing("IC_LISTING", false),

    @SerialName("OSS_RETURN")
    OssReturn("OSS_RETURN", false),

    // ═══════════════════════════════════════════════════════════════════
    // TAX - CORPORATE
    // ═══════════════════════════════════════════════════════════════════

    @SerialName("CORPORATE_TAX")
    CorporateTax("CORPORATE_TAX", false),

    @SerialName("CORPORATE_TAX_ADVANCE")
    CorporateTaxAdvance("CORPORATE_TAX_ADVANCE", false),

    @SerialName("TAX_ASSESSMENT")
    TaxAssessment("TAX_ASSESSMENT", false),

    // ═══════════════════════════════════════════════════════════════════
    // TAX - PERSONAL (eenmanszaak / freelancer)
    // ═══════════════════════════════════════════════════════════════════

    @SerialName("PERSONAL_TAX")
    PersonalTax("PERSONAL_TAX", false),

    @SerialName("WITHHOLDING_TAX")
    WithholdingTax("WITHHOLDING_TAX", false),

    // ═══════════════════════════════════════════════════════════════════
    // SOCIAL CONTRIBUTIONS (Belgium)
    // ═══════════════════════════════════════════════════════════════════

    @SerialName("SOCIAL_CONTRIBUTION")
    SocialContribution("SOCIAL_CONTRIBUTION", false),

    @SerialName("SOCIAL_FUND")
    SocialFund("SOCIAL_FUND", false),

    @SerialName("SELF_EMPLOYED_CONTRIBUTION")
    SelfEmployedContribution("SELF_EMPLOYED_CONTRIBUTION", false),

    @SerialName("VAPZ")
    Vapz("VAPZ", false),

    // ═══════════════════════════════════════════════════════════════════
    // PAYROLL / HR
    // ═══════════════════════════════════════════════════════════════════

    @SerialName("SALARY_SLIP")
    SalarySlip("SALARY_SLIP", false),

    @SerialName("PAYROLL_SUMMARY")
    PayrollSummary("PAYROLL_SUMMARY", false),

    @SerialName("EMPLOYMENT_CONTRACT")
    EmploymentContract("EMPLOYMENT_CONTRACT", false),

    @SerialName("DIMONA")
    Dimona("DIMONA", false),

    @SerialName("C4")
    C4("C4", false),

    @SerialName("HOLIDAY_PAY")
    HolidayPay("HOLIDAY_PAY", false),

    // ═══════════════════════════════════════════════════════════════════
    // LEGAL / CONTRACTS
    // ═══════════════════════════════════════════════════════════════════

    @SerialName("CONTRACT")
    Contract("CONTRACT", false),

    @SerialName("LEASE")
    Lease("LEASE", false),

    @SerialName("LOAN")
    Loan("LOAN", false),

    @SerialName("INSURANCE")
    Insurance("INSURANCE", false),

    // ═══════════════════════════════════════════════════════════════════
    // CORPORATE DOCUMENTS
    // ═══════════════════════════════════════════════════════════════════

    @SerialName("DIVIDEND")
    Dividend("DIVIDEND", false),

    @SerialName("SHAREHOLDER_REGISTER")
    ShareholderRegister("SHAREHOLDER_REGISTER", false),

    @SerialName("COMPANY_EXTRACT")
    CompanyExtract("COMPANY_EXTRACT", false),

    @SerialName("ANNUAL_ACCOUNTS")
    AnnualAccounts("ANNUAL_ACCOUNTS", false),

    @SerialName("BOARD_MINUTES")
    BoardMinutes("BOARD_MINUTES", false),

    // ═══════════════════════════════════════════════════════════════════
    // GOVERNMENT / REGULATORY
    // ═══════════════════════════════════════════════════════════════════

    @SerialName("SUBSIDY")
    Subsidy("SUBSIDY", false),

    @SerialName("FINE")
    Fine("FINE", false),

    @SerialName("PERMIT")
    Permit("PERMIT", false),

    // ═══════════════════════════════════════════════════════════════════
    // INTERNATIONAL TRADE
    // ═══════════════════════════════════════════════════════════════════

    @SerialName("CUSTOMS_DECLARATION")
    CustomsDeclaration("CUSTOMS_DECLARATION", false),

    @SerialName("INTRASTAT")
    Intrastat("INTRASTAT", false),

    // ═══════════════════════════════════════════════════════════════════
    // ASSETS
    // ═══════════════════════════════════════════════════════════════════

    @SerialName("DEPRECIATION_SCHEDULE")
    DepreciationSchedule("DEPRECIATION_SCHEDULE", false),

    @SerialName("INVENTORY")
    Inventory("INVENTORY", false),

    // ═══════════════════════════════════════════════════════════════════
    // CATCH-ALL
    // ═══════════════════════════════════════════════════════════════════

    @SerialName("OTHER")
    Other("OTHER", false),

    @SerialName("UNKNOWN")
    Unknown("UNKNOWN", true);
}

/**
 * Status of a document ingestion run (AI extraction attempt).
 * Simplified lifecycle: Queued -> Processing -> Succeeded/Failed
 */
@Serializable
enum class IngestionStatus(override val dbValue: String) : DbEnum {
    /** Run is queued for processing */
    @SerialName("QUEUED")
    Queued("QUEUED"),

    /** AI is actively extracting data */
    @SerialName("PROCESSING")
    Processing("PROCESSING"),

    /** Extraction completed successfully */
    @SerialName("SUCCEEDED")
    Succeeded("SUCCEEDED"),

    /** Extraction failed (may be retryable) */
    @SerialName("FAILED")
    Failed("FAILED");

    companion object {
        fun fromDbValue(value: String): IngestionStatus = entries.find { it.dbValue == value }!!
    }
}

/**
 * Outcome of a processing run, derived from confidence thresholds.
 * Stored on ingestion runs for audit/history.
 */
@Serializable
enum class ProcessingOutcome(override val dbValue: String) : DbEnum {
    /** Confidence meets auto-confirm threshold */
    @SerialName("AUTO_CONFIRM_ELIGIBLE")
    AutoConfirmEligible("AUTO_CONFIRM_ELIGIBLE"),

    /** Confidence below auto-confirm threshold; manual review required */
    @SerialName("MANUAL_REVIEW_REQUIRED")
    ManualReviewRequired("MANUAL_REVIEW_REQUIRED");

    companion object {
        fun fromDbValue(value: String): ProcessingOutcome = entries.find { it.dbValue == value }!!
    }
}

/**
 * Status of a document draft (editable extraction state).
 * Represents business review state, separate from ingestion lifecycle.
 */
@Serializable
enum class DocumentStatus(override val dbValue: String) : DbEnum {
    /** Draft has partial data and needs user review */
    @SerialName("NEEDS_REVIEW")
    NeedsReview("NEEDS_REVIEW"),

    /** Draft is ready for confirmation (all required fields present) */
    @SerialName("READY")
    Ready("READY"),

    /** User confirmed, financial entity created */
    @SerialName("CONFIRMED")
    Confirmed("CONFIRMED"),

    /** User rejected extraction */
    @SerialName("REJECTED")
    Rejected("REJECTED");

    companion object {
        fun fromDbValue(value: String): DocumentStatus = entries.find { it.dbValue == value }!!
    }
}

/**
 * Counterparty intent for document drafts.
 * Tracks whether the user has explicitly chosen to link or skip a counterparty.
 */
@Serializable
enum class CounterpartyIntent(override val dbValue: String) : DbEnum {
    @SerialName("NONE")
    None("NONE"),

    @SerialName("PENDING")
    Pending("PENDING");

    companion object {
        fun fromDbValue(value: String): CounterpartyIntent = entries.find { it.dbValue == value }!!
    }
}

/**
 * Reason for rejecting a document during confirmation.
 */
@Serializable
enum class DocumentRejectReason(override val dbValue: String) : DbEnum {
    @SerialName("NOT_MY_BUSINESS")
    NotMyBusiness("NOT_MY_BUSINESS"),

    @SerialName("DUPLICATE")
    Duplicate("DUPLICATE"),

    @SerialName("SPAM")
    Spam("SPAM"),

    @SerialName("TEST")
    Test("TEST"),

    @SerialName("OTHER")
    Other("OTHER");
}

/**
 * Status of document chunk indexing (RAG preparation).
 * Tracked separately from ingestion status to allow retry of indexing.
 */
@Serializable
enum class IndexingStatus(override val dbValue: String) : DbEnum {
    /** Indexing not yet started */
    @SerialName("PENDING")
    Pending("PENDING"),

    /** Chunks created and indexed successfully */
    @SerialName("SUCCEEDED")
    Succeeded("SUCCEEDED"),

    /** Indexing failed (retryable) */
    @SerialName("FAILED")
    Failed("FAILED");
}

/**
 * Type of link between documents.
 *
 * Used in DocumentLinksTable to track document-to-document relationships:
 * - ConvertedTo: ProForma converted to Invoice
 * - OriginalDocument: CreditNote referencing original Invoice/Bill
 * - RelatedTo: Generic document relationship
 *
 * No source/target type enums needed - they are derivable from the linked documents.
 */
@Serializable
enum class DocumentLinkType(override val dbValue: String) : DbEnum {
    /** ProForma → Invoice conversion */
    @SerialName("CONVERTED_TO")
    ConvertedTo("CONVERTED_TO"),

    /** CreditNote → Original Invoice/Bill reference */
    @SerialName("ORIGINAL_DOC")
    OriginalDocument("ORIGINAL_DOC"),

    /** Generic document relationship */
    @SerialName("RELATED_TO")
    RelatedTo("RELATED_TO");
}

enum class DocumentTypeCategory {
    FINANCIAL,  // Invoices, bills, quotes, orders, assets, financial statements
    BANKING,    // Bank statements, fees, payment confirmations
    TAX,        // VAT, corporate/personal tax, social contributions, trade reports
    PAYROLL,    // Payroll/HR documents
    LEGAL       // Contracts, corporate governance, permits, fines
}

val DocumentType.category: DocumentTypeCategory
    get() = when (this) {
        DocumentType.Invoice,
        DocumentType.CreditNote,
        DocumentType.ProForma,
        DocumentType.Quote,
        DocumentType.OrderConfirmation,
        DocumentType.DeliveryNote,
        DocumentType.Reminder,
        DocumentType.StatementOfAccount,
        DocumentType.Bill,
        DocumentType.Receipt,
        DocumentType.PurchaseOrder,
        DocumentType.ExpenseClaim,
        DocumentType.Dividend,
        DocumentType.AnnualAccounts,
        DocumentType.DepreciationSchedule,
        DocumentType.Inventory,
        DocumentType.Other,
        DocumentType.Unknown -> DocumentTypeCategory.FINANCIAL
        DocumentType.BankStatement,
        DocumentType.BankFee,
        DocumentType.InterestStatement,
        DocumentType.PaymentConfirmation -> DocumentTypeCategory.BANKING
        DocumentType.VatReturn,
        DocumentType.VatListing,
        DocumentType.VatAssessment,
        DocumentType.IcListing,
        DocumentType.OssReturn,
        DocumentType.CorporateTax,
        DocumentType.CorporateTaxAdvance,
        DocumentType.TaxAssessment,
        DocumentType.PersonalTax,
        DocumentType.WithholdingTax,
        DocumentType.SocialContribution,
        DocumentType.SocialFund,
        DocumentType.SelfEmployedContribution,
        DocumentType.Vapz,
        DocumentType.CustomsDeclaration,
        DocumentType.Intrastat -> DocumentTypeCategory.TAX
        DocumentType.SalarySlip,
        DocumentType.PayrollSummary,
        DocumentType.EmploymentContract,
        DocumentType.Dimona,
        DocumentType.C4,
        DocumentType.HolidayPay -> DocumentTypeCategory.PAYROLL
        DocumentType.Contract,
        DocumentType.Lease,
        DocumentType.Loan,
        DocumentType.Insurance,
        DocumentType.ShareholderRegister,
        DocumentType.CompanyExtract,
        DocumentType.BoardMinutes,
        DocumentType.Subsidy,
        DocumentType.Fine,
        DocumentType.Permit -> DocumentTypeCategory.LEGAL
    }
