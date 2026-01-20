package tech.dokus.features.ai.orchestrator

import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLModel
import kotlinx.serialization.json.JsonElement
import tech.dokus.domain.repository.ChunkRepository
import tech.dokus.domain.repository.ExampleRepository
import tech.dokus.features.ai.models.ExtractedDocumentData
import tech.dokus.features.ai.orchestrator.tools.CreateContactTool
import tech.dokus.features.ai.orchestrator.tools.EmbedTextTool
import tech.dokus.features.ai.orchestrator.tools.ExtractBillTool
import tech.dokus.features.ai.orchestrator.tools.ExtractExpenseTool
import tech.dokus.features.ai.orchestrator.tools.ExtractInvoiceTool
import tech.dokus.features.ai.orchestrator.tools.ExtractReceiptTool
import tech.dokus.features.ai.orchestrator.tools.FindSimilarDocumentTool
import tech.dokus.features.ai.orchestrator.tools.GenerateDescriptionTool
import tech.dokus.features.ai.orchestrator.tools.GenerateKeywordsTool
import tech.dokus.features.ai.orchestrator.tools.GetDocumentImagesTool
import tech.dokus.features.ai.orchestrator.tools.GetPeppolDataTool
import tech.dokus.features.ai.orchestrator.tools.IndexAsExampleTool
import tech.dokus.features.ai.orchestrator.tools.LookupContactTool
import tech.dokus.features.ai.orchestrator.tools.PrepareRagChunksTool
import tech.dokus.features.ai.orchestrator.tools.SeeDocumentTool
import tech.dokus.features.ai.orchestrator.tools.StoreChunksTool
import tech.dokus.features.ai.orchestrator.tools.StoreExtractionTool
import tech.dokus.features.ai.prompts.AgentPrompt
import tech.dokus.features.ai.services.ChunkingService
import tech.dokus.features.ai.services.DocumentImageService
import tech.dokus.features.ai.services.EmbeddingService
import tech.dokus.features.ai.tools.LookupCompanyTool
import tech.dokus.features.ai.tools.ValidateIbanTool
import tech.dokus.features.ai.tools.ValidateOgmTool
import tech.dokus.features.ai.tools.VerifyTotalsTool
import tech.dokus.foundation.backend.lookup.CbeApiClient

/**
 * Factory for creating the orchestrator's tool registry.
 *
 * The orchestrator uses tools to:
 * 1. Document tools - Get images and PEPPOL data
 * 2. Vision tools - Classify and extract documents
 * 3. Enrichment tools - Generate descriptions, keywords, chunks
 * 4. Storage tools - Persist extractions, chunks, examples
 * 5. Lookup tools - Find examples, contacts
 * 6. Validation tools - Verify totals, IBAN, OGM
 */
object OrchestratorToolRegistry {

    /**
     * Configuration for creating the tool registry.
     */
    data class Config(
        val executor: PromptExecutor,
        val visionModel: LLModel,
        val documentImageService: DocumentImageService,
        val chunkingService: ChunkingService,
        val embeddingService: EmbeddingService,
        val exampleRepository: ExampleRepository,
        val chunkRepository: ChunkRepository,
        val cbeApiClient: CbeApiClient?,
        val tenantContext: AgentPrompt.TenantContext,

        // Function hooks for database operations
        val documentFetcher: suspend (documentId: String) -> GetDocumentImagesTool.DocumentData?,
        val peppolDataFetcher: suspend (documentId: String) -> ExtractedDocumentData?,
        val storeExtraction: suspend (
            documentId: String,
            tenantId: String,
            extraction: JsonElement,
            description: String,
            keywords: List<String>,
            confidence: Double
        ) -> Boolean,
        val contactLookup: suspend (tenantId: String, vatNumber: String) -> LookupContactTool.ContactInfo?,
        val contactCreator: suspend (
            tenantId: String,
            name: String,
            vatNumber: String?,
            address: String?
        ) -> CreateContactTool.CreateResult
    )

    /**
     * Create a full tool registry for the orchestrator.
     */
    fun create(config: Config): ToolRegistry {
        return ToolRegistry {
            // Document tools
            tool(GetDocumentImagesTool(config.documentImageService, config.documentFetcher))
            tool(GetPeppolDataTool(config.peppolDataFetcher))

            // Vision tools
            tool(
                SeeDocumentTool(
                    executor = config.executor,
                    model = config.visionModel,
                    prompt = AgentPrompt.DocumentClassification,
                    tenantContext = config.tenantContext
                )
            )
            tool(
                ExtractInvoiceTool(
                    executor = config.executor,
                    model = config.visionModel,
                    prompt = AgentPrompt.Extraction.Invoice
                )
            )
            tool(
                ExtractBillTool(
                    executor = config.executor,
                    model = config.visionModel,
                    prompt = AgentPrompt.Extraction.Bill
                )
            )
            tool(
                ExtractReceiptTool(
                    executor = config.executor,
                    model = config.visionModel,
                    prompt = AgentPrompt.Extraction.Receipt
                )
            )
            tool(
                ExtractExpenseTool(
                    executor = config.executor,
                    model = config.visionModel,
                    prompt = AgentPrompt.Extraction.Expense
                )
            )

            // Enrichment tools
            tool(GenerateDescriptionTool)
            tool(GenerateKeywordsTool)
            tool(PrepareRagChunksTool(config.chunkingService))
            tool(EmbedTextTool(config.embeddingService))

            // Storage tools
            tool(StoreExtractionTool(config.storeExtraction))
            tool(StoreChunksTool(config.chunkRepository))
            tool(IndexAsExampleTool(config.exampleRepository))

            // Lookup tools
            tool(FindSimilarDocumentTool(config.exampleRepository))
            tool(LookupContactTool(config.contactLookup))
            tool(CreateContactTool(config.contactCreator))

            // Validation tools (existing)
            tool(VerifyTotalsTool)
            tool(ValidateOgmTool)
            tool(ValidateIbanTool)
            if (config.cbeApiClient != null) {
                tool(LookupCompanyTool(config.cbeApiClient))
            }
        }
    }

    /**
     * Create a minimal tool registry for testing.
     */
    fun createMinimal(): ToolRegistry {
        return ToolRegistry {
            tool(GenerateDescriptionTool)
            tool(GenerateKeywordsTool)
            tool(VerifyTotalsTool)
            tool(ValidateOgmTool)
            tool(ValidateIbanTool)
        }
    }
}
