package tech.dokus.features.ai.graph

import ai.koog.agents.core.agent.entity.AIAgentGraphStrategy
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.strategy
import tech.dokus.features.ai.graph.sub.acceptDocumentOnPeppolSubGraph
import tech.dokus.features.ai.graph.sub.acceptDocumentOnVisionSubGraph
import tech.dokus.features.ai.models.DocumentAiProcessingResult
import tech.dokus.features.ai.services.DocumentFetcher
import tech.dokus.foundation.backend.config.AIConfig

fun acceptDocumentGraph(
    aiConfig: AIConfig,
    documentFetcher: DocumentFetcher,
): AIAgentGraphStrategy<AcceptDocumentInput, DocumentAiProcessingResult> {
    return strategy<AcceptDocumentInput, DocumentAiProcessingResult>("accept-document-parent") {
        val visionSubGraph by acceptDocumentOnVisionSubGraph(aiConfig, documentFetcher)
        val peppolSubGraph by acceptDocumentOnPeppolSubGraph(aiConfig, documentFetcher)

        edge(
            nodeStart forwardTo peppolSubGraph onCondition { input ->
                input.isPeppol()
            }
        )
        edge(
            nodeStart forwardTo visionSubGraph onCondition { input ->
                input.isUpload()
            }
        )
        edge(visionSubGraph forwardTo nodeFinish)
        edge(peppolSubGraph forwardTo nodeFinish)
    }
}