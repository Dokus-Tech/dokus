package tech.dokus.features.ai.config

import ai.koog.agents.core.agent.GraphAIAgent.FeatureContext
import ai.koog.agents.features.opentelemetry.feature.OpenTelemetry
import ai.koog.agents.features.opentelemetry.integration.langfuse.addLangfuseExporter
import tech.dokus.foundation.backend.config.LangfuseConfig

/**
 * Installs Langfuse tracing via Koog's OpenTelemetry feature.
 * These are set on the `dokus-server` container via the `docker-compose.langfuse.yml` overlay.
 */
@PublishedApi
internal fun FeatureContext.installLangfuseTracing(config: LangfuseConfig) {
    if (!config.enabled) return

    install(OpenTelemetry) {
        addLangfuseExporter(
            langfuseUrl = config.host,
            langfusePublicKey = config.publicKey,
            langfuseSecretKey = config.secretKey,
        )
        setVerbose(true)
    }
}
