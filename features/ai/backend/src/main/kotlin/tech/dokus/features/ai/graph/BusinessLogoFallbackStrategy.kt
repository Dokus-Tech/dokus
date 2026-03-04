package tech.dokus.features.ai.graph

import ai.koog.agents.core.agent.entity.AIAgentGraphStrategy
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.ext.agent.ConditionResult
import ai.koog.agents.ext.agent.subgraphWithRetrySimple
import tech.dokus.features.ai.graph.sub.businessLogoFallbackSubGraph
import tech.dokus.features.ai.models.BusinessLogoFallbackResult
import tech.dokus.features.ai.models.BusinessLogoFallbackInput
import tech.dokus.foundation.backend.config.AIConfig
import java.net.URI

fun businessLogoFallbackGraph(
    aiConfig: AIConfig
): AIAgentGraphStrategy<BusinessLogoFallbackInput, BusinessLogoFallbackResult> {
    return strategy<BusinessLogoFallbackInput, BusinessLogoFallbackResult>(
        "business-logo-fallback-graph"
    ) {
        val findWithRetry by subgraphWithRetrySimple<BusinessLogoFallbackInput, BusinessLogoFallbackResult>(
            name = "find-logo-candidates-with-retry",
            maxRetries = 2,
            strict = false,
            conditionDescription = "Candidates must be <= 8 and use absolute http(s) URLs.",
            condition = { result ->
                val error = validateLogoFallbackForRetry(result)
                if (error == null) ConditionResult.Approve else ConditionResult.Reject(error)
            }
        ) {
            val find by businessLogoFallbackSubGraph(aiConfig)
            nodeStart then find then nodeFinish
        }

        edge(nodeStart forwardTo findWithRetry)
        edge(findWithRetry forwardTo nodeFinish)
    }
}

internal fun validateLogoFallbackForRetry(result: BusinessLogoFallbackResult): String? {
    if (result.candidates.size > 8) return "candidates must contain at most 8 URLs."
    result.candidates.forEachIndexed { index, candidate ->
        if (!isAbsoluteHttpUrl(candidate.url)) {
            return "candidate[$index] must be an absolute http(s) URL."
        }
        if (candidate.confidence !in 0.0..1.0) {
            return "candidate[$index] confidence must be between 0.0 and 1.0."
        }
    }
    return null
}

private fun isAbsoluteHttpUrl(value: String): Boolean {
    val uri = runCatching { URI(value) }.getOrNull() ?: return false
    val scheme = uri.scheme?.lowercase()
    return (scheme == "http" || scheme == "https") && !uri.host.isNullOrBlank()
}
