package tech.dokus.peppol.config

/**
 * Sealed class containing static configuration for Peppol providers.
 *
 * Provider URLs are fixed and don't change between environments.
 * Only API credentials differ (test vs production keys).
 */
sealed class PeppolProviderConfig(
    val providerId: String,
    val providerName: String,
    val baseUrl: String
) {
    /**
     * Recommand.eu provider configuration.
     * API Reference: https://peppol.recommand.eu/api-reference
     */
    data object Recommand : PeppolProviderConfig(
        providerId = "recommand",
        providerName = "Recommand.eu",
        baseUrl = "https://peppol.recommand.eu"
    )
}
