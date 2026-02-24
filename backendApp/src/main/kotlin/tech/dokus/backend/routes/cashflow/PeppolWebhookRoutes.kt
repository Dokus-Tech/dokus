package tech.dokus.backend.routes.cashflow

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.inject
import tech.dokus.backend.worker.PeppolPollingWorker
import tech.dokus.database.repository.peppol.PeppolSettingsRepository
import tech.dokus.domain.utils.json
import tech.dokus.foundation.backend.utils.loggerFor
import tech.dokus.peppol.config.PeppolModuleConfig
import tech.dokus.peppol.service.PeppolService

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
    val peppolService by inject<PeppolService>()
    val peppolModuleConfig by inject<PeppolModuleConfig>()

    /**
     * POST /api/v1/peppol/webhook?token={tenantToken}
     *
     * Webhook endpoint for Peppol provider notifications.
     * Does NOT require JWT authentication - uses webhook token instead.
     *
     * Rate limit: 10 requests/minute per token (TODO: implement rate limiting)
     */
    post("/api/v1/peppol/webhook") {
        val rawBody = call.receiveText()
        val token = call.request.queryParameters["token"]

        if (token.isNullOrBlank()) {
            logger.warn("Webhook called without token")
            call.respond(HttpStatusCode.Unauthorized, WebhookResponse(success = false, message = "Missing token"))
            return@post
        }

        val payload = runCatching { json.decodeFromString<WebhookPayload>(rawBody) }.getOrNull()
        if (payload == null) {
            logger.warn("Webhook payload parse failed, ignoring event body")
            call.respond(HttpStatusCode.OK, WebhookResponse(success = true, message = "Notification ignored"))
            return@post
        }

        val companyId = payload.companyId?.trim()
        if (companyId.isNullOrBlank()) {
            logger.warn("Webhook payload missing companyId, ignoring")
            call.respond(HttpStatusCode.OK, WebhookResponse(success = true, message = "Notification ignored"))
            return@post
        }

        val settings = peppolSettingsRepository.getEnabledSettingsByCompanyId(companyId)
            .getOrElse {
                logger.error("Failed to resolve tenant by companyId {}", companyId, it)
                call.respond(
                    HttpStatusCode.InternalServerError,
                    WebhookResponse(success = false, message = "Internal error")
                )
                return@post
            }

        if (settings == null) {
            logger.warn("Webhook received for unknown/disabled companyId {}, ignored", companyId)
            call.respond(HttpStatusCode.OK, WebhookResponse(success = true, message = "Notification ignored"))
            return@post
        }

        val expectedToken = settings.webhookToken?.trim()
        if (expectedToken.isNullOrBlank() || token != expectedToken) {
            logger.warn(
                "Webhook token mismatch for tenant {} companyId {}",
                settings.tenantId,
                companyId
            )
            call.respond(HttpStatusCode.Unauthorized, WebhookResponse(success = false, message = "Invalid token"))
            return@post
        }

        payload.documentId?.takeIf { it.isNotBlank() }?.let { externalDocumentId ->
            peppolService.reconcileOutboundByExternalDocumentId(settings.tenantId, externalDocumentId)
                .onFailure { error ->
                    logger.warn(
                        "Failed outbound reconciliation on webhook for tenant {} and document {}",
                        settings.tenantId,
                        externalDocumentId,
                        error
                    )
                }
        }

        logger.info("Webhook received for tenant {}, event: {}", settings.tenantId, payload.eventType ?: "unknown")

        val debounceGranted = peppolSettingsRepository.tryAcquireWebhookPollSlot(
            tenantId = settings.tenantId,
            now = Clock.System.now().toLocalDateTime(TimeZone.UTC),
            debounceSeconds = peppolModuleConfig.webhook.pollDebounceSeconds
        ).getOrElse {
            logger.error("Failed webhook debounce check for tenant {}", settings.tenantId, it)
            call.respond(HttpStatusCode.InternalServerError, WebhookResponse(success = false, message = "Internal error"))
            return@post
        }

        if (!debounceGranted) {
            logger.debug(
                "Webhook poll debounced for tenant {} companyId {}",
                settings.tenantId,
                companyId
            )
        } else {
            val pollTriggered = peppolPollingWorker.pollNow(settings.tenantId) // inbound safety net
            if (pollTriggered) {
                logger.info("Poll triggered for tenant {}", settings.tenantId)
            } else {
                logger.debug("Poll skipped for tenant {} (in-memory guard)", settings.tenantId)
            }
        }

        call.respond(HttpStatusCode.OK, WebhookResponse(success = true, message = "Notification received"))
    }
}

@Serializable
private data class WebhookPayload(
    val eventType: String? = null,
    val documentId: String? = null,
    val companyId: String? = null,
    val teamId: String? = null,
    val timestamp: String? = null
)

@Serializable
private data class WebhookResponse(
    val success: Boolean,
    val message: String
)
