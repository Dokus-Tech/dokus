package tech.dokus.features.ai.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import tech.dokus.domain.enums.DocumentDirection

/**
 * Evidence source used to resolve business direction relative to tenant.
 */
@Serializable
enum class DirectionResolutionSource {
    @SerialName("VAT_MATCH")
    VatMatch,

    @SerialName("NAME_MATCH")
    NameMatch,

    @SerialName("AI_HINT")
    AiHint,

    @SerialName("UNKNOWN")
    Unknown,
}

/**
 * Deterministic direction resolution metadata for audit/debug.
 */
@Serializable
data class DirectionResolution(
    val direction: DocumentDirection = DocumentDirection.Unknown,
    val source: DirectionResolutionSource = DirectionResolutionSource.Unknown,
    val confidence: Double = 0.0,
    val matchedField: String? = null,
    val matchedValue: String? = null,
    val tenantVat: String? = null,
    val counterpartyVat: String? = null,
    val reasoning: String? = null,
)
