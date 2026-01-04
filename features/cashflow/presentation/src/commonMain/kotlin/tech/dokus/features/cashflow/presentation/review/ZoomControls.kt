package tech.dokus.features.cashflow.presentation.review

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.action_fit
import tech.dokus.aura.resources.action_zoom_in
import tech.dokus.aura.resources.action_zoom_out
import tech.dokus.aura.resources.common_percent_value

// Layout dimensions
private val ControlsCornerRadius = 8.dp
private val ControlsPadding = 4.dp
private val ControlsSpacing = 4.dp
private val PercentLabelHorizontalPadding = 4.dp

// Zoom constants
private const val ZoomStep = 0.25f
private const val ZoomMin = 0.5f
private const val ZoomMax = 3f
private const val ZoomDefault = 1f
private const val SurfaceBackgroundAlpha = 0.9f
private const val PercentMultiplier = 100

/**
 * Zoom control component for PDF preview.
 *
 * Features:
 * - Zoom in/out buttons
 * - Current zoom percentage display
 * - Fit button to reset to 100%
 * - Zoom range: 50% - 300%
 *
 * @param zoomLevel Current zoom level (1.0 = 100%)
 * @param onZoomChange Callback when zoom level changes
 * @param modifier Modifier for the container
 */
@Composable
fun ZoomControls(
    zoomLevel: Float,
    onZoomChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.surface.copy(alpha = SurfaceBackgroundAlpha),
                shape = RoundedCornerShape(ControlsCornerRadius)
            )
            .padding(ControlsPadding),
        horizontalArrangement = Arrangement.spacedBy(ControlsSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = { onZoomChange((zoomLevel - ZoomStep).coerceAtLeast(ZoomMin)) }
        ) {
            Icon(
                imageVector = Icons.Default.ZoomOut,
                contentDescription = stringResource(Res.string.action_zoom_out)
            )
        }

        Text(
            text = stringResource(Res.string.common_percent_value, (zoomLevel * PercentMultiplier).toInt()),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = PercentLabelHorizontalPadding)
        )

        IconButton(
            onClick = { onZoomChange((zoomLevel + ZoomStep).coerceAtMost(ZoomMax)) }
        ) {
            Icon(
                imageVector = Icons.Default.ZoomIn,
                contentDescription = stringResource(Res.string.action_zoom_in)
            )
        }

        TextButton(
            onClick = { onZoomChange(ZoomDefault) }
        ) {
            Text(stringResource(Res.string.action_fit))
        }
    }
}
