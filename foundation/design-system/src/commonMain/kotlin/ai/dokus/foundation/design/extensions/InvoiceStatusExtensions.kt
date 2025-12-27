package ai.dokus.foundation.design.extensions

import tech.dokus.domain.enums.InvoiceStatus
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Extension property to get a localized display name for an InvoiceStatus.
 *
 * Usage:
 * ```kotlin
 * @Composable
 * fun StatusBadge(status: InvoiceStatus) {
 *     Text(text = status.localized)
 * }
 * ```
 */
val InvoiceStatus.localized: String
    @Composable get() = when (this) {
        InvoiceStatus.Draft -> "DRAFT"
        InvoiceStatus.Sent -> "SENT"
        InvoiceStatus.Viewed -> "VIEWED"
        InvoiceStatus.PartiallyPaid -> "PARTIAL"
        InvoiceStatus.Paid -> "PAID"
        InvoiceStatus.Overdue -> "OVERDUE"
        InvoiceStatus.Cancelled -> "CANCELLED"
        InvoiceStatus.Refunded -> "REFUNDED"
    }

/**
 * Extension property to get the localized status name in uppercase for display.
 *
 * Usage:
 * ```kotlin
 * @Composable
 * fun StatusHeader(status: InvoiceStatus) {
 *     Text(text = status.localizedUppercase) // "DRAFT", "SENT", etc.
 * }
 * ```
 */
val InvoiceStatus.localizedUppercase: String
    @Composable get() = localized.uppercase()

/**
 * Extension property to get the appropriate color for an InvoiceStatus.
 * Returns a color suitable for badges, indicators, etc.
 *
 * Usage:
 * ```kotlin
 * @Composable
 * fun StatusBadge(status: InvoiceStatus) {
 *     Box(Modifier.background(status.color)) { ... }
 * }
 * ```
 */
val InvoiceStatus.color: Color
    @Composable get() = when (this) {
        InvoiceStatus.Draft -> MaterialTheme.colorScheme.surfaceVariant
        InvoiceStatus.Sent -> MaterialTheme.colorScheme.primary
        InvoiceStatus.Viewed -> MaterialTheme.colorScheme.tertiary
        InvoiceStatus.PartiallyPaid -> MaterialTheme.colorScheme.secondary
        InvoiceStatus.Paid -> MaterialTheme.colorScheme.primary
        InvoiceStatus.Overdue -> MaterialTheme.colorScheme.error
        InvoiceStatus.Cancelled -> MaterialTheme.colorScheme.outline
        InvoiceStatus.Refunded -> MaterialTheme.colorScheme.secondary
    }

/**
 * Extension property to get the text/content color for an InvoiceStatus badge.
 *
 * Usage:
 * ```kotlin
 * @Composable
 * fun StatusBadge(status: InvoiceStatus) {
 *     Text(color = status.onColor, text = status.localized)
 * }
 * ```
 */
val InvoiceStatus.onColor: Color
    @Composable get() = when (this) {
        InvoiceStatus.Draft -> MaterialTheme.colorScheme.onSurfaceVariant
        InvoiceStatus.Sent -> MaterialTheme.colorScheme.onPrimary
        InvoiceStatus.Viewed -> MaterialTheme.colorScheme.onTertiary
        InvoiceStatus.PartiallyPaid -> MaterialTheme.colorScheme.onSecondary
        InvoiceStatus.Paid -> MaterialTheme.colorScheme.onPrimary
        InvoiceStatus.Overdue -> MaterialTheme.colorScheme.onError
        InvoiceStatus.Cancelled -> MaterialTheme.colorScheme.surface
        InvoiceStatus.Refunded -> MaterialTheme.colorScheme.onSecondary
    }

/**
 * Checks if this invoice status indicates the invoice is in a draft/editable state.
 */
val InvoiceStatus.isDraft: Boolean
    get() = this == InvoiceStatus.Draft

/**
 * Checks if this invoice status indicates the invoice is fully paid.
 */
val InvoiceStatus.isPaid: Boolean
    get() = this == InvoiceStatus.Paid

/**
 * Checks if this invoice status indicates the invoice requires attention.
 */
val InvoiceStatus.needsAttention: Boolean
    get() = this == InvoiceStatus.Sent || this == InvoiceStatus.Overdue

/**
 * Checks if this invoice status indicates the invoice is in a final/completed state.
 */
val InvoiceStatus.isFinal: Boolean
    get() = this == InvoiceStatus.Paid || this == InvoiceStatus.Refunded || this == InvoiceStatus.Cancelled
