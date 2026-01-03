@file:Suppress("ReturnCount") // Validation requires multiple early returns

package tech.dokus.domain.validators

import tech.dokus.domain.ids.PeppolId
import tech.dokus.domain.ids.VatNumber

/**
 * Validates PEPPOL participant IDs
 *
 * PEPPOL ID format: scheme:identifier
 * - scheme: identifier scheme (e.g., "0208" for Belgian company numbers)
 * - identifier: the actual identifier value
 *
 * Examples:
 * - 0208:BE0123456789 (Belgian company number)
 * - 9925:BE0123456789 (Alternative Belgian scheme)
 *
 * For Belgian scheme (0208), the identifier must be a valid VAT number.
 */
object ValidatePeppolIdUseCase : Validator<PeppolId> {
    override operator fun invoke(value: PeppolId): Boolean {
        if (value.value.isBlank()) return false

        // Must be in format "scheme:identifier"
        val parts = value.value.split(":")
        if (parts.size != 2) return false

        val (scheme, identifier) = parts

        // Both parts must not be blank
        if (scheme.isBlank() || identifier.isBlank()) return false

        // For Belgian scheme (0208), validate the identifier as a VAT number
        if (scheme == "0208") {
            return ValidateVatNumberUseCase(VatNumber(identifier))
        }

        // For other schemes, accept any non-empty identifier
        return true
    }
}
