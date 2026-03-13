package tech.dokus.backend.services.cashflow.matching

import tech.dokus.domain.ids.StructuredCommunication
import tech.dokus.domain.model.TransactionCommunication

/**
 * Validates and compares Belgian OGM structured communications.
 */
internal object OgmValidator {

    /**
     * Extract and normalize the OGM from a transaction's communication.
     * Returns null if no valid OGM is present.
     */
    fun extractNormalized(communication: TransactionCommunication?): StructuredCommunication? {
        val raw = (communication as? TransactionCommunication.Structured)?.raw ?: return null
        return StructuredCommunication.from(raw)
    }

    /**
     * Check if a transaction's OGM matches an invoice's structured reference.
     */
    fun matches(
        txCommunication: TransactionCommunication?,
        invoiceStructuredRef: String?,
    ): Boolean {
        if (invoiceStructuredRef.isNullOrBlank()) return false
        val txOgm = extractNormalized(txCommunication) ?: return false
        val invoiceOgm = StructuredCommunication.from(invoiceStructuredRef) ?: return false
        return txOgm.value == invoiceOgm.value
    }
}
