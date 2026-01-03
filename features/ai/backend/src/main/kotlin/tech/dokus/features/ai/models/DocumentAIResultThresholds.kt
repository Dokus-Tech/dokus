package tech.dokus.features.ai.models

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
        is DocumentAIResult.Invoice -> extractedData.hasMinimalInvoiceData()
        is DocumentAIResult.Bill -> extractedData.hasMinimalBillData()
        is DocumentAIResult.Receipt -> extractedData.hasMinimalReceiptData()
        is DocumentAIResult.Unknown -> false
    }
}

private fun ExtractedInvoiceData.hasMinimalInvoiceData(): Boolean {
    val hasTotal = !totalAmount.isNullOrBlank()
    val hasSubtotalAndVat = !subtotal.isNullOrBlank() && !totalVatAmount.isNullOrBlank()
    val hasTotalWithContext = hasTotal && (!issueDate.isNullOrBlank() || !vendorName.isNullOrBlank())
    return hasTotal || hasSubtotalAndVat || hasTotalWithContext
}

private fun ExtractedBillData.hasMinimalBillData(): Boolean {
    val hasAmount = !amount.isNullOrBlank()
    val hasAmountWithContext =
        hasAmount && (!issueDate.isNullOrBlank() || !supplierName.isNullOrBlank())
    return hasAmount || hasAmountWithContext
}

private fun ExtractedReceiptData.hasMinimalReceiptData(): Boolean {
    val hasAmount = !totalAmount.isNullOrBlank()
    val hasMerchantOrDate = !merchantName.isNullOrBlank() || !transactionDate.isNullOrBlank()
    return hasAmount && hasMerchantOrDate
}
