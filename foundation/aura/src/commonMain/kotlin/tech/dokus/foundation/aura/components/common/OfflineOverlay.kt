package tech.dokus.foundation.aura.components.common

import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.state_offline
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource

/**
 * A reusable overlay component that blurs content when offline and shows an indicator.
 *
 * Features:
 * - Shows content normally when [isOffline] is false
 * - Applies blur effect and gray overlay when [isOffline] is true
 * - Displays an offline icon at the top-right corner
 * - Maintains the same layout size regardless of online/offline state
 *
 * Usage:
 * ```kotlin
 * OfflineOverlay(
 *     isOffline = !isOnline
 * ) {
 *     ActivitySummarySection(state = activityState)
 * }
 * ```
 *
 * @param isOffline Whether the device is offline and overlay should be shown
 * @param modifier Optional modifier for the component
 * @param content The content to display (will be blurred when offline)
 */
@Composable
fun OfflineOverlay(
    isOffline: Boolean,
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
                contentAlignment = Alignment.TopEnd
            ) {
                // Offline icon badge at top-right
                Box(
                    modifier = Modifier
                        .padding(8.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = CircleShape
                        )
                        .padding(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudOff,
                        contentDescription = stringResource(Res.string.state_offline),
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
