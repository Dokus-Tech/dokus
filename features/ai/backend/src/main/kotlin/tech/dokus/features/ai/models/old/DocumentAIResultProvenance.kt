package tech.dokus.features.ai.models.old

import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

/**
 * Extract the provenance data for separate storage.
 * Returns null if no provenance is available.
 */
fun DocumentAIResult.getProvenanceJson(): String? {
    return when (this) {
        is DocumentAIResult.Invoice -> extractedData.provenance?.let {
            Json.encodeToString(InvoiceProvenance.serializer(), it)
        }

        is DocumentAIResult.Bill -> extractedData.provenance?.let {
            Json.encodeToString(BillProvenance.serializer(), it)
        }

        is DocumentAIResult.Receipt -> extractedData.provenance?.let {
            Json.encodeToString(ReceiptProvenance.serializer(), it)
        }

        is DocumentAIResult.CreditNote -> extractedData.provenance?.let {
            Json.encodeToString(InvoiceProvenance.serializer(), it)
        }

        is DocumentAIResult.ProForma -> extractedData.provenance?.let {
            Json.encodeToString(InvoiceProvenance.serializer(), it)
        }

        is DocumentAIResult.Expense -> extractedData.provenance?.let {
            Json.encodeToString(ExpenseProvenance.serializer(), it)
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
        is DocumentAIResult.CreditNote -> extractedData.provenance?.toFieldConfidences()
        is DocumentAIResult.ProForma -> extractedData.provenance?.toFieldConfidences()
        is DocumentAIResult.Expense -> extractedData.provenance?.toFieldConfidences()
        is DocumentAIResult.Unknown -> null
    }
    return confidences?.let {
        Json.encodeToString(serializer<Map<String, Double>>(), it)
    }
}

internal fun InvoiceProvenance.toFieldConfidences(): Map<String, Double> {
    return buildMap {
        putIfConfidence("vendorName", vendorName?.fieldConfidence)
        putIfConfidence("vendorVatNumber", vendorVatNumber?.fieldConfidence)
        putIfConfidence("vendorAddress", vendorAddress?.fieldConfidence)
        putIfConfidence("invoiceNumber", invoiceNumber?.fieldConfidence)
        putIfConfidence("issueDate", issueDate?.fieldConfidence)
        putIfConfidence("dueDate", dueDate?.fieldConfidence)
        putIfConfidence("paymentTerms", paymentTerms?.fieldConfidence)
        putIfConfidence("currency", currency?.fieldConfidence)
        putIfConfidence("subtotal", subtotal?.fieldConfidence)
        putIfConfidence("totalVatAmount", totalVatAmount?.fieldConfidence)
        putIfConfidence("totalAmount", totalAmount?.fieldConfidence)
        putIfConfidence("iban", iban?.fieldConfidence)
        putIfConfidence("bic", bic?.fieldConfidence)
        putIfConfidence("paymentReference", paymentReference?.fieldConfidence)
    }
}

internal fun BillProvenance.toFieldConfidences(): Map<String, Double> {
    return buildMap {
        putIfConfidence("supplierName", supplierName?.fieldConfidence)
        putIfConfidence("supplierVatNumber", supplierVatNumber?.fieldConfidence)
        putIfConfidence("supplierAddress", supplierAddress?.fieldConfidence)
        putIfConfidence("invoiceNumber", invoiceNumber?.fieldConfidence)
        putIfConfidence("issueDate", issueDate?.fieldConfidence)
        putIfConfidence("dueDate", dueDate?.fieldConfidence)
        putIfConfidence("amount", amount?.fieldConfidence)
        putIfConfidence("vatAmount", vatAmount?.fieldConfidence)
        putIfConfidence("vatRate", vatRate?.fieldConfidence)
        putIfConfidence("currency", currency?.fieldConfidence)
        putIfConfidence("category", category?.fieldConfidence)
        putIfConfidence("description", description?.fieldConfidence)
    }
}

internal fun ReceiptProvenance.toFieldConfidences(): Map<String, Double> {
    return buildMap {
        putIfConfidence("merchantName", merchantName?.fieldConfidence)
        putIfConfidence("merchantAddress", merchantAddress?.fieldConfidence)
        putIfConfidence("merchantVatNumber", merchantVatNumber?.fieldConfidence)
        putIfConfidence("transactionDate", transactionDate?.fieldConfidence)
        putIfConfidence("transactionTime", transactionTime?.fieldConfidence)
        putIfConfidence("totalAmount", totalAmount?.fieldConfidence)
        putIfConfidence("vatAmount", vatAmount?.fieldConfidence)
        putIfConfidence("paymentMethod", paymentMethod?.fieldConfidence)
        putIfConfidence("category", category?.fieldConfidence)
    }
}

internal fun ExpenseProvenance.toFieldConfidences(): Map<String, Double> {
    return buildMap {
        putIfConfidence("merchantName", merchantName?.fieldConfidence)
        putIfConfidence("description", description?.fieldConfidence)
        putIfConfidence("date", date?.fieldConfidence)
        putIfConfidence("totalAmount", totalAmount?.fieldConfidence)
        putIfConfidence("currency", currency?.fieldConfidence)
        putIfConfidence("category", category?.fieldConfidence)
        putIfConfidence("paymentMethod", paymentMethod?.fieldConfidence)
    }
}

private fun MutableMap<String, Double>.putIfConfidence(key: String, value: Double?) {
    if (value != null) {
        put(key, value)
    }
}
