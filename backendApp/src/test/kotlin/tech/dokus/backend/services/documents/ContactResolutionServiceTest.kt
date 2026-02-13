package tech.dokus.backend.services.documents

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDateTime
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tech.dokus.database.repository.contacts.ContactRepository
import tech.dokus.domain.Name
import tech.dokus.domain.enums.ClientType
import tech.dokus.domain.enums.ContactSource
import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.model.InvoiceDraftData
import tech.dokus.domain.model.contact.ContactResolution
import tech.dokus.domain.model.contact.CounterpartySnapshot
import tech.dokus.foundation.backend.lookup.CbeApiClient
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ContactResolutionServiceTest {

    private val contactRepository = mockk<ContactRepository>()
    private val cbeApiClient = mockk<CbeApiClient>()

    private val service = ContactResolutionService(
        contactRepository = contactRepository,
        cbeApiClient = cbeApiClient
    )

    private val tenantId = TenantId.parse("44e8ed5c-020a-4bbb-9439-ac85899c5589")
    private val tenantVat = VatNumber.from("BE0777887045")!!

    @BeforeEach
    fun setup() {
        coEvery { contactRepository.findByVatNumber(any(), any()) } returns Result.success(null)
        coEvery { contactRepository.findByIban(any(), any()) } returns Result.success(emptyList())
        coEvery { contactRepository.findByName(any(), any(), any()) } returns Result.success(emptyList())
        coEvery { contactRepository.updateContact(any(), any(), any()) } returns Result.failure(
            IllegalStateException("updateContact should not be called in this test")
        )
        coEvery { cbeApiClient.searchByVat(any()) } returns Result.failure(
            IllegalStateException("CBE should not be called in this test")
        )
    }

    @Test
    fun `empty authoritative snapshot is kept pending and never matched`() = runBlocking {
        val result = service.resolve(
            tenantId = tenantId,
            draftData = InvoiceDraftData(direction = DocumentDirection.Inbound),
            authoritativeSnapshot = CounterpartySnapshot(),
            tenantVat = tenantVat
        )

        assertIs<ContactResolution.PendingReview>(result.resolution)

        coVerify(exactly = 0) { contactRepository.findByVatNumber(any(), any()) }
        coVerify(exactly = 0) { contactRepository.findByIban(any(), any()) }
        coVerify(exactly = 0) { contactRepository.findByName(any(), any(), any()) }
    }

    @Test
    fun `invoice unknown direction with tenant vat is kept pending and never auto-linked`() = runBlocking {
        val result = service.resolve(
            tenantId = tenantId,
            tenantVat = tenantVat,
            draftData = InvoiceDraftData(direction = DocumentDirection.Unknown),
            authoritativeSnapshot = CounterpartySnapshot(
                name = "INVOID VISION BV",
                vatNumber = VatNumber.from("BE0777887045")
            )
        )

        assertIs<ContactResolution.PendingReview>(result.resolution)
        assertEquals("BE0777887045", result.snapshot.vatNumber?.normalized)

        coVerify(exactly = 0) { contactRepository.findByVatNumber(any(), any()) }
        coVerify(exactly = 0) { cbeApiClient.searchByVat(any()) }
    }

    @Test
    fun `valid authoritative vat matches existing contact with vat-first policy`() = runBlocking {
        val contact = contact(
            id = "aaaaaaaa-1111-2222-3333-bbbbbbbbbbbb",
            name = "Apple Account",
            vat = "IE9700053D",
            source = ContactSource.Manual
        )
        coEvery { contactRepository.findByVatNumber(tenantId, "IE9700053D") } returns Result.success(contact)

        val result = service.resolve(
            tenantId = tenantId,
            tenantVat = tenantVat,
            draftData = InvoiceDraftData(direction = DocumentDirection.Inbound),
            authoritativeSnapshot = CounterpartySnapshot(
                name = "Apple Account",
                vatNumber = VatNumber.from("IE9700053D")
            )
        )

        val resolution = assertIs<ContactResolution.Matched>(result.resolution)
        assertEquals(contact.id, resolution.contactId)

        coVerify(exactly = 1) { contactRepository.findByVatNumber(tenantId, "IE9700053D") }
        coVerify(exactly = 0) { contactRepository.updateContact(any(), any(), any()) }
    }

    @Test
    fun `vat-first match self-heals payment token ai contact name`() = runBlocking {
        val existing = contact(
            id = "cccccccc-1111-2222-3333-dddddddddddd",
            name = "Visa .... 9803 (Apple Pay)",
            vat = "IE9700053D",
            source = ContactSource.AI
        )
        val healed = existing.copy(name = Name("Apple Account"))

        coEvery { contactRepository.findByVatNumber(tenantId, "IE9700053D") } returns Result.success(existing)
        coEvery {
            contactRepository.updateContact(
                existing.id,
                tenantId,
                match { it.name?.value == "Apple Account" }
            )
        } returns Result.success(healed)

        val result = service.resolve(
            tenantId = tenantId,
            tenantVat = tenantVat,
            draftData = InvoiceDraftData(direction = DocumentDirection.Inbound),
            authoritativeSnapshot = CounterpartySnapshot(
                name = "Apple Account",
                vatNumber = VatNumber.from("IE9700053D")
            )
        )

        val resolution = assertIs<ContactResolution.Matched>(result.resolution)
        assertEquals(existing.id, resolution.contactId)

        coVerify(exactly = 1) {
            contactRepository.updateContact(
                existing.id,
                tenantId,
                match { it.name?.value == "Apple Account" }
            )
        }
    }

    @Test
    fun `self-heal never renames manual or legitimate business contacts`() = runBlocking {
        val manualAlias = contact(
            id = "eeeeeeee-1111-2222-3333-ffffffffffff",
            name = "Visa .... 9803 (Apple Pay)",
            vat = "IE9700053D",
            source = ContactSource.Manual
        )
        val legitBusiness = contact(
            id = "99999999-1111-2222-3333-888888888888",
            name = "Visa Europe",
            vat = "IE9700053D",
            source = ContactSource.AI
        )

        coEvery { contactRepository.findByVatNumber(tenantId, "IE9700053D") } returnsMany listOf(
            Result.success(manualAlias),
            Result.success(legitBusiness)
        )

        service.resolve(
            tenantId = tenantId,
            tenantVat = tenantVat,
            draftData = InvoiceDraftData(direction = DocumentDirection.Inbound),
            authoritativeSnapshot = CounterpartySnapshot(
                name = "Apple Account",
                vatNumber = VatNumber.from("IE9700053D")
            )
        )
        service.resolve(
            tenantId = tenantId,
            tenantVat = tenantVat,
            draftData = InvoiceDraftData(direction = DocumentDirection.Inbound),
            authoritativeSnapshot = CounterpartySnapshot(
                name = "Visa Europe",
                vatNumber = VatNumber.from("IE9700053D")
            )
        )

        coVerify(exactly = 0) { contactRepository.updateContact(any(), any(), any()) }
    }

    private fun contact(
        id: String,
        name: String,
        vat: String,
        source: ContactSource
    ): tech.dokus.domain.model.contact.ContactDto {
        return tech.dokus.domain.model.contact.ContactDto(
            id = ContactId.parse(id),
            tenantId = tenantId,
            name = Name(name),
            vatNumber = VatNumber.from(vat),
            businessType = ClientType.Business,
            createdAt = LocalDateTime.parse("2026-01-01T00:00:00"),
            updatedAt = LocalDateTime.parse("2026-01-01T00:00:00"),
            source = source
        )
    }
}
