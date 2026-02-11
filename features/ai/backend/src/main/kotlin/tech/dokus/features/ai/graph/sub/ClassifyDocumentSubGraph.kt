package tech.dokus.features.ai.graph.sub

import ai.koog.agents.core.dsl.builder.AIAgentSubgraphBuilderBase
import ai.koog.agents.core.dsl.builder.AIAgentSubgraphDelegate
import ai.koog.agents.core.tools.Tool
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.ext.agent.subgraphWithTask
import ai.koog.prompt.params.LLMParams
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.model.Tenant
import tech.dokus.features.ai.config.asVisionModel
import tech.dokus.features.ai.config.documentProcessing
import tech.dokus.features.ai.extensions.description
import tech.dokus.features.ai.graph.nodes.InputWithDocumentId
import tech.dokus.features.ai.graph.nodes.InputWithTenantContext
import tech.dokus.foundation.backend.config.AIConfig

fun AIAgentSubgraphBuilderBase<*, *>.classifyDocumentSubGraph(
    aiConfig: AIConfig,
): AIAgentSubgraphDelegate<ClassifyDocumentInput, ClassificationResult> {
    return subgraphWithTask(
        name = "Classify document",
        llmModel = aiConfig.mode.asVisionModel,
        tools = emptyList(),
        llmParams = LLMParams.documentProcessing,
        finishTool = ClassificationFinishTool()
    ) { it.prompt }
}

private class ClassificationFinishTool : Tool<ClassificationToolInput, ClassificationResult>(
    argsSerializer = ClassificationToolInput.serializer(),
    resultSerializer = ClassificationResult.serializer(),
    name = "submit_classification",
    description = "Submit the final document classification after analyzing the document"
) {
    override suspend fun execute(args: ClassificationToolInput): ClassificationResult {
        return ClassificationResult(
            documentType = args.documentType,
            confidence = args.confidence,
            language = args.language,
            reasoning = args.reasoning
        )
    }
}

@Serializable
data class ClassificationToolInput(
    @property:LLMDescription("The detected document type")
    val documentType: DocumentType,
    @property:LLMDescription("Confidence score from 0.0 to 1.0")
    val confidence: Double,
    @property:LLMDescription("Detected language: nl, fr, en, or de")
    val language: String,
    @property:LLMDescription("Brief explanation of key indicators")
    val reasoning: String
)

@Serializable
data class ClassifyDocumentInput(
    override val documentId: DocumentId,
    override val tenant: Tenant
) : InputWithTenantContext, InputWithDocumentId

@Serializable
@SerialName("ClassificationResult")
@LLMDescription("Classification result for a document")
data class ClassificationResult(
    @property:LLMDescription("The detected document type (use exact enum name)")
    val documentType: DocumentType,
    @property:LLMDescription("Confidence score from 0.0 to 1.0")
    val confidence: Double,
    @property:LLMDescription("Detected language: nl, fr, en, or de")
    val language: String,
    @property:LLMDescription("Brief explanation of key indicators that led to this classification")
    val reasoning: String
)

@Suppress("UnusedReceiverParameter")
private val ClassifyDocumentInput.prompt
    get() = """    
    You will receive document pages as images in context.

    Task: classify the document type and language.
    Output MUST be submitted via tool: submit_classification.
    
    ## DOCUMENT TYPES
    
    ${
        DocumentType.entries.joinToString("\n") { type ->
            "- ${type.name}: ${type.description}"
        }
    }
    
    ## CONFIDENCE GUIDE
    - 0.95+: Obvious, clear indicators (e.g., "FACTUUR" header with VAT breakdown)
    - 0.80-0.95: Strong indicators present
    - 0.60-0.80: Some uncertainty, mixed signals
    - <0.60: Unclear, use UNKNOWN
    
    ## KEY DISTINCTIONS
    
    INVOICE vs BILL:
    - INVOICE: Issued BY the business (your company logo/header at top)
    - BILL: Received FROM supplier (supplier's header, you're in "Klant/Client" section)
    
    RECEIPT vs BILL:
    - RECEIPT: Small thermal ticket proving payment already made (retail/restaurant)
    - BILL: A4 supplier invoice requesting payment (including recurring utilities)
    
    CREDIT_NOTE: Look for "Creditnota", "Note de crédit", "Avoir" — references original invoice
    """.trimIndent()