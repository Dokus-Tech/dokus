package tech.dokus.features.ai.graph

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.annotation.ExperimentalAgentsApi
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.strategy
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.datetime.LocalDateTime
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import tech.dokus.domain.DisplayName
import tech.dokus.domain.LegalName
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.enums.Language
import tech.dokus.domain.enums.SubscriptionTier
import tech.dokus.domain.enums.TenantStatus
import tech.dokus.domain.enums.TenantType
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.model.Tenant
import tech.dokus.features.ai.config.AIProviderFactory
import tech.dokus.features.ai.config.asVisionModel
import tech.dokus.features.ai.graph.nodes.documentImagesInjectorNode
import tech.dokus.features.ai.graph.nodes.tenantContextInjectorNode
import tech.dokus.features.ai.graph.sub.ClassificationResult
import tech.dokus.features.ai.graph.sub.ClassifyDocumentInput
import tech.dokus.features.ai.graph.sub.classifyDocumentSubGraph
import tech.dokus.features.ai.orchestrator.DocumentFetcher
import tech.dokus.features.ai.orchestrator.DocumentFetcher.FetchedDocumentData
import tech.dokus.features.ai.tools.TenantDocumentsRegistry
import tech.dokus.foundation.backend.config.AIConfig
import tech.dokus.foundation.backend.config.IntelligenceMode
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.ExperimentalUuidApi

private val testAiConfig = AIConfig(
    mode = IntelligenceMode.Sovereign,
    ollamaHost = "",
    lmStudioHost = "http://192.168.0.150:1234"
)

private val testTenant = Tenant(
    id = TenantId.generate(),
    type = TenantType.Company,
    legalName = LegalName("Invoid Vision B.V."),
    displayName = DisplayName("Invoid Vision"),
    subscription = SubscriptionTier.CoreFounder,
    status = TenantStatus.Active,
    language = Language.En,
    vatNumber = VatNumber(""),
    createdAt = LocalDateTime(2024, 1, 1, 10, 0),
    updatedAt = LocalDateTime(2024, 1, 1, 10, 0),
)

/**
 * Folder name -> Expected DocumentType mapping
 */
private val folderToDocumentType = mapOf(
    "invoices" to DocumentType.Invoice,
    "receipts" to DocumentType.Receipt,
    "credit-notes" to DocumentType.CreditNote,
    "quotes" to DocumentType.Quote,
    "contracts" to DocumentType.Contract,
    "bank-statements" to DocumentType.BankStatement,
    "salary" to DocumentType.SalarySlip,
    "self-employed-contribution" to DocumentType.SelfEmployedContribution,
    "vat-return" to DocumentType.VatReturn,
)

class ClassificationGraphTest {

    @OptIn(ExperimentalUuidApi::class)
    @TestFactory
    fun `classify documents from fixtures`(): List<DynamicTest> {
        val fixturesPath = "fixtures"
        val fixturesUrl = ClassLoader.getSystemResource(fixturesPath)
            ?: error("Fixtures directory not found: $fixturesPath")

        val fixturesDir = File(fixturesUrl.toURI())

        return fixturesDir.listFiles { file -> file.isDirectory }
            ?.flatMap { folder ->
                val expectedType = folderToDocumentType[folder.name]
                    ?: return@flatMap emptyList()

                folder.listFiles { file -> file.extension == "pdf" }
                    ?.map { pdfFile ->
                        DynamicTest.dynamicTest("${folder.name}/${pdfFile.name} -> $expectedType") {
                            runClassificationTest(pdfFile, expectedType)
                        }
                    } ?: emptyList()
            } ?: emptyList()
    }

    @OptIn(ExperimentalUuidApi::class, ExperimentalAgentsApi::class)
    private fun runClassificationTest(pdfFile: File, expectedType: DocumentType) = runBlocking {
        val documentBytes = pdfFile.readBytes()

        val mockFetcher = DocumentFetcher { _, _ ->
            Result.success(FetchedDocumentData(documentBytes, "application/pdf"))
        }

        val tenantId = TenantId.generate()
        val toolRegistry = TenantDocumentsRegistry(tenantId, mockFetcher)

        val strategy = strategy<AcceptDocumentInput, ClassificationResult>("test") {
            val classify by classifyDocumentSubGraph(testAiConfig)
            val injectImages by documentImagesInjectorNode<AcceptDocumentInput>(mockFetcher)
            val injectTenant by tenantContextInjectorNode<AcceptDocumentInput>(testTenant)

            // Transform AcceptDocumentInput → ClassifyDocumentInput
            val prepareClassifyInput by node<AcceptDocumentInput, ClassifyDocumentInput>("prepare-classify") { input ->
                ClassifyDocumentInput(input.documentId, input.tenant)
            }

            // Context setup
            edge(nodeStart forwardTo injectTenant)
            edge(injectTenant forwardTo injectImages)
            edge(injectImages forwardTo prepareClassifyInput)

            // Classification
            edge(prepareClassifyInput forwardTo classify)

            edge(classify forwardTo nodeFinish)
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

        val result = withTimeout(120.seconds) {
            try {
                agent.run(AcceptDocumentInput(DocumentId.generate(), testTenant))
            } finally {
                runCatching { agent.close() }
            }
        }

        assertEquals(expectedType, result.documentType, "File: ${pdfFile.name}")
        assertTrue(result.confidence >= 0.7, "Low confidence ${result.confidence} for ${pdfFile.name}")
    }
}