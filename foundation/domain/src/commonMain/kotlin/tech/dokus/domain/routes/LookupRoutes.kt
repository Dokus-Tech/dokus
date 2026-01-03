package tech.dokus.domain.routes

import io.ktor.resources.Resource
import kotlinx.serialization.Serializable
import tech.dokus.domain.LegalName
import tech.dokus.domain.ids.VatNumber

/**
 * Type-safe route definitions for Lookup API.
 * Base path: /api/v1/lookup
 *
 * Used for external data lookups (CBE company search, etc.)
 */
@Serializable
@Resource("/api/v1/lookup")
class Lookup {
    /**
     * GET /api/v1/lookup/company?name={name}
     * Search for companies by name in CBE (Crossroads Bank for Enterprises)
     */
    @Serializable
    @Resource("company")
    data class Company(val parent: Lookup = Lookup(), val name: LegalName, val number: VatNumber)
}
