package ai.thepredict.app.platform

internal actual fun isDebugBuild(): Boolean {
    // Check if running in development mode via system property
    return System.getProperty("app.debug", "false").toBoolean() ||
           System.getProperty("java.class.path").contains("build")
}
