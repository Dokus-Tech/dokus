package tech.dokus.backend.processor

import ai.dokus.foundation.ktor.utils.loggerFor
import io.ktor.client.HttpClient

/**
 * Factory for creating AI extraction providers.
 * Manages provider selection based on configuration and availability.
 */
class ExtractionProviderFactory(
    private val httpClient: HttpClient,
    private val config: AIConfig
) {
    private val logger = loggerFor()

    private val providers: Map<String, AIExtractionProvider> by lazy {
        mapOf(
            "openai" to OpenAIExtractionProvider(
                httpClient = httpClient,
                apiKey = config.openaiApiKey,
                model = config.openaiModel,
                baseUrl = config.openaiBaseUrl
            ),
            "ollama" to OllamaExtractionProvider(
                httpClient = httpClient,
                baseUrl = config.localBaseUrl,
                model = config.localModel
            ),
            // Alias for convenience
            "local" to OllamaExtractionProvider(
                httpClient = httpClient,
                baseUrl = config.localBaseUrl,
                model = config.localModel
            )
        )
    }

    /**
     * Get the default provider based on configuration.
     */
    fun getDefaultProvider(): AIExtractionProvider {
        return providers[config.defaultProvider]
            ?: providers["openai"]
            ?: throw IllegalStateException("No AI providers available")
    }

    /**
     * Get a specific provider by name.
     */
    fun getProvider(name: String): AIExtractionProvider? {
        return providers[name]
    }

    /**
     * Get the first available provider, preferring the default.
     */
    suspend fun getFirstAvailableProvider(): AIExtractionProvider? {
        // Try default first
        val defaultProvider = getDefaultProvider()
        if (defaultProvider.isAvailable()) {
            logger.info("Using provider: ${defaultProvider.name}")
            return defaultProvider
        }

        // Try others
        for ((name, provider) in providers) {
            if (provider.isAvailable()) {
                logger.info("Using fallback provider: ${provider.name}")
                return provider
            }
        }

        logger.warn("No AI extraction provider available")
        return null
    }

    /**
     * Get all available providers.
     */
    suspend fun getAvailableProviders(): List<AIExtractionProvider> {
        return providers.values.filter { it.isAvailable() }
    }
}

/**
 * Configuration for AI extraction providers.
 */
data class AIConfig(
    val defaultProvider: String = "openai",
    val openaiApiKey: String = "",
    val openaiModel: String = "gpt-4o",
    val openaiBaseUrl: String = "https://api.openai.com/v1",
    val anthropicApiKey: String = "",
    val anthropicModel: String = "claude-sonnet-4-20250514",
    val anthropicBaseUrl: String = "https://api.anthropic.com",
    val localBaseUrl: String = "http://localhost:11434",
    val localModel: String = "llama3.2"
)
