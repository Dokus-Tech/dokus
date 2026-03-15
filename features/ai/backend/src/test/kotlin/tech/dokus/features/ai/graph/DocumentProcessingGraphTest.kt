package tech.dokus.features.ai.graph

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.annotation.ExperimentalAgentsApi
import ai.koog.agents.core.tools.ToolRegistry
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import tech.dokus.domain.ids.DocumentId
import tech.dokus.features.ai.config.AIProviderFactory
import tech.dokus.features.ai.config.asVisionModel
import tech.dokus.features.ai.graph.AcceptDocumentInput
import tech.dokus.features.ai.models.FinancialExtractionResult
import tech.dokus.features.ai.services.DocumentFetcher
import tech.dokus.features.ai.services.DocumentFetcher.FetchedDocumentData
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.ExperimentalUuidApi

class DocumentProcessingGraphTest {

    @OptIn(ExperimentalUuidApi::class, ExperimentalAgentsApi::class)
    @Tag("ai")
    @Disabled("Requires running AI model endpoint (LM Studio / OpenAI)")
    @Test
    fun `process invoice document`() = runBlocking {
        val pdfFile = TestAiFixtures.loadFixture("fixtures/invoices/test-invoice.pdf")
        val documentBytes = pdfFile.readBytes()

        val mockFetcher = DocumentFetcher { _, _ ->
            Result.success(FetchedDocumentData(documentBytes, "application/pdf"))
        }

        val toolRegistry = ToolRegistry { }

        val agent = AIAgent(
            promptExecutor = AIProviderFactory.createOpenAiExecutor(TestAiFixtures.aiConfig),
            toolRegistry = toolRegistry,
            strategy = acceptDocumentGraph(TestAiFixtures.aiConfig, mockFetcher),
            agentConfig = AIAgentConfig.withSystemPrompt(
                prompt = "You are a document processor.",
                llm = TestAiFixtures.aiConfig.mode.asVisionModel,
                maxAgentIterations = TestAiFixtures.aiConfig.mode.maxIterations
            )
        )

        val result = withTimeout(180.seconds) {
            try {
                agent.run(AcceptDocumentInput.Upload(documentId = DocumentId.generate(), tenant = TestAiFixtures.tenant, associatedPersonNames = emptyList(), userFeedback = null, maxPagesOverride = null, dpiOverride = null))
            } finally {
                runCatching { agent.close() }
            }
        }

        assertTrue(
            result.classification.documentType.supported,
            "Expected supported document type, got ${result.classification.documentType}"
        )
        assertTrue(
            result.extraction !is FinancialExtractionResult.Unsupported,
            "Expected extraction result, got Unsupported"
        )
        assertTrue(
            result.auditReport.checks.isNotEmpty(),
            "Expected validation checks to run"
        )
    }
}
