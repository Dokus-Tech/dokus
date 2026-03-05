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
