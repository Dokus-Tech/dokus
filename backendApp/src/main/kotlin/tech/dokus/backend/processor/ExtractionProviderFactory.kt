package tech.dokus.backend.processor

import tech.dokus.foundation.ktor.config.AIConfig
import tech.dokus.foundation.ktor.utils.loggerFor
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
                apiKey = config.openai.apiKey,
                model = config.models.documentExtraction.takeIf {
                    config.defaultProvider == AIConfig.AIProvider.OPENAI
                } ?: config.openai.defaultModel,
                baseUrl = "https://api.openai.com/v1"
            ),
            "ollama" to OllamaExtractionProvider(
                httpClient = httpClient,
                baseUrl = config.ollama.baseUrl,
                model = config.models.documentExtraction.takeIf {
                    config.defaultProvider == AIConfig.AIProvider.OLLAMA
                } ?: config.ollama.defaultModel
            ),
            // Alias for convenience
            "local" to OllamaExtractionProvider(
                httpClient = httpClient,
                baseUrl = config.ollama.baseUrl,
                model = config.models.documentExtraction.takeIf {
                    config.defaultProvider == AIConfig.AIProvider.OLLAMA
                } ?: config.ollama.defaultModel
            )
        )
    }

    /**
     * Get the default provider based on configuration.
     */
    fun getDefaultProvider(): AIExtractionProvider {
        val providerName = when (config.defaultProvider) {
            AIConfig.AIProvider.OLLAMA -> "ollama"
            AIConfig.AIProvider.OPENAI -> "openai"
        }
        return providers[providerName]
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
