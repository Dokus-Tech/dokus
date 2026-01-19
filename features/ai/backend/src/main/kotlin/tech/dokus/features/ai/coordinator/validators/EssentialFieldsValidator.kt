package tech.dokus.features.ai.coordinator.validators

import tech.dokus.features.ai.models.ExtractedBillData
import tech.dokus.features.ai.models.ExtractedExpenseData
import tech.dokus.features.ai.models.ExtractedInvoiceData
import tech.dokus.features.ai.models.ExtractedReceiptData
import tech.dokus.features.ai.utils.AmountParser

/**
 * Result of checking essential fields.
 */
internal data class EssentialFieldsCheck(
    val hasAllFields: Boolean,
    val missingFields: List<String>
)

/**
 * Validator for checking essential fields in extracted document data.
 */
internal object EssentialFieldsValidator {

    /**
     * Check essential invoice fields:
     * - totalAmount: must be present AND parseable as a number
     * - issueDate: required
     * - vendorIdentity: vendorName OR vendorVatNumber
     */
    fun checkInvoice(invoice: ExtractedInvoiceData): EssentialFieldsCheck {
        val missing = mutableListOf<String>()

        // Amount: must be present AND parseable
        if (!AmountParser.isParseable(invoice.totalAmount)) {
            missing.add("totalAmount")
        }

        // Date: required
        if (invoice.issueDate.isNullOrBlank()) {
            missing.add("issueDate")
        }

        // Vendor identity: name OR VAT number
        val hasVendorIdentity = !invoice.vendorName.isNullOrBlank() ||
            !invoice.vendorVatNumber.isNullOrBlank()
        if (!hasVendorIdentity) {
            missing.add("vendorIdentity (vendorName or vendorVatNumber)")
        }

        return EssentialFieldsCheck(missing.isEmpty(), missing)
    }

    /**
     * Check essential bill fields:
     * - totalAmount: must be present AND parseable
     * - issueDate: required
     * - supplierIdentity: supplierName OR supplierVatNumber
     */
    fun checkBill(bill: ExtractedBillData): EssentialFieldsCheck {
        val missing = mutableListOf<String>()

        if (!AmountParser.isParseable(bill.totalAmount)) {
            missing.add("totalAmount")
        }

        if (bill.issueDate.isNullOrBlank()) {
            missing.add("issueDate")
        }

        val hasSupplierIdentity = !bill.supplierName.isNullOrBlank() ||
            !bill.supplierVatNumber.isNullOrBlank()
        if (!hasSupplierIdentity) {
            missing.add("supplierIdentity (supplierName or supplierVatNumber)")
        }

        return EssentialFieldsCheck(missing.isEmpty(), missing)
    }

    /**
     * Check essential receipt fields:
     * - totalAmount: must be present AND parseable
     * - transactionDate: required
     * - merchantIdentity: merchantName OR merchantVatNumber
     */
    fun checkReceipt(receipt: ExtractedReceiptData): EssentialFieldsCheck {
        val missing = mutableListOf<String>()

        if (!AmountParser.isParseable(receipt.totalAmount)) {
            missing.add("totalAmount")
        }

        if (receipt.transactionDate.isNullOrBlank()) {
            missing.add("transactionDate")
        }

        val hasMerchantIdentity = !receipt.merchantName.isNullOrBlank() ||
            !receipt.merchantVatNumber.isNullOrBlank()
        if (!hasMerchantIdentity) {
            missing.add("merchantIdentity (merchantName or merchantVatNumber)")
        }

        return EssentialFieldsCheck(missing.isEmpty(), missing)
    }

    /**
     * Check essential expense fields:
     * - totalAmount: must be present AND parseable
     * - date: required
     * (merchant is optional for expenses)
     */
    fun checkExpense(expense: ExtractedExpenseData): EssentialFieldsCheck {
        val missing = mutableListOf<String>()

        if (!AmountParser.isParseable(expense.totalAmount)) {
            missing.add("totalAmount")
        }

        if (expense.date.isNullOrBlank()) {
            missing.add("date")
        }

        return EssentialFieldsCheck(missing.isEmpty(), missing)
    }

    /**
     * Get extraction confidence from any extraction type.
     */
    fun getExtractionConfidence(extraction: Any): Double {
        return when (extraction) {
            is ExtractedInvoiceData -> extraction.confidence
            is ExtractedBillData -> extraction.confidence
            is ExtractedReceiptData -> extraction.confidence
            is ExtractedExpenseData -> extraction.confidence
            else -> 0.5 // Default confidence for unknown types
        }
    }
}
