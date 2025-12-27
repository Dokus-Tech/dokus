package tech.dokus.domain.model.auth

import kotlinx.serialization.Serializable

@Serializable
data class IpLocationInfo(
    val city: String?,
    val country: String? = null,
    val countryCode: String? = null,
    val region: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val postalCode: String? = null,
)