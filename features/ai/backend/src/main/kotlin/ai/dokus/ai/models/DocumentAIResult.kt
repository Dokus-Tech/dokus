package ai.dokus.ai.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Sealed class representing the result of AI document processing.
 *
 * Contains:
 * - Classification result
 * - Type-specific extracted payload with provenance (AI-only, not for domain DTOs)
 * - Confidence and warnings
 *
 * The worker converts this to:
 * - ExtractedDocumentData (domain, business-only) -> stored in extracted_data
 * - provenance json -> provenance_data column
 * - field confidences json -> field_confidences column
 */
@Serializable
sealed class DocumentAIResult {

    /** The classification that determined this document type */
    abstract val classification: DocumentClassification

    /** Overall extraction confidence (0.0 - 1.0) */
    abstract val confidence: Double

    /** Warnings generated during extraction (non-fatal issues) */
    abstract val warnings: List<String>

    /** Raw text used for extraction (for storage/debugging) */
    abstract val rawText: String

    /**
     * Invoice document extraction result.
     * Outgoing invoices to clients.
     */
    @Serializable
    @SerialName("Invoice")
    data class Invoice(
        override val classification: DocumentClassification,
        val extractedData: ExtractedInvoiceData,
        override val confidence: Double,
        override val warnings: List<String> = emptyList(),
        override val rawText: String
    ) : DocumentAIResult()

    /**
     * Bill document extraction result.
     * Incoming invoices from suppliers.
     */
    @Serializable
    @SerialName("Bill")
    data class Bill(
        override val classification: DocumentClassification,
        val extractedData: ExtractedBillData,
        override val confidence: Double,
        override val warnings: List<String> = emptyList(),
        override val rawText: String
    ) : DocumentAIResult()

    /**
     * Receipt/Expense document extraction result.
     * Point-of-sale receipts and expense documents.
     */
    @Serializable
    @SerialName("Receipt")
    data class Receipt(
        override val classification: DocumentClassification,
        val extractedData: ExtractedReceiptData,
        override val confidence: Double,
        override val warnings: List<String> = emptyList(),
        override val rawText: String
    ) : DocumentAIResult()

    /**
     * Unknown document type - could not be classified.
     */
    @Serializable
    @SerialName("Unknown")
    data class Unknown(
        override val classification: DocumentClassification,
        override val confidence: Double = 0.0,
        override val warnings: List<String> = emptyList(),
        override val rawText: String
    ) : DocumentAIResult()
}

/**
 * Check if extraction meets minimum threshold for draft creation.
 *
 * Rules per type:
 * - Invoice: needs totalAmount OR (subtotal+vat) OR (totalAmount+date+clientName)
 * - Bill: needs amount OR (amount+date+supplierName)
 * - Receipt: needs amount AND (merchant OR date)
 * - Unknown: never creates draft
 */
fun DocumentAIResult.meetsMinimalThreshold(): Boolean {
    return when (this) {
        is DocumentAIResult.Invoice -> {
            val data = extractedData
            val hasTotal = !data.totalAmount.isNullOrBlank()
            val hasSubtotalAndVat = !data.subtotal.isNullOrBlank() && !data.totalVatAmount.isNullOrBlank()
            val hasTotalWithContext = hasTotal && (!data.issueDate.isNullOrBlank() || !data.vendorName.isNullOrBlank())
            hasTotal || hasSubtotalAndVat || hasTotalWithContext
        }
        is DocumentAIResult.Bill -> {
            val data = extractedData
            val hasAmount = !data.amount.isNullOrBlank()
            val hasAmountWithContext = hasAmount && (!data.issueDate.isNullOrBlank() || !data.supplierName.isNullOrBlank())
            hasAmount || hasAmountWithContext
        }
        is DocumentAIResult.Receipt -> {
            val data = extractedData
            val hasAmount = !data.totalAmount.isNullOrBlank()
            val hasMerchantOrDate = !data.merchantName.isNullOrBlank() || !data.transactionDate.isNullOrBlank()
            hasAmount && hasMerchantOrDate
        }
        is DocumentAIResult.Unknown -> false
    }
}

/**
 * Extract the provenance data for separate storage.
 * Returns null if no provenance is available.
 */
