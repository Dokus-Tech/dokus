package tech.dokus.foundation.aura.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import tech.dokus.domain.enums.DocumentStatus
import tech.dokus.foundation.aura.extensions.localized
import tech.dokus.foundation.aura.extensions.onColor

/**
 * A specialized status badge for document draft states.
 * Maps DocumentStatus enum values to appropriate colors for visual indication.
 *
 * @param status The draft status to display
 */
@Composable
fun DraftStatusBadge(
    status: DocumentStatus,
    modifier: Modifier = Modifier,
) {
    StatusBadge(
        text = status.localized,
        color = status.onColor,
        modifier = modifier,
    )
}
