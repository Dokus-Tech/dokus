package ai.dokus.foundation.ktor.lookup

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
import org.slf4j.LoggerFactory

/**
 * Client for CBE (Crossroads Bank for Enterprises) API.
 * Provides company lookup functionality for Belgian enterprises.
 *
 * API Documentation: https://cbeapi.be
 */
class CbeApiClient(
    private val httpClient: HttpClient,
    private val appId: String = "63NY0WSQ",
    private val appSecret: String = "rK1Enmltoey8DYnw5raPnPNFN1xuoGxG",
) {
    private val logger = LoggerFactory.getLogger(CbeApiClient::class.java)
    private val baseUrl = "https://api.cbeapi.be"

    /**
     * Search for companies by name.
     * @param name Company name to search for (min 3 characters)
     * @return List of matching entities
     */
    suspend fun searchByName(name: String): Result<List<EntityLookup>> = runCatching {
        logger.debug("Searching CBE for company: $name")

        val response = httpClient.get("$baseUrl/v2/search") {
            parameter("name", name)
            header("X-App-Id", appId)
            header("X-App-Secret", appSecret)
        }

        val cbeResponse = response.body<CbeSearchResponse>()
        logger.debug("CBE returned ${cbeResponse.enterprises.size} results for '$name'")

        cbeResponse.enterprises.map { it.toEntityLookup() }
    }.onFailure { e ->
        logger.error("CBE API search failed for '$name'", e)
    }
}

// ============================================================================
// CBE API Response Models (internal)
// ============================================================================

@Serializable
private data class CbeSearchResponse(
    val enterprises: List<CbeEnterprise> = emptyList(),
    val total: Int = 0,
)

@Serializable
private data class CbeEnterprise(
    @SerialName("enterprise_number")
    val enterpriseNumber: String,
    val name: String,
    val status: String? = null,
    val address: CbeAddress? = null,
)

@Serializable
private data class CbeAddress(
    val street: String? = null,
    @SerialName("house_number")
    val houseNumber: String? = null,
    @SerialName("box_number")
    val boxNumber: String? = null,
    val city: String? = null,
    @SerialName("postal_code")
    val postalCode: String? = null,
    val country: String? = null,
)

// ============================================================================
// Mapping Functions
// ============================================================================

private fun CbeEnterprise.toEntityLookup(): EntityLookup {
    // Format enterprise number to VAT number (BE + digits only)
    val vatNumber = enterpriseNumber
        .replace(".", "")
        .replace(" ", "")
        .let { "BE$it" }
        .let { VatNumber(it) }

    return EntityLookup(
        enterpriseNumber = enterpriseNumber,
        vatNumber = vatNumber,
        name = name,
        address = address?.toEntityAddress(),
        status = when (status?.lowercase()) {
            "active", "ac" -> EntityStatus.Active
            "inactive", "stopped", "st" -> EntityStatus.Inactive
            else -> EntityStatus.Unknown
        }
    )
}

private fun CbeAddress.toEntityAddress(): EntityAddress? {
    // Need at minimum street and city
    if (street.isNullOrBlank() || city.isNullOrBlank()) return null

    val streetLine1 = buildString {
        append(street)
        if (!houseNumber.isNullOrBlank()) {
            append(" $houseNumber")
        }
    }

    val streetLine2 = boxNumber?.takeIf { it.isNotBlank() }?.let { "Box $it" }

    return EntityAddress(
        streetLine1 = streetLine1,
        streetLine2 = streetLine2,
        city = city,
        postalCode = postalCode ?: "",
        country = Country.Belgium, // CBE is Belgium-only
    )
}
