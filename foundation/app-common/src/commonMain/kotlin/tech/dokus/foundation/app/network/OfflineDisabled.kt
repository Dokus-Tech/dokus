package tech.dokus.foundation.app.network

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Wrapper that overlays content with a semi-transparent layer when server is not connected.
 *
 * Uses [LocalServerConnection] to determine connection status.
 *
 * Usage:
 * ```kotlin
 * OfflineDisabled {
 *     Column {
 *         // This content will be visually disabled when offline
 *         Button(onClick = { ... }) { Text("Submit") }
 *     }
 * }
 * ```
 *
 * @param modifier Modifier to apply to the outer Box
 * @param content The composable content that will be overlaid when offline
 */
@Composable
fun OfflineDisabled(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val connectionState = LocalServerConnection.current

    Box(modifier = modifier) {
        content()

        if (!connectionState.isConnected) {
            // Semi-transparent overlay to indicate disabled state
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
            )
        }
    }
}
