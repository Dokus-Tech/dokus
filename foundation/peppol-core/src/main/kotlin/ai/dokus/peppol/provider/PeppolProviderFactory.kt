package ai.dokus.peppol.provider

import ai.dokus.peppol.config.PeppolModuleConfig
import ai.dokus.peppol.providers.recommand.RecommandProvider
import io.ktor.client.HttpClient
import org.slf4j.LoggerFactory

/**
 * Factory for creating Peppol provider instances.
 *
 * Manages provider registration and instantiation based on tenant configuration.
 */
class PeppolProviderFactory(
    private val httpClient: HttpClient,
    private val config: PeppolModuleConfig
) {
    private val logger = LoggerFactory.getLogger(PeppolProviderFactory::class.java)
    private val providerFactories = mutableMapOf<String, () -> PeppolProvider>()

    init {
        // Register default providers
        registerProvider("recommand") {
            RecommandProvider(httpClient, config.recommand.baseUrl)
        }
    }

    /**
     * Register a provider factory.
     *
     * @param providerId Unique identifier for the provider
     * @param factory Lambda that creates a new provider instance
     */
    fun registerProvider(providerId: String, factory: () -> PeppolProvider) {
        logger.info("Registering Peppol provider: $providerId")
        providerFactories[providerId] = factory
    }

    /**
     * Create and configure a provider instance.
     *
     * @param credentials Provider-specific credentials
     * @return Configured provider instance
     * @throws IllegalArgumentException if provider is not registered
     */
    fun createProvider(credentials: PeppolCredentials): PeppolProvider {
        val factory = providerFactories[credentials.providerId]
            ?: throw IllegalArgumentException("Unknown Peppol provider: ${credentials.providerId}. " +
                "Available providers: ${getAvailableProviders()}")

        return factory().apply {
            configure(credentials)
            logger.debug("Created ${providerName} provider for Peppol ID: ${credentials.peppolId}")
        }
    }

    /**
     * Get list of available provider IDs.
     */
    fun getAvailableProviders(): List<String> = providerFactories.keys.toList()

    /**
     * Check if a provider is registered.
     */
    fun isProviderAvailable(providerId: String): Boolean = providerFactories.containsKey(providerId)
}
