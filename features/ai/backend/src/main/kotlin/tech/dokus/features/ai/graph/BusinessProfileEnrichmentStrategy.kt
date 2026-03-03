package tech.dokus.features.ai.graph

import ai.koog.agents.core.agent.entity.AIAgentGraphStrategy
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.ext.agent.ConditionResult
import ai.koog.agents.ext.agent.subgraphWithRetrySimple
import tech.dokus.features.ai.graph.sub.businessProfileDiscoverySubGraph
import tech.dokus.features.ai.models.BusinessDiscoveryStatus
import tech.dokus.features.ai.models.BusinessProfileDiscoveryResult
import tech.dokus.features.ai.models.BusinessProfileEnrichmentInput
import tech.dokus.foundation.backend.config.AIConfig
import java.net.URI

fun businessProfileEnrichmentGraph(
    aiConfig: AIConfig,
    registry: ToolRegistry,
): AIAgentGraphStrategy<BusinessProfileEnrichmentInput, BusinessProfileDiscoveryResult> {
    return strategy<BusinessProfileEnrichmentInput, BusinessProfileDiscoveryResult>("business-profile-enrichment-graph") {
        val discoverWithRetry by subgraphWithRetrySimple<BusinessProfileEnrichmentInput, BusinessProfileDiscoveryResult>(
            name = "discover-with-retry",
            maxRetries = 2,
            strict = false,
            conditionDescription = "Output must be structured, with valid absolute website/logo URLs when provided.",
            condition = { result ->
                val error = validateDiscoveryForRetry(result)
                if (error == null) ConditionResult.Approve else ConditionResult.Reject(error)
            }
        ) {
            val discover by businessProfileDiscoverySubGraph(aiConfig, registry.tools)
            nodeStart then discover then nodeFinish
        }

        edge(nodeStart forwardTo discoverWithRetry)
        edge(discoverWithRetry forwardTo nodeFinish)
    }
}

internal fun validateDiscoveryForRetry(result: BusinessProfileDiscoveryResult): String? {
    if (result.status == BusinessDiscoveryStatus.NotFound) return null

    val website = result.candidateWebsiteUrl
    if (website.isNullOrBlank()) {
        return "When status is FOUND, candidateWebsiteUrl must be a non-empty absolute URL."
    }
    if (!isAbsoluteHttpUrl(website)) {
        return "candidateWebsiteUrl must be an absolute http(s) URL."
    }
    val logo = result.logoUrl
    if (!logo.isNullOrBlank() && !isAbsoluteHttpUrl(logo)) {
        return "logoUrl must be absolute http(s) URL when present."
    }
    if (result.activities.size > 8) {
        return "activities must have at most 8 entries."
    }
    if (result.confidence !in 0.0..1.0) {
        return "confidence must be between 0.0 and 1.0."
    }
    return null
}

private fun isAbsoluteHttpUrl(value: String): Boolean {
    val uri = runCatching { URI(value) }.getOrNull() ?: return false
    val scheme = uri.scheme?.lowercase()
    return (scheme == "http" || scheme == "https") && !uri.host.isNullOrBlank()
}
