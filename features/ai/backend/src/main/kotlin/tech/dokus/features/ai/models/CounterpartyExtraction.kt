package tech.dokus.features.ai.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class CounterpartyRole {
    @SerialName("SELLER")
    Seller,

    @SerialName("BUYER")
    Buyer,

    @SerialName("MERCHANT")
    Merchant,

    @SerialName("UNKNOWN")
    Unknown
}

@Serializable
data class CounterpartyExtraction(
    val name: String? = null,
    val vatNumber: String? = null,
    val email: String? = null,
    val streetLine1: String? = null,
    val postalCode: String? = null,
    val city: String? = null,
    val country: String? = null,
    val role: CounterpartyRole = CounterpartyRole.Unknown,
    val reasoning: String? = null
)
