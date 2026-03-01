package tech.dokus.features.cashflow.mvi

import tech.dokus.domain.Name
import tech.dokus.domain.ids.VatNumber
import tech.dokus.features.cashflow.mvi.model.ClientSuggestion
import tech.dokus.features.cashflow.mvi.model.ExternalClientCandidate
import tech.dokus.features.cashflow.presentation.cashflow.components.invoice.Mocks
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CreateInvoiceLookupLogicTest {

    @Test
    fun `local lookup threshold starts at two chars`() {
        assertFalse(shouldLookupLocalClient("a"))
        assertTrue(shouldLookupLocalClient("ab"))
    }

    @Test
    fun `external lookup starts at valid vat or three chars`() {
        assertFalse(shouldLookupExternalClient("ab"))
        assertTrue(shouldLookupExternalClient("abc"))
        assertTrue(shouldLookupExternalClient("BE0400378485"))
    }

    @Test
    fun `merge keeps local winner for duplicate vat and appends manual option`() {
        val local = Mocks.sampleClient.copy(
            name = Name("Colruyt Group NV"),
            vatNumber = VatNumber("BE0400378485")
        )

        val externalDuplicate = ExternalClientCandidate(
            name = "Colruyt Group NV",
            vatNumber = VatNumber("BE0400378485"),
            enterpriseNumber = "0400378485"
        )
        val externalUnique = ExternalClientCandidate(
            name = "Barco NV",
            vatNumber = VatNumber("BE0473191041"),
            enterpriseNumber = "0473191041"
        )

        val merged = mergeClientLookupSuggestions(
            query = "col",
            normalizedVat = null,
            localResults = listOf(local),
            externalResults = listOf(externalDuplicate, externalUnique)
        )

        assertEquals(3, merged.size)
        assertTrue(merged[0] is ClientSuggestion.LocalContact)
        assertTrue(merged[1] is ClientSuggestion.ExternalCompany)
        assertTrue(merged[2] is ClientSuggestion.CreateManual)

        val external = merged[1] as ClientSuggestion.ExternalCompany
        assertEquals("Barco NV", external.candidate.name)
    }

    @Test
    fun `merge prioritizes exact vat local match first`() {
        val vat = VatNumber("BE0867686774")
        val exactVatContact = Mocks.sampleClient.copy(
            name = Name("Coolblue Belgie NV"),
            vatNumber = vat
        )
        val otherContact = Mocks.sampleClientWithoutPeppol.copy(
            name = Name("Donckers Schoten NV"),
            vatNumber = VatNumber("BE0428927169")
        )

        val merged = mergeClientLookupSuggestions(
            query = "BE0867686774",
            normalizedVat = vat,
            localResults = listOf(otherContact, exactVatContact),
            externalResults = emptyList()
        )

        val first = merged.first() as ClientSuggestion.LocalContact
        assertEquals("Coolblue Belgie NV", first.contact.name.value)
    }
}
