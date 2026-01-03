package tech.dokus.foundation.app.network

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.launch

/**
 * Represents the current server connection state.
 *
 * @property isConnected Whether the server is currently reachable
 * @property lastCheckTime Epoch milliseconds of last successful connection (null if never connected)
 * @property onRetry Callback to manually trigger a connection check
 */
data class ServerConnectionState(
    val isConnected: Boolean,
    val lastCheckTime: Long?,
    val onRetry: () -> Unit
)

/**
 * CompositionLocal providing access to server connection state.
 *
 * Usage in any composable:
 * ```kotlin
 * val connectionState = LocalServerConnection.current
 * if (!connectionState.isConnected) {
 *     // Show offline UI
 * }
 * ```
 */
val LocalServerConnection = staticCompositionLocalOf<ServerConnectionState> {
    error("ServerConnection not provided. Wrap your composable tree with ServerConnectionProvided.")
}

/**
 * Extension to check if connected from the CompositionLocal.
 */
val ProvidableCompositionLocal<ServerConnectionState>.isConnected: Boolean
    @Composable get() = current.isConnected

/**
 * Extension to check if offline from the CompositionLocal.
 */
val ProvidableCompositionLocal<ServerConnectionState>.isOffline: Boolean
    @Composable get() = !current.isConnected

/**
 * Provider for server connection state.
 *
 * Wraps app content and provides connection state throughout the Compose tree.
 * Should be placed high in the composition, typically in App.kt.
 *
 * @param monitor The [ServerConnectionMonitor] instance (injected via Koin)
 * @param content The composable content that will have access to [LocalServerConnection]
 */
@Composable
fun ServerConnectionProvided(
    monitor: ServerConnectionMonitor,
    content: @Composable () -> Unit
) {
    val isConnected by monitor.isConnected.collectAsState()
    val lastSuccessTime by monitor.lastSuccessTime.collectAsState()
    val scope = rememberCoroutineScope()

    val state = remember(isConnected, lastSuccessTime) {
        ServerConnectionState(
            isConnected = isConnected,
            lastCheckTime = lastSuccessTime,
            onRetry = { scope.launch { monitor.checkConnection() } }
        )
    }

    CompositionLocalProvider(LocalServerConnection provides state) {
        content()
    }
}

/**
 * Simple helper to get the current online status.
 *
 * Use this in components that need to enable/disable based on connection status:
 * ```kotlin
 * val isOnline = rememberIsOnline()
 * PButton(
 *     text = "Create Invoice",
 *     isEnabled = isOnline,
 *     onClick = { ... }
 * )
 * ```
 */
@Composable
fun rememberIsOnline(): Boolean {
    return LocalServerConnection.current.isConnected
}
