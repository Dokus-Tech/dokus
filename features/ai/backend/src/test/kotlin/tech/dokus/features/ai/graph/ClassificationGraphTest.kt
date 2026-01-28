package tech.dokus.features.ai.graph

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.dsl.builder.strategy
import kotlinx.coroutines.runBlocking
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.features.ai.config.AIProviderFactory
import tech.dokus.features.ai.config.asVisionModel
import tech.dokus.features.ai.orchestrator.DocumentFetcher
import tech.dokus.features.ai.orchestrator.DocumentFetcher.FetchedDocumentData
import tech.dokus.features.ai.services.DocumentImageService
import tech.dokus.features.ai.tools.TenantDocumentsRegistry
import tech.dokus.foundation.backend.config.AIConfig
import tech.dokus.foundation.backend.config.IntelligenceMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.uuid.ExperimentalUuidApi

private val testAiConfig = AIConfig(
    mode = IntelligenceMode.Sovereign,
    ollamaHost = "",
    lmStudioHost = "http://192.168.0.150:1234"
)

class ClassificationGraphTest {

    @OptIn(ExperimentalUuidApi::class)
    @Test
    fun `should classify invoice correctly`() = runBlocking {
        // Load test invoice
        val invoiceBytes = ClassLoader.getSystemResourceAsStream("test-invoice.pdf").readBytes()

        val mockFetcher = DocumentFetcher { tenantId, documentId ->
            Result.success(
                FetchedDocumentData(
                    invoiceBytes,
                    "application/pdf"
                )
            )
        }

        val tenantId = TenantId.generate()
        val toolRegistry = TenantDocumentsRegistry(tenantId, mockFetcher, DocumentImageService())

        val strategy = strategy<ClassifyDocumentInput, ClassificationResult>("test") {
            val classify by classifyDocumentSubGraph(testAiConfig, toolRegistry)
            nodeStart then classify then nodeFinish
        }

        val agent = AIAgent(
            promptExecutor = AIProviderFactory.createOpenAiExecutor(testAiConfig),
            toolRegistry = toolRegistry,
            strategy = strategy,
            agentConfig = AIAgentConfig.withSystemPrompt(
                prompt = "You are a document classifier.",
                llm = testAiConfig.mode.asVisionModel,
                maxAgentIterations = testAiConfig.mode.maxIterations
            )
        )

        val result = agent.run(ClassifyDocumentInput(DocumentId.generate(), tenantId))

        assertEquals(DocumentType.Invoice, result.documentType)
        assertTrue(result.confidence >= 0.8)
        assertEquals("nl", result.language)
    }
}