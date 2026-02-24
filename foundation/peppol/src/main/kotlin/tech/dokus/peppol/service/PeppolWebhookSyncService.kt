package tech.dokus.peppol.service

import tech.dokus.database.repository.peppol.PeppolSettingsRepository
import tech.dokus.domain.model.PeppolSettingsDto
import tech.dokus.foundation.backend.utils.loggerFor
import tech.dokus.peppol.config.PeppolModuleConfig
import tech.dokus.peppol.provider.client.RecommandWebhooksClient
import tech.dokus.peppol.provider.client.recommand.model.RecommandWebhook
import java.time.Instant

data class PeppolWebhookSyncSummary(
    val tenantsProcessed: Int,
    val created: Int,
    val updated: Int,
    val deleted: Int,
    val failures: Int
)

class PeppolWebhookSyncService(
    private val settingsRepository: PeppolSettingsRepository,
    private val webhooksClient: RecommandWebhooksClient,
    private val moduleConfig: PeppolModuleConfig
) {
    private val logger = loggerFor("PeppolWebhookSyncService")

    suspend fun ensureSingleWebhookForSettings(settings: PeppolSettingsDto): Result<RecommandWebhook> = runCatching {
        val outcome = convergeWebhook(settings).getOrThrow()
        logger.info(
            "Synced Recommand webhook for tenant {} company {} (created={}, updated={}, deleted={})",
            settings.tenantId,
            settings.companyId,
            outcome.created,
            outcome.updated,
            outcome.deleted
        )
        outcome.webhook
    }

    suspend fun syncAllEnabledTenants(): Result<PeppolWebhookSyncSummary> = runCatching {
        val enabledSettings = settingsRepository.getAllEnabled().getOrThrow()

        var created = 0
        var updated = 0
        var deleted = 0
        var failures = 0

        for (settings in enabledSettings) {
            val result = convergeWebhook(settings)
            result.onSuccess {
                created += it.created
                updated += it.updated
                deleted += it.deleted
            }.onFailure { error ->
                failures++
                logger.warn(
                    "Failed syncing Recommand webhook for tenant {} company {}",
                    settings.tenantId,
                    settings.companyId,
                    error
                )
            }
        }

        PeppolWebhookSyncSummary(
            tenantsProcessed = enabledSettings.size,
            created = created,
            updated = updated,
            deleted = deleted,
            failures = failures
        )
    }

    fun buildCallbackUrl(webhookToken: String): String {
        val base = moduleConfig.webhook.publicBaseUrl.trimEnd('/')
        val path = moduleConfig.webhook.callbackPath.trim().let {
            if (it.startsWith("/")) it else "/$it"
        }
        return "$base$path?token=$webhookToken"
    }

    private suspend fun convergeWebhook(settings: PeppolSettingsDto): Result<SyncOutcome> = runCatching {
        val token = settings.webhookToken?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: throw IllegalStateException("Missing webhook token for tenant ${settings.tenantId}")
        val expectedUrl = buildCallbackUrl(token)
        val creds = moduleConfig.masterCredentials

        val existing = webhooksClient.listWebhooks(
            apiKey = creds.apiKey,
            apiSecret = creds.apiSecret,
            companyId = settings.companyId
        ).getOrThrow()

        var created = 0
        var updated = 0
        var deleted = 0

        val keeper = if (existing.isEmpty()) {
            created++
            webhooksClient.createWebhook(
                apiKey = creds.apiKey,
                apiSecret = creds.apiSecret,
                url = expectedUrl,
                companyId = settings.companyId
            ).getOrThrow()
        } else {
            var candidate = existing
                .sortedWith(compareBy<RecommandWebhook>({ parseTimestamp(it.createdAt) }, { it.id }))
                .first()

            if (candidate.url != expectedUrl || candidate.companyId != settings.companyId) {
                candidate = webhooksClient.updateWebhook(
                    apiKey = creds.apiKey,
                    apiSecret = creds.apiSecret,
                    webhookId = candidate.id,
                    url = expectedUrl,
                    companyId = settings.companyId
                ).getOrThrow()
                updated++
            }

            for (stale in existing.filter { it.id != candidate.id }) {
                val removed = webhooksClient.deleteWebhook(
                    apiKey = creds.apiKey,
                    apiSecret = creds.apiSecret,
                    webhookId = stale.id
                ).getOrThrow()
                if (!removed) {
                    throw IllegalStateException("Failed deleting stale webhook ${stale.id}")
                }
                deleted++
            }

            candidate
        }

        val finalState = webhooksClient.listWebhooks(
            apiKey = creds.apiKey,
            apiSecret = creds.apiSecret,
            companyId = settings.companyId
        ).getOrThrow()
        if (finalState.size != 1) {
            throw IllegalStateException(
                "Expected exactly one webhook for company ${settings.companyId}, found ${finalState.size}"
            )
        }
        val finalWebhook = finalState.single()
        val normalized = if (finalWebhook.url != expectedUrl || finalWebhook.companyId != settings.companyId) {
            updated++
            webhooksClient.updateWebhook(
                apiKey = creds.apiKey,
                apiSecret = creds.apiSecret,
                webhookId = finalWebhook.id,
                url = expectedUrl,
                companyId = settings.companyId
            ).getOrThrow()
        } else {
            finalWebhook
        }

        SyncOutcome(
            webhook = normalized,
            created = created,
            updated = updated,
            deleted = deleted
        )
    }

    private fun parseTimestamp(raw: String): Instant = runCatching { Instant.parse(raw) }.getOrElse { Instant.EPOCH }

    private data class SyncOutcome(
        val webhook: RecommandWebhook,
        val created: Int,
        val updated: Int,
        val deleted: Int
    )
}
