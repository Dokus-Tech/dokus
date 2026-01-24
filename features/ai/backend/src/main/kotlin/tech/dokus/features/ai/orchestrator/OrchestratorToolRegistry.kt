package tech.dokus.features.ai.orchestrator

import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLModel
import tech.dokus.domain.repository.ChunkRepository
import tech.dokus.domain.repository.ExampleRepository
import tech.dokus.features.ai.orchestrator.tools.ContactCreatorHandler
import tech.dokus.features.ai.orchestrator.tools.ContactLookupHandler
import tech.dokus.features.ai.orchestrator.tools.CreateContactTool
import tech.dokus.features.ai.orchestrator.tools.DocumentImageFetcher
import tech.dokus.features.ai.orchestrator.tools.EmbedTextTool
import tech.dokus.features.ai.orchestrator.tools.ExtractBillTool
import tech.dokus.features.ai.orchestrator.tools.ExtractExpenseTool
import tech.dokus.features.ai.orchestrator.tools.ExtractInvoiceTool
import tech.dokus.features.ai.orchestrator.tools.ExtractReceiptTool
import tech.dokus.features.ai.orchestrator.tools.FindSimilarDocumentTool
import tech.dokus.features.ai.orchestrator.tools.GenerateDescriptionTool
import tech.dokus.features.ai.orchestrator.tools.GenerateKeywordsTool
import tech.dokus.features.ai.orchestrator.tools.GetDocumentImagesTool
import tech.dokus.features.ai.orchestrator.tools.IndexingStatusUpdater
import tech.dokus.features.ai.orchestrator.tools.GetPeppolDataTool
import tech.dokus.features.ai.orchestrator.tools.IndexAsExampleTool
import tech.dokus.features.ai.orchestrator.tools.LookupContactTool
import tech.dokus.features.ai.orchestrator.tools.PeppolDataFetcher
import tech.dokus.features.ai.orchestrator.tools.PrepareRagChunksTool
import tech.dokus.features.ai.orchestrator.tools.SeeDocumentTool
import tech.dokus.features.ai.orchestrator.tools.StoreChunksTool
import tech.dokus.features.ai.orchestrator.tools.StoreExtractionTool
import tech.dokus.features.ai.orchestrator.tools.StoreExtractionHandler
import tech.dokus.features.ai.prompts.AgentPrompt
import tech.dokus.features.ai.prompts.DocumentClassificationPrompt
import tech.dokus.features.ai.prompts.ExtractionPrompt
import tech.dokus.features.ai.services.ChunkingService
import tech.dokus.features.ai.services.DocumentImageCache
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
        val imageCache: DocumentImageCache,
        val chunkingService: ChunkingService,
        val embeddingService: EmbeddingService,
        val exampleRepository: ExampleRepository,
        val chunkRepository: ChunkRepository,
        val cbeApiClient: CbeApiClient?,
        val tenantContext: AgentPrompt.TenantContext,
        val indexingUpdater: IndexingStatusUpdater?,
        val traceSink: ToolTraceSink? = null,

        // Function hooks for database operations
        val documentFetcher: DocumentImageFetcher,
        val peppolDataFetcher: PeppolDataFetcher,
        val storeExtraction: StoreExtractionHandler,
        val contactLookup: ContactLookupHandler,
        val contactCreator: ContactCreatorHandler
    )

    /**
     * Create a full tool registry for the orchestrator.
     */
    fun create(config: Config): ToolRegistry {
        return ToolRegistry {
            // Document tools
            tool(
                GetDocumentImagesTool(
                    config.documentImageService,
                    config.documentFetcher,
                    config.imageCache,
                    config.traceSink
                )
            )
            tool(GetPeppolDataTool(config.peppolDataFetcher))

            // Vision tools
            tool(
                SeeDocumentTool(
                    executor = config.executor,
                    model = config.visionModel,
                    prompt = DocumentClassificationPrompt,
                    tenantContext = config.tenantContext,
                    imageCache = config.imageCache,
                    traceSink = config.traceSink
                )
            )
            tool(
                ExtractInvoiceTool(
                    executor = config.executor,
                    model = config.visionModel,
                    prompt = ExtractionPrompt.Invoice,
                    tenantContext = config.tenantContext,
                    imageCache = config.imageCache,
                    traceSink = config.traceSink
                )
            )
            tool(
                ExtractBillTool(
                    executor = config.executor,
                    model = config.visionModel,
                    prompt = ExtractionPrompt.Bill,
                    tenantContext = config.tenantContext,
                    imageCache = config.imageCache,
                    traceSink = config.traceSink
                )
            )
            tool(
                ExtractReceiptTool(
                    executor = config.executor,
                    model = config.visionModel,
                    prompt = ExtractionPrompt.Receipt,
                    tenantContext = config.tenantContext,
                    imageCache = config.imageCache,
                    traceSink = config.traceSink
                )
            )
            tool(
                ExtractExpenseTool(
                    executor = config.executor,
                    model = config.visionModel,
                    prompt = ExtractionPrompt.Expense,
                    tenantContext = config.tenantContext,
                    imageCache = config.imageCache,
                    traceSink = config.traceSink
                )
            )

            // Enrichment tools
            tool(GenerateDescriptionTool)
            tool(GenerateKeywordsTool)
            tool(PrepareRagChunksTool(config.chunkingService))
            tool(EmbedTextTool(config.embeddingService))

            // Storage tools
            tool(StoreExtractionTool(config.storeExtraction, config.traceSink))
            tool(StoreChunksTool(config.chunkRepository, config.indexingUpdater))
            tool(IndexAsExampleTool(config.exampleRepository))

            // Lookup tools
            tool(FindSimilarDocumentTool(config.exampleRepository))
            tool(LookupContactTool(config.contactLookup, config.traceSink))
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
}
