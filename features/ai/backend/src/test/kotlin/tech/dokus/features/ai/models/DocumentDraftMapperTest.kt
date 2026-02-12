package tech.dokus.features.ai.models

import org.junit.jupiter.api.Test
import tech.dokus.domain.enums.Currency
import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.model.DocumentDraftData
import tech.dokus.domain.model.InvoiceDraftData
import tech.dokus.domain.utils.json
import tech.dokus.features.ai.graph.sub.ClassificationResult
import tech.dokus.features.ai.graph.sub.extraction.financial.InvoiceExtractionResult
import tech.dokus.features.ai.validation.AuditReport
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class DocumentDraftMapperTest {

    @Test
    fun `invoice mapping keeps seller buyer and does not populate legacy customer fields`() {
        val draft = invoiceProcessingResult().toDraftData()
        val invoice = assertIs<InvoiceDraftData>(draft)

        assertEquals("Apple Distribution International Ltd.", invoice.seller.name)
        assertEquals("Artem Kuznetsov", invoice.buyer.name)
    }

    @Test
    fun `serialized invoice draft omits legacy customer keys when unset`() {
        val draft = invoiceProcessingResult().toDraftData()
        val invoice = assertIs<InvoiceDraftData>(draft)

        val serialized = json.encodeToString(DocumentDraftData.serializer(), invoice)
        assertTrue("\"customerName\"" !in serialized)
        assertTrue("\"customerVat\"" !in serialized)
        assertTrue("\"customerEmail\"" !in serialized)
    }

    private fun invoiceProcessingResult(): DocumentAiProcessingResult {
        val extraction = FinancialExtractionResult.Invoice(
            InvoiceExtractionResult(
                invoiceNumber = "1-12218196743",
                issueDate = null,
                dueDate = null,
                currency = Currency.Eur,
                subtotalAmount = null,
                vatAmount = null,
                totalAmount = null,
                sellerName = "Apple Distribution International Ltd.",
                sellerVat = null,
                buyerName = "Artem Kuznetsov",
                buyerVat = null,
                confidence = 0.95,
                reasoning = null
            )
        )

        return DocumentAiProcessingResult(
            classification = ClassificationResult(
                documentType = DocumentType.Invoice,
                confidence = 0.98,
                language = "en",
                reasoning = "invoice"
            ),
            extraction = extraction,
            directionResolution = DirectionResolution(
                direction = DocumentDirection.Inbound,
                source = DirectionResolutionSource.NameMatch,
                confidence = 0.8
            ),
            auditReport = AuditReport.EMPTY
        )
    }
}
