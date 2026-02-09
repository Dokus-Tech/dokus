package tech.dokus.features.ai.graph.sub

import ai.koog.agents.core.dsl.builder.AIAgentSubgraphBuilderBase
import ai.koog.agents.core.dsl.builder.AIAgentSubgraphDelegate
import tech.dokus.features.ai.graph.nodes.InputWithDocumentId
import tech.dokus.features.ai.graph.nodes.InputWithTenantContext
import tech.dokus.features.ai.graph.nodes.documentImagesInjectorNode
import tech.dokus.features.ai.graph.nodes.tenantContextInjectorNode
import tech.dokus.features.ai.services.DocumentFetcher

internal inline fun <reified Input> AIAgentSubgraphBuilderBase<*, *>.documentPreparationSubGraph(
    documentFetcher: DocumentFetcher,
): AIAgentSubgraphDelegate<Input, ClassifyDocumentInput> where Input : InputWithDocumentId, Input : InputWithTenantContext {
    return subgraph(name = "document-preparation") {
        val injectImages by documentImagesInjectorNode<Input>(documentFetcher)
        val injectTenant by tenantContextInjectorNode<Input>()
        val prepareClassifyInput by node<Input, ClassifyDocumentInput>("prepare-classify") { input ->
            ClassifyDocumentInput(input.documentId, input.tenant)
        }

        nodeStart then injectTenant then injectImages then prepareClassifyInput then nodeFinish
    }
}
