package tech.dokus.backend.services.documents

import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import tech.dokus.database.repository.contacts.ContactRepository
import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.model.CreditNoteDraftData
import tech.dokus.domain.model.InvoiceDraftData
import tech.dokus.domain.model.contact.ContactResolution
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

    @Test
    fun `invoice unknown direction with tenant vat is kept pending and never auto-linked`() = runBlocking {
        val tenantId = TenantId.parse("44e8ed5c-020a-4bbb-9439-ac85899c5589")
        val tenantVat = VatNumber.from("BE0777887045")!!

        val result = service.resolve(
            tenantId = tenantId,
            tenantVat = tenantVat,
            draftData = InvoiceDraftData(
                direction = DocumentDirection.Unknown,
                customerName = "INVOID VISION BV",
                customerVat = VatNumber.from("BE0777887045")!!
            )
        )

        assertIs<ContactResolution.PendingReview>(result.resolution)
        assertEquals("BE0777887045", result.snapshot.vatNumber?.normalized)

        coVerify(exactly = 0) { contactRepository.findByVatNumber(any(), any()) }
        coVerify(exactly = 0) { cbeApiClient.searchByVat(any()) }
    }

    @Test
    fun `credit note unknown direction with tenant vat is kept pending and never auto-linked`() = runBlocking {
        val tenantId = TenantId.parse("44e8ed5c-020a-4bbb-9439-ac85899c5589")
        val tenantVat = VatNumber.from("BE0777887045")!!

        val result = service.resolve(
            tenantId = tenantId,
            tenantVat = tenantVat,
            draftData = CreditNoteDraftData(
                direction = DocumentDirection.Unknown,
                counterpartyName = "INVOID VISION BV",
                counterpartyVat = VatNumber.from("BE0777887045")!!
            )
        )

        assertIs<ContactResolution.PendingReview>(result.resolution)
        assertEquals("BE0777887045", result.snapshot.vatNumber?.normalized)

        coVerify(exactly = 0) { contactRepository.findByVatNumber(any(), any()) }
        coVerify(exactly = 0) { cbeApiClient.searchByVat(any()) }
    }
}
