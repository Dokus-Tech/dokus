package tech.dokus.domain.model

import kotlinx.serialization.Serializable
import tech.dokus.domain.validators.ValidateOgmUseCase

/**
 * Canonical representation of payment reference information.
 *
 * - structuredComm: Belgian OGM (structured communication), normalized to +++XXX/XXXX/XXXXX+++
 * - reference: Free-text payment reference (trimmed, no validation)
 */
@Serializable
data class CanonicalPayment(
    val structuredComm: String? = null,
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
        fun from(raw: String?): CanonicalPayment? {
            if (raw == null) return null
            val trimmed = raw.trim()
            if (trimmed.isEmpty()) {
                return CanonicalPayment(reference = trimmed)
            }

            val ogmResult = ValidateOgmUseCase.validate(trimmed)
            val normalized = ogmResult.normalizedOrNull
            return if (normalized != null) {
                CanonicalPayment(structuredComm = normalized)
            } else {
                CanonicalPayment(reference = trimmed)
            }
        }
    }
}
