package tech.dokus.features.ai.graph

import ai.koog.agents.core.agent.entity.AIAgentGraphStrategy
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.tools.ToolRegistry
import kotlinx.serialization.Serializable
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.model.Tenant
import tech.dokus.features.ai.graph.nodes.InputWithDocumentId
import tech.dokus.features.ai.graph.nodes.InputWithTenantContext
import tech.dokus.features.ai.graph.nodes.documentImagesInjectorNode
import tech.dokus.features.ai.graph.nodes.tenantContextInjectorNode
import tech.dokus.features.ai.orchestrator.DocumentFetcher
import tech.dokus.foundation.backend.config.AIConfig

@Serializable
data class AcceptDocumentInput(
    override val documentId: DocumentId,
    override val tenant: Tenant
) : InputWithDocumentId, InputWithTenantContext

fun acceptDocumentGraph(
    aiConfig: AIConfig,
    registries: List<ToolRegistry>,
    documentFetcher: DocumentFetcher,
): AIAgentGraphStrategy<AcceptDocumentInput, Boolean> {
    return strategy<AcceptDocumentInput, Boolean>("accept-document-graph") {
        val godRegistry = ToolRegistry { tools(registries.flatMap { it.tools }) }

        val classify by classifyDocumentSubGraph(aiConfig)
        val documentInjector by documentImagesInjectorNode(documentFetcher)
        val tenantInjector by tenantContextInjectorNode<ClassifyDocumentInput>()

        
    }
}