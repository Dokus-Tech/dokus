package ai.dokus.peppol.backend.mapper

import ai.dokus.foundation.domain.Email
import ai.dokus.foundation.domain.Money
import ai.dokus.foundation.domain.Name
import ai.dokus.foundation.domain.VatRate
import ai.dokus.foundation.domain.enums.Currency
import ai.dokus.foundation.domain.enums.ExpenseCategory
import ai.dokus.foundation.domain.enums.InvoiceStatus
import ai.dokus.foundation.domain.ids.Bic
import ai.dokus.foundation.domain.ids.ClientId
import ai.dokus.foundation.domain.ids.Iban
import ai.dokus.foundation.domain.ids.InvoiceId
import ai.dokus.foundation.domain.ids.InvoiceNumber
import ai.dokus.foundation.domain.ids.PeppolId
import ai.dokus.foundation.domain.ids.PeppolSettingsId
import ai.dokus.foundation.domain.ids.TenantId
import ai.dokus.foundation.domain.ids.VatNumber
import ai.dokus.foundation.domain.model.ClientDto
import ai.dokus.foundation.domain.model.FinancialDocumentDto
import ai.dokus.foundation.domain.model.InvoiceItemDto
import ai.dokus.foundation.domain.model.PeppolSettingsDto
import ai.dokus.foundation.domain.model.RecommandMonetaryTotal
import ai.dokus.foundation.domain.model.RecommandParty
import ai.dokus.foundation.domain.model.RecommandReceivedDocument
import ai.dokus.foundation.domain.model.RecommandTaxSubtotal
import ai.dokus.foundation.domain.model.RecommandTaxTotal
import ai.dokus.foundation.domain.model.TenantSettings
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class PeppolMapperTest {
    private val mapper = PeppolMapper()
    private val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)

    private fun createTenantSettings(address: String? = "Rue de Test 123, 1000 Brussels, Belgium") = TenantSettings(
        tenantId = TenantId.generate(),
        companyName = "Test Company BV",
        companyVatNumber = VatNumber("BE0123456789"),
        companyAddress = address,
        companyIban = Iban("BE68539007547034"),
        companyBic = Bic("GKCCBEBB"),
        createdAt = now,
        updatedAt = now
    )

    private fun createPeppolSettings() = PeppolSettingsDto(
        id = PeppolSettingsId.generate(),
        tenantId = TenantId.generate(),
        companyId = "test-company",
        peppolId = PeppolId("0208:BE0123456789"),
        isEnabled = true,
        testMode = false,
        createdAt = now,
        updatedAt = now
    )

    private fun createClient() = ClientDto(
        id = ClientId.generate(),
        tenantId = TenantId.generate(),
        name = Name("Client Company"),
        email = Email("contact@client.be"),
        vatNumber = VatNumber("BE0987654321"),
        companyNumber = "BE0987654321",
        peppolId = "0208:BE0987654321",
        peppolEnabled = true,
        addressLine1 = "Client Street 456",
        city = "Ghent",
        postalCode = "9000",
        country = "BE",
        createdAt = now,
        updatedAt = now
    )

    private fun createInvoice() = FinancialDocumentDto.InvoiceDto(
        id = InvoiceId.generate(),
        tenantId = TenantId.generate(),
        clientId = ClientId.generate(),
        invoiceNumber = InvoiceNumber("INV-2024-001"),
        issueDate = LocalDate.parse("2024-01-15"),
        dueDate = LocalDate.parse("2024-02-15"),
        subtotalAmount = Money("1000.00"),
        vatAmount = Money("210.00"),
        totalAmount = Money("1210.00"),
        status = InvoiceStatus.Draft,
        currency = Currency.Eur,
        notes = "Test invoice notes",
        items = listOf(
            InvoiceItemDto(
                description = "Consulting services",
                quantity = 10.0,
                unitPrice = Money("100.00"),
                vatRate = VatRate("21"),
                lineTotal = Money("1000.00"),
                vatAmount = Money("210.00")
            )
        ),
        createdAt = now,
        updatedAt = now
    )

    @Test
    fun `toRecommandSendRequest creates valid request`() {
        val request = mapper.toRecommandSendRequest(
            invoice = createInvoice(),
            client = createClient(),
            tenantSettings = createTenantSettings(),
            peppolSettings = createPeppolSettings()
        )

        assertEquals("0208:BE0987654321", request.recipient)
        assertEquals("invoice", request.documentType)
        assertNotNull(request.document)
        assertEquals("INV-2024-001", request.document!!.invoiceNumber)
        assertEquals("2024-01-15", request.document!!.issueDate)
        assertEquals("2024-02-15", request.document!!.dueDate)
        assertEquals("EUR", request.document!!.documentCurrencyCode)
    }

    @Test
    fun `toRecommandSendRequest includes buyer reference from company number`() {
        val request = mapper.toRecommandSendRequest(
            invoice = createInvoice(),
            client = createClient(),
            tenantSettings = createTenantSettings(),
            peppolSettings = createPeppolSettings()
        )

        assertEquals("BE0987654321", request.document!!.buyerReference)
    }

    @Test
    fun `toRecommandSendRequest falls back to VAT number for buyer reference`() {
        val clientWithoutCompanyNumber = createClient().copy(companyNumber = null)

        val request = mapper.toRecommandSendRequest(
            invoice = createInvoice(),
            client = clientWithoutCompanyNumber,
            tenantSettings = createTenantSettings(),
            peppolSettings = createPeppolSettings()
        )

        assertEquals("BE0987654321", request.document!!.buyerReference)
    }

    @Test
    fun `toRecommandSendRequest maps buyer correctly`() {
        val request = mapper.toRecommandSendRequest(
            invoice = createInvoice(),
            client = createClient(),
            tenantSettings = createTenantSettings(),
            peppolSettings = createPeppolSettings()
        )

        val buyer = request.document!!.buyer
        assertEquals("Client Company", buyer.name)
        assertEquals("BE0987654321", buyer.vatNumber)
        assertEquals("Client Street 456", buyer.streetName)
        assertEquals("Ghent", buyer.cityName)
        assertEquals("9000", buyer.postalZone)
        assertEquals("BE", buyer.countryCode)
        assertEquals("contact@client.be", buyer.contactEmail)
    }

    @Test
    fun `toRecommandSendRequest maps seller with parsed address`() {
        val request = mapper.toRecommandSendRequest(
            invoice = createInvoice(),
            client = createClient(),
            tenantSettings = createTenantSettings("Rue de Test 123, 1000 Brussels, Belgium"),
            peppolSettings = createPeppolSettings()
        )

        val seller = request.document!!.seller
        assertNotNull(seller)
        assertEquals("Test Company BV", seller!!.name)
        assertEquals("BE0123456789", seller.vatNumber)
        assertEquals("Rue de Test 123", seller.streetName)
        assertEquals("Brussels", seller.cityName)
        assertEquals("1000", seller.postalZone)
        assertEquals("BE", seller.countryCode)
    }

    @Test
    fun `toRecommandSendRequest maps line items correctly`() {
        val request = mapper.toRecommandSendRequest(
            invoice = createInvoice(),
            client = createClient(),
            tenantSettings = createTenantSettings(),
            peppolSettings = createPeppolSettings()
        )

        assertEquals(1, request.document!!.lineItems.size)
        val lineItem = request.document!!.lineItems[0]
        assertEquals("1", lineItem.id)
        assertEquals("Consulting services", lineItem.name)
        assertEquals(10.0, lineItem.quantity)
        assertEquals(100.0, lineItem.unitPrice)
        assertEquals(1000.0, lineItem.lineTotal)
        assertEquals("S", lineItem.taxCategory)  // Standard rate
        assertEquals(21.0, lineItem.taxPercent)
    }

    @Test
    fun `toRecommandSendRequest maps zero VAT rate correctly`() {
        val invoiceWithZeroVat = createInvoice().copy(
            items = listOf(
                InvoiceItemDto(
                    description = "Export service",
                    quantity = 1.0,
                    unitPrice = Money("100.00"),
                    vatRate = VatRate("0"),
                    lineTotal = Money("100.00"),
                    vatAmount = Money("0.00")
                )
            )
        )

        val request = mapper.toRecommandSendRequest(
            invoice = invoiceWithZeroVat,
            client = createClient(),
            tenantSettings = createTenantSettings(),
            peppolSettings = createPeppolSettings()
        )

        assertEquals("Z", request.document!!.lineItems[0].taxCategory)
    }

    @Test
    fun `toRecommandSendRequest includes payment means when IBAN is present`() {
        val request = mapper.toRecommandSendRequest(
            invoice = createInvoice(),
            client = createClient(),
            tenantSettings = createTenantSettings(),
            peppolSettings = createPeppolSettings()
        )

        assertNotNull(request.document!!.paymentMeans)
        assertEquals("BE68539007547034", request.document!!.paymentMeans?.iban)
        assertEquals("GKCCBEBB", request.document!!.paymentMeans?.bic)
        assertEquals("30", request.document!!.paymentMeans?.paymentMeansCode)
    }

    @Test
    fun `toRecommandSendRequest omits payment means when IBAN is missing`() {
        val settingsWithoutIban = createTenantSettings().copy(companyIban = null)

        val request = mapper.toRecommandSendRequest(
            invoice = createInvoice(),
            client = createClient(),
            tenantSettings = settingsWithoutIban,
            peppolSettings = createPeppolSettings()
        )

        assertNull(request.document!!.paymentMeans)
    }

    @Test
    fun `toCreateBillRequest creates bill from received document`() {
        val receivedDocument = RecommandReceivedDocument(
            invoiceNumber = "SUPP-2024-001",
            issueDate = "2024-01-15",
            dueDate = "2024-02-15",
            seller = RecommandParty(
                name = "Software Supplier",
                vatNumber = "BE0111222333"
            ),
            legalMonetaryTotal = RecommandMonetaryTotal(
                taxInclusiveAmount = 1210.0,
                payableAmount = 1210.0
            ),
            taxTotal = RecommandTaxTotal(
                taxAmount = 210.0,
                taxSubtotals = listOf(
                    RecommandTaxSubtotal(
                        taxableAmount = 1000.0,
                        taxAmount = 210.0,
                        taxPercent = 21.0
                    )
                )
            ),
            note = "Monthly software license"
        )

        val billRequest = mapper.toCreateBillRequest(receivedDocument, "0208:BE0111222333")

        assertEquals("Software Supplier", billRequest.supplierName)
        assertEquals("BE0111222333", billRequest.supplierVatNumber)
        assertEquals("SUPP-2024-001", billRequest.invoiceNumber)
        assertEquals(LocalDate.parse("2024-01-15"), billRequest.issueDate)
        assertEquals(LocalDate.parse("2024-02-15"), billRequest.dueDate)
        assertEquals("1210.0", billRequest.amount.value)
        assertEquals("210.0", billRequest.vatAmount?.value)
        assertEquals("21.0", billRequest.vatRate?.value)
        assertEquals("Monthly software license", billRequest.description)
        assertTrue(billRequest.notes?.contains("Peppol") == true)
    }

    @Test
    fun `toCreateBillRequest infers Software category from keywords`() {
        val receivedDocument = RecommandReceivedDocument(
            seller = RecommandParty(name = "Cloud Software Inc"),
            legalMonetaryTotal = RecommandMonetaryTotal(payableAmount = 100.0)
        )

        val billRequest = mapper.toCreateBillRequest(receivedDocument, "0208:test")

        assertEquals(ExpenseCategory.Software, billRequest.category)
    }

    @Test
    fun `toCreateBillRequest infers Travel category from keywords`() {
        val receivedDocument = RecommandReceivedDocument(
            seller = RecommandParty(name = "Travel Agency"),
            note = "Flight booking to Paris",
            legalMonetaryTotal = RecommandMonetaryTotal(payableAmount = 500.0)
        )

        val billRequest = mapper.toCreateBillRequest(receivedDocument, "0208:test")

        assertEquals(ExpenseCategory.Travel, billRequest.category)
    }

    @Test
    fun `toCreateBillRequest defaults to Other category when no keywords match`() {
        val receivedDocument = RecommandReceivedDocument(
            seller = RecommandParty(name = "Generic Vendor"),
            legalMonetaryTotal = RecommandMonetaryTotal(payableAmount = 100.0)
        )

        val billRequest = mapper.toCreateBillRequest(receivedDocument, "0208:test")

        assertEquals(ExpenseCategory.Other, billRequest.category)
    }

    @Test
    fun `throws exception when client has no Peppol ID`() {
        val clientWithoutPeppol = createClient().copy(peppolId = null)

        assertThrows(IllegalArgumentException::class.java) {
            mapper.toRecommandSendRequest(
                invoice = createInvoice(),
                client = clientWithoutPeppol,
                tenantSettings = createTenantSettings(),
                peppolSettings = createPeppolSettings()
            )
        }
    }
}
