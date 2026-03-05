package tech.dokus.backend.services.contacts

import tech.dokus.database.repository.contacts.ContactRepository
import tech.dokus.database.repository.contacts.ContactAddressRepository
import tech.dokus.database.repository.peppol.PeppolDirectoryCacheRepository
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.common.PaginatedResponse
import tech.dokus.domain.model.contact.ContactDto
import tech.dokus.domain.model.contact.ContactStats
import tech.dokus.domain.model.contact.CreateContactRequest
import tech.dokus.domain.model.contact.UpdateContactRequest
import tech.dokus.domain.model.BusinessProfileUpdateResponse
import tech.dokus.domain.model.PinBusinessProfileFieldsRequest
import tech.dokus.domain.model.UpdateBusinessProfileRequest
import tech.dokus.backend.services.business.BusinessProfileService
import tech.dokus.foundation.backend.utils.loggerFor

/**
 * Service for contact business operations.
 *
 * Contacts represent both customers (who receive invoices) and vendors (who send inbound invoices).
 * This service handles all business logic related to contacts and delegates data access
 * to the repository layer.
 */
class ContactService(
    private val contactRepository: ContactRepository,
    private val contactAddressRepository: ContactAddressRepository,
    private val businessProfileService: BusinessProfileService,
    private val peppolCacheRepository: PeppolDirectoryCacheRepository? = null // Optional for cache invalidation
) {
    private val logger = loggerFor()

    /**
     * Create a new contact for a tenant.
     */
    suspend fun createContact(
        tenantId: TenantId,
        request: CreateContactRequest
    ): Result<ContactDto> {
        logger.info("Creating contact for tenant: $tenantId, name: ${request.name}")
        return runCatching {
            val created = contactRepository.createContact(tenantId, request).getOrThrow()
            var allAddressesPersisted = true
            for (addressInput in request.addresses) {
                contactAddressRepository.addAddress(
                    tenantId = tenantId,
                    contactId = created.id,
                    input = addressInput
                ).onFailure { e ->
                    allAddressesPersisted = false
                    logger.warn("Failed to persist contact address for ${created.id}: ${e.message}")
                }
            }

            if (allAddressesPersisted) {
                businessProfileService.enqueueContact(
                    tenantId = tenantId,
                    contactId = created.id,
                    triggerReason = "CONTACT_CREATED"
                ).onFailure { e ->
                    logger.warn("Failed to enqueue business enrichment for contact ${created.id}: ${e.message}")
                }
            } else {
                logger.warn("Skipping business enrichment enqueue for contact ${created.id} due to address persistence failure")
            }

            hydrateSingle(tenantId, created) ?: created
        }.onSuccess { logger.info("Contact created: ${it.id}") }
            .onFailure { logger.error("Failed to create contact for tenant: $tenantId", it) }
    }

    /**
     * Get a contact by ID.
     */
    suspend fun getContact(
        contactId: ContactId,
        tenantId: TenantId
    ): Result<ContactDto?> {
        logger.debug("Fetching contact: $contactId for tenant: $tenantId")
        return runCatching {
            val raw = contactRepository.getContact(contactId, tenantId).getOrThrow()
            raw?.let { hydrateSingle(tenantId, it) }
        }
            .onFailure { logger.error("Failed to fetch contact: $contactId", it) }
    }

    /**
     * List contacts with optional filters.
     * NOTE: peppolEnabled filter removed - PEPPOL status is now in PeppolDirectoryCacheTable
     */
    suspend fun listContacts(
        tenantId: TenantId,
        isActive: Boolean? = null,
        limit: Int = 50,
        offset: Int = 0
    ): Result<PaginatedResponse<ContactDto>> {
        logger.debug(
            "Listing contacts for tenant: $tenantId " +
                "(isActive=$isActive, limit=$limit, offset=$offset)"
        )
        return contactRepository.listContacts(
            tenantId,
            isActive,
            limit,
            offset
        )
            .map { page ->
                page.copy(items = hydrateContacts(tenantId, page.items))
            }
            .onSuccess { logger.debug("Retrieved ${it.items.size} contacts (total=${it.total})") }
            .onFailure { logger.error("Failed to list contacts for tenant: $tenantId", it) }
    }

    suspend fun lookupContacts(
        tenantId: TenantId,
        query: String,
        isActive: Boolean? = null,
        limit: Int = 50,
        offset: Int = 0
    ): Result<PaginatedResponse<ContactDto>> {
        logger.debug(
            "Lookup contacts for tenant: $tenantId " +
                "(queryLength=${query.length}, isActive=$isActive, limit=$limit, offset=$offset)"
        )
        return contactRepository.lookupContacts(
            tenantId = tenantId,
            query = query,
            isActive = isActive,
            limit = limit,
            offset = offset
        )
            .map { page ->
                page.copy(items = hydrateContacts(tenantId, page.items))
            }
            .onSuccess { logger.debug("Lookup returned ${it.items.size} contacts (total=${it.total})") }
            .onFailure { logger.error("Failed contact lookup for tenant: $tenantId", it) }
    }

    /**
     * List customers only (ContactType.Customer or ContactType.Both).
     */
    suspend fun listCustomers(
        tenantId: TenantId,
        isActive: Boolean? = true,
        limit: Int = 50,
        offset: Int = 0
    ): Result<PaginatedResponse<ContactDto>> {
        logger.debug("Listing customers for tenant: $tenantId")
        return contactRepository.listCustomers(tenantId, isActive, limit, offset)
            .map { page ->
                page.copy(items = hydrateContacts(tenantId, page.items))
            }
            .onSuccess { logger.debug("Retrieved ${it.items.size} customers (total=${it.total})") }
            .onFailure { logger.error("Failed to list customers for tenant: $tenantId", it) }
    }

    /**
     * List vendors only (ContactType.Vendor or ContactType.Both).
     */
    suspend fun listVendors(
        tenantId: TenantId,
        isActive: Boolean? = true,
        limit: Int = 50,
        offset: Int = 0
    ): Result<PaginatedResponse<ContactDto>> {
        logger.debug("Listing vendors for tenant: $tenantId")
        return contactRepository.listVendors(tenantId, isActive, limit, offset)
            .map { page ->
                page.copy(items = hydrateContacts(tenantId, page.items))
            }
            .onSuccess { logger.debug("Retrieved ${it.items.size} vendors (total=${it.total})") }
            .onFailure { logger.error("Failed to list vendors for tenant: $tenantId", it) }
    }

    /**
     * Update a contact.
     * Invalidates PEPPOL cache if VAT number or company number changes.
     */
    suspend fun updateContact(
        contactId: ContactId,
        tenantId: TenantId,
        request: UpdateContactRequest
    ): Result<ContactDto> {
        logger.info("Updating contact: $contactId for tenant: $tenantId")

        // Invalidate PEPPOL cache if VAT or company number is being updated
        // (cache staleness is detected by snapshot comparison, but proactive invalidation is cleaner)
        if (request.vatNumber != null || request.companyNumber != null) {
            peppolCacheRepository?.invalidateForContact(tenantId, contactId)?.onSuccess {
                if (it) logger.debug("Invalidated PEPPOL cache for contact: $contactId")
            }
        }

        return contactRepository.updateContact(contactId, tenantId, request)
            .map { updated ->
                hydrateSingle(tenantId, updated) ?: updated
            }
            .onSuccess { logger.info("Contact updated: $contactId") }
            .onFailure { logger.error("Failed to update contact: $contactId", it) }
    }

    // NOTE: updateContactPeppol() removed - PEPPOL status is now managed by PeppolDirectoryCacheRepository

    /**
     * Delete a contact.
     */
    suspend fun deleteContact(
        contactId: ContactId,
        tenantId: TenantId
    ): Result<Boolean> {
        logger.info("Deleting contact: $contactId")
        return contactRepository.deleteContact(contactId, tenantId)
            .onSuccess { logger.info("Contact deleted: $contactId") }
            .onFailure { logger.error("Failed to delete contact: $contactId", it) }
    }

    /**
     * Deactivate a contact (soft delete).
     */
    suspend fun deactivateContact(
        contactId: ContactId,
        tenantId: TenantId
    ): Result<Boolean> {
        logger.info("Deactivating contact: $contactId")
        return contactRepository.deactivateContact(contactId, tenantId)
            .onSuccess { logger.info("Contact deactivated: $contactId") }
            .onFailure { logger.error("Failed to deactivate contact: $contactId", it) }
    }

    /**
     * Reactivate a contact.
     */
    suspend fun reactivateContact(
        contactId: ContactId,
        tenantId: TenantId
    ): Result<Boolean> {
        logger.info("Reactivating contact: $contactId")
        return contactRepository.reactivateContact(contactId, tenantId)
            .onSuccess { logger.info("Contact reactivated: $contactId") }
            .onFailure { logger.error("Failed to reactivate contact: $contactId", it) }
    }

    /**
     * Check if a contact exists.
     */
    suspend fun exists(
        contactId: ContactId,
        tenantId: TenantId
    ): Result<Boolean> {
        return contactRepository.exists(contactId, tenantId)
    }

    /**
     * Get contact statistics for dashboard.
     */
    suspend fun getContactStats(tenantId: TenantId): Result<ContactStats> {
        logger.debug("Getting contact stats for tenant: $tenantId")
        return contactRepository.getContactStats(tenantId)
            .onFailure { logger.error("Failed to get contact stats for tenant: $tenantId", it) }
    }

    suspend fun updateContactProfile(
        tenantId: TenantId,
        contactId: ContactId,
        request: UpdateBusinessProfileRequest
    ): BusinessProfileUpdateResponse {
        return businessProfileService.updateContactProfile(tenantId, contactId, request)
    }

    suspend fun updateContactProfilePins(
        tenantId: TenantId,
        contactId: ContactId,
        request: PinBusinessProfileFieldsRequest
    ): BusinessProfileUpdateResponse {
        return businessProfileService.updateContactPins(tenantId, contactId, request)
    }

    private suspend fun hydrateSingle(tenantId: TenantId, contact: ContactDto): ContactDto? {
        return hydrateContacts(tenantId, listOf(contact)).firstOrNull()
    }

    private suspend fun hydrateContacts(tenantId: TenantId, contacts: List<ContactDto>): List<ContactDto> {
        if (contacts.isEmpty()) return contacts
        val addresses = contactAddressRepository.batchLoadAddresses(
            tenantId = tenantId,
            contactIds = contacts.map { it.id }
        ).getOrDefault(emptyMap())

        val withAddresses = contacts.map { contact ->
            contact.copy(addresses = addresses[contact.id].orEmpty())
        }

        return businessProfileService.projectContacts(tenantId, withAddresses)
    }

    // NOTE: listPeppolEnabledContacts() removed - PEPPOL status is now in PeppolDirectoryCacheTable
}
