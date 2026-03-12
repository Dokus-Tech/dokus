package tech.dokus.domain.enums

import kotlinx.serialization.Serializable

/**
 * Trust derived from source class. Ordered by authority.
 * Peppol is authoritative because of origin and structure, not because of confidence score.
 *
 * Ordering: Peppol > Email > UploadScan > ManualEntry
 */
@Serializable
enum class SourceTrust {
    Peppol,
    Email,
    UploadScan,
    ManualEntry,
}
