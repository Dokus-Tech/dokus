package tech.dokus.features.ai.graph

import ai.koog.agents.core.agent.entity.AIAgentGraphStrategy
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.ext.agent.ConditionResult
import ai.koog.agents.ext.agent.subgraphWithRetrySimple
import tech.dokus.features.ai.graph.sub.businessProfileContentExtractionSubGraph
import tech.dokus.features.ai.models.BusinessProfileContentExtractionInput
import tech.dokus.features.ai.models.BusinessProfileContentExtractionResult
import tech.dokus.foundation.backend.config.AIConfig

fun businessProfileContentExtractionGraph(
    aiConfig: AIConfig
): AIAgentGraphStrategy<BusinessProfileContentExtractionInput, BusinessProfileContentExtractionResult> {
    return strategy<BusinessProfileContentExtractionInput, BusinessProfileContentExtractionResult>(
        "business-profile-content-extraction-graph"
    ) {
        val extractWithRetry by subgraphWithRetrySimple<BusinessProfileContentExtractionInput, BusinessProfileContentExtractionResult>(
            name = "extract-with-retry",
            maxRetries = 2,
            strict = false,
            conditionDescription = "Output must contain <= 8 activities and confidence in 0.0..1.0.",
            condition = { result ->
                val error = validateContentExtractionForRetry(result)
                if (error == null) ConditionResult.Approve else ConditionResult.Reject(error)
            }
        ) {
            val extract by businessProfileContentExtractionSubGraph(aiConfig)
            nodeStart then extract then nodeFinish
        }

        edge(nodeStart forwardTo extractWithRetry)
        edge(extractWithRetry forwardTo nodeFinish)
    }
}

internal fun validateContentExtractionForRetry(result: BusinessProfileContentExtractionResult): String? {
    if (result.activities.size > 8) return "activities must have at most 8 entries."
    if (result.confidence !in 0.0..1.0) return "confidence must be between 0.0 and 1.0."
    return null
}
