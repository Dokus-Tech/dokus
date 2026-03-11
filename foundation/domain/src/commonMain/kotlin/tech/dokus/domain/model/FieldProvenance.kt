package tech.dokus.domain.model

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import tech.dokus.domain.enums.SourceTrust
import tech.dokus.domain.ids.DocumentSourceId
import tech.dokus.domain.ids.IngestionRunId
import tech.dokus.domain.ids.UserId

/**
 * Per-field provenance tracking for canonical document data.
 *
 * Separates source trust (where data came from) from extraction confidence
 * (how certain the extraction was). User locks always take precedence.
 *
 * Trust ordering for merge/update decisions:
 * 1. userLocked = true — nothing outranks the user
 * 2. Peppol — structured, authoritative by origin
 * 3. Email — structured forwarding
 * 4. UploadScan — AI-extracted, modulated by extractionConfidence
 * 5. ManualEntry — lowest automatic trust (but often user-locked)
 *
 * Within the same SourceTrust level, higher extractionConfidence wins.
 *
 * Storage: Map<String, FieldProvenance> where keys are field paths:
 * - Top-level: "invoiceNumber", "totalAmount", "direction", "issueDate"
 * - Nested: "seller.vat", "seller.name", "buyer.city"
 * - Absent key = no provenance recorded (legacy data or computed field)
 */
@Serializable
data class FieldProvenance(
    val sourceId: DocumentSourceId? = null,
    val sourceRunId: IngestionRunId? = null,
    val sourceTrust: SourceTrust,
    val extractionConfidence: Double? = null,
    val userLocked: Boolean = false,
    val lockedAt: LocalDateTime? = null,
    val lockedBy: UserId? = null,
) {
    /**
     * Whether this provenance should be overwritten by [other].
     * Returns true if [other] has higher trust and this field is not user-locked.
     */
    fun shouldBeOverwrittenBy(other: FieldProvenance): Boolean {
        if (userLocked) return false
        if (other.sourceTrust.ordinal < sourceTrust.ordinal) return true
        if (other.sourceTrust == sourceTrust) {
            val thisConf = extractionConfidence ?: 0.0
            val otherConf = other.extractionConfidence ?: 0.0
            return otherConf > thisConf
        }
        return false
    }
}
