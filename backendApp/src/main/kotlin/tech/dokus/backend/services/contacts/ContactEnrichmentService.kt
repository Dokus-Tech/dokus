package tech.dokus.backend.services.contacts

import ai.dokus.foundation.database.repository.contacts.ContactRepository
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.contact.ContactDto
import tech.dokus.domain.model.contact.UpdateContactRequest
import tech.dokus.foundation.ktor.utils.loggerFor

/**
 * Service for progressively enriching contacts with data extracted from documents.
 *
 * Safety rules:
 * - Never overwrite existing non-null fields
 * - Only populate empty fields with extracted data
 * - Log all enrichment actions for audit
 * - All enrichment requires user confirmation (no auto-update)
 *
 * Used during document processing when AI extracts counterparty data
 * that could enhance an existing contact's profile.
 */
class ContactEnrichmentService(
    private val contactRepository: ContactRepository
) {
    private val logger = loggerFor()

    /**
     * Data that can be used to enrich an existing contact.
     * Typically extracted from AI document processing.
     */
    data class EnrichmentData(
        val email: String? = null,
        val phone: String? = null,
        val addressLine1: String? = null,
        val addressLine2: String? = null,
        val city: String? = null,
        val postalCode: String? = null,
        val country: String? = null,
        val peppolId: String? = null,
        val companyNumber: String? = null,
        val contactPerson: String? = null,
        val vatNumber: String? = null
    )

    /**
     * Result of attempting to enrich a contact.
     */
    data class EnrichmentResult(
        val contactId: ContactId,
        val fieldsEnriched: List<String>,
        val fieldsSkipped: List<String>,
        val updatedContact: ContactDto?
    ) {
        val wasEnriched: Boolean get() = fieldsEnriched.isNotEmpty()
    }

    /**
     * Preview what fields would be enriched without actually updating.
     *
     * Use this to show the user what data would be added before confirming.
     *
     * @param contactId The contact to potentially enrich
     * @param tenantId Tenant isolation
     * @param enrichmentData Data extracted from document
     * @return EnrichmentResult with fieldsEnriched and fieldsSkipped (updatedContact is null)
     */
    suspend fun previewEnrichment(
        contactId: ContactId,
        tenantId: TenantId,
        enrichmentData: EnrichmentData
    ): Result<EnrichmentResult> = runCatching {
        val contact = contactRepository.getContact(contactId, tenantId).getOrThrow()
            ?: throw IllegalArgumentException("Contact not found: $contactId")

        val (toEnrich, toSkip) = calculateEnrichmentFields(contact, enrichmentData)

        EnrichmentResult(
            contactId = contactId,
            fieldsEnriched = toEnrich.map { it.first },
            fieldsSkipped = toSkip,
            updatedContact = null
        )
    }

    /**
     * Enrich a contact with extracted data.
     *
     * Only populates fields that are currently null/empty.
     * Fields with existing values are skipped (never overwritten).
     *
     * @param contactId The contact to enrich
     * @param tenantId Tenant isolation
     * @param enrichmentData Data extracted from document
     * @param sourceDocumentId Optional document ID for audit trail
     * @return EnrichmentResult with updated contact and field lists
     */
    suspend fun enrichContact(
        contactId: ContactId,
        tenantId: TenantId,
        enrichmentData: EnrichmentData,
        sourceDocumentId: DocumentId? = null
    ): Result<EnrichmentResult> = runCatching {
        val contact = contactRepository.getContact(contactId, tenantId).getOrThrow()
            ?: throw IllegalArgumentException("Contact not found: $contactId")

        val (toEnrich, toSkip) = calculateEnrichmentFields(contact, enrichmentData)

        if (toEnrich.isEmpty()) {
            logger.info("No fields to enrich for contact: $contactId")
            return@runCatching EnrichmentResult(
                contactId = contactId,
                fieldsEnriched = emptyList(),
                fieldsSkipped = toSkip,
                updatedContact = contact
            )
        }

        // Build update request with only the fields to enrich
        val updateRequest = buildUpdateRequest(toEnrich)

        logger.info(
            "Enriching contact $contactId with fields: ${toEnrich.map { it.first }}. " +
                    "Source document: $sourceDocumentId"
        )

        val updatedContact = contactRepository.updateContact(contactId, tenantId, updateRequest)
            .getOrThrow()

        EnrichmentResult(
            contactId = contactId,
            fieldsEnriched = toEnrich.map { it.first },
            fieldsSkipped = toSkip,
            updatedContact = updatedContact
        )
    }

    /**
     * Calculate which fields can be enriched and which should be skipped.
     *
     * @return Pair of (fieldsToEnrich, fieldsToSkip)
     *         fieldsToEnrich is List<Pair<fieldName, newValue>>
     *         fieldsToSkip is List<fieldName>
     */
    private fun calculateEnrichmentFields(
        contact: ContactDto,
        data: EnrichmentData
    ): Pair<List<Pair<String, String>>, List<String>> {
        val toEnrich = mutableListOf<Pair<String, String>>()
        val toSkip = mutableListOf<String>()

        // Email
        if (!data.email.isNullOrBlank()) {
            if (contact.email == null) {
                toEnrich.add("email" to data.email)
            } else {
                toSkip.add("email")
            }
        }

        // Phone
        if (!data.phone.isNullOrBlank()) {
            if (contact.phone.isNullOrBlank()) {
                toEnrich.add("phone" to data.phone)
            } else {
                toSkip.add("phone")
            }
        }

        // Address Line 1
        if (!data.addressLine1.isNullOrBlank()) {
            if (contact.addressLine1.isNullOrBlank()) {
                toEnrich.add("addressLine1" to data.addressLine1)
            } else {
                toSkip.add("addressLine1")
            }
        }

        // Address Line 2
        if (!data.addressLine2.isNullOrBlank()) {
            if (contact.addressLine2.isNullOrBlank()) {
                toEnrich.add("addressLine2" to data.addressLine2)
            } else {
                toSkip.add("addressLine2")
            }
        }

        // City
        if (!data.city.isNullOrBlank()) {
            if (contact.city.isNullOrBlank()) {
                toEnrich.add("city" to data.city)
            } else {
                toSkip.add("city")
            }
        }

        // Postal Code
        if (!data.postalCode.isNullOrBlank()) {
            if (contact.postalCode.isNullOrBlank()) {
                toEnrich.add("postalCode" to data.postalCode)
            } else {
                toSkip.add("postalCode")
            }
        }

        // Country
        if (!data.country.isNullOrBlank()) {
            if (contact.country.isNullOrBlank()) {
                toEnrich.add("country" to data.country)
            } else {
                toSkip.add("country")
            }
        }

        // Peppol ID
        if (!data.peppolId.isNullOrBlank()) {
            if (contact.peppolId.isNullOrBlank()) {
                toEnrich.add("peppolId" to data.peppolId)
            } else {
                toSkip.add("peppolId")
            }
        }

        // Company Number
        if (!data.companyNumber.isNullOrBlank()) {
            if (contact.companyNumber.isNullOrBlank()) {
                toEnrich.add("companyNumber" to data.companyNumber)
            } else {
                toSkip.add("companyNumber")
            }
        }

        // Contact Person
        if (!data.contactPerson.isNullOrBlank()) {
            if (contact.contactPerson.isNullOrBlank()) {
                toEnrich.add("contactPerson" to data.contactPerson)
            } else {
                toSkip.add("contactPerson")
            }
        }

        // VAT Number - special handling: only enrich if contact has none
        if (!data.vatNumber.isNullOrBlank()) {
            if (contact.vatNumber == null) {
                toEnrich.add("vatNumber" to data.vatNumber)
            } else {
                toSkip.add("vatNumber")
            }
        }

        return toEnrich to toSkip
    }

    /**
     * Build an UpdateContactRequest from the fields to enrich.
     */
    private fun buildUpdateRequest(fieldsToEnrich: List<Pair<String, String>>): UpdateContactRequest {
        var request = UpdateContactRequest()

        for ((field, value) in fieldsToEnrich) {
            request = when (field) {
                "email" -> request.copy(email = value)
                "phone" -> request.copy(phone = value)
                "addressLine1" -> request.copy(addressLine1 = value)
                "addressLine2" -> request.copy(addressLine2 = value)
                "city" -> request.copy(city = value)
                "postalCode" -> request.copy(postalCode = value)
                "country" -> request.copy(country = value)
                "peppolId" -> request.copy(peppolId = value)
                "companyNumber" -> request.copy(companyNumber = value)
                "contactPerson" -> request.copy(contactPerson = value)
                "vatNumber" -> request.copy(vatNumber = value)
                else -> request
            }
        }

        return request
    }
}
