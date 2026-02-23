package tech.dokus.foundation.aura.components.status

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.style.statusConfirmed
import tech.dokus.foundation.aura.style.statusError
import tech.dokus.foundation.aura.style.statusWarning
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

/**
 * Confidence level for AI-extracted document fields.
 */
enum class ConfidenceLevel {
    High,
    Medium,
    Low,
    Missing,
}

/**
 * Confidence level indicator dot.
 *
 * Uses: Document inspector invoice detail fields.
 *
 * @param level Confidence level determining color
 */
@Composable
fun ConfDot(
    level: ConfidenceLevel,
    modifier: Modifier = Modifier,
) {
    val color = when (level) {
        ConfidenceLevel.High -> MaterialTheme.colorScheme.statusConfirmed
        ConfidenceLevel.Medium -> MaterialTheme.colorScheme.statusWarning
        ConfidenceLevel.Low -> MaterialTheme.colorScheme.statusError
        ConfidenceLevel.Missing -> MaterialTheme.colorScheme.statusError
    }

    Box(
        modifier = modifier
            .size(Constraints.StatusDot.size)
            .background(color, CircleShape),
    )
}

@Preview
@Composable
private fun ConfDotPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        ConfDot(level = ConfidenceLevel.High)
    }
}
