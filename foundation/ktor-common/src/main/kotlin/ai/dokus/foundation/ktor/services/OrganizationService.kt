package ai.dokus.foundation.ktor.services

import ai.dokus.foundation.domain.ids.InvoiceNumber
import ai.dokus.foundation.domain.ids.OrganizationId
import ai.dokus.foundation.domain.ids.VatNumber
import ai.dokus.foundation.domain.enums.Language
import ai.dokus.foundation.domain.enums.OrganizationPlan
import ai.dokus.foundation.domain.model.Organization
import ai.dokus.foundation.domain.model.OrganizationSettings
import kotlinx.rpc.annotations.Rpc

@Rpc
interface OrganizationService {
    /**
     * Creates a new tenant with default settings
     *
     * @param name The tenant's company or organization name
     * @param email The tenant's primary email address
     * @param plan The subscription plan (defaults to Free)
     * @param country The tenant's country code (ISO 3166-1 alpha-2)
     * @param language The tenant's preferred language
     * @param vatNumber The tenant's VAT number (optional)
     * @return The created tenant
     */
    suspend fun createTenant(
        name: String,
        email: String,
        plan: OrganizationPlan = OrganizationPlan.Free,
        country: String = "BE",
        language: Language = Language.En,
        vatNumber: VatNumber? = null
    ): Organization

    /**
     * Finds a tenant by their unique ID
     *
     * @param id The tenant's unique identifier
     * @return The tenant if found, null otherwise
     */
    suspend fun findById(id: OrganizationId): Organization?

    /**
     * Finds a tenant by their email address
     *
     * @param email The tenant's email address
     * @return The tenant if found, null otherwise
     */
    suspend fun findByEmail(email: String): Organization?

    /**
     * Updates the settings for a tenant
     *
     * @param settings The updated tenant settings
     */
    suspend fun updateSettings(settings: OrganizationSettings)

    /**
     * Retrieves the settings for a tenant
     *
     * @param organizationId The tenant's unique identifier
     * @return The tenant's settings
     * @throws IllegalArgumentException if no settings found for the tenant
     */
    suspend fun getSettings(organizationId: OrganizationId): OrganizationSettings

    /**
     * Retrieves and increments the next invoice number for a tenant
     * This operation is atomic and thread-safe
     *
     * @param organizationId The tenant's unique identifier
     * @return The next available invoice number
     */
    suspend fun getNextInvoiceNumber(organizationId: OrganizationId): InvoiceNumber

    /**
     * Lists all active tenants in the system
     *
     * @return List of active tenants
     */
    suspend fun listActiveTenants(): List<Organization>
}
