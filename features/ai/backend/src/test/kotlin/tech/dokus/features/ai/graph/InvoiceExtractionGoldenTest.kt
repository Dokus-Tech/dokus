package tech.dokus.features.ai.graph

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.annotation.ExperimentalAgentsApi
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.strategy
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.features.ai.config.AIProviderFactory
import tech.dokus.features.ai.config.asVisionModel
import tech.dokus.features.ai.graph.sub.documentProcessingSubGraph
import tech.dokus.features.ai.models.DocumentAiProcessingResult
import tech.dokus.features.ai.models.FinancialExtractionResult
import tech.dokus.features.ai.services.DocumentFetcher
import tech.dokus.features.ai.services.DocumentFetcher.FetchedDocumentData
import tech.dokus.features.ai.tools.TenantDocumentsRegistry
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.ExperimentalUuidApi

class InvoiceExtractionGoldenTest {

    @OptIn(ExperimentalUuidApi::class, ExperimentalAgentsApi::class)
    @Tag("ai")
    @Test
    fun `extracts line items and vat breakdown for test invoice`() = runBlocking {
        val pdfFile = TestAiFixtures.loadFixture("fixtures/invoices/test-invoice.pdf")
        val documentBytes = pdfFile.readBytes()

        val mockFetcher = DocumentFetcher { _, _ ->
            Result.success(FetchedDocumentData(documentBytes, "application/pdf"))
        }

        val tenantId = TenantId.generate()
        val toolRegistry = TenantDocumentsRegistry(tenantId, mockFetcher)

        val strategy = strategy<AcceptDocumentInput, DocumentAiProcessingResult>("test") {
            val process by documentProcessingSubGraph(TestAiFixtures.aiConfig, mockFetcher)
            edge(nodeStart forwardTo process)
            edge(process forwardTo nodeFinish)
        }

        val agent = AIAgent(
            promptExecutor = AIProviderFactory.createOpenAiExecutor(TestAiFixtures.aiConfig),
            toolRegistry = toolRegistry,
            strategy = strategy,
            agentConfig = AIAgentConfig.withSystemPrompt(
                prompt = "You are a document processor.",
                llm = TestAiFixtures.aiConfig.mode.asVisionModel,
                maxAgentIterations = TestAiFixtures.aiConfig.mode.maxIterations
            )
        )

        val result = withTimeout(180.seconds) {
            agent.run(AcceptDocumentInput(DocumentId.generate(), TestAiFixtures.tenant))
        }

        assertEquals(
            DocumentType.Bill,
            result.classification.documentType,
            "Expected bill classification for supplier invoice"
        )
        assertTrue(
            result.extraction is FinancialExtractionResult.Bill,
            "Expected bill extraction, got ${result.extraction::class.simpleName}"
        )

        val bill = result.extraction.data

        assertEquals(bill.invoiceNumber?.contains("7647894"), true, "Expected invoice number 7647894")
        assertEquals(523L, bill.vatAmount?.minor, "Expected VAT amount 5.23")
        assertWithinCents(16397L, bill.totalAmount?.minor, 2L, "Expected total amount ~163.97")

        assertEquals(2, bill.lineItems.size, "Expected 2 line items")

        val charging = bill.lineItems.firstOrNull { it.description.contains("Charging costs", ignoreCase = true) }
        assertNotNull(charging, "Expected Charging costs line item")
        assertEquals(15606L, charging.netAmount, "Expected Charging costs net amount 156.06")

        val pricePerPeriod = bill.lineItems.firstOrNull { it.description.contains("Price per Period", ignoreCase = true) }
        assertNotNull(pricePerPeriod, "Expected Price per Period line item")
        assertEquals(1L, pricePerPeriod.quantity, "Expected quantity 1")
        assertEquals(267L, pricePerPeriod.unitPrice, "Expected unit price 2.67")
        assertEquals(267L, pricePerPeriod.netAmount, "Expected net amount 2.67")

        val lineItemsNetSum = bill.lineItems.sumOf { it.netAmount ?: 0L }
        assertEquals(15873L, lineItemsNetSum, "Expected line items net sum 158.73")

        assertTrue(
            bill.vatBreakdown.isNotEmpty(),
            "Expected VAT breakdown rows"
        )

        val rate21 = bill.vatBreakdown.firstOrNull { it.rate == 2100 }
        assertNotNull(rate21, "Expected 21% VAT breakdown")
        assertEquals(2491L, rate21.base, "Expected 21% base 24.91")
        assertEquals(523L, rate21.amount, "Expected 21% VAT amount 5.23")

        val reverseChargeEntries = bill.vatBreakdown.filter { it.rate == 0 }
        assertTrue(reverseChargeEntries.isNotEmpty(), "Expected reverse charge entries with 0% rate")
        val reverseChargeBaseSum = reverseChargeEntries.sumOf { it.base }
        val reverseChargeAmountSum = reverseChargeEntries.sumOf { it.amount }
        assertEquals(13382L, reverseChargeBaseSum, "Expected reverse charge base sum 133.82")
        assertEquals(0L, reverseChargeAmountSum, "Expected reverse charge VAT amount 0.00")
    }

    private fun assertWithinCents(expected: Long, actual: Long?, tolerance: Long, message: String) {
        val value = actual ?: error("Missing amount for: $message")
        val diff = kotlin.math.abs(expected - value)
        assertTrue(diff <= tolerance, "$message (expected $expected, got $value)")
    }
}
