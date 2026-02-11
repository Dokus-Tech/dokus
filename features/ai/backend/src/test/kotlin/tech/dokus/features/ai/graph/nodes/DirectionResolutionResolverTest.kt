package tech.dokus.features.ai.graph.nodes

import kotlinx.datetime.LocalDateTime
import org.junit.jupiter.api.Test
import tech.dokus.domain.DisplayName
import tech.dokus.domain.LegalName
import tech.dokus.domain.enums.Currency
import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.enums.Language
import tech.dokus.domain.enums.SubscriptionTier
import tech.dokus.domain.enums.TenantStatus
import tech.dokus.domain.enums.TenantType
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.model.Tenant
import tech.dokus.features.ai.graph.sub.extraction.financial.CreditNoteExtractionResult
import tech.dokus.features.ai.graph.sub.extraction.financial.InvoiceExtractionResult
import tech.dokus.features.ai.graph.sub.extraction.financial.ReceiptExtractionResult
import tech.dokus.features.ai.models.DirectionResolutionSource
import tech.dokus.features.ai.models.FinancialExtractionResult
import kotlin.test.assertEquals

class DirectionResolutionResolverTest {

    private val tenantVat = VatNumber.from("BE0777887045")!!
    private val tenant = Tenant(
        id = TenantId.parse("44e8ed5c-020a-4bbb-9439-ac85899c5589"),
        type = TenantType.Company,
        legalName = LegalName("INVOID VISION"),
        displayName = DisplayName("INVOID VISION"),
        subscription = SubscriptionTier.Core,
        status = TenantStatus.Active,
        language = Language.En,
        vatNumber = tenantVat,
        createdAt = LocalDateTime.parse("2026-01-01T00:00:00"),
        updatedAt = LocalDateTime.parse("2026-01-01T00:00:00")
    )

    @Test
    fun `vat match wins over ai hint for invoice`() {
        val extraction = FinancialExtractionResult.Invoice(
            InvoiceExtractionResult(
                invoiceNumber = "INV-1",
                issueDate = null,
                dueDate = null,
                currency = Currency.Eur,
                subtotalAmount = null,
                vatAmount = null,
                totalAmount = null,
                sellerName = "INVOID VISION",
                sellerVat = tenantVat,
                buyerName = "Google Cloud EMEA Limited",
                buyerVat = VatNumber.from("IE6388047V"),
                directionHint = DocumentDirection.Inbound,
                directionHintConfidence = 0.95,
                confidence = 0.99
            )
        )

        val resolved = DirectionResolutionResolver.resolve(extraction, tenant, emptyList())

        assertEquals(DocumentDirection.Outbound, resolved.direction)
        assertEquals(DirectionResolutionSource.VatMatch, resolved.source)
        assertEquals(1.0, resolved.confidence)
    }

    @Test
    fun `name match wins over ai hint for credit note`() {
        val extraction = FinancialExtractionResult.CreditNote(
            CreditNoteExtractionResult(
                creditNoteNumber = "CN-1",
                issueDate = null,
                currency = Currency.Eur,
                subtotalAmount = null,
                vatAmount = null,
                totalAmount = null,
                sellerName = "INVOID VISION",
                sellerVat = null,
                buyerName = "Acme BV",
                buyerVat = null,
                directionHint = DocumentDirection.Inbound,
                directionHintConfidence = 0.90,
                originalInvoiceNumber = null,
                reason = null,
                confidence = 0.9,
                reasoning = null
            )
        )

        val resolved = DirectionResolutionResolver.resolve(extraction, tenant, listOf("Artem Kuznetsov"))

        assertEquals(DocumentDirection.Outbound, resolved.direction)
        assertEquals(DirectionResolutionSource.NameMatch, resolved.source)
    }

    @Test
    fun `ai hint is used only when vat and name cannot resolve`() {
        val extraction = FinancialExtractionResult.Receipt(
            ReceiptExtractionResult(
                merchantName = null,
                merchantVat = null,
                date = null,
                currency = Currency.Eur,
                totalAmount = null,
                vatAmount = null,
                receiptNumber = null,
                paymentMethod = null,
                directionHint = DocumentDirection.Inbound,
                directionHintConfidence = 0.77,
                confidence = 0.8,
                reasoning = null
            )
        )

        val resolved = DirectionResolutionResolver.resolve(extraction, tenant, emptyList())

        assertEquals(DocumentDirection.Inbound, resolved.direction)
        assertEquals(DirectionResolutionSource.AiHint, resolved.source)
        assertEquals(0.77, resolved.confidence)
    }

    @Test
    fun `unknown is returned when there is no evidence`() {
        val extraction = FinancialExtractionResult.Invoice(
            InvoiceExtractionResult(
                invoiceNumber = null,
                issueDate = null,
                dueDate = null,
                currency = Currency.Eur,
                subtotalAmount = null,
                vatAmount = null,
                totalAmount = null,
                sellerName = null,
                sellerVat = null,
                buyerName = null,
                buyerVat = null,
                directionHint = DocumentDirection.Unknown,
                directionHintConfidence = null,
                confidence = 0.5,
                reasoning = null
            )
        )

        val resolved = DirectionResolutionResolver.resolve(extraction, tenant, emptyList())

        assertEquals(DocumentDirection.Unknown, resolved.direction)
        assertEquals(DirectionResolutionSource.Unknown, resolved.source)
    }

    @Test
    fun `resolved counterparty vat uses non-tenant party for inbound invoice`() {
        val extraction = FinancialExtractionResult.Invoice(
            InvoiceExtractionResult(
                invoiceNumber = "INV-2",
                issueDate = null,
                dueDate = null,
                currency = Currency.Eur,
                subtotalAmount = null,
                vatAmount = null,
                totalAmount = null,
                sellerName = "Google Cloud EMEA Limited",
                sellerVat = VatNumber.from("IE6388047V"),
                buyerName = "INVOID VISION",
                buyerVat = tenantVat,
                confidence = 0.99
            )
        )

        val counterpartyVat = DirectionResolutionResolver.resolvedCounterpartyVat(
            extraction = extraction,
            direction = DocumentDirection.Inbound
        )

        assertEquals("IE6388047V", counterpartyVat)
    }
}
