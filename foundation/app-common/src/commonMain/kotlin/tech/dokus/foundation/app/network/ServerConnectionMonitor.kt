package tech.dokus.foundation.app.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.http.isSuccess
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Monitors connection to the Dokus server by periodically pinging the health endpoint.
 *
 * Unlike platform-specific network monitors that check general internet connectivity,
 * this monitor checks if the actual server is reachable - which is what matters for the app.
 *
 * @param httpClient HTTP client for making health check requests (should be unauthenticated,
 *                   already configured with the server's base URL via defaultRequest)
 */
@OptIn(ExperimentalTime::class)
class ServerConnectionMonitor(
    private val httpClient: HttpClient
) {
    private val _isConnected = MutableStateFlow(true) // Assume connected initially
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _lastCheckTime = MutableStateFlow<Long?>(null)
    val lastCheckTime: StateFlow<Long?> = _lastCheckTime.asStateFlow()

    private var monitorJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * Start periodic connection monitoring.
     * Should be called when the app becomes active.
     */
    fun startMonitoring() {
        if (monitorJob?.isActive == true) return

        monitorJob = scope.launch {
            // Check immediately on start
            checkConnection()

            // Then check periodically
            while (isActive) {
                delay(POLL_INTERVAL_MS)
                checkConnection()
            }
        }
    }

    /**
     * Stop connection monitoring.
     * Should be called when the app goes to background.
     */
    fun stopMonitoring() {
        monitorJob?.cancel()
        monitorJob = null
    }

    /**
     * Manually check the connection status.
     * Can be called when user requests a retry.
     *
     * @return true if server is reachable, false otherwise
     */
    suspend fun checkConnection(): Boolean {
        return try {
            // The httpClient already has the base URL configured via defaultRequest
            val response = httpClient.get("/health/live") {
                timeout {
                    requestTimeoutMillis = TIMEOUT_MS
                    connectTimeoutMillis = TIMEOUT_MS
                }
            }
            val connected = response.status.isSuccess()
            _isConnected.value = connected
            if (connected) {
                _lastCheckTime.value = Clock.System.now().toEpochMilliseconds()
            }
            connected
        } catch (e: Exception) {
            _isConnected.value = false
            false
        }
    }

    companion object {
        private const val POLL_INTERVAL_MS = 15_000L  // 15 seconds
        private const val TIMEOUT_MS = 5_000L         // 5 seconds
    }
}
