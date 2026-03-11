package tech.dokus.domain.model

import kotlinx.serialization.Serializable
import tech.dokus.domain.ids.StructuredCommunication

/**
 * Typed representation of a bank transaction's payment communication.
 *
 * Belgian bank statements distinguish between:
 * - **Structured**: OGM format (+++XXX/XXXX/XXXXX+++) — validated and normalized.
 * - **FreeForm**: Unstructured reference text (invoice number, reference code, etc.).
 *
 * A transaction has at most one communication type.
 */
@Serializable
sealed interface TransactionCommunication {

    /** Belgian structured communication (OGM). */
    @Serializable
    data class Structured(
        val raw: String,
        val normalized: StructuredCommunication,
    ) : TransactionCommunication

    /** Free-form payment reference (e.g. invoice number, reference code). */
    @Serializable
    data class FreeForm(val text: String) : TransactionCommunication

    companion object {
        /**
         * Build from raw extracted fields. Prefers structured if valid OGM, otherwise freeform.
         */
        fun from(
            structuredCommunicationRaw: String?,
            freeCommunication: String?,
        ): TransactionCommunication? {
            if (!structuredCommunicationRaw.isNullOrBlank()) {
                val normalized = StructuredCommunication.from(structuredCommunicationRaw)
                if (normalized != null) {
                    return Structured(raw = structuredCommunicationRaw.trim(), normalized = normalized)
                }
            }
            if (!freeCommunication.isNullOrBlank()) {
                return FreeForm(text = freeCommunication.trim())
            }
            return null
        }
    }
}
