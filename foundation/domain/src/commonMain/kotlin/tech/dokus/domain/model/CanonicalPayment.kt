package tech.dokus.domain.model

import kotlinx.serialization.Serializable
import tech.dokus.domain.ids.StructuredCommunication

/**
 * Canonical representation of payment reference information.
 *
 * - structuredComm: Belgian OGM (structured communication), normalized to +++XXX/XXXX/XXXXX+++
 * - reference: Free-text payment reference (trimmed, no validation)
 */
@Serializable
data class CanonicalPaymentDto(
    val structuredComm: StructuredCommunication? = null,
    val reference: String? = null
) {
    companion object {
        /**
         * Normalize a raw payment reference into structuredComm/reference.
         *
         * Rules:
         * - If raw is a valid OGM (including OCR-corrected), store normalized structuredComm.
         * - Otherwise store the trimmed raw value as reference.
         * - Do not null out non-null input, even if malformed after cleanup.
         */
        fun from(raw: String?): CanonicalPaymentDto? {
            if (raw == null) return null
            val trimmed = raw.trim()
            if (trimmed.isEmpty()) {
                return CanonicalPaymentDto(reference = trimmed)
            }

            val structured = StructuredCommunication.from(trimmed)
            return if (structured != null) {
                CanonicalPaymentDto(structuredComm = structured)
            } else {
                CanonicalPaymentDto(reference = trimmed)
            }
        }
    }
}
