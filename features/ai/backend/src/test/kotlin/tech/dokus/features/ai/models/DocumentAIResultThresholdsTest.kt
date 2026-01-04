package tech.dokus.features.ai.models

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DocumentAIResultThresholdsTest {

    private fun classification(type: ClassifiedDocumentType) =
        DocumentClassification(type, confidence = 0.9, reasoning = "test")

    @Test
    fun `invoice meets threshold with total amount`() {
        val result = DocumentAIResult.Invoice(
            classification = classification(ClassifiedDocumentType.INVOICE),
            extractedData = ExtractedInvoiceData(totalAmount = "100.00"),
            confidence = 0.8,
            rawText = "invoice"
        )

        assertTrue(result.meetsMinimalThreshold())
    }

    @Test
    fun `invoice meets threshold with subtotal and vat`() {
        val result = DocumentAIResult.Invoice(
            classification = classification(ClassifiedDocumentType.INVOICE),
            extractedData = ExtractedInvoiceData(subtotal = "80.00", totalVatAmount = "20.00"),
            confidence = 0.8,
            rawText = "invoice"
        )

        assertTrue(result.meetsMinimalThreshold())
    }

    @Test
    fun `invoice fails threshold without totals`() {
        val result = DocumentAIResult.Invoice(
            classification = classification(ClassifiedDocumentType.INVOICE),
            extractedData = ExtractedInvoiceData(vendorName = "Vendor"),
            confidence = 0.8,
            rawText = "invoice"
        )

        assertFalse(result.meetsMinimalThreshold())
    }

    @Test
    fun `bill meets threshold with amount`() {
        val result = DocumentAIResult.Bill(
            classification = classification(ClassifiedDocumentType.BILL),
            extractedData = ExtractedBillData(amount = "120.00"),
            confidence = 0.7,
            rawText = "bill"
        )

        assertTrue(result.meetsMinimalThreshold())
    }

    @Test
    fun `receipt requires amount and merchant or date`() {
        val withMerchant = DocumentAIResult.Receipt(
            classification = classification(ClassifiedDocumentType.RECEIPT),
            extractedData = ExtractedReceiptData(totalAmount = "10.00", merchantName = "Shop"),
            confidence = 0.6,
            rawText = "receipt"
        )
        val withoutContext = DocumentAIResult.Receipt(
            classification = classification(ClassifiedDocumentType.RECEIPT),
            extractedData = ExtractedReceiptData(totalAmount = "10.00"),
            confidence = 0.6,
            rawText = "receipt"
        )

        assertTrue(withMerchant.meetsMinimalThreshold())
        assertFalse(withoutContext.meetsMinimalThreshold())
    }

    @Test
    fun `unknown never meets threshold`() {
        val result = DocumentAIResult.Unknown(
            classification = classification(ClassifiedDocumentType.UNKNOWN),
            rawText = "unknown"
        )

        assertFalse(result.meetsMinimalThreshold())
    }
}
