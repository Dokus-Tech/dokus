package tech.dokus.peppol.provider.client.recommand.model

import kotlinx.serialization.Serializable

/**
 * Webhook configuration as returned by the Recommand API.
 *
 * Returned by:
 * - `GET /api/v1/webhooks` (query params: `companyId`)
 * - `POST /api/v1/webhooks`
 * - `GET /api/v1/webhooks/{webhookId}` (path params: `webhookId`)
 * - `PUT /api/v1/webhooks/{webhookId}` (path params: `webhookId`)
 */
@Serializable
data class RecommandWebhook(
    val id: String,
    val teamId: String,
    val companyId: String? = null,
    val url: String,
    val createdAt: String,
    val updatedAt: String,
)

/**
 * Query parameters for `GET /api/v1/webhooks`.
 */
@Serializable
data class RecommandGetWebhooksRequest(
    val companyId: String? = null,
)

/**
 * Wrapper response for `GET /api/v1/webhooks`.
 */
@Serializable
data class RecommandGetWebhooksResponse(
    val success: Boolean,
    val webhooks: List<RecommandWebhook>,
)

/**
 * Request body for `POST /api/v1/webhooks`.
 */
@Serializable
data class RecommandCreateWebhookRequest(
    val url: String,
    val companyId: String? = null,
)

/**
 * Wrapper response for `POST /api/v1/webhooks`.
 */
@Serializable
data class RecommandCreateWebhookResponse(
    val success: Boolean,
    val webhook: RecommandWebhook,
)

/**
 * Path parameters for `GET /api/v1/webhooks/{webhookId}`.
 */
@Serializable
data class RecommandGetWebhookRequest(
    val webhookId: String,
)

/**
 * Wrapper response for `GET /api/v1/webhooks/{webhookId}`.
 */
@Serializable
data class RecommandGetWebhookResponse(
    val success: Boolean,
    val webhook: RecommandWebhook,
)

/**
 * Request body for `PUT /api/v1/webhooks/{webhookId}`.
 */
@Serializable
data class RecommandUpdateWebhookRequest(
    val url: String,
    val companyId: String? = null,
)

/**
 * Path parameters for `PUT /api/v1/webhooks/{webhookId}`.
 */
@Serializable
data class RecommandUpdateWebhookPath(
    val webhookId: String,
)

/**
 * Wrapper response for `PUT /api/v1/webhooks/{webhookId}`.
 */
@Serializable
data class RecommandUpdateWebhookResponse(
    val success: Boolean,
    val webhook: RecommandWebhook,
)

/**
 * Path parameters for `DELETE /api/v1/webhooks/{webhookId}`.
 */
@Serializable
data class RecommandDeleteWebhookRequest(
    val webhookId: String,
)

/**
 * Wrapper response for `DELETE /api/v1/webhooks/{webhookId}`.
 */
@Serializable
data class RecommandDeleteWebhookResponse(
    val success: Boolean,
)
