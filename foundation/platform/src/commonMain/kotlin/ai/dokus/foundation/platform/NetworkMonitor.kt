package ai.dokus.foundation.platform

import kotlinx.coroutines.flow.StateFlow

/**
 * Platform-specific network connectivity monitor.
 *
 * Provides real-time network state for offline detection and UI indicators.
 * Each platform implements this using native APIs:
 * - Android: ConnectivityManager.NetworkCallback
 * - iOS: NWPathMonitor
 * - Desktop: Periodic InetAddress reachability check
 * - WASM: navigator.onLine and online/offline events
 */
expect class NetworkMonitor {
    /**
     * Observable network connectivity state.
     * Emits true when network is available, false when offline.
     */
    val isOnline: StateFlow<Boolean>

    /**
     * Start monitoring network connectivity.
     * Should be called when the app becomes active.
     */
    fun startMonitoring()

    /**
     * Stop monitoring network connectivity.
     * Should be called when the app goes to background.
     */
    fun stopMonitoring()
}