fun DocumentAIResult.getProvenanceJson(): String? {
    return when (this) {
        is DocumentAIResult.Invoice -> extractedData.provenance?.let {
            kotlinx.serialization.json.Json.encodeToString(InvoiceProvenance.serializer(), it)
        }
        is DocumentAIResult.Bill -> extractedData.provenance?.let {
            kotlinx.serialization.json.Json.encodeToString(BillProvenance.serializer(), it)
        }
        is DocumentAIResult.Receipt -> extractedData.provenance?.let {
            kotlinx.serialization.json.Json.encodeToString(ReceiptProvenance.serializer(), it)
        }
        is DocumentAIResult.Unknown -> null
    }
}

/**
 * Extract field confidences for separate storage.
 */
fun DocumentAIResult.getFieldConfidencesJson(): String? {
    val confidences = when (this) {
        is DocumentAIResult.Invoice -> extractedData.provenance?.toFieldConfidences()
        is DocumentAIResult.Bill -> extractedData.provenance?.toFieldConfidences()
        is DocumentAIResult.Receipt -> extractedData.provenance?.toFieldConfidences()
        is DocumentAIResult.Unknown -> null
    }
    return confidences?.let {
        kotlinx.serialization.json.Json.encodeToString(
            kotlinx.serialization.serializer<Map<String, Double>>(),
            it
        )
    }
}

// Extension to convert provenance to field confidence map
private fun InvoiceProvenance.toFieldConfidences(): Map<String, Double> {
    val map = mutableMapOf<String, Double>()
    vendorName?.fieldConfidence?.let { map["vendorName"] = it }
    vendorVatNumber?.fieldConfidence?.let { map["vendorVatNumber"] = it }
    vendorAddress?.fieldConfidence?.let { map["vendorAddress"] = it }
    invoiceNumber?.fieldConfidence?.let { map["invoiceNumber"] = it }
    issueDate?.fieldConfidence?.let { map["issueDate"] = it }
    dueDate?.fieldConfidence?.let { map["dueDate"] = it }
    paymentTerms?.fieldConfidence?.let { map["paymentTerms"] = it }
    currency?.fieldConfidence?.let { map["currency"] = it }
    subtotal?.fieldConfidence?.let { map["subtotal"] = it }
    totalVatAmount?.fieldConfidence?.let { map["totalVatAmount"] = it }
    totalAmount?.fieldConfidence?.let { map["totalAmount"] = it }
    iban?.fieldConfidence?.let { map["iban"] = it }
    bic?.fieldConfidence?.let { map["bic"] = it }
    paymentReference?.fieldConfidence?.let { map["paymentReference"] = it }
    return map
}

private fun BillProvenance.toFieldConfidences(): Map<String, Double> {
    val map = mutableMapOf<String, Double>()
    supplierName?.fieldConfidence?.let { map["supplierName"] = it }
    supplierVatNumber?.fieldConfidence?.let { map["supplierVatNumber"] = it }
    supplierAddress?.fieldConfidence?.let { map["supplierAddress"] = it }
    invoiceNumber?.fieldConfidence?.let { map["invoiceNumber"] = it }
    issueDate?.fieldConfidence?.let { map["issueDate"] = it }
    dueDate?.fieldConfidence?.let { map["dueDate"] = it }
    amount?.fieldConfidence?.let { map["amount"] = it }
    vatAmount?.fieldConfidence?.let { map["vatAmount"] = it }
    vatRate?.fieldConfidence?.let { map["vatRate"] = it }
    currency?.fieldConfidence?.let { map["currency"] = it }
    category?.fieldConfidence?.let { map["category"] = it }
    description?.fieldConfidence?.let { map["description"] = it }
    return map
}

private fun ReceiptProvenance.toFieldConfidences(): Map<String, Double> {
    val map = mutableMapOf<String, Double>()
    merchantName?.fieldConfidence?.let { map["merchantName"] = it }
    merchantAddress?.fieldConfidence?.let { map["merchantAddress"] = it }
    merchantVatNumber?.fieldConfidence?.let { map["merchantVatNumber"] = it }
    transactionDate?.fieldConfidence?.let { map["transactionDate"] = it }
    transactionTime?.fieldConfidence?.let { map["transactionTime"] = it }
    totalAmount?.fieldConfidence?.let { map["totalAmount"] = it }
    vatAmount?.fieldConfidence?.let { map["vatAmount"] = it }
    paymentMethod?.fieldConfidence?.let { map["paymentMethod"] = it }
    category?.fieldConfidence?.let { map["category"] = it }
    return map
}
