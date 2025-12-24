package ai.dokus.foundation.platform

import kotlinx.browser.window
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.w3c.dom.events.Event

/**
 * WASM/Web implementation of NetworkMonitor using navigator.onLine.
 */
actual class NetworkMonitor {
    private val _isOnline = MutableStateFlow(window.navigator.onLine)
    actual val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private var isMonitoring = false

    private val onlineHandler: (Event) -> Unit = {
        _isOnline.value = true
    }

    private val offlineHandler: (Event) -> Unit = {
        _isOnline.value = false
    }

    actual fun startMonitoring() {
        if (isMonitoring) return
        isMonitoring = true

        _isOnline.value = window.navigator.onLine
        window.addEventListener("online", onlineHandler)
        window.addEventListener("offline", offlineHandler)
    }

    actual fun stopMonitoring() {
        if (!isMonitoring) return
        isMonitoring = false

        window.removeEventListener("online", onlineHandler)
        window.removeEventListener("offline", offlineHandler)
    }
}
