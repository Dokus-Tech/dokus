package tech.dokus.features.ai.graph

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.tools.ToolRegistry
import kotlinx.coroutines.runBlocking
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.features.ai.config.AIProviderFactory
import tech.dokus.features.ai.orchestrator.DocumentFetcher
import tech.dokus.features.ai.orchestrator.DocumentFetcher.FetchedDocumentData
import tech.dokus.features.ai.tools.DocumentFetcherTool
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
        val invoiceBytes = javaClass.getResourceAsStream("/test-data/invoice-nl.pdf")!!.readBytes()

        val mockFetcher = DocumentFetcher { tenantId, documentId ->
            Result.success(
                FetchedDocumentData(
                    invoiceBytes,
                    "application/pdf"
                )
            )
        }

        val tenantId = TenantId("test")
        val toolRegistry = ToolRegistry {
            tool(DocumentFetcherTool(tenantId, mockFetcher))
        }

        val strategy = strategy<ClassifyDocumentInput, ClassificationResult>("test") {
            val classify by classifyDocumentSubGraph(testAiConfig, toolRegistry)
            nodeStart then classify then nodeFinish
        }

        val agent = AIAgent(
            promptExecutor = AIProviderFactory.createOpenAiExecutor(testAiConfig),
            toolRegistry = toolRegistry,
            strategy = strategy,
            agentConfig = AIAgentConfig.withSystemPrompt("You are a document classifier.")
        )

        val result = agent.run(ClassifyDocumentInput(DocumentId.generate(), tenantId))

        assertEquals(DocumentType.Invoice, result.documentType)
        assertTrue(result.confidence >= 0.8)
        assertEquals("nl", result.language)
    }
}