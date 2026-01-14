package tech.dokus.features.ai.config

import ai.koog.prompt.dsl.Prompt
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.message.Message
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
 *
 * Uses delegation pattern - delegates all PromptExecutor methods to the underlying
 * executor, but wraps the execute() method with semaphore-based throttling.
 */
class ThrottledPromptExecutor(
    private val delegate: PromptExecutor,
    maxConcurrent: Int
) : PromptExecutor by delegate {

    private val semaphore = Semaphore(maxConcurrent)
    private val logger = loggerFor()

    override suspend fun execute(
        prompt: Prompt,
        model: LLModel,
        tools: List<ai.koog.prompt.tools.ToolDescriptor>
    ): List<Message.Response> = semaphore.withPermit {
        logger.debug("Acquired semaphore permit, executing prompt (model: {})", model.id)
        delegate.execute(prompt, model, tools)
    }
}
