package ai.dokus.app.cashflow.presentation.review

import ai.dokus.app.resources.generated.Res
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
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = { onZoomChange((zoomLevel - 0.25f).coerceAtLeast(0.5f)) }
        ) {
            Icon(
                imageVector = Icons.Default.ZoomOut,
                contentDescription = stringResource(Res.string.action_zoom_out)
            )
        }

        Text(
            text = "${(zoomLevel * 100).toInt()}%",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        IconButton(
            onClick = { onZoomChange((zoomLevel + 0.25f).coerceAtMost(3f)) }
        ) {
            Icon(
                imageVector = Icons.Default.ZoomIn,
                contentDescription = stringResource(Res.string.action_zoom_in)
            )
        }

        TextButton(
            onClick = { onZoomChange(1f) }
        ) {
            Text(stringResource(Res.string.action_fit))
        }
    }
}
