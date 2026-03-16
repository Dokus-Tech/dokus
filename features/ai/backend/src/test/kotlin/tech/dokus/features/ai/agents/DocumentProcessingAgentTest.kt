package tech.dokus.features.ai.agents

import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.prompt.dsl.ModerationResult
import ai.koog.prompt.dsl.Prompt
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.message.Message
import ai.koog.prompt.streaming.StreamFrame
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.ids.DocumentId
import tech.dokus.features.ai.config.KoogAgentRunner
import tech.dokus.features.ai.graph.AcceptDocumentInput
import tech.dokus.features.ai.graph.TestAiFixtures
import tech.dokus.features.ai.models.FinancialExtractionResult
import tech.dokus.features.ai.services.DocumentFetcher
import tech.dokus.features.ai.services.DocumentFetcher.FetchedDocumentData
import tech.dokus.features.ai.validation.CheckType
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.uuid.ExperimentalUuidApi

class DocumentProcessingAgentTest {

    @OptIn(ExperimentalUuidApi::class)
    @Test
    fun `classification failure returns deterministic contract failure result`() = runBlocking {
        val pdfFile = TestAiFixtures.loadFixture("fixtures/self-employed-contribution/avixi-Q1-2026.pdf")
        val documentBytes = pdfFile.readBytes()
        val documentFetcher = DocumentFetcher { _, _ ->
            Result.success(FetchedDocumentData(documentBytes, "application/pdf"))
        }
        val agent = DocumentProcessingAgent(
            agentRunner = KoogAgentRunner(ThrowingPromptExecutor(), TestAiFixtures.aiConfig),
            aiConfig = TestAiFixtures.aiConfig,
            documentFetcher = documentFetcher
        )

        val result = agent.process(AcceptDocumentInput.Upload(documentId = DocumentId.generate(), tenant = TestAiFixtures.tenant, associatedPersonNames = emptyList(), userFeedback = null, maxPagesOverride = null, dpiOverride = null))

        assertEquals(DocumentType.Unknown, result.classification.documentType)
        assertIs<FinancialExtractionResult.Unsupported>(result.extraction)
        assertTrue(result.auditReport.criticalFailures.any { it.type == CheckType.AI_CONTRACT })
        assertTrue(result.classification.reasoning.contains("failed", ignoreCase = true))
    }
}

private class ThrowingPromptExecutor : PromptExecutor {
    override suspend fun execute(
        prompt: Prompt,
        model: LLModel,
        tools: List<ToolDescriptor>
    ): List<Message.Response> = error("native finish tool call missing")

    override fun executeStreaming(
        prompt: Prompt,
        model: LLModel,
        tools: List<ToolDescriptor>
    ): Flow<StreamFrame> = emptyFlow()

    override suspend fun moderate(prompt: Prompt, model: LLModel): ModerationResult =
        error("unused in this test")

    override fun close() = Unit
}
