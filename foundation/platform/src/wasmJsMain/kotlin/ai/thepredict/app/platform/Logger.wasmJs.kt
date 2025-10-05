package ai.thepredict.app.platform

internal actual fun isDebugBuild(): Boolean {
    // For WASM/JS, always enable debug logging during development
    // In production, this should be controlled by build configuration
    return true
}
