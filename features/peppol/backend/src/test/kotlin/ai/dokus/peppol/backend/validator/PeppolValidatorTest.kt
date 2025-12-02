package ai.dokus.peppol.backend.validator

import ai.dokus.foundation.domain.Money
import ai.dokus.foundation.domain.Name
import ai.dokus.foundation.domain.VatRate
import ai.dokus.foundation.domain.enums.Currency
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
import ai.dokus.foundation.domain.model.TenantSettings
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class PeppolValidatorTest {
    private val validator = PeppolValidator()
    private val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)

    private fun createValidTenantSettings() = TenantSettings(
        tenantId = TenantId.generate(),
        companyName = "Test Company BV",
        companyVatNumber = VatNumber("BE0123456749"),
        companyAddress = "Rue de Test 123, 1000 Brussels, Belgium",
        companyIban = Iban("BE68539007547034"),
        companyBic = Bic("GKCCBEBB"),
        createdAt = now,
        updatedAt = now
    )

    private fun createValidPeppolSettings() = PeppolSettingsDto(
        id = PeppolSettingsId.generate(),
        tenantId = TenantId.generate(),
        companyId = "test-company",
        peppolId = PeppolId("0208:BE0123456749"),
        isEnabled = true,
        testMode = false,
        createdAt = now,
        updatedAt = now
    )

    private fun createValidClient() = ClientDto(
        id = ClientId.generate(),
        tenantId = TenantId.generate(),
        name = Name("Client Company"),
        vatNumber = VatNumber("BE0987654321"),
        peppolId = "0208:BE0987654321",
        peppolEnabled = true,
        addressLine1 = "Client Street 456",
        city = "Ghent",
        postalCode = "9000",
        country = "BE",
        createdAt = now,
        updatedAt = now
    )

    private fun createValidInvoice() = FinancialDocumentDto.InvoiceDto(
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
    fun `valid invoice passes validation`() {
        val result = validator.validateForSending(
            invoice = createValidInvoice(),
            client = createValidClient(),
            tenantSettings = createValidTenantSettings(),
            peppolSettings = createValidPeppolSettings()
        )

        assertTrue(result.isValid, "Expected validation to pass. Errors: ${result.errors.map { it.message }}")
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `validation fails when Peppol is not enabled`() {
        val disabledSettings = createValidPeppolSettings().copy(isEnabled = false)

        val result = validator.validateForSending(
            invoice = createValidInvoice(),
            client = createValidClient(),
            tenantSettings = createValidTenantSettings(),
            peppolSettings = disabledSettings
        )

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.code == "PEPPOL_NOT_ENABLED" })
    }

    @Test
    fun `validation fails when seller name is missing`() {
        val settingsWithoutName = createValidTenantSettings().copy(companyName = null)

        val result = validator.validateForSending(
            invoice = createValidInvoice(),
            client = createValidClient(),
            tenantSettings = settingsWithoutName,
            peppolSettings = createValidPeppolSettings()
        )

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.code == "BT-27" })
    }

    @Test
    fun `validation fails when seller VAT number is missing`() {
        val settingsWithoutVat = createValidTenantSettings().copy(companyVatNumber = null)

        val result = validator.validateForSending(
            invoice = createValidInvoice(),
            client = createValidClient(),
            tenantSettings = settingsWithoutVat,
            peppolSettings = createValidPeppolSettings()
        )

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.code == "BT-31" })
    }

    @Test
    fun `validation fails when seller VAT number format is invalid`() {
        val settingsWithInvalidVat = createValidTenantSettings().copy(
            companyVatNumber = VatNumber("INVALID123")
        )

        val result = validator.validateForSending(
            invoice = createValidInvoice(),
            client = createValidClient(),
            tenantSettings = settingsWithInvalidVat,
            peppolSettings = createValidPeppolSettings()
        )

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.code == "BT-31-FORMAT" })
    }

    @Test
    fun `validation fails when buyer Peppol ID is missing`() {
        val clientWithoutPeppol = createValidClient().copy(peppolId = null)

        val result = validator.validateForSending(
            invoice = createValidInvoice(),
            client = clientWithoutPeppol,
            tenantSettings = createValidTenantSettings(),
            peppolSettings = createValidPeppolSettings()
        )

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.code == "PEPPOL_ID_REQUIRED" })
    }

    @Test
    fun `validation fails when buyer Peppol ID format is invalid`() {
        val clientWithInvalidPeppol = createValidClient().copy(peppolId = "invalid-format")

        val result = validator.validateForSending(
            invoice = createValidInvoice(),
            client = clientWithInvalidPeppol,
            tenantSettings = createValidTenantSettings(),
            peppolSettings = createValidPeppolSettings()
        )

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.code == "PEPPOL_ID_FORMAT" })
    }

    @Test
    fun `validation fails when invoice has no items`() {
        val invoiceWithoutItems = createValidInvoice().copy(items = emptyList())

        val result = validator.validateForSending(
            invoice = invoiceWithoutItems,
            client = createValidClient(),
            tenantSettings = createValidTenantSettings(),
            peppolSettings = createValidPeppolSettings()
        )

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.code == "BG-25" })
    }

    @Test
    fun `validation fails when line item has zero quantity`() {
        val invoiceWithZeroQuantity = createValidInvoice().copy(
            items = listOf(
                InvoiceItemDto(
                    description = "Service",
                    quantity = 0.0,
                    unitPrice = Money("100.00"),
                    vatRate = VatRate("21"),
                    lineTotal = Money("0.00"),
                    vatAmount = Money("0.00")
                )
            )
        )

        val result = validator.validateForSending(
            invoice = invoiceWithZeroQuantity,
            client = createValidClient(),
            tenantSettings = createValidTenantSettings(),
            peppolSettings = createValidPeppolSettings()
        )

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.code == "BT-129" })
    }

    @Test
    fun `validation generates warning when seller address is missing`() {
        val settingsWithoutAddress = createValidTenantSettings().copy(companyAddress = null)

        val result = validator.validateForSending(
            invoice = createValidInvoice(),
            client = createValidClient(),
            tenantSettings = settingsWithoutAddress,
            peppolSettings = createValidPeppolSettings()
        )

        // Should still be valid, just with a warning
        assertTrue(result.isValid)
        assertTrue(result.warnings.any { it.code == "BT-35" })
    }

    @Test
    fun `validation generates warning when buyer VAT is missing`() {
        val clientWithoutVat = createValidClient().copy(vatNumber = null)

        val result = validator.validateForSending(
            invoice = createValidInvoice(),
            client = clientWithoutVat,
            tenantSettings = createValidTenantSettings(),
            peppolSettings = createValidPeppolSettings()
        )

        assertTrue(result.isValid)
        assertTrue(result.warnings.any { it.code == "BT-48" })
    }

    @Test
    fun `validation generates warning when IBAN is missing`() {
        val settingsWithoutIban = createValidTenantSettings().copy(companyIban = null)

        val result = validator.validateForSending(
            invoice = createValidInvoice(),
            client = createValidClient(),
            tenantSettings = settingsWithoutIban,
            peppolSettings = createValidPeppolSettings()
        )

        assertTrue(result.isValid)
        assertTrue(result.warnings.any { it.code == "BT-84" })
    }

    @Test
    fun `incoming validation passes with valid data`() {
        val result = validator.validateIncoming(
            documentId = "doc-123",
            senderPeppolId = "0208:BE0123456789"
        )

        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `incoming validation fails with empty document ID`() {
        val result = validator.validateIncoming(
            documentId = "",
            senderPeppolId = "0208:BE0123456789"
        )

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.code == "DOCUMENT_ID_REQUIRED" })
    }

    @Test
    fun `incoming validation fails with invalid sender Peppol ID`() {
        val result = validator.validateIncoming(
            documentId = "doc-123",
            senderPeppolId = "invalid"
        )

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.code == "SENDER_PEPPOL_ID_FORMAT" })
    }
}
