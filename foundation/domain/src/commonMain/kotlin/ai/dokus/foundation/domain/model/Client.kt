package ai.dokus.foundation.domain.model

import ai.dokus.foundation.domain.ids.ClientId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


/**
 * Client statistics for dashboard
 */
@Serializable
data class ClientStats(
    val totalClients: Long,
    val activeClients: Long,
    val inactiveClients: Long,
    val peppolEnabledClients: Long
)

/**
 * Real-time client events for reactive UI updates
 */
@Serializable
sealed class ClientEvent {
    @Serializable
    @SerialName("ClientEvent.ClientCreated")
    data class ClientCreated(val client: Client) : ClientEvent()

    @Serializable
    @SerialName("ClientEvent.ClientUpdated")
    data class ClientUpdated(val client: Client) : ClientEvent()

    @Serializable
    @SerialName("ClientEvent.ClientDeleted")
    data class ClientDeleted(val clientId: ClientId) : ClientEvent()
}