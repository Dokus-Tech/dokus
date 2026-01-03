package tech.dokus.peppol.provider.client

import kotlinx.serialization.Serializable
import tech.dokus.peppol.provider.PeppolCredentials

/**
 * Recommand.eu specific credentials.
 */
@Serializable
data class RecommandCredentials(
    /** Recommand company ID from dashboard */
    val companyId: String,

    /** API key from Recommand dashboard */
    val apiKey: String,

    /** API secret from Recommand dashboard */
    val apiSecret: String,

    /** Tenant's Peppol participant ID (format: scheme:identifier) */
    override val peppolId: String,

    /** Whether to use test mode */
    override val testMode: Boolean = false
) : PeppolCredentials {
    override val providerId: String = "recommand"
}
