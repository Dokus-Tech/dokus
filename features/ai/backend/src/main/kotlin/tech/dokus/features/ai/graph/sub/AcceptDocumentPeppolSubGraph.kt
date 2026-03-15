package tech.dokus.features.ai.graph.sub

import ai.koog.agents.core.dsl.builder.AIAgentSubgraphBuilderBase
import ai.koog.agents.core.dsl.builder.AIAgentSubgraphDelegate
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json.Default.decodeFromString
import tech.dokus.domain.model.DocumentDraftData
import tech.dokus.features.ai.graph.AcceptDocumentInput
import tech.dokus.features.ai.models.DocumentAiProcessingResult
import tech.dokus.features.ai.models.toPeppolProcessingResult
import tech.dokus.features.ai.services.DocumentFetcher
import tech.dokus.foundation.backend.config.AIConfig

internal fun AIAgentSubgraphBuilderBase<*, *>.acceptDocumentOnPeppolSubGraph(
    aiConfig: AIConfig,
    documentFetcher: DocumentFetcher,
): AIAgentSubgraphDelegate<AcceptDocumentInput, DocumentAiProcessingResult> {
    return subgraph(
        name = "peppol-structured-extraction-subgraph"
    ) {
        val processStructured by node<AcceptDocumentInput, DocumentAiProcessingResult>("parse-peppol-structured-snapshot") { input ->
            require(input is AcceptDocumentInput.Peppol)
            val draftData = try {
                decodeFromString<DocumentDraftData>(input.peppolStructuredSnapshotJson)
            } catch (e: SerializationException) {
                throw IllegalStateException(
                    "Failed to deserialize PEPPOL structured snapshot (version=${input.peppolSnapshotVersion}); " +
                            "document ${input.documentId} will need vision extraction",
                    e
                )
            }
            draftData.toPeppolProcessingResult(input.peppolSnapshotVersion)
        }
        nodeStart then processStructured then nodeFinish
    }
}