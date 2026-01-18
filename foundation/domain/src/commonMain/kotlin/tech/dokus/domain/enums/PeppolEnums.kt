package tech.dokus.domain.enums

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import tech.dokus.domain.database.DbEnum

/**
 * Status of a PEPPOL directory lookup.
 * Stored in peppol_directory_cache table.
 */
@Serializable
enum class PeppolLookupStatus(override val dbValue: String) : DbEnum {
    @SerialName("found")
    Found("found"),

    @SerialName("not_found")
    NotFound("not_found"),

    @SerialName("error")
    Error("error")
}

/**
 * Source of a PEPPOL directory cache entry.
 */
@Serializable
enum class PeppolLookupSource(override val dbValue: String) : DbEnum {
    @SerialName("directory")
    Directory("directory"),

    @SerialName("manual")
    Manual("manual")
}

/**
 * Status of a tenant's PEPPOL registration.
 * Used by the PEPPOL registration state machine.
 */
@Serializable
enum class PeppolRegistrationStatus(override val dbValue: String) : DbEnum {
    /** No PEPPOL setup attempted */
    @SerialName("not_configured")
    NotConfigured("not_configured"),

    /** Registration submitted, awaiting activation */
    @SerialName("pending")
    Pending("pending"),

    /** Fully active (can receive and send) */
    @SerialName("active")
    Active("active"),

    /** ID registered elsewhere, waiting for transfer */
    @SerialName("waiting_transfer")
    WaitingTransfer("waiting_transfer"),

    /** Can send but cannot receive (blocked by another provider) */
    @SerialName("sending_only")
    SendingOnly("sending_only"),

    /** User opted to manage PEPPOL elsewhere */
    @SerialName("external")
    External("external"),

    /** Registration failed with error */
    @SerialName("failed")
    Failed("failed")
}
