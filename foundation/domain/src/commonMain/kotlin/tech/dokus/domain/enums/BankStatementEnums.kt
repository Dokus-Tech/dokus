package tech.dokus.domain.enums

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import tech.dokus.domain.database.DbEnum

@Serializable
enum class BankTransactionStatus(override val dbValue: String) : DbEnum {
    @SerialName("UNMATCHED")
    Unmatched("UNMATCHED"),

    @SerialName("NEEDS_REVIEW")
    NeedsReview("NEEDS_REVIEW"),

    @SerialName("MATCHED")
    Matched("MATCHED"),

    @SerialName("IGNORED")
    Ignored("IGNORED")
}

@Serializable
enum class BankTransactionSource(override val dbValue: String) : DbEnum {
    @SerialName("CODA")
    Coda("CODA"),

    @SerialName("MT940")
    Mt940("MT940"),

    @SerialName("PDF_STATEMENT")
    PdfStatement("PDF_STATEMENT"),

    @SerialName("PLAID")
    Plaid("PLAID"),

    @SerialName("TINK")
    Tink("TINK")
}

@Serializable
enum class ResolutionType(override val dbValue: String) : DbEnum {
    @SerialName("DOCUMENT")
    Document("DOCUMENT"),

    @SerialName("TRANSFER")
    Transfer("TRANSFER")
}

@Serializable
enum class MatchedBy(override val dbValue: String) : DbEnum {
    @SerialName("AUTO")
    Auto("AUTO"),

    @SerialName("REVIEW")
    Review("REVIEW"),

    @SerialName("MANUAL")
    Manual("MANUAL")
}

@Serializable
enum class IgnoredReason(override val dbValue: String) : DbEnum {
    @SerialName("BANK_FEE")
    BankFee("BANK_FEE"),

    @SerialName("DUPLICATE_IMPORT")
    DuplicateImport("DUPLICATE_IMPORT"),

    @SerialName("PERSONAL")
    Personal("PERSONAL"),

    @SerialName("NOT_BUSINESS")
    NotBusiness("NOT_BUSINESS"),

    @SerialName("IRRELEVANT")
    Irrelevant("IRRELEVANT"),

    @SerialName("OTHER")
    Other("OTHER")
}

@Serializable
enum class StatementTrust(override val dbValue: String) : DbEnum {
    @SerialName("HIGH")
    High("HIGH"),

    @SerialName("MEDIUM")
    Medium("MEDIUM"),

    @SerialName("LOW")
    Low("LOW")
}

@Serializable
enum class BankAccountStatus(override val dbValue: String) : DbEnum {
    @SerialName("CONFIRMED")
    Confirmed("CONFIRMED"),

    @SerialName("PENDING_REVIEW")
    PendingReview("PENDING_REVIEW")
}

@Serializable
enum class AutoMatchStatus(override val dbValue: String) : DbEnum {
    @SerialName("SUGGESTED")
    Suggested("SUGGESTED"),

    @SerialName("AUTO_MATCHED")
    AutoMatched("AUTO_MATCHED"),

    @SerialName("AUTO_PAID")
    AutoPaid("AUTO_PAID"),

    @SerialName("REVERSED")
    Reversed("REVERSED")
}

@Serializable
enum class AutoPaymentTriggerSource(override val dbValue: String) : DbEnum {
    @SerialName("INVOICE_CONFIRMED")
    InvoiceConfirmed("INVOICE_CONFIRMED"),

    @SerialName("BANK_IMPORT")
    BankImport("BANK_IMPORT"),

    @SerialName("CONTACT_UPDATED")
    ContactUpdated("CONTACT_UPDATED"),

    @SerialName("MANUAL_RETRY")
    ManualRetry("MANUAL_RETRY"),

    @SerialName("UNDO_REQUEST")
    UndoRequest("UNDO_REQUEST"),

    @SerialName("MANUAL_PAYMENT")
    ManualPayment("MANUAL_PAYMENT")
}

/**
 * Type of signal used during transaction-to-document matching.
 * Each signal contributes a weighted log-odds value to the Bayesian score.
 */
@Serializable
enum class MatchSignalType(override val dbValue: String) : DbEnum {
    @SerialName("OGM")
    Ogm("OGM"),

    @SerialName("INVOICE_REF")
    InvoiceRef("INVOICE_REF"),

    @SerialName("COUNTERPARTY_IBAN")
    CounterpartyIban("COUNTERPARTY_IBAN"),

    @SerialName("AMOUNT")
    Amount("AMOUNT"),

    @SerialName("CONTACT_NAME")
    ContactName("CONTACT_NAME"),

    @SerialName("DATE_PROXIMITY")
    DateProximity("DATE_PROXIMITY"),

    @SerialName("HISTORICAL_PATTERN")
    HistoricalPattern("HISTORICAL_PATTERN"),

    @SerialName("REJECTED_GUARD")
    RejectedGuard("REJECTED_GUARD"),
}

@Serializable
enum class AutoPaymentDecision(override val dbValue: String) : DbEnum {
    @SerialName("SKIPPED")
    Skipped("SKIPPED"),

    @SerialName("NEEDS_REVIEW_ONLY")
    NeedsReviewOnly("NEEDS_REVIEW_ONLY"),

    @SerialName("AUTO_MATCHED")
    AutoMatched("AUTO_MATCHED"),

    @SerialName("AUTO_PAID")
    AutoPaid("AUTO_PAID"),

    @SerialName("UNDO_APPLIED")
    UndoApplied("UNDO_APPLIED"),

    @SerialName("UNDO_REJECTED")
    UndoRejected("UNDO_REJECTED"),

    @SerialName("MANUAL_PAID")
    ManualPaid("MANUAL_PAID")
}
