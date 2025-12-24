package ai.dokus.foundation.platform

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.Network.nw_path_get_status
import platform.Network.nw_path_monitor_cancel
import platform.Network.nw_path_monitor_create
import platform.Network.nw_path_monitor_set_queue
import platform.Network.nw_path_monitor_set_update_handler
import platform.Network.nw_path_monitor_start
import platform.Network.nw_path_monitor_t
import platform.Network.nw_path_status_satisfied
import platform.darwin.dispatch_get_main_queue

/**
 * iOS implementation of NetworkMonitor using NWPathMonitor.
 */
actual class NetworkMonitor {
    private val _isOnline = MutableStateFlow(true) // Assume online initially
    actual val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private var pathMonitor: nw_path_monitor_t? = null
    private var isMonitoring = false

    actual fun startMonitoring() {
        if (isMonitoring) return
        isMonitoring = true

        pathMonitor = nw_path_monitor_create()
        pathMonitor?.let { monitor ->
            nw_path_monitor_set_queue(monitor, dispatch_get_main_queue())
            nw_path_monitor_set_update_handler(monitor) { path ->
                val status = nw_path_get_status(path)
                _isOnline.value = status == nw_path_status_satisfied
            }
            nw_path_monitor_start(monitor)
        }
    }

    actual fun stopMonitoring() {
        if (!isMonitoring) return
        isMonitoring = false

        pathMonitor?.let { monitor ->
            nw_path_monitor_cancel(monitor)
        }
        pathMonitor = null
    }
}
