package ai.dokus.app.cashflow.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Document upload zone component with dashed border.
 * Displays icon and text, with loading state support.
 *
 * @param onUploadClick Callback when the zone is clicked
 * @param isUploading Whether upload is in progress
 * @param title Text to display in the zone
 * @param icon Icon to display (Camera or Document)
 * @param modifier Optional modifier
 */
@Composable
fun DocumentUploadZone(
    onUploadClick: () -> Unit,
    isUploading: Boolean,
    modifier: Modifier = Modifier,
    title: String = "Drag and Drop file here or Choose file",
    icon: UploadIcon = UploadIcon.Document
) {
    val dashedBorderColor = MaterialTheme.colorScheme.outlineVariant

    Card(
        onClick = { if (!isUploading) onUploadClick() },
        modifier = modifier
            .height(200.dp)
            .drawBehind {
                // Draw dashed border
                val stroke = Stroke(
                    width = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )
                drawRoundRect(
                    color = dashedBorderColor,
                    style = stroke,
                    cornerRadius = CornerRadius(12.dp.toPx())
                )
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isUploading) {
                CircularProgressIndicator()
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = when (icon) {
                            UploadIcon.Camera -> Icons.Outlined.CameraAlt
                            UploadIcon.Document -> Icons.Outlined.Description
                        },
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * Icon type for upload zone.
 */
enum class UploadIcon {
    Camera,
    Document
}
