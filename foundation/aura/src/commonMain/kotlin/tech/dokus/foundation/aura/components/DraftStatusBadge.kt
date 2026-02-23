package tech.dokus.foundation.aura.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import tech.dokus.domain.enums.DocumentStatus
import tech.dokus.foundation.aura.extensions.localized
import tech.dokus.foundation.aura.extensions.onColor
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

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

@Preview
@Composable
private fun DraftStatusBadgePreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        DraftStatusBadge(status = DocumentStatus.NeedsReview)
    }
}
