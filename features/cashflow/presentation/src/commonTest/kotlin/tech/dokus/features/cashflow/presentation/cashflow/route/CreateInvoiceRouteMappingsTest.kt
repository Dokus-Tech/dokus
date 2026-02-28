package tech.dokus.features.cashflow.presentation.cashflow.route

import tech.dokus.features.cashflow.mvi.CreateInvoiceAction
import kotlin.test.Test
import kotlin.test.assertEquals

class CreateInvoiceRouteMappingsTest {

    @Test
    fun `navigate to create contact keeps prefill payload`() {
        val action = CreateInvoiceAction.NavigateToCreateContact(
            prefillCompanyName = "Colruyt Group NV",
            prefillVat = "BE0400378485",
            prefillAddress = "Edingensesteenweg 196, 1500 Halle",
            origin = "InvoiceCreate"
        )

        val destination = action.toCreateContactDestination()

        assertEquals("Colruyt Group NV", destination.prefillCompanyName)
        assertEquals("BE0400378485", destination.prefillVat)
        assertEquals("Edingensesteenweg 196, 1500 Halle", destination.prefillAddress)
        assertEquals("InvoiceCreate", destination.origin)
    }

    @Test
    fun `navigate to create contact allows empty payload`() {
        val action = CreateInvoiceAction.NavigateToCreateContact()

        val destination = action.toCreateContactDestination()

        assertEquals(null, destination.prefillCompanyName)
        assertEquals(null, destination.prefillVat)
        assertEquals(null, destination.prefillAddress)
        assertEquals(null, destination.origin)
    }
}
