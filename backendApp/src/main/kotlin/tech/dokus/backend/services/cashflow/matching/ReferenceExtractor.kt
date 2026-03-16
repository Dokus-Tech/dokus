package tech.dokus.backend.services.cashflow.matching

import tech.dokus.domain.model.TransactionCommunication

/**
 * Extracts invoice references from free-form transaction communication text.
 */
object ReferenceExtractor {

    /**
     * Check if the free-form communication contains an invoice number reference.
     *
     * Normalizes both sides (uppercase, strip spaces) and does substring matching.
     */
    fun containsInvoiceNumber(
        communication: TransactionCommunication?,
        invoiceNumber: String?,
    ): Boolean {
        if (invoiceNumber.isNullOrBlank()) return false

        val normalizedInvoiceNumber = invoiceNumber.uppercase().replace(" ", "")
        if (normalizedInvoiceNumber.isBlank()) return false

        // Check structured raw text
        val structuredRaw = (communication as? TransactionCommunication.Structured)?.raw
        if (structuredRaw != null) {
            val normalizedStructured = structuredRaw.uppercase().replace(" ", "")
            if (normalizedStructured.contains(normalizedInvoiceNumber)) return true
        }

        // Check free-form text
        val freeText = (communication as? TransactionCommunication.FreeForm)?.text
        if (freeText != null) {
            val normalizedFree = freeText.uppercase().replace(" ", "")
            if (normalizedFree.contains(normalizedInvoiceNumber)) return true
        }

        return false
    }

    /**
     * Extract the raw text from any communication type for description-based matching.
     */
    fun extractText(communication: TransactionCommunication?): String? {
        return when (communication) {
            is TransactionCommunication.Structured -> communication.raw
            is TransactionCommunication.FreeForm -> communication.text
            null -> null
        }
    }
}
