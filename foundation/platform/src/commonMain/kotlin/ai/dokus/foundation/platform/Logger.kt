package ai.dokus.foundation.platform

import co.touchlab.kermit.Logger as KermitLogger
import co.touchlab.kermit.Severity
import co.touchlab.kermit.StaticConfig
import co.touchlab.kermit.platformLogWriter

/**
 * Multiplatform logger wrapper around Kermit
 *
 * Usage:
 * ```
 * logger.d { "Debug message" }
 * logger.i { "Info message" }
 * logger.w { "Warning message" }
 * logger.e(throwable) { "Error message" }
 * ```
 */
class Logger(private val tag: String) {

    private val kermit = KermitLogger(
        config = StaticConfig(
            minSeverity = if (BuildConfig.DEBUG) Severity.Verbose else Severity.Info,
            logWriterList = listOf(platformLogWriter())
        ),
        tag = tag
    )

    /**
     * Log verbose message
     */
    fun v(throwable: Throwable? = null, message: () -> String) {
        kermit.v(throwable, tag, message)
    }

    /**
     * Log debug message
     */
    fun d(throwable: Throwable? = null, message: () -> String) {
        kermit.d(throwable, tag, message)
    }

    /**
     * Log info message
     */
    fun i(throwable: Throwable? = null, message: () -> String) {
        kermit.i(throwable, tag, message)
    }

    /**
     * Log warning message
     */
    fun w(throwable: Throwable? = null, message: () -> String) {
        kermit.w(throwable, tag, message)
    }

    /**
     * Log error message
     */
    fun e(throwable: Throwable? = null, message: () -> String) {
        kermit.e(throwable, tag, message)
    }

    /**
     * Log assert/wtf message
     */
    fun a(throwable: Throwable? = null, message: () -> String) {
        kermit.a(throwable, tag, message)
    }

    companion object {
        /**
         * Create a logger with the given tag
         */
        fun withTag(tag: String): Logger = Logger(tag)

        /**
         * Create a logger with the class name as tag
         */
        inline fun <reified T> forClass(): Logger = Logger(T::class.simpleName ?: "Unknown")
    }
}
