package tech.dokus.foundation.backend.lookup

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import tech.dokus.domain.LegalName
import tech.dokus.domain.enums.Country
import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.model.entity.EntityAddress
import tech.dokus.domain.model.entity.EntityLookup
import tech.dokus.domain.model.entity.EntityStatus
import tech.dokus.foundation.backend.utils.loggerFor

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
    suspend fun searchByName(name: LegalName): Result<List<EntityLookup>> = runCatching {
        logger.debug("Searching CBE for company: $name")

        val response = httpClient.get("$baseUrl/v1/company/search") {
            parameter("name", name.value)
            header("Authorization", "Bearer $apiSecret")
        }

        val cbeResponse = response.body<CbeSearchResponse>()
        logger.debug("CBE returned ${cbeResponse.data.size} results for '$name'")

        cbeResponse.data.map { it.toEntityLookup() }
    }.onFailure { e ->
        logger.error("CBE API search failed for '$name'", e)
    }

    /**
     * Search for companies by name.
     * @param number Company VAT number to search for (min 3 characters)
     * @return List of matching entities
     */
    suspend fun searchByVat(number: VatNumber): Result<List<EntityLookup>> = runCatching {
        logger.debug("Searching CBE for company: $number")

        val response = httpClient.get("$baseUrl/v1/company/search/${number}") {
            header("Authorization", "Bearer $apiSecret")
        }

        val cbeResponse = response.body<CbeSearchResponse>()
        logger.debug("CBE returned ${cbeResponse.data.size} results for '$number'")

        cbeResponse.data.map { it.toEntityLookup() }
    }.onFailure { e ->
        logger.error("CBE API search failed for '$number'", e)
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
        name = LegalName(companyName),
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
