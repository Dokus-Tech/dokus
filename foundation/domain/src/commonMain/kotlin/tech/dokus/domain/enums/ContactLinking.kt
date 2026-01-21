package tech.dokus.domain.enums

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Decision type for contact linking in document processing.
 */
@Serializable
enum class ContactLinkDecisionType {
    @SerialName("AUTO_LINK")
    AutoLink,

    @SerialName("SUGGEST")
    Suggest,

    @SerialName("NONE")
    None
}

/**
 * Policy for auto-linking contacts during AI processing.
 */
@Serializable
enum class ContactLinkPolicy {
    @SerialName("VAT_ONLY")
    VatOnly,

    @SerialName("VAT_OR_STRONG_SIGNALS")
    VatOrStrongSignals;

    companion object {
        fun fromConfig(raw: String?): ContactLinkPolicy {
            val normalized = raw?.trim()?.uppercase() ?: return VatOnly
            return when (normalized) {
                "VAT_OR_STRONG_SIGNALS" -> VatOrStrongSignals
                "VAT_ONLY" -> VatOnly
                else -> VatOnly
            }
        }
    }
}
