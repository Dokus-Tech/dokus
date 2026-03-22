package tech.dokus.backend.services.cashflow.matching

import tech.dokus.domain.model.TransactionCommunicationDto

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
        communication: TransactionCommunicationDto?,
        invoiceNumber: String?,
    ): Boolean {
        if (invoiceNumber.isNullOrBlank()) return false

        val normalizedInvoiceNumber = invoiceNumber.uppercase().replace(" ", "")
        if (normalizedInvoiceNumber.isBlank()) return false

        // Check structured raw text
        val structuredRaw = (communication as? TransactionCommunicationDto.Structured)?.raw
        if (structuredRaw != null) {
            val normalizedStructured = structuredRaw.uppercase().replace(" ", "")
            if (normalizedStructured.contains(normalizedInvoiceNumber)) return true
        }

        // Check free-form text
        val freeText = (communication as? TransactionCommunicationDto.FreeForm)?.text
        if (freeText != null) {
            val normalizedFree = freeText.uppercase().replace(" ", "")
            if (normalizedFree.contains(normalizedInvoiceNumber)) return true
        }

        return false
    }

    /**
     * Extract the raw text from any communication type for description-based matching.
     */
    fun extractText(communication: TransactionCommunicationDto?): String? {
        return when (communication) {
            is TransactionCommunicationDto.Structured -> communication.raw
            is TransactionCommunicationDto.FreeForm -> communication.text
            null -> null
        }
    }
}
