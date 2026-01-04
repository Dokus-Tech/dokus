package tech.dokus.peppol.provider

/**
 * Base interface for provider-specific credentials.
 *
 * Each provider implementation (Recommand, Storecove, etc.) will have
 * its own credentials class that implements this interface.
 */
interface PeppolCredentials {
    /** The provider this credential is for */
    val providerId: String

    /** The tenant's Peppol participant ID */
    val peppolId: String

    /** Whether to use test mode */
    val testMode: Boolean
}
