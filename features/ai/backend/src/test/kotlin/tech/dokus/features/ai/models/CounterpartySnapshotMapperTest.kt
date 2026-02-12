package tech.dokus.features.ai.models

import org.junit.jupiter.api.Test
import tech.dokus.domain.enums.Currency
import tech.dokus.features.ai.graph.sub.extraction.financial.CreditNoteExtractionResult
import tech.dokus.features.ai.graph.sub.extraction.financial.InvoiceExtractionResult
import tech.dokus.features.ai.graph.sub.extraction.financial.ReceiptExtractionResult
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class CounterpartySnapshotMapperTest {

    @Test
    fun `invalid counterparty vat token is sanitized to null`() {
        val extraction = FinancialExtractionResult.Invoice(
            InvoiceExtractionResult(
                invoiceNumber = "INV-1",
                issueDate = null,
                dueDate = null,
                currency = Currency.Eur,
                subtotalAmount = null,
                vatAmount = null,
                totalAmount = null,
                sellerName = "Apple Account",
                sellerVat = null,
                buyerName = "Artem Kuznetsov",
                buyerVat = null,
                counterparty = CounterpartyExtraction(
                    name = "Apple Account",
                    vatNumber = "VISA9803APPLEPAY",
                    email = " no_reply@email.apple.com ",
                    role = CounterpartyRole.Seller
                ),
                confidence = 0.95
            )
        )

        val snapshot = extraction.toAuthoritativeCounterpartySnapshot()
        assertNotNull(snapshot)
        assertEquals("Apple Account", snapshot.name)
        assertNull(snapshot.vatNumber)
        assertEquals("no_reply@email.apple.com", snapshot.email?.value)
    }

    @Test
    fun `unsupported country maps to null`() {
        val extraction = FinancialExtractionResult.CreditNote(
            CreditNoteExtractionResult(
                creditNoteNumber = "CN-1",
                issueDate = null,
                currency = Currency.Eur,
                subtotalAmount = null,
                vatAmount = null,
                totalAmount = null,
                sellerName = "ACME",
                sellerVat = null,
                buyerName = "Buyer",
                buyerVat = null,
                counterparty = CounterpartyExtraction(
                    name = "ACME",
                    country = "Ireland",
                    role = CounterpartyRole.Seller
                ),
                originalInvoiceNumber = null,
                reason = null,
                confidence = 0.9,
                reasoning = null
            )
        )

        val snapshot = extraction.toAuthoritativeCounterpartySnapshot()
        assertNotNull(snapshot)
        assertNull(snapshot.country)
    }

    @Test
    fun `missing or unusable counterparty yields null snapshot`() {
        val missing = FinancialExtractionResult.Receipt(
            ReceiptExtractionResult(
                merchantName = "Store",
                merchantVat = null,
                date = null,
                currency = Currency.Eur,
                totalAmount = null,
                vatAmount = null,
                receiptNumber = null,
                paymentMethod = null,
                counterparty = null,
                confidence = 0.8,
                reasoning = null
            )
        )
        val unusable = FinancialExtractionResult.Receipt(
            ReceiptExtractionResult(
                merchantName = "Store",
                merchantVat = null,
                date = null,
                currency = Currency.Eur,
                totalAmount = null,
                vatAmount = null,
                receiptNumber = null,
                paymentMethod = null,
                counterparty = CounterpartyExtraction(
                    vatNumber = "VISA9803APPLEPAY",
                    role = CounterpartyRole.Merchant
                ),
                confidence = 0.8,
                reasoning = null
            )
        )

        assertNull(missing.toAuthoritativeCounterpartySnapshot())
        assertNull(unusable.toAuthoritativeCounterpartySnapshot())
    }
}
