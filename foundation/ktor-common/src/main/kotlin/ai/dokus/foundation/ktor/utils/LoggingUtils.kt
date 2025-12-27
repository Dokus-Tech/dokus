package ai.dokus.foundation.ktor.utils

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Logger factory utilities.
 *
 * Usage:
 *   private val logger = loggerFor("MyClass")
 *   private val logger = loggerFor(MyClass::class.java)
 *   private val logger = loggerFor()  // uses enclosing class name
 *
 * Security-relevant logs should use [SECURITY] prefix:
 *   logger.info("[SECURITY] User logged in: userId={}", userId)
 *   logger.warn("[SECURITY] Access denied: userId={}", userId)
 */
fun loggerFor(clazz: Class<*>): Logger = LoggerFactory.getLogger(clazz)
fun loggerFor(name: String): Logger = LoggerFactory.getLogger(name)
inline fun <reified T> T.loggerFor(): Logger = loggerFor(T::class.java)
inline fun <reified T> loggerFor(): Logger = loggerFor(T::class.java)
