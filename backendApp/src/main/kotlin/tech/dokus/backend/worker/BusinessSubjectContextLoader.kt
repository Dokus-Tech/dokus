@file:OptIn(ExperimentalUuidApi::class)

package tech.dokus.backend.worker

import tech.dokus.database.repository.auth.AddressRepository
import tech.dokus.database.repository.auth.TenantRepository
import tech.dokus.database.repository.business.BusinessProfileEnrichmentJobEntity
import tech.dokus.database.repository.contacts.ContactAddressRepository
import tech.dokus.database.repository.contacts.ContactRepository
import tech.dokus.domain.enums.BusinessProfileSubjectType
import tech.dokus.domain.ids.ContactId
import kotlin.uuid.ExperimentalUuidApi

internal class BusinessSubjectContextLoader(
    private val tenantRepository: TenantRepository,
    private val addressRepository: AddressRepository,
    private val contactRepository: ContactRepository,
    private val contactAddressRepository: ContactAddressRepository,
) {
    suspend fun load(job: BusinessProfileEnrichmentJobEntity): BusinessSubjectContext? {
        return when (job.subjectType) {
            BusinessProfileSubjectType.Tenant -> {
                val tenant = tenantRepository.findById(job.tenantId) ?: return null
                val address = addressRepository.getCompanyAddress(job.tenantId)
                BusinessSubjectContext(
                    name = tenant.legalName.value,
                    vatNumber = tenant.vatNumber.value,
                    country = address?.country,
                    city = address?.city,
                    postalCode = address?.postalCode,
                    language = tenant.language
                )
            }

            BusinessProfileSubjectType.Contact -> {
                val contactId = ContactId(job.subjectId)
                val contact = contactRepository.getContact(contactId, job.tenantId).getOrNull() ?: return null
                val contactAddress = contactAddressRepository.listAddresses(job.tenantId, contactId)
                    .getOrDefault(emptyList())
                    .let { addresses -> addresses.firstOrNull { it.isDefault } ?: addresses.firstOrNull() }
                    ?.address
                val tenant = tenantRepository.findById(job.tenantId) ?: return null
                val companyAddress = addressRepository.getCompanyAddress(job.tenantId)
                BusinessSubjectContext(
                    name = contact.name.value,
                    vatNumber = contact.vatNumber?.value,
                    country = contactAddress?.country ?: companyAddress?.country,
                    city = contactAddress?.city,
                    postalCode = contactAddress?.postalCode,
                    email = contact.email?.value,
                    phone = contact.phone?.value,
                    language = tenant.language
                )
            }
        }
    }
}
