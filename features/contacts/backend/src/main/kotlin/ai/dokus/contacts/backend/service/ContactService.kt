package ai.dokus.contacts.backend.service

import ai.dokus.foundation.database.repository.contacts.ContactRepository
import ai.dokus.foundation.domain.ids.ContactId
import ai.dokus.foundation.domain.ids.TenantId
import ai.dokus.foundation.domain.model.ContactDto
import ai.dokus.foundation.domain.model.ContactStats
import ai.dokus.foundation.domain.model.CreateContactRequest
import ai.dokus.foundation.domain.model.PaginatedResponse
import ai.dokus.foundation.domain.model.UpdateContactRequest
import org.slf4j.LoggerFactory

/**
 * Service for contact business operations.
 *
 * Contacts represent both customers (who receive invoices) and vendors (who send bills).
 * This service handles all business logic related to contacts and delegates data access
 * to the repository layer.
 */
class ContactService(
    private val contactRepository: ContactRepository
) {
    private val logger = LoggerFactory.getLogger(ContactService::class.java)

    /**
     * Create a new contact for a tenant.
     */
    suspend fun createContact(
        tenantId: TenantId,
        request: CreateContactRequest
    ): Result<ContactDto> {
        logger.info("Creating contact for tenant: $tenantId, name: ${request.name}")
        return contactRepository.createContact(tenantId, request)
            .onSuccess { logger.info("Contact created: ${it.id}") }
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
        return contactRepository.getContact(contactId, tenantId)
            .onFailure { logger.error("Failed to fetch contact: $contactId", it) }
    }

    /**
     * List contacts with optional filters.
     */
    suspend fun listContacts(
        tenantId: TenantId,
        isActive: Boolean? = null,
        peppolEnabled: Boolean? = null,
        searchQuery: String? = null,
        limit: Int = 50,
        offset: Int = 0
    ): Result<PaginatedResponse<ContactDto>> {
        logger.debug("Listing contacts for tenant: $tenantId (isActive=$isActive, peppolEnabled=$peppolEnabled, limit=$limit, offset=$offset)")
        return contactRepository.listContacts(tenantId, isActive, peppolEnabled, searchQuery, limit, offset)
            .onSuccess { logger.debug("Retrieved ${it.items.size} contacts (total=${it.total})") }
            .onFailure { logger.error("Failed to list contacts for tenant: $tenantId", it) }
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
            .onSuccess { logger.debug("Retrieved ${it.items.size} vendors (total=${it.total})") }
            .onFailure { logger.error("Failed to list vendors for tenant: $tenantId", it) }
    }

    /**
     * Update a contact.
     */
    suspend fun updateContact(
        contactId: ContactId,
        tenantId: TenantId,
        request: UpdateContactRequest
    ): Result<ContactDto> {
        logger.info("Updating contact: $contactId for tenant: $tenantId")
        return contactRepository.updateContact(contactId, tenantId, request)
            .onSuccess { logger.info("Contact updated: $contactId") }
            .onFailure { logger.error("Failed to update contact: $contactId", it) }
    }

    /**
     * Update a contact's Peppol settings.
     */
    suspend fun updateContactPeppol(
        contactId: ContactId,
        tenantId: TenantId,
        peppolId: String?,
        peppolEnabled: Boolean
    ): Result<ContactDto> {
        logger.info("Updating contact Peppol settings: $contactId (peppolId=$peppolId, peppolEnabled=$peppolEnabled)")
        return contactRepository.updateContactPeppol(contactId, tenantId, peppolId, peppolEnabled)
            .onSuccess { logger.info("Contact Peppol settings updated: $contactId") }
            .onFailure { logger.error("Failed to update contact Peppol settings: $contactId", it) }
    }

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

    /**
     * List Peppol-enabled contacts.
     */
    suspend fun listPeppolEnabledContacts(tenantId: TenantId): Result<List<ContactDto>> {
        logger.debug("Listing Peppol-enabled contacts for tenant: $tenantId")
        return contactRepository.listPeppolEnabledContacts(tenantId)
            .onSuccess { logger.debug("Retrieved ${it.size} Peppol-enabled contacts") }
            .onFailure { logger.error("Failed to list Peppol-enabled contacts for tenant: $tenantId", it) }
    }
}
