package ai.dokus.foundation.domain.model

import ai.dokus.foundation.domain.ids.ClientId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request DTO for creating a client
 */
@Serializable
data class CreateClientRequest(
    val name: String,
    val email: String? = null,
    val phone: String? = null,
    val vatNumber: String? = null,
    val addressLine1: String? = null,
    val addressLine2: String? = null,
    val city: String? = null,
    val postalCode: String? = null,
    val country: String? = null,
    val contactPerson: String? = null,
    val notes: String? = null
)

/**
 * Request DTO for updating a client
 */
@Serializable
data class UpdateClientRequest(
    val name: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val vatNumber: String? = null,
    val addressLine1: String? = null,
    val addressLine2: String? = null,
    val city: String? = null,
    val postalCode: String? = null,
    val country: String? = null,
    val contactPerson: String? = null,
    val notes: String? = null
)

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
    data class ClientCreated(val client: ClientDto) : ClientEvent()

    @Serializable
    @SerialName("ClientEvent.ClientUpdated")
    data class ClientUpdated(val client: ClientDto) : ClientEvent()

    @Serializable
    @SerialName("ClientEvent.ClientDeleted")
    data class ClientDeleted(val clientId: ClientId) : ClientEvent()
}