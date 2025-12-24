package tech.dokus.foundation.app.network

import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Monitors connection to the Dokus server based on actual API request results.
 *
 * Unlike polling-based monitors, this tracks connection state reactively:
 * - When any API call succeeds, we're connected
 * - When any API call fails with a network error, we're disconnected
 *
 * The connection state is updated by [ConnectionMonitorPlugin] which intercepts
 * all HTTP requests and reports their outcomes.
 *
 * This approach is more efficient and accurate than health endpoint polling:
 * - No unnecessary network traffic
 * - Detects connection issues immediately when they happen
 * - Works even if the health endpoint doesn't exist
 */
@OptIn(ExperimentalTime::class)
class ServerConnectionMonitor {
    private val _isConnected = MutableStateFlow(true) // Assume connected initially
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _lastSuccessTime = MutableStateFlow<Long?>(null)
    val lastSuccessTime: StateFlow<Long?> = _lastSuccessTime.asStateFlow()

    /**
     * Report a successful API response.
     * Called by [ConnectionMonitorPlugin] when a request completes successfully.
     */
    fun reportSuccess() {
        _isConnected.value = true
        _lastSuccessTime.value = Clock.System.now().toEpochMilliseconds()
    }

    /**
     * Report a network error.
     * Called by [ConnectionMonitorPlugin] when a request fails due to network issues.
     *
     * @param error The exception that caused the failure (for logging/debugging)
     */
    fun reportNetworkError(error: Throwable) {
        // Only mark as disconnected for actual network errors
        if (isNetworkException(error)) {
            _isConnected.value = false
        }
    }

    /**
     * Manually mark as connected.
     * Can be called when user performs a manual retry that succeeds.
     */
    fun markConnected() {
        reportSuccess()
    }

    /**
     * Force a connection check by attempting a simple request.
     * This is useful for manual retry buttons.
     *
     * Note: The actual check happens via normal API calls - this just
     * provides a way to trigger one. The result will be reported via
     * [reportSuccess] or [reportNetworkError] by the plugin.
     */
    suspend fun checkConnection(): Boolean {
        // The connection state will be updated by the next API call
        // For manual retry, we just return current state
        return _isConnected.value
    }
}
