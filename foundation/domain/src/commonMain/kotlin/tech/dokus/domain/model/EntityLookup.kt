package tech.dokus.domain.model

import ai.dokus.foundation.domain.enums.Country
import ai.dokus.foundation.domain.ids.VatNumber
import kotlinx.serialization.Serializable

/**
 * Result from CBE (Crossroads Bank for Enterprises) company lookup.
 * Used to prefill workspace creation with official company data.
 */
@Serializable
data class EntityLookup(
    /** CBE enterprise number (e.g., "0123.456.789") */
    val enterpriseNumber: String,
    /** Formatted VAT number (e.g., "BE0123456789") */
    val vatNumber: VatNumber?,
    /** Official company legal name */
    val name: String,
    /** Registered business address */
    val address: EntityAddress?,
    /** Company status */
    val status: EntityStatus,
)

/**
 * Address from CBE lookup.
 */
@Serializable
data class EntityAddress(
    val streetLine1: String,
    val streetLine2: String? = null,
    val city: String,
    val postalCode: String,
    val country: Country = Country.Belgium,
)

/**
 * Company status from CBE.
 */
@Serializable
enum class EntityStatus {
    Active,
    Inactive,
    Unknown
}

/**
 * Response from company lookup endpoint.
 */
@Serializable
data class EntityLookupResponse(
    val results: List<EntityLookup>,
    val query: String,
    val totalCount: Int,
)
