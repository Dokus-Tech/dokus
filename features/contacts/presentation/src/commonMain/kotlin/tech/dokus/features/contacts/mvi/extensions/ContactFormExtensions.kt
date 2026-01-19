package tech.dokus.features.contacts.mvi.extensions

import tech.dokus.domain.City
import tech.dokus.domain.Email
import tech.dokus.domain.PhoneNumber
import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.model.contact.ContactAddressInput
import tech.dokus.domain.model.contact.ContactDto
import tech.dokus.domain.model.contact.CreateContactRequest
import tech.dokus.domain.model.contact.UpdateContactRequest
import tech.dokus.features.contacts.mvi.ContactFormData

/**
 * Convert ContactDto to ContactFormData for editing.
 */
internal fun ContactDto.toFormData(): ContactFormData = ContactFormData(
    name = name,
    email = email ?: Email.Empty,
    phone = phone ?: PhoneNumber.Empty,
    contactPerson = contactPerson ?: "",
    vatNumber = vatNumber ?: VatNumber.Empty,
    companyNumber = companyNumber ?: "",
    businessType = businessType,
    addressLine1 = addressLine1 ?: "",
    addressLine2 = addressLine2 ?: "",
    city = City(city ?: ""),  // Wrap in City for form validation
    postalCode = postalCode ?: "",
    country = country ?: "",
    // NOTE: peppolId/peppolEnabled removed - PEPPOL status is in PeppolDirectoryCacheTable
    defaultPaymentTerms = defaultPaymentTerms,
    defaultVatRate = defaultVatRate?.toString() ?: "",
    tags = tags ?: "",
    isActive = isActive
)

/**
 * Convert ContactFormData to CreateContactRequest.
 * Note: Addresses are managed separately via ContactAddressRepository.
 */
internal fun ContactFormData.toCreateRequest(): CreateContactRequest = CreateContactRequest(
    name = name,
    email = email.takeIf { it.value.isNotBlank() },
    phone = phone.takeIf { it.value.isNotBlank() },
    vatNumber = vatNumber.takeIf { it.value.isNotBlank() },
    businessType = businessType,
    addresses = toContactAddresses(),
    contactPerson = contactPerson.takeIf { it.isNotBlank() },
    companyNumber = companyNumber.takeIf { it.isNotBlank() },
    defaultPaymentTerms = defaultPaymentTerms,
    defaultVatRate = defaultVatRate.takeIf { it.isNotBlank() },
    // NOTE: peppolId/peppolEnabled removed - PEPPOL status is in PeppolDirectoryCacheTable
    tags = tags.takeIf { it.isNotBlank() },
    initialNote = initialNote.takeIf { it.isNotBlank() }
)

/**
 * Convert ContactFormData to UpdateContactRequest.
 * Note: Addresses are managed separately via ContactAddressRepository.
 */
internal fun ContactFormData.toUpdateRequest(): UpdateContactRequest = UpdateContactRequest(
    name = name,
    email = email.takeIf { it.value.isNotBlank() },
    phone = phone.takeIf { it.value.isNotBlank() },
    vatNumber = vatNumber.takeIf { it.value.isNotBlank() },
    businessType = businessType,
    // Note: Address updates are handled separately via ContactAddressRepository
    contactPerson = contactPerson.takeIf { it.isNotBlank() },
    companyNumber = companyNumber.takeIf { it.isNotBlank() },
    defaultPaymentTerms = defaultPaymentTerms,
    defaultVatRate = defaultVatRate.takeIf { it.isNotBlank() },
    // NOTE: peppolId/peppolEnabled removed - PEPPOL status is in PeppolDirectoryCacheTable
    tags = tags.takeIf { it.isNotBlank() },
    isActive = isActive
)

/**
 * Convert address fields to list of ContactAddressInput for create request.
 * Returns empty list if required address fields are missing.
 */
internal fun ContactFormData.toContactAddresses(): List<ContactAddressInput> {
    val requiredFields = listOf(addressLine1, city.value, postalCode, country)
    if (requiredFields.any { it.isBlank() }) {
        return emptyList()
    }
    return listOf(
        ContactAddressInput(
            streetLine1 = addressLine1,
            streetLine2 = addressLine2.takeIf { it.isNotBlank() },
            city = city.value,  // Extract value from City wrapper
            postalCode = postalCode,
            country = country
        )
    )
}
