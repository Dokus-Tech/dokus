package tech.dokus.features.ai.graph

import ai.koog.agents.core.agent.entity.AIAgentGraphStrategy
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.tools.ToolRegistry
import kotlinx.serialization.Serializable
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.features.ai.orchestrator.DocumentFetcher
import tech.dokus.features.ai.services.DocumentImageService
import tech.dokus.foundation.backend.config.AIConfig

@Serializable
data class AcceptDocumentInput(
    val documentId: DocumentId,
    val tenantId: TenantId
)

fun acceptDocumentGraph(
    aiConfig: AIConfig,
    registries: List<ToolRegistry>,
    documentFetcher: DocumentFetcher,
    imageService: DocumentImageService,
): AIAgentGraphStrategy<AcceptDocumentInput, Boolean> {
    return strategy<AcceptDocumentInput, Boolean>("accept-document-graph") {
        val godRegistry = ToolRegistry { tools(registries.flatMap { it.tools }) }

        val classifyDocument by classifyDocumentSubGraph(aiConfig, godRegistry, documentFetcher, imageService)
    }
}