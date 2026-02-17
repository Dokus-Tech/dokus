package tech.dokus.peppol.provider.client.recommand.model

import kotlinx.serialization.Serializable

/**
 * Playground team description as returned by the Recommand API.
 *
 * Returned by:
 * - `GET /api/v1/playgrounds/current`
 * - `POST /api/v1/playgrounds`
 */
@Serializable
data class RecommandPlayground(
    val id: String,
    val name: String,
    val teamDescription: String? = null,
    val isPlayground: Boolean,
    val useTestNetwork: Boolean,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

/**
 * Wrapper response for `GET /api/v1/playgrounds/current`.
 */
@Serializable
data class RecommandGetPlaygroundResponse(
    val success: Boolean,
    val playground: RecommandPlayground? = null,
)

/**
 * Request body for `POST /api/v1/playgrounds`.
 */
@Serializable
data class RecommandCreatePlaygroundRequest(
    val name: String,
    val useTestNetwork: Boolean = false,
)

/**
 * Wrapper response for `POST /api/v1/playgrounds`.
 */
@Serializable
data class RecommandCreatePlaygroundResponse(
    val success: Boolean,
    val playground: RecommandPlayground? = null,
)
