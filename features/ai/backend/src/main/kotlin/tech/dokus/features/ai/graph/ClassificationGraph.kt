package tech.dokus.features.ai.graph

import ai.koog.agents.core.dsl.builder.AIAgentSubgraphBuilderBase
import ai.koog.agents.core.dsl.builder.AIAgentSubgraphDelegate
import ai.koog.agents.core.tools.Tool
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.ext.agent.subgraphWithTask
import ai.koog.prompt.message.AttachmentContent
import ai.koog.prompt.message.ContentPart
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.features.ai.config.asVisionModel
import tech.dokus.features.ai.extensions.description
import tech.dokus.features.ai.orchestrator.DocumentFetcher
import tech.dokus.features.ai.services.DocumentImageService
import tech.dokus.foundation.backend.config.AIConfig

fun AIAgentSubgraphBuilderBase<*, *>.classifyDocumentSubGraph(
    aiConfig: AIConfig,
    registry: ToolRegistry,
    documentFetcher: DocumentFetcher,
    documentImageService: DocumentImageService = DocumentImageService()
): AIAgentSubgraphDelegate<ClassifyDocumentInput, ClassificationResult> {
    return subgraphWithTask(
        name = "Classify document",
        llmModel = aiConfig.mode.asVisionModel,
        tools = registry.tools,
        finishTool = ClassificationFinishTool()
    ) { input ->
        val document = documentFetcher(input.tenantId, input.documentId).getOrElse {
            return@subgraphWithTask "Failed to get the document"
        }
        val images = documentImageService.getDocumentImages(document.bytes, document.mimeType)
        llm.writeSession {
            appendPrompt {
                user {
                    images.images.forEach { image ->
                        image(
                            ContentPart.Image(
                                content = AttachmentContent.Binary.Bytes(image.imageBytes),
                                format = "png",
                                mimeType = image.mimeType,
                            )
                        )
                    }
                }
            }
        }

        input.prompt
    }
}

internal class ClassificationFinishTool : Tool<ClassificationToolInput, ClassificationResult>(
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
    val documentId: DocumentId,
    val tenantId: TenantId
)

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

private val ClassifyDocumentInput.prompt
    get() = """
    Classify the document with ID: $documentId
    
    ## STEPS
    1. Call fetch_document_pages to get first pages (default: pages 1-3)
    2. Analyze content, layout, keywords
    3. For most documents, first 1-3 pages are enough to classify
    4. If unclear, fetch more pages using nextPageStart
    5. Call submitClassification with your result
    
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
    
    RECEIPT vs EXPENSE:
    - RECEIPT: Small thermal ticket from retail/restaurant
    - EXPENSE: Formal A4 document from utilities/subscriptions
    
    CREDIT_NOTE: Look for "Creditnota", "Note de crédit", "Avoir" — references original invoice
    """.trimIndent()