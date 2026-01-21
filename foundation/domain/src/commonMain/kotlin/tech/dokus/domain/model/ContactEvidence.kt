package tech.dokus.domain.model

import kotlinx.serialization.Serializable

/**
 * Evidence used for contact linking decisions.
 *
 * Stored on drafts for auditability and future policy tuning.
 */
@Serializable
data class ContactEvidence(
    val vatExtracted: String? = null,
    val vatValid: Boolean? = null,
    val vatMatched: Boolean? = null,
    val cbeExists: Boolean? = null,
    val ibanMatched: Boolean? = null,
    val nameSimilarity: Double? = null,
    val addressMatched: Boolean? = null,
    val ambiguityCount: Int? = null
)
