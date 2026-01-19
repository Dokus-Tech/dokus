package tech.dokus.domain.model

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

/**
 * PEPPOL activity timestamps for a tenant.
 *
 * Provides last activity timestamps for inbound and outbound PEPPOL transmissions,
 * used to show activity status in the workspace settings.
 *
 * @property lastInboundAt Timestamp of last successful inbound transmission
 * @property lastOutboundAt Timestamp of last successful outbound transmission
 */
@Serializable
data class PeppolActivityDto(
    val lastInboundAt: LocalDateTime?,
    val lastOutboundAt: LocalDateTime?,
)
