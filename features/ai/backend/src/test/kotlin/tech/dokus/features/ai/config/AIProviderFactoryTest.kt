package tech.dokus.features.ai.config

import ai.koog.prompt.executor.clients.openai.OpenAIClientSettings
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.executor.clients.retry.RetryConfig
import ai.koog.prompt.executor.clients.retry.RetryingLLMClient
import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import tech.dokus.foundation.backend.config.AIConfig
import tech.dokus.foundation.backend.config.IntelligenceMode
import tech.dokus.foundation.backend.config.LangfuseConfig

class AIProviderFactoryTest {

    @Test
    fun `openai executor uses retrying client with explicit timeout config`() {
        val config = AIConfig(
            mode = IntelligenceMode.Sovereign,
            ollamaHost = "http://localhost:11434",
            lmStudioHost = "http://localhost:1234/v1",
            langfuse = LangfuseConfig(enabled = false, host = "", publicKey = "", secretKey = ""),
        )

        val executor = AIProviderFactory.createOpenAiExecutor(config)
        val promptExecutor = assertIs<SingleLLMPromptExecutor>(executor)

        val retryingClient = assertIs<RetryingLLMClient>(promptExecutor.readPrivateField("llmClient"))
        val retryConfig = assertIs<RetryConfig>(retryingClient.readPrivateField("config"))
        val openAiClient = assertIs<OpenAILLMClient>(retryingClient.readPrivateField("delegate"))
        val settings = assertIs<OpenAIClientSettings>(openAiClient.readPrivateField("settings"))

        assertEquals(config.lmStudioHost, settings.baseUrl)
        assertEquals(config.llmRequestTimeout.inWholeMilliseconds, settings.timeoutConfig.requestTimeoutMillis)
        assertEquals(config.llmConnectTimeout.inWholeMilliseconds, settings.timeoutConfig.connectTimeoutMillis)
        assertEquals(config.llmSocketTimeout.inWholeMilliseconds, settings.timeoutConfig.socketTimeoutMillis)
        assertEquals(config.llmRetryMaxAttempts, retryConfig.maxAttempts)
        assertEquals(config.llmRetryInitialDelay.inWholeMilliseconds, retryConfig.initialDelay.inWholeMilliseconds)
        assertEquals(config.llmRetryMaxDelay.inWholeMilliseconds, retryConfig.maxDelay.inWholeMilliseconds)
    }

    private fun Any.readPrivateField(name: String): Any? {
        val field = javaClass.getDeclaredField(name)
        field.isAccessible = true
        return field.get(this)
    }
}
