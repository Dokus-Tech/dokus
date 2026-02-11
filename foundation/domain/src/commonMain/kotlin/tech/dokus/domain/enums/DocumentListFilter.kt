package tech.dokus.domain.enums

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * High-level document list filters used by the Documents screen.
 *
 * This is intentionally coarse and maps to user-facing tabs:
 * - ALL: everything
 * - NEEDS_ATTENTION: workflow items that require user action or are still processing
 * - CONFIRMED: confirmed documents (draft confirmed + financial fact exists)
 */
@Serializable
enum class DocumentListFilter {
    @SerialName("ALL")
    All,

    @SerialName("NEEDS_ATTENTION")
    NeedsAttention,

    @SerialName("CONFIRMED")
    Confirmed
}

