package tech.dokus.foundation.aura.extensions

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.draft_status_confirmed
import tech.dokus.aura.resources.draft_status_needs_input
import tech.dokus.aura.resources.draft_status_needs_review
import tech.dokus.aura.resources.draft_status_ready
import tech.dokus.aura.resources.draft_status_rejected
import tech.dokus.domain.enums.DraftStatus

/**
 * Extension property to get a localized display name for a DraftStatus.
 *
 * Usage:
 * ```kotlin
 * @Composable
 * fun StatusBadge(status: DraftStatus) {
 *     Text(text = status.localized)
 * }
 * ```
 */
val DraftStatus.localized: String
    @Composable get() = when (this) {
        DraftStatus.NeedsInput -> stringResource(Res.string.draft_status_needs_input)
        DraftStatus.NeedsReview -> stringResource(Res.string.draft_status_needs_review)
        DraftStatus.Ready -> stringResource(Res.string.draft_status_ready)
        DraftStatus.Confirmed -> stringResource(Res.string.draft_status_confirmed)
        DraftStatus.Rejected -> stringResource(Res.string.draft_status_rejected)
    }

/**
 * Extension property to get the localized status name in uppercase for display.
 *
 * Usage:
 * ```kotlin
 * @Composable
 * fun StatusHeader(status: DraftStatus) {
 *     Text(text = status.localizedUppercase) // "NEEDS REVIEW", "READY", etc.
 * }
 * ```
 */
val DraftStatus.localizedUppercase: String
    @Composable get() = localized.uppercase()

/**
 * Extension property to get the appropriate background color for a DraftStatus badge.
 * Returns a color suitable for badges, indicators, etc.
 *
 * Usage:
 * ```kotlin
 * @Composable
 * fun StatusBadge(status: DraftStatus) {
 *     Box(Modifier.background(status.color)) { ... }
 * }
 * ```
 */
val DraftStatus.color: Color
    @Composable get() = when (this) {
        DraftStatus.NeedsInput -> MaterialTheme.colorScheme.errorContainer
        DraftStatus.NeedsReview -> MaterialTheme.colorScheme.primaryContainer
        DraftStatus.Ready -> MaterialTheme.colorScheme.tertiaryContainer
        DraftStatus.Confirmed -> MaterialTheme.colorScheme.tertiaryContainer
        DraftStatus.Rejected -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
    }

/**
 * Extension property to get the text/content color for a DraftStatus badge.
 *
 * Usage:
 * ```kotlin
 * @Composable
 * fun StatusBadge(status: DraftStatus) {
 *     Text(color = status.onColor, text = status.localized)
 * }
 * ```
 */
val DraftStatus.onColor: Color
    @Composable get() = when (this) {
        DraftStatus.NeedsInput -> MaterialTheme.colorScheme.onErrorContainer
        DraftStatus.NeedsReview -> MaterialTheme.colorScheme.onPrimaryContainer
        DraftStatus.Ready -> MaterialTheme.colorScheme.onTertiaryContainer
        DraftStatus.Confirmed -> MaterialTheme.colorScheme.onTertiaryContainer
        DraftStatus.Rejected -> MaterialTheme.colorScheme.onSurfaceVariant
    }

/**
 * Checks if this draft status indicates the document needs user attention.
 */
val DraftStatus.needsAttention: Boolean
    get() = this == DraftStatus.NeedsInput || this == DraftStatus.NeedsReview

/**
 * Checks if this draft status indicates the document is ready for confirmation.
 */
val DraftStatus.isReady: Boolean
    get() = this == DraftStatus.Ready

/**
 * Checks if this draft status indicates the document is in a final/completed state.
 */
val DraftStatus.isFinal: Boolean
    get() = this == DraftStatus.Confirmed || this == DraftStatus.Rejected
