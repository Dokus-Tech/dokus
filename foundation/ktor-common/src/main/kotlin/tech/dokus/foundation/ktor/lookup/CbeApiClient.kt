package tech.dokus.foundation.ktor.lookup

import ai.dokus.foundation.domain.enums.Country
import ai.dokus.foundation.domain.ids.VatNumber
import ai.dokus.foundation.domain.model.EntityAddress
import ai.dokus.foundation.domain.model.EntityLookup
import ai.dokus.foundation.domain.model.EntityStatus
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import tech.dokus.foundation.ktor.utils.loggerFor

/**
 * Client for CBE (Crossroads Bank for Enterprises) API.
 * Provides company lookup functionality for Belgian enterprises.
 *
 * API Documentation: https://cbeapi.be/docs/api
 */
class CbeApiClient(
    private val httpClient: HttpClient,
    private val apiSecret: String,
) {
    private val logger = loggerFor()
    private val baseUrl = "https://cbeapi.be/api"

    /**
     * Search for companies by name.
     * @param name Company name to search for (min 3 characters)
     * @return List of matching entities
     */
    suspend fun searchByName(name: String): Result<List<EntityLookup>> = runCatching {
        logger.debug("Searching CBE for company: $name")

        val response = httpClient.get("$baseUrl/v1/company/search") {
            parameter("name", name)
            header("Authorization", "Bearer $apiSecret")
        }

        val cbeResponse = response.body<CbeSearchResponse>()
        logger.debug("CBE returned ${cbeResponse.data.size} results for '$name'")

        cbeResponse.data.map { it.toEntityLookup() }
    }.onFailure { e ->
        logger.error("CBE API search failed for '$name'", e)
    }
}

// ============================================================================
// CBE API Response Models (internal)
// ============================================================================

@Serializable
private data class CbeSearchResponse(
    val data: List<CbeCompany> = emptyList(),
)

@Serializable
private data class CbeCompany(
    @SerialName("cbe_number")
    val cbeNumber: String,
    @SerialName("cbe_number_formatted")
    val cbeNumberFormatted: String? = null,
    val denomination: String? = null,
    @SerialName("commercial_name")
    val commercialName: String? = null,
    val abbreviation: String? = null,
    val status: String? = null,
    val address: CbeAddress? = null,
)

@Serializable
private data class CbeAddress(
    val street: String? = null,
    @SerialName("street_number")
    val streetNumber: String? = null,
    val box: String? = null,
    @SerialName("post_code")
    val postCode: String? = null,
    val city: String? = null,
    @SerialName("country_code")
    val countryCode: String? = null,
    @SerialName("full_address")
    val fullAddress: String? = null,
)

// ============================================================================
// Mapping Functions
// ============================================================================

private fun CbeCompany.toEntityLookup(): EntityLookup {
    // Format enterprise number to VAT number (BE + digits only)
    val vatNumber = cbeNumber
        .replace(".", "")
        .replace(" ", "")
        .let { "BE$it" }
        .let { VatNumber(it) }

    // Use denomination, commercial name, or abbreviation as the name
    val companyName = denomination ?: commercialName ?: abbreviation ?: cbeNumber

    return EntityLookup(
        enterpriseNumber = cbeNumberFormatted ?: cbeNumber,
        vatNumber = vatNumber,
        name = companyName,
        address = address?.toEntityAddress(),
        status = when (status?.lowercase()) {
            "active" -> EntityStatus.Active
            "inactive", "stopped" -> EntityStatus.Inactive
            else -> EntityStatus.Unknown
        }
    )
}

private fun CbeAddress.toEntityAddress(): EntityAddress? {
    // Need at minimum street and city
    if (street.isNullOrBlank() || city.isNullOrBlank()) return null

    val streetLine1 = buildString {
        append(street)
        if (!streetNumber.isNullOrBlank()) {
            append(" $streetNumber")
        }
    }

    val streetLine2 = box?.takeIf { it.isNotBlank() }?.let { "Box $it" }

    return EntityAddress(
        streetLine1 = streetLine1,
        streetLine2 = streetLine2,
        city = city,
        postalCode = postCode ?: "",
        country = Country.Belgium, // CBE is Belgium-only
    )
}
