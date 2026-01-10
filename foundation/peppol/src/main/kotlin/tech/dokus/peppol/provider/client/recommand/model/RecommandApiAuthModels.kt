package tech.dokus.peppol.provider.client.recommand.model

import kotlinx.serialization.Serializable

/**
 * Response for verifying authentication.
 *
 * Returned by:
 * - `GET /api/core/auth/verify`
 */
@Serializable
data class RecommandVerifyAuthResponse(
    val success: Boolean,
)

