package tech.dokus.backend.services.contacts

import tech.dokus.database.repository.contacts.ContactRepository
import tech.dokus.domain.City
import tech.dokus.domain.Email
import tech.dokus.domain.PhoneNumber
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.model.contact.ContactAddress
import tech.dokus.domain.model.contact.ContactDto
import tech.dokus.domain.model.contact.UpdateContactRequest
import tech.dokus.foundation.backend.utils.loggerFor

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

        collectField("email", data.email, contact.email?.value, toEnrich, toSkip)
        collectField("phone", data.phone, contact.phone?.value, toEnrich, toSkip)
        collectField("addressLine1", data.addressLine1, contact.addressLine1, toEnrich, toSkip)
        collectField("addressLine2", data.addressLine2, contact.addressLine2, toEnrich, toSkip)
        collectField("city", data.city, contact.city?.value, toEnrich, toSkip)
        collectField("postalCode", data.postalCode, contact.postalCode, toEnrich, toSkip)
        collectField("country", data.country, contact.country, toEnrich, toSkip)
        collectField("peppolId", data.peppolId, contact.peppolId, toEnrich, toSkip)
        collectField("companyNumber", data.companyNumber, contact.companyNumber, toEnrich, toSkip)
        collectField("contactPerson", data.contactPerson, contact.contactPerson, toEnrich, toSkip)
        collectField("vatNumber", data.vatNumber, contact.vatNumber?.value, toEnrich, toSkip)

        return toEnrich to toSkip
    }

    /**
     * Build an UpdateContactRequest from the fields to enrich.
     */
    private fun buildUpdateRequest(fieldsToEnrich: List<Pair<String, String>>): UpdateContactRequest {
        var request = UpdateContactRequest()
        val addressParts = AddressParts()

        for ((field, value) in fieldsToEnrich) {
            request = applyFieldUpdate(request, field, value, addressParts)
        }

        addressParts.toContactAddressOrNull()?.let { address ->
            request = request.copy(address = address)
        }

        return request
    }

    private fun collectField(
        fieldName: String,
        newValue: String?,
        existingValue: String?,
        toEnrich: MutableList<Pair<String, String>>,
        toSkip: MutableList<String>
    ) {
        if (newValue.isNullOrBlank()) return
        if (existingValue.isNullOrBlank()) {
            toEnrich.add(fieldName to newValue)
        } else {
            toSkip.add(fieldName)
        }
    }

    private fun applyFieldUpdate(
        request: UpdateContactRequest,
        field: String,
        value: String,
        addressParts: AddressParts
    ): UpdateContactRequest {
        return when (field) {
            "email" -> request.copy(email = Email(value))
            "phone" -> request.copy(phone = PhoneNumber(value))
            "addressLine1", "addressLine2", "city", "postalCode", "country" -> {
                addressParts.apply(field, value)
                request
            }
            "peppolId" -> request.copy(peppolId = value)
            "companyNumber" -> request.copy(companyNumber = value)
            "contactPerson" -> request.copy(contactPerson = value)
            "vatNumber" -> request.copy(vatNumber = VatNumber(value))
            else -> request
        }
    }

    private class AddressParts {
        private var addressLine1: String? = null
        private var addressLine2: String? = null
        private var city: String? = null
        private var postalCode: String? = null
        private var country: String? = null

        fun apply(field: String, value: String) {
            when (field) {
                "addressLine1" -> addressLine1 = value
                "addressLine2" -> addressLine2 = value
                "city" -> city = value
                "postalCode" -> postalCode = value
                "country" -> country = value
            }
        }

        fun toContactAddressOrNull(): ContactAddress? {
            val requiredFields = listOf(addressLine1, city, postalCode, country)
            if (requiredFields.all { it == null }) return null
            if (requiredFields.any { it == null }) return null

            return ContactAddress(
                streetLine1 = addressLine1!!,
                streetLine2 = addressLine2,
                city = City(city!!),
                postalCode = postalCode!!,
                country = country!!
            )
        }
    }
}
