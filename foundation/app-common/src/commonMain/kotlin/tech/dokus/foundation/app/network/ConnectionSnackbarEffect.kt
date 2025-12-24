package tech.dokus.foundation.app.network

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

/**
 * Effect that shows a snackbar when server connection state changes.
 *
 * - Shows "Server unreachable" with a "Retry" action when going offline
 * - Shows "Connected" briefly when coming back online
 *
 * Usage in screens with Scaffold:
 * ```kotlin
 * @Composable
 * fun SomeScreen() {
 *     val snackbarHostState = remember { SnackbarHostState() }
 *
 *     // Auto-show snackbar on connection changes
 *     ConnectionSnackbarEffect(snackbarHostState)
 *
 *     Scaffold(
 *         snackbarHost = { SnackbarHost(snackbarHostState) }
 *     ) { ... }
 * }
 * ```
 *
 * @param snackbarHostState The SnackbarHostState from the screen's Scaffold
 */
@Composable
fun ConnectionSnackbarEffect(
    snackbarHostState: SnackbarHostState
) {
    val connectionState = LocalServerConnection.current
    val isConnected = connectionState.isConnected

    // Track previous state to detect transitions
    var wasConnected by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(isConnected) {
        when {
            // Just went offline
            wasConnected && !isConnected -> {
                val result = snackbarHostState.showSnackbar(
                    message = "Server unreachable",
                    actionLabel = "Retry",
                    duration = SnackbarDuration.Long
                )
                if (result == SnackbarResult.ActionPerformed) {
                    connectionState.onRetry()
                }
            }
            // Just came back online
            !wasConnected && isConnected -> {
                snackbarHostState.showSnackbar(
                    message = "Connected",
                    duration = SnackbarDuration.Short
                )
            }
        }
        wasConnected = isConnected
    }
}
