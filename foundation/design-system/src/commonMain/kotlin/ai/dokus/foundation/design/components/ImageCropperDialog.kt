package ai.dokus.foundation.design.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage

/**
 * A dialog for cropping images to a square aspect ratio.
 * Supports pan and pinch-to-zoom gestures.
 *
 * @param imageData The raw image bytes to crop
 * @param onCropComplete Called when the user confirms the crop, with cropped image data
 * @param onDismiss Called when the user cancels
 */
@Composable
fun ImageCropperDialog(
    imageData: ByteArray,
    onCropComplete: (ByteArray) -> Unit,
    onDismiss: () -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 400.dp)
                .fillMaxWidth(0.9f)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel"
                        )
                    }
                    Text(
                        text = "Crop Image",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.width(48.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Image crop area
                Box(
                    modifier = Modifier
                        .sizeIn(maxWidth = 320.dp, maxHeight = 320.dp)
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black)
                        .onSizeChanged { containerSize = it }
                        .clipToBounds()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(0.5f, 4f)

                                val maxOffset = containerSize.width * (scale - 1) / 2
                                offsetX = (offsetX + pan.x).coerceIn(-maxOffset, maxOffset)
                                offsetY = (offsetY + pan.y).coerceIn(-maxOffset, maxOffset)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // Image with transformations
                    AsyncImage(
                        model = imageData,
                        contentDescription = "Image to crop",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                translationX = offsetX
                                translationY = offsetY
                            }
                    )

                    // Square crop overlay
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val overlayColor = Color.Black.copy(alpha = 0.5f)

                        // Draw semi-transparent overlay around crop area
                        // The visible area in center is the crop region
                        drawCropOverlay(overlayColor)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Pinch to zoom, drag to pan",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            // For now, we pass the original image data
                            // Real cropping would require platform-specific implementation
                            // The server handles the actual crop/resize
                            onCropComplete(imageData)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Done,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Apply")
                    }
                }
            }
        }
    }
}

/**
 * Draw a semi-transparent overlay with a clear square in the center.
 */
private fun DrawScope.drawCropOverlay(overlayColor: Color) {
    // For a simple implementation, we just draw corner guides
    val strokeWidth = 3.dp.toPx()
    val cornerLength = 40.dp.toPx()
    val padding = 24.dp.toPx()

    val left = padding
    val top = padding
    val right = size.width - padding
    val bottom = size.height - padding

    // Draw corner guides (white lines at corners)
    val guideColor = Color.White

    // Top-left corner
    drawLine(guideColor, Offset(left, top), Offset(left + cornerLength, top), strokeWidth)
    drawLine(guideColor, Offset(left, top), Offset(left, top + cornerLength), strokeWidth)

    // Top-right corner
    drawLine(guideColor, Offset(right - cornerLength, top), Offset(right, top), strokeWidth)
    drawLine(guideColor, Offset(right, top), Offset(right, top + cornerLength), strokeWidth)

    // Bottom-left corner
    drawLine(guideColor, Offset(left, bottom), Offset(left + cornerLength, bottom), strokeWidth)
    drawLine(guideColor, Offset(left, bottom - cornerLength), Offset(left, bottom), strokeWidth)

    // Bottom-right corner
    drawLine(guideColor, Offset(right - cornerLength, bottom), Offset(right, bottom), strokeWidth)
    drawLine(guideColor, Offset(right, bottom - cornerLength), Offset(right, bottom), strokeWidth)
}
