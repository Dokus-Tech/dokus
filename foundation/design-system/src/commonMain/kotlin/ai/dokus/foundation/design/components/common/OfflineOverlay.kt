package ai.dokus.foundation.design.components.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * A reusable overlay component that blurs content when offline and shows a message.
 *
 * Features:
 * - Shows content normally when [isOffline] is false
 * - Applies blur effect and gray overlay when [isOffline] is true
 * - Displays a centered message indicating the content is unavailable offline
 * - Maintains the same layout size regardless of online/offline state
 *
 * Usage:
 * ```kotlin
 * OfflineOverlay(
 *     isOffline = !isOnline,
 *     message = "Activity unavailable offline"
 * ) {
 *     ActivitySummarySection(state = activityState)
 * }
 * ```
 *
 * @param isOffline Whether the device is offline and overlay should be shown
 * @param message The message to display when offline
 * @param modifier Optional modifier for the component
 * @param content The content to display (will be blurred when offline)
 */
@Composable
fun OfflineOverlay(
    isOffline: Boolean,
    message: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier) {
        // Content - blurred when offline
        Box(
            modifier = if (isOffline) {
                Modifier.blur(6.dp)
            } else {
                Modifier
            }
        ) {
            content()
        }

        // Overlay when offline
        if (isOffline) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}
