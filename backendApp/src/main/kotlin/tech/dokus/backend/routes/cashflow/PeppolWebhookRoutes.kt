package tech.dokus.backend.routes.cashflow

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.inject
import tech.dokus.backend.worker.PeppolPollingWorker
import tech.dokus.database.repository.peppol.PeppolSettingsRepository
import tech.dokus.foundation.backend.utils.loggerFor

private val logger = loggerFor("PeppolWebhook")

/**
 * Peppol Webhook Routes - NOT authenticated.
 *
 * This endpoint receives notifications from the Peppol provider (Recommand)
 * when new documents arrive. It uses a per-tenant webhook token for authentication.
 *
 * The webhook simply triggers an immediate poll - all document processing
 * goes through the standard pollInbox() flow.
 */
internal fun Route.peppolWebhookRoutes() {
    val peppolSettingsRepository by inject<PeppolSettingsRepository>()
    val peppolPollingWorker by inject<PeppolPollingWorker>()

    /**
     * POST /api/v1/peppol/webhook?token={tenantToken}
     *
     * Webhook endpoint for Peppol provider notifications.
     * Does NOT require JWT authentication - uses webhook token instead.
     *
     * Rate limit: 10 requests/minute per token (TODO: implement rate limiting)
     */
    post("/api/v1/peppol/webhook") {
        val token = call.request.queryParameters["token"]

        if (token.isNullOrBlank()) {
            logger.warn("Webhook called without token")
            call.respond(HttpStatusCode.Unauthorized, WebhookResponse(success = false, message = "Missing token"))
            return@post
        }

        // Look up tenant by webhook token
        val tenantId = peppolSettingsRepository.getTenantIdByWebhookToken(token)
            .getOrElse {
                logger.error("Failed to look up tenant by webhook token", it)
                call.respond(
                    HttpStatusCode.InternalServerError,
                    WebhookResponse(success = false, message = "Internal error")
                )
                return@post
            }

        if (tenantId == null) {
            logger.warn("Webhook called with invalid token")
            call.respond(HttpStatusCode.Unauthorized, WebhookResponse(success = false, message = "Invalid token"))
            return@post
        }

        // Parse webhook payload (optional - we may not need it)
        val payload = runCatching { call.receive<WebhookPayload>() }.getOrNull()

        logger.info("Webhook received for tenant $tenantId, event: ${payload?.eventType ?: "unknown"}")

        // Trigger immediate poll for this tenant
        val pollTriggered = peppolPollingWorker.pollNow(tenantId)

        if (pollTriggered) {
            logger.info("Poll triggered for tenant $tenantId")
        } else {
            logger.debug("Poll skipped for tenant $tenantId (too recent)")
        }

        // Always return 200 OK to the provider
        call.respond(HttpStatusCode.OK, WebhookResponse(success = true, message = "Notification received"))
    }
}

@Serializable
private data class WebhookPayload(
    val eventType: String? = null,
    val documentId: String? = null,
    val timestamp: String? = null
)

@Serializable
private data class WebhookResponse(
    val success: Boolean,
    val message: String
)
