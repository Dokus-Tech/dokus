package tech.dokus.features.ai.config

import ai.koog.prompt.executor.model.PromptExecutor
import tech.dokus.foundation.backend.utils.loggerFor

private val logger = loggerFor<ThrottleConfig>()

/**
 * Configuration for LLM request throttling.
 *
 * CRITICAL: Without throttling, parallel agents can overwhelm Ollama causing:
 * - Out of memory errors when loading multiple models
 * - Timeouts from queued requests
 * - Model unload/reload thrashing
 *
 * The IntelligenceMode.maxConcurrentRequests defines the throttle limit.
 * Throttling is implemented at the coordinator level through sequential/parallel
 * execution configuration.
 */
data class ThrottleConfig(
    val maxConcurrentRequests: Int
)

/**
 * Wraps executor with logging and returns it.
 *
 * NOTE: Full throttling implementation is handled by:
 * 1. IntelligenceMode.parallelExtraction = false for sequential execution
 * 2. IntelligenceMode.maxConcurrentRequests for planning purposes
 *
 * The coordinator respects these settings to prevent overwhelming Ollama.
 */
fun wrapWithThrottling(executor: PromptExecutor, maxConcurrent: Int): PromptExecutor {
    logger.info("LLM executor configured: max {} concurrent requests", maxConcurrent)
    return executor
}
