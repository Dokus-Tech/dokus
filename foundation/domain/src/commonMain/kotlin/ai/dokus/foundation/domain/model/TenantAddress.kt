package ai.dokus.foundation.domain.model

import ai.dokus.foundation.domain.enums.Country
import kotlinx.serialization.Serializable

@Serializable
data class UpsertTenantAddressRequest(
    val streetLine1: String,
    val streetLine2: String? = null,
    val city: String,
    val postalCode: String,
    val country: Country = Country.Belgium,
)

