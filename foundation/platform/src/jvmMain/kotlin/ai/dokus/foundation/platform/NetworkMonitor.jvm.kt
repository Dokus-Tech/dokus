package ai.dokus.foundation.platform

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.InetAddress

/**
 * Desktop/JVM implementation of NetworkMonitor using InetAddress reachability.
 * Polls connectivity periodically since JVM doesn't have native network callbacks.
 */
actual class NetworkMonitor {
    private val _isOnline = MutableStateFlow(checkConnectivity())
    actual val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO)
    private var monitorJob: Job? = null

    actual fun startMonitoring() {
        if (monitorJob?.isActive == true) return

        monitorJob = scope.launch {
            while (isActive) {
                _isOnline.value = checkConnectivity()
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    actual fun stopMonitoring() {
        monitorJob?.cancel()
        monitorJob = null
    }

    private fun checkConnectivity(): Boolean {
        return try {
            // Try to reach common DNS servers
            val addresses = listOf("8.8.8.8", "1.1.1.1", "208.67.222.222")
            addresses.any { address ->
                try {
                    InetAddress.getByName(address).isReachable(TIMEOUT_MS)
                } catch (e: Exception) {
                    false
                }
            }
        } catch (e: Exception) {
            false
        }
    }

    companion object {
        private const val POLL_INTERVAL_MS = 10_000L // 10 seconds
        private const val TIMEOUT_MS = 3_000 // 3 seconds
    }
}
