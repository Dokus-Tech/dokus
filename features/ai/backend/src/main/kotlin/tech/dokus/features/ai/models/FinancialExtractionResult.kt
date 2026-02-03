package tech.dokus.features.ai.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import tech.dokus.features.ai.graph.sub.extraction.financial.BillExtractionResult
import tech.dokus.features.ai.graph.sub.extraction.financial.CreditNoteExtractionResult
import tech.dokus.features.ai.graph.sub.extraction.financial.InvoiceExtractionResult
import tech.dokus.features.ai.graph.sub.extraction.financial.ProFormaExtractionResult
import tech.dokus.features.ai.graph.sub.extraction.financial.PurchaseOrderExtractionResult
import tech.dokus.features.ai.graph.sub.extraction.financial.QuoteExtractionResult
import tech.dokus.features.ai.graph.sub.extraction.financial.ReceiptExtractionResult

/**
 * Unified extraction output for all financial document extractors.
 *
 * Use this to converge multiple extractor subgraphs into a single mapper node:
 *   FinancialExtractionResult -> FinancialDocumentDto
 *
 * IMPORTANT:
 * - This is "facts only" extraction output (no ids/status/timestamps).
 * - Mapping to domain/frontend DTO happens deterministically in code.
 */
@Serializable
sealed interface FinancialExtractionResult : ExtractionResult {

    @Serializable
    @SerialName("InvoiceExtraction")
    data class Invoice(
        val data: InvoiceExtractionResult
    ) : FinancialExtractionResult

    @Serializable
    @SerialName("BillExtraction")
    data class Bill(
        val data: BillExtractionResult
    ) : FinancialExtractionResult

    @Serializable
    @SerialName("CreditNoteExtraction")
    data class CreditNote(
        val data: CreditNoteExtractionResult
    ) : FinancialExtractionResult

    @Serializable
    @SerialName("QuoteExtraction")
    data class Quote(
        val data: QuoteExtractionResult
    ) : FinancialExtractionResult

    @Serializable
    @SerialName("ProFormaExtraction")
    data class ProForma(
        val data: ProFormaExtractionResult
    ) : FinancialExtractionResult

    @Serializable
    @SerialName("PurchaseOrderExtraction")
    data class PurchaseOrder(
        val data: PurchaseOrderExtractionResult
    ) : FinancialExtractionResult

    @Serializable
    @SerialName("ReceiptExtraction")
    data class Receipt(
        val data: ReceiptExtractionResult
    ) : FinancialExtractionResult

    /**
     * Optional: when classification says it's financial but you don't support the type yet,
     * or extraction couldn't proceed.
     */
    @Serializable
    @SerialName("UnsupportedFinancialExtraction")
    data class Unsupported(
        val documentType: String,
        val reason: String? = null
    ) : FinancialExtractionResult
}