package tech.dokus.features.cashflow.presentation.cashflow.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Description
import tech.dokus.foundation.aura.components.common.DokusLoader
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.upload_drag_hint
import tech.dokus.aura.resources.upload_drop_files
import tech.dokus.aura.resources.upload_select_or_drag

// UI dimensions
private val ZoneMinHeight = 160.dp
private val ZonePadding = 24.dp
private val ZoneCornerRadius = 12.dp
private val BorderStrokeWidth = 2.dp
private val IconContainerSize = 56.dp
private val IconSize = 28.dp
private val IconToTextSpacing = 12.dp
private val TextToHintSpacing = 4.dp

// Border dash pattern
private const val DashLength = 10f
private const val DashGap = 10f
private const val DashPhase = 0f

// Alpha values
private const val IconBackgroundAlpha = 0.15f
private const val HintTextAlpha = 0.7f

/**
 * Document upload zone component with dashed border and drag state visual feedback.
 *
 * Like Pulse's PulseDropZone, this component:
 * - Shows a dashed border that changes color when dragging
 * - Displays an icon and text that update based on drag state
 * - Supports both click to upload and drag and drop (on desktop)
 *
 * @param isDragging Whether files are currently being dragged over the zone
 * @param onClick Callback when the zone is clicked
 * @param onDragStateChange Callback when drag state changes
 * @param onFilesDropped Callback when files are dropped
 * @param isUploading Whether upload is in progress
 * @param title Text to display in the zone (used when not dragging)
 * @param icon Icon to display (Camera or Document)
 * @param modifier Optional modifier
 */
@Composable
fun DocumentUploadZone(
    isDragging: Boolean,
    onClick: () -> Unit,
    onDragStateChange: (Boolean) -> Unit,
    onFilesDropped: (List<DroppedFile>) -> Unit,
    modifier: Modifier = Modifier,
    isUploading: Boolean = false,
    title: String? = null,
    icon: UploadIcon = UploadIcon.Document
) {
    val borderColor = if (isDragging) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline
    }

    val resolvedTitle = title ?: stringResource(Res.string.upload_select_or_drag)
    val displayText = if (isDragging) {
        stringResource(Res.string.upload_drop_files)
    } else {
        resolvedTitle
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .requiredHeightIn(min = ZoneMinHeight)
            .clip(MaterialTheme.shapes.medium)
            .fileDropTarget(
                onDragStateChange = onDragStateChange,
                onFilesDropped = onFilesDropped
            )
            .clickable(enabled = !isUploading, onClick = onClick)
            .drawBehind {
                val stroke = Stroke(
                    width = BorderStrokeWidth.toPx(),
                    pathEffect = PathEffect.dashPathEffect(
                        intervals = floatArrayOf(DashLength, DashGap),
                        phase = DashPhase
                    )
                )
                drawRoundRect(
                    color = borderColor,
                    style = stroke,
                    cornerRadius = CornerRadius(ZoneCornerRadius.toPx())
                )
            }
            .padding(ZonePadding),
        contentAlignment = Alignment.Center
    ) {
        if (isUploading) {
            CircularProgressIndicator()
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Icon with background circle (like Pulse)
                Box(
                    modifier = Modifier
                        .size(IconContainerSize)
                        .background(
                            color = borderColor.copy(alpha = IconBackgroundAlpha),
                            shape = MaterialTheme.shapes.medium
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isDragging) {
                            Icons.Default.Add
                        } else {
                            when (icon) {
                                UploadIcon.Camera -> Icons.Outlined.CameraAlt
                                UploadIcon.Document -> Icons.Outlined.Description
                            }
                        },
                        contentDescription = null,
                        modifier = Modifier.size(IconSize),
                        tint = borderColor
                    )
                }

                Spacer(modifier = Modifier.height(IconToTextSpacing))

                Text(
                    text = displayText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = borderColor,
                    textAlign = TextAlign.Center
                )

                if (isDragDropSupported && !isDragging) {
                    Spacer(modifier = Modifier.height(TextToHintSpacing))
                    Text(
                        text = stringResource(Res.string.upload_drag_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = borderColor.copy(alpha = HintTextAlpha),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * Simplified document upload zone without drag state management.
 * Use this when the parent manages drag state.
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
    title: String? = null,
    icon: UploadIcon = UploadIcon.Document
) {
    val borderColor = MaterialTheme.colorScheme.outline
    val resolvedTitle = title ?: stringResource(Res.string.upload_select_or_drag)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .requiredHeightIn(min = ZoneMinHeight)
            .clip(MaterialTheme.shapes.medium)
            .clickable(enabled = !isUploading, onClick = onUploadClick)
            .drawBehind {
                val stroke = Stroke(
                    width = BorderStrokeWidth.toPx(),
                    pathEffect = PathEffect.dashPathEffect(
                        intervals = floatArrayOf(DashLength, DashGap),
                        phase = DashPhase
                    )
                )
                drawRoundRect(
                    color = borderColor,
                    style = stroke,
                    cornerRadius = CornerRadius(ZoneCornerRadius.toPx())
                )
            }
            .padding(ZonePadding),
        contentAlignment = Alignment.Center
    ) {
        if (isUploading) {
            CircularProgressIndicator()
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(IconContainerSize)
                        .background(
                            color = borderColor.copy(alpha = IconBackgroundAlpha),
                            shape = MaterialTheme.shapes.medium
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (icon) {
                            UploadIcon.Camera -> Icons.Outlined.CameraAlt
                            UploadIcon.Document -> Icons.Outlined.Description
                        },
                        contentDescription = null,
                        modifier = Modifier.size(IconSize),
                        tint = borderColor
                    )
                }

                Spacer(modifier = Modifier.height(IconToTextSpacing))

                Text(
                    text = resolvedTitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = borderColor,
                    textAlign = TextAlign.Center
                )

                if (isDragDropSupported) {
                    Spacer(modifier = Modifier.height(TextToHintSpacing))
                    Text(
                        text = stringResource(Res.string.upload_drag_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = borderColor.copy(alpha = HintTextAlpha),
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
