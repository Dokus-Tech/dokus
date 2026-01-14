package tech.dokus.features.ai.config

import ai.koog.prompt.dsl.Prompt
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.message.Message
import ai.koog.prompt.tools.Tool
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import tech.dokus.foundation.backend.utils.loggerFor

/**
 * Wraps a PromptExecutor with semaphore-based concurrency control.
 *
 * CRITICAL: Without throttling, parallel agents can overwhelm Ollama causing:
 * - Out of memory errors when loading multiple models
 * - Timeouts from queued requests
 * - Model unload/reload thrashing
 *
 * This wrapper ensures only N concurrent requests are made at once,
 * where N is determined by [IntelligenceMode.maxConcurrentRequests].
 */
class ThrottledPromptExecutor(
    private val delegate: PromptExecutor,
    maxConcurrent: Int
) : PromptExecutor {

    private val semaphore = Semaphore(maxConcurrent)
    private val logger = loggerFor()

    override suspend fun execute(
        prompt: Prompt<*>,
        model: LLModel,
        tools: List<Tool<*, *>>
    ): List<Message.Response> = semaphore.withPermit {
        logger.debug("Acquired semaphore permit, executing prompt (model: {})", model.id)
        delegate.execute(prompt, model, tools)
    }

    override fun executeStreaming(
        prompt: Prompt<*>,
        model: LLModel,
        tools: List<Tool<*, *>>
    ): Flow<String> {
        // Note: Streaming doesn't use semaphore to avoid blocking the flow
        // This is intentional - streaming is typically used for user-facing chat
        // where latency matters more than throughput
        return delegate.executeStreaming(prompt, model, tools)
    }
}
