package tech.dokus.features.ai.graph.sub.extraction.financial
import kotlin.test.Test
import kotlin.test.assertTrue
import tech.dokus.domain.enums.DocumentType
import tech.dokus.features.ai.models.ExtractDocumentInput
import tech.dokus.features.ai.models.ExtractionToolDescriptions

class ExtractionPromptDateGuidanceTest {

    @Test
    fun `receipt and invoice prompts clarify document date examples versus tool output`() {
        val receiptPrompt = promptFromFile("ExtractReceiptSubGraphKt", "getReceiptPrompt", DocumentType.Receipt)
        val invoicePrompt = promptFromFile("ExtractInvoiceSubGraphKt", "getPrompt", DocumentType.Invoice)

        listOf(receiptPrompt, invoicePrompt).forEach { prompt ->
            assertTrue(prompt.contains(ExtractionToolDescriptions.LocalDateToolOutputGuidance))
            assertTrue(prompt.contains(ExtractionToolDescriptions.DocumentDateFormatClarification))
            assertTrue(prompt.contains("appear on the document"))
        }
    }

    @Test
    fun `all prompts with local date fields include shared ISO output guidance`() {
        val prompts = listOf(
            promptFromFile("ExtractReceiptSubGraphKt", "getReceiptPrompt", DocumentType.Receipt),
            promptFromFile("ExtractInvoiceSubGraphKt", "getPrompt", DocumentType.Invoice),
            promptFromFile("ExtractCreditNoteSubGraphKt", "getCreditNotePrompt", DocumentType.CreditNote),
            promptFromFile("ExtractQuoteSubGraphKt", "getQuotePrompt", DocumentType.Quote),
            promptFromFile("ExtractPurchaseOrderSubGraphKt", "getPurchaseOrderPrompt", DocumentType.PurchaseOrder),
            promptFromFile("ExtractProFormaSubGraphKt", "getProFormaPrompt", DocumentType.ProForma),
            promptFromFile("ExtractBankStatementSubGraphKt", "getBankStatementPrompt", DocumentType.BankStatement),
        )

        prompts.forEach { prompt ->
            assertTrue(prompt.contains(ExtractionToolDescriptions.LocalDateToolOutputGuidance))
        }
    }

    private fun promptFromFile(fileClassName: String, getterName: String, documentType: DocumentType): String {
        val fileClass = Class.forName(
            "tech.dokus.features.ai.graph.sub.extraction.financial.$fileClassName"
        )
        val getter = fileClass.getDeclaredMethod(getterName, ExtractDocumentInput::class.java)
        getter.isAccessible = true

        return getter.invoke(
            null,
            ExtractDocumentInput(documentType = documentType, language = "en")
        ) as String
    }
}
