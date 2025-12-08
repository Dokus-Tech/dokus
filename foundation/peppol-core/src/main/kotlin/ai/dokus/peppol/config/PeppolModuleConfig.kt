package ai.dokus.peppol.config

/**
 * Configuration for the Peppol module.
 *
 * Loaded from environment variables since peppol-core is a library module.
 */
data class PeppolModuleConfig(
    /** Default provider ID (e.g., "recommand") */
    val defaultProvider: String = "recommand",

    /** Whether inbox polling is enabled */
    val pollingEnabled: Boolean = true,

    /** Polling interval in minutes */
    val pollingIntervalMinutes: Int = 10,

    /** Recommand API base URL */
    val recommandBaseUrl: String = "https://app.recommand.eu",

    /** Global test mode override (if true, all tenants use test mode) */
    val globalTestMode: Boolean = false
) {
    companion object {
        /**
         * Create config from environment variables.
         */
        fun fromEnvironment(): PeppolModuleConfig {
            return PeppolModuleConfig(
                defaultProvider = System.getenv("PEPPOL_DEFAULT_PROVIDER") ?: "recommand",
                pollingEnabled = System.getenv("PEPPOL_INBOX_POLL_ENABLED")?.toBooleanStrictOrNull() ?: true,
                pollingIntervalMinutes = System.getenv("PEPPOL_INBOX_POLL_INTERVAL_MINUTES")?.toIntOrNull() ?: 10,
                recommandBaseUrl = System.getenv("RECOMMAND_BASE_URL") ?: "https://app.recommand.eu",
                globalTestMode = System.getenv("PEPPOL_TEST_MODE")?.toBooleanStrictOrNull() ?: false
            )
        }
    }
}
