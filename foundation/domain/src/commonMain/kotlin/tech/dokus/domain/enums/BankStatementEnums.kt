package tech.dokus.domain.enums

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import tech.dokus.domain.database.DbEnum

@Serializable
enum class ImportedBankTransactionStatus(override val dbValue: String) : DbEnum {
    @SerialName("UNMATCHED")
    Unmatched("UNMATCHED"),

    @SerialName("SUGGESTED")
    Suggested("SUGGESTED"),

    @SerialName("LINKED")
    Linked("LINKED"),

    @SerialName("IGNORED")
    Ignored("IGNORED")
}

@Serializable
enum class PaymentCandidateTier(override val dbValue: String) : DbEnum {
    @SerialName("STRONG")
    Strong("STRONG"),

    @SerialName("POSSIBLE")
    Possible("POSSIBLE")
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

@Serializable
enum class AutoPaymentDecision(override val dbValue: String) : DbEnum {
    @SerialName("SKIPPED")
    Skipped("SKIPPED"),

    @SerialName("SUGGESTED_ONLY")
    SuggestedOnly("SUGGESTED_ONLY"),

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
