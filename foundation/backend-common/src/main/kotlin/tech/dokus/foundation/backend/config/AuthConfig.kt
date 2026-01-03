package tech.dokus.foundation.backend.config

import com.typesafe.config.Config

data class AuthConfig(
    val maxLoginAttempts: Int,
    val lockDurationMinutes: Int,
    val sessionDurationHours: Int,
    val rememberMeDurationDays: Int,
    val maxConcurrentSessions: Int,
    val password: PasswordConfig,
    val rateLimit: RateLimitConfig,
    val enableDeviceFingerprinting: Boolean,
    val enableSessionSlidingExpiration: Boolean,
    val sessionActivityWindowMinutes: Int,
    val logSecurityEvents: Boolean,
    val enableDebugMode: Boolean
) {
    data class PasswordConfig(
        val expiryDays: Int,
        val minLength: Int,
        val requireUppercase: Boolean,
        val requireLowercase: Boolean,
        val requireDigits: Boolean,
        val requireSpecialChars: Boolean,
        val historySize: Int
    )

    data class RateLimitConfig(
        val windowSeconds: Int,
        val maxAttempts: Int
    )

    companion object {
        fun fromConfig(config: Config): AuthConfig {
            val passwordConfig = config.getConfig("password")
            val rateLimitConfig = config.getConfig("rateLimit")

            return AuthConfig(
                maxLoginAttempts = config.getInt("maxLoginAttempts"),
                lockDurationMinutes = config.getInt("lockDurationMinutes"),
                sessionDurationHours = config.getInt("sessionDurationHours"),
                rememberMeDurationDays = config.getInt("rememberMeDurationDays"),
                maxConcurrentSessions = config.getInt("maxConcurrentSessions"),
                password = PasswordConfig(
                    expiryDays = passwordConfig.getInt("expiryDays"),
                    minLength = passwordConfig.getInt("minLength"),
                    requireUppercase = passwordConfig.getBoolean("requireUppercase"),
                    requireLowercase = passwordConfig.getBoolean("requireLowercase"),
                    requireDigits = passwordConfig.getBoolean("requireDigits"),
                    requireSpecialChars = passwordConfig.getBoolean("requireSpecialChars"),
                    historySize = passwordConfig.getInt("historySize")
                ),
                rateLimit = RateLimitConfig(
                    windowSeconds = rateLimitConfig.getInt("windowSeconds"),
                    maxAttempts = rateLimitConfig.getInt("maxAttempts")
                ),
                enableDeviceFingerprinting = config.getBoolean("enableDeviceFingerprinting"),
                enableSessionSlidingExpiration = config.getBoolean("enableSessionSlidingExpiration"),
                sessionActivityWindowMinutes = config.getInt("sessionActivityWindowMinutes"),
                logSecurityEvents = config.getBoolean("logSecurityEvents"),
                enableDebugMode = config.getBoolean("enableDebugMode")
            )
        }
    }
}
