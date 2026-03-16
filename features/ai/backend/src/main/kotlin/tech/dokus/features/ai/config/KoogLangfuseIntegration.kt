package tech.dokus.features.ai.config

import ai.koog.agents.core.agent.GraphAIAgent.FeatureContext
import ai.koog.agents.features.opentelemetry.attribute.CustomAttribute
import ai.koog.agents.features.opentelemetry.feature.OpenTelemetry
import ai.koog.agents.features.opentelemetry.integration.langfuse.addLangfuseExporter
import tech.dokus.foundation.backend.config.LangfuseConfig

/**
 * Fixed set of trace tags for Langfuse. Prevents typo-driven tag proliferation.
 */
enum class LangfuseTraceTag(val value: String) {
    DocumentProcessing("document-processing"),
    PurposeEnrichment("purpose-enrichment"),
    BusinessEnrichment("business-enrichment"),
    LogoFallback("logo-fallback"),
    Chat("chat"),
    SourceUpload("source:upload"),
    SourcePeppol("source:peppol"),
}

/**
 * Per-invocation context passed to Langfuse as trace-level attributes.
 */
data class LangfuseTraceContext(
    val tenantId: String? = null,
    val sessionId: String? = null,
    val tags: List<LangfuseTraceTag> = emptyList(),
    val metadata: Map<String, String> = emptyMap(),
)

/**
 * Installs Langfuse tracing via Koog's OpenTelemetry feature.
 *
 * Trace attributes are attached to the root `InvokeAgentSpan` and appear
 * in the Langfuse UI as user, session, tags, and metadata.
 */
@PublishedApi
internal fun FeatureContext.installLangfuseTracing(
    config: LangfuseConfig,
    traceContext: LangfuseTraceContext = LangfuseTraceContext(),
    serviceName: String = "dokus",
    serviceVersion: String = "unknown",
) {
    if (!config.enabled) return

    val attributes = buildList {
        traceContext.tenantId?.let { add(CustomAttribute("langfuse.user.id", it)) }
        traceContext.sessionId?.let { add(CustomAttribute("langfuse.session.id", it)) }
        if (traceContext.tags.isNotEmpty()) {
            add(CustomAttribute("langfuse.trace.tags", traceContext.tags.map { it.value }))
        }
        traceContext.metadata.forEach { (key, value) ->
            add(CustomAttribute("langfuse.trace.metadata.$key", value))
        }
    }

    install(OpenTelemetry) {
        setServiceInfo(serviceName, serviceVersion)
        addLangfuseExporter(
            langfuseUrl = config.host,
            langfusePublicKey = config.publicKey,
            langfuseSecretKey = config.secretKey,
            traceAttributes = attributes,
        )
        setVerbose(true)
    }
}
