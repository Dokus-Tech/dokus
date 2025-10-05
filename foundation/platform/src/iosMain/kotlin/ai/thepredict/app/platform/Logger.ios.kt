package ai.thepredict.app.platform

import platform.Foundation.NSProcessInfo

internal actual fun isDebugBuild(): Boolean {
    // In iOS, we check if DEBUG is defined via environment
    // For production builds, this will return false
    val environment = NSProcessInfo.processInfo.environment
    return environment["DEBUG"] != null || environment["SIMULATOR_DEVICE_NAME"] != null
}
