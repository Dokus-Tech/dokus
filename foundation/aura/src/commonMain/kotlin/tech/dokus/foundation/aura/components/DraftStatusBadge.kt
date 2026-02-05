package tech.dokus.foundation.aura.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import tech.dokus.domain.enums.DocumentStatus
import tech.dokus.foundation.aura.extensions.color
import tech.dokus.foundation.aura.extensions.localized
import tech.dokus.foundation.aura.extensions.onColor

/**
 * A specialized status badge for document draft states.
 * Maps DocumentStatus enum values to appropriate colors for visual indication.
 *
 * Color mapping:
 * - NeedsReview: Primary container - document awaiting user review
 * - Ready: Tertiary container - document ready for confirmation
 * - Confirmed: Tertiary container - user confirmed, entity created
 * - Rejected: Surface variant - user rejected extraction
 *
 * @param status The draft status to display
 * @param modifier Optional modifier for the badge
 */
@Composable
fun DraftStatusBadge(
    status: DocumentStatus,
    modifier: Modifier = Modifier
) {
    StatusBadge(
        text = status.localized,
        backgroundColor = status.color,
        textColor = status.onColor,
        modifier = modifier
    )
}
