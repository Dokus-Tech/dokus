@file:OptIn(ExperimentalUuidApi::class)

package tech.dokus.backend.services.contacts

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDateTime
import org.junit.jupiter.api.Test
import tech.dokus.backend.services.business.BusinessProfileService
import tech.dokus.database.repository.contacts.ContactAddressRepository
import tech.dokus.database.repository.contacts.ContactRepository
import tech.dokus.domain.Name
import tech.dokus.domain.enums.AddressType
import tech.dokus.domain.enums.ClientType
import tech.dokus.domain.enums.ContactSource
import tech.dokus.domain.ids.AddressId
import tech.dokus.domain.ids.ContactAddressId
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.contact.AddressDto
import tech.dokus.domain.model.contact.ContactAddressDto
import tech.dokus.domain.model.contact.ContactAddressInput
import tech.dokus.domain.model.contact.ContactDto
import tech.dokus.domain.model.contact.CreateContactRequest
import kotlin.test.assertEquals
import kotlin.uuid.ExperimentalUuidApi

class ContactServiceEnrichmentTriggerTest {
    private val contactRepository = mockk<ContactRepository>()
    private val contactAddressRepository = mockk<ContactAddressRepository>()
    private val businessProfileService = mockk<BusinessProfileService>()
    private val service = ContactService(
        contactRepository = contactRepository,
        contactAddressRepository = contactAddressRepository,
        businessProfileService = businessProfileService,
        peppolCacheRepository = null
    )

    @Test
    fun `enqueues enrichment only after addresses are persisted`() = runBlocking {
        val tenantId = TenantId.generate()
        val created = sampleContact(tenantId)
        val request = CreateContactRequest(
            name = created.name,
            businessType = ClientType.Business,
            addresses = listOf(
                ContactAddressInput(
                    streetLine1 = "Main Street 1",
                    city = "Brussels",
                    postalCode = "1000",
                    country = "BE"
                )
            )
        )

        coEvery { contactRepository.createContact(tenantId, request) } returns Result.success(created)
        coEvery { contactAddressRepository.addAddress(tenantId, created.id, request.addresses.first()) } returns
            Result.success(
                ContactAddressDto(
                    id = ContactAddressId.generate(),
                    address = AddressDto(
                        id = AddressId.generate(),
                        streetLine1 = "Main Street 1",
                        city = "Brussels",
                        postalCode = "1000",
                        country = "BE"
                    ),
                    addressType = AddressType.Registered,
                    isDefault = true
                )
            )
        coEvery { contactAddressRepository.batchLoadAddresses(tenantId, listOf(created.id)) } returns
            Result.success(mapOf(created.id to emptyList()))
        coEvery { businessProfileService.projectContacts(tenantId, any()) } answers { secondArg() }
        coEvery {
            businessProfileService.enqueueContact(
                tenantId = tenantId,
                contactId = created.id,
                triggerReason = "CONTACT_CREATED"
            )
        } returns Result.success(Unit)

        val result = service.createContact(tenantId, request).getOrThrow()

        assertEquals(created.id, result.id)
        coVerify(exactly = 1) {
            businessProfileService.enqueueContact(
                tenantId = tenantId,
                contactId = created.id,
                triggerReason = "CONTACT_CREATED"
            )
        }
    }

    @Test
    fun `skips enqueue when any address persistence fails`() = runBlocking {
        val tenantId = TenantId.generate()
        val created = sampleContact(tenantId)
        val request = CreateContactRequest(
            name = created.name,
            businessType = ClientType.Business,
            addresses = listOf(
                ContactAddressInput(
                    streetLine1 = "Main Street 1",
                    city = "Brussels",
                    postalCode = "1000",
                    country = "BE"
                )
            )
        )

        coEvery { contactRepository.createContact(tenantId, request) } returns Result.success(created)
        coEvery { contactAddressRepository.addAddress(tenantId, created.id, request.addresses.first()) } returns
            Result.failure(IllegalStateException("address write failed"))
        coEvery { contactAddressRepository.batchLoadAddresses(tenantId, listOf(created.id)) } returns
            Result.success(mapOf(created.id to emptyList()))
        coEvery { businessProfileService.projectContacts(tenantId, any()) } answers { secondArg() }

        val result = service.createContact(tenantId, request).getOrThrow()

        assertEquals(created.id, result.id)
        coVerify(exactly = 0) {
            businessProfileService.enqueueContact(
                tenantId = tenantId,
                contactId = created.id,
                triggerReason = "CONTACT_CREATED"
            )
        }
    }

    private fun sampleContact(tenantId: TenantId): ContactDto {
        return ContactDto(
            id = ContactId.generate(),
            tenantId = tenantId,
            name = Name("Acme Logistics"),
            businessType = ClientType.Business,
            createdAt = LocalDateTime(2026, 1, 1, 10, 0),
            updatedAt = LocalDateTime(2026, 1, 1, 10, 0),
            source = ContactSource.Manual
        )
    }
}
