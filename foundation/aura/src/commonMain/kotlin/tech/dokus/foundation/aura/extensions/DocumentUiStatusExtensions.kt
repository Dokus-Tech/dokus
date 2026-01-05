package tech.dokus.foundation.aura.extensions

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.document_status_failed
import tech.dokus.aura.resources.document_status_processing
import tech.dokus.aura.resources.document_status_queued
import tech.dokus.aura.resources.document_status_ready
import tech.dokus.aura.resources.document_status_review
import tech.dokus.foundation.aura.model.DocumentUiStatus

/**
 * Extension property to get a localized display name for a DocumentUiStatus.
 *
 * Usage:
 * ```kotlin
 * @Composable
 * fun StatusBadge(status: DocumentUiStatus) {
 *     Text(text = status.localized)
 * }
 * ```
 */
val DocumentUiStatus.localized: String
    @Composable get() = when (this) {
        DocumentUiStatus.Queued -> stringResource(Res.string.document_status_queued)
        DocumentUiStatus.Processing -> stringResource(Res.string.document_status_processing)
        DocumentUiStatus.Review -> stringResource(Res.string.document_status_review)
        DocumentUiStatus.Ready -> stringResource(Res.string.document_status_ready)
        DocumentUiStatus.Failed -> stringResource(Res.string.document_status_failed)
    }

/**
 * Extension property to get the appropriate background color for a DocumentUiStatus badge.
 *
 * Color mapping:
 * - Queued: surfaceVariant (neutral, waiting)
 * - Processing: primaryContainer (active, in progress)
 * - Review: secondaryContainer (attention needed, but not error)
 * - Ready: tertiaryContainer (positive, complete)
 * - Failed: errorContainer (error, action needed)
 */
val DocumentUiStatus.color: Color
    @Composable get() = when (this) {
        DocumentUiStatus.Queued -> MaterialTheme.colorScheme.surfaceVariant
        DocumentUiStatus.Processing -> MaterialTheme.colorScheme.primaryContainer
        DocumentUiStatus.Review -> MaterialTheme.colorScheme.secondaryContainer
        DocumentUiStatus.Ready -> MaterialTheme.colorScheme.tertiaryContainer
        DocumentUiStatus.Failed -> MaterialTheme.colorScheme.errorContainer
    }

/**
 * Extension property to get the text/content color for a DocumentUiStatus badge.
 * @deprecated Use statusColor for Design System v1 dot + text pattern
 */
@Deprecated("Use statusColor for Design System v1 dot + text pattern")
val DocumentUiStatus.onColor: Color
    @Composable get() = when (this) {
        DocumentUiStatus.Queued -> MaterialTheme.colorScheme.onSurfaceVariant
        DocumentUiStatus.Processing -> MaterialTheme.colorScheme.onPrimaryContainer
        DocumentUiStatus.Review -> MaterialTheme.colorScheme.onSecondaryContainer
        DocumentUiStatus.Ready -> MaterialTheme.colorScheme.onTertiaryContainer
        DocumentUiStatus.Failed -> MaterialTheme.colorScheme.onErrorContainer
    }

/**
 * Extension property to get the status color for Design System v1 dot + text pattern.
 * Uses primary colors (not container colors) for better visibility.
 *
 * Color mapping:
 * - Queued: onSurfaceVariant (neutral, waiting)
 * - Processing: primary (active, in progress)
 * - Review: secondary (attention needed, but not error)
 * - Ready: tertiary (positive, complete)
 * - Failed: error (error, action needed)
 */
val DocumentUiStatus.statusColor: Color
    @Composable get() = when (this) {
        DocumentUiStatus.Queued -> MaterialTheme.colorScheme.onSurfaceVariant
        DocumentUiStatus.Processing -> MaterialTheme.colorScheme.primary
        DocumentUiStatus.Review -> MaterialTheme.colorScheme.secondary
        DocumentUiStatus.Ready -> MaterialTheme.colorScheme.tertiary
        DocumentUiStatus.Failed -> MaterialTheme.colorScheme.error
    }
