package tech.dokus.foundation.aura.extensions

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.draft_status_confirmed
import tech.dokus.aura.resources.draft_status_needs_review
import tech.dokus.aura.resources.draft_status_rejected
import tech.dokus.domain.enums.DocumentStatus

/**
 * Extension property to get a localized display name for a DocumentStatus.
 *
 * Usage:
 * ```kotlin
 * @Composable
 * fun StatusBadge(status: DocumentStatus) {
 *     Text(text = status.localized)
 * }
 * ```
 */
val DocumentStatus.localized: String
    @Composable get() = when (this) {
        DocumentStatus.NeedsReview -> stringResource(Res.string.draft_status_needs_review)
        DocumentStatus.Confirmed -> stringResource(Res.string.draft_status_confirmed)
        DocumentStatus.Rejected -> stringResource(Res.string.draft_status_rejected)
    }

/**
 * Extension property to get the localized status name in uppercase for display.
 *
 * Usage:
 * ```kotlin
 * @Composable
 * fun StatusHeader(status: DocumentStatus) {
 *     Text(text = status.localizedUppercase) // "NEEDS REVIEW", "CONFIRMED", etc.
 * }
 * ```
 */
val DocumentStatus.localizedUppercase: String
    @Composable get() = localized.uppercase()

/**
 * Extension property to get the appropriate background color for a DocumentStatus badge.
 * Returns a color suitable for badges, indicators, etc.
 *
 * Usage:
 * ```kotlin
 * @Composable
 * fun StatusBadge(status: DocumentStatus) {
 *     Box(Modifier.background(status.color)) { ... }
 * }
 * ```
 */
val DocumentStatus.color: Color
    @Composable get() = when (this) {
        DocumentStatus.NeedsReview -> MaterialTheme.colorScheme.primaryContainer
        DocumentStatus.Confirmed -> MaterialTheme.colorScheme.tertiaryContainer
        DocumentStatus.Rejected -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
    }

/**
 * Extension property to get the text/content color for a DocumentStatus badge.
 *
 * Usage:
 * ```kotlin
 * @Composable
 * fun StatusBadge(status: DocumentStatus) {
 *     Text(color = status.onColor, text = status.localized)
 * }
 * ```
 */
val DocumentStatus.onColor: Color
    @Composable get() = when (this) {
        DocumentStatus.NeedsReview -> MaterialTheme.colorScheme.onPrimaryContainer
        DocumentStatus.Confirmed -> MaterialTheme.colorScheme.onTertiaryContainer
        DocumentStatus.Rejected -> MaterialTheme.colorScheme.onSurfaceVariant
    }

/**
 * Checks if this draft status indicates the document needs user attention.
 */
val DocumentStatus.needsAttention: Boolean
    get() = this == DocumentStatus.NeedsReview

/**
 * Checks if this draft status indicates the document is in a final/completed state.
 */
val DocumentStatus.isFinal: Boolean
    get() = this == DocumentStatus.Confirmed || this == DocumentStatus.Rejected
