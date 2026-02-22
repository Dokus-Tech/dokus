package tech.dokus.features.cashflow.presentation.cashflow.components.upload

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.common_percent_value
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

/**
 * Upload progress indicator showing a linear progress bar with percentage text.
 *
 * Used to display file upload progress with:
 * - Visual progress bar showing completion
 * - Numeric percentage label
 *
 * @param progress Current progress value (0.0 to 1.0)
 * @param progressPercent Display percentage (0 to 100)
 * @param modifier Modifier to apply to the row
 */
@Composable
fun UploadProgressIndicator(
    progress: Float,
    progressPercent: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .weight(1f)
                .height(4.dp),
            strokeCap = StrokeCap.Round
        )
        Text(
            text = stringResource(Res.string.common_percent_value, progressPercent),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Deletion countdown progress indicator with animated fill.
 *
 * Shows a red progress bar that fills as the deletion countdown progresses.
 * Includes smooth animation between progress values.
 *
 * @param progress Current progress value (0.0 to 1.0)
 * @param modifier Modifier to apply to the indicator
 */
@Composable
fun DeletionProgressIndicator(
    progress: Float,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 100),
        label = "deletion-progress"
    )

    LinearProgressIndicator(
        progress = { animatedProgress },
        modifier = modifier
            .fillMaxWidth()
            .height(4.dp),
        color = MaterialTheme.colorScheme.error,
        trackColor = MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
        strokeCap = StrokeCap.Round
    )
}

// =============================================================================
// Previews
// =============================================================================

@Preview
@Composable
private fun UploadProgressIndicatorPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        UploadProgressIndicator(
            progress = 0.65f,
            progressPercent = 65
        )
    }
}

@Preview
@Composable
private fun DeletionProgressIndicatorPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        DeletionProgressIndicator(progress = 0.4f)
    }
}
