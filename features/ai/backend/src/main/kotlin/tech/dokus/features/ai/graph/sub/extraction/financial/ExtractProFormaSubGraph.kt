package tech.dokus.features.ai.graph.sub.extraction.financial

import ai.koog.agents.core.dsl.builder.AIAgentSubgraphBuilderBase
import ai.koog.agents.core.dsl.builder.AIAgentSubgraphDelegate
import ai.koog.agents.core.tools.Tool
import ai.koog.agents.ext.agent.subgraphWithTask
import ai.koog.prompt.params.LLMParams
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import tech.dokus.features.ai.config.asVisionModel
import tech.dokus.features.ai.models.ExtractDocumentInput
import tech.dokus.features.ai.models.FinancialExtractionResult
import tech.dokus.foundation.backend.config.AIConfig

fun AIAgentSubgraphBuilderBase<*, *>.extractProFormaSubGraph(
    aiConfig: AIConfig,
): AIAgentSubgraphDelegate<ExtractDocumentInput, FinancialExtractionResult.ProForma> {
    return subgraphWithTask(
        name = "Extract pro forma invoice information",
        llmModel = aiConfig.mode.asVisionModel,
        tools = emptyList(),
        llmParams = LLMParams(temperature = 0.1),
        finishTool = ProFormaExtractionFinishTool(),
    ) { it.proFormaPrompt }
}

@Serializable
@SerialName("ProFormaExtractionResult")
data class ProFormaExtractionResult(
    val proFormaNumber: String?,
    val issueDate: LocalDate?,

    val currency: String,
    val subtotalAmount: String?,
    val vatAmount: String?,
    val totalAmount: String?,

    val customerName: String?,
    val customerVat: String?,
    val customerEmail: String?,

    val confidence: Double,
    val reasoning: String?,
)

@Serializable
data class ProFormaExtractionToolInput(
    val proFormaNumber: String?,
    val issueDate: LocalDate?,
    val currency: String = "EUR",
    val subtotalAmount: String?,
    val vatAmount: String?,
    val totalAmount: String?,
    val customerName: String?,
    val customerVat: String?,
    val customerEmail: String? = null,
    val confidence: Double,
    val reasoning: String? = null,
)

private class ProFormaExtractionFinishTool : Tool<ProFormaExtractionToolInput, FinancialExtractionResult.ProForma>(
    argsSerializer = ProFormaExtractionToolInput.serializer(),
    resultSerializer = FinancialExtractionResult.ProForma.serializer(),
    name = "submit_proforma_extraction",
    description = "Submit extracted pro forma invoice fields from the document. Only include values you can see.",
) {
    override suspend fun execute(args: ProFormaExtractionToolInput): FinancialExtractionResult.ProForma {
        return FinancialExtractionResult.ProForma(
            ProFormaExtractionResult(
                proFormaNumber = args.proFormaNumber,
                issueDate = args.issueDate,
                currency = args.currency,
                subtotalAmount = args.subtotalAmount,
                vatAmount = args.vatAmount,
                totalAmount = args.totalAmount,
                customerName = args.customerName,
                customerVat = args.customerVat,
                customerEmail = args.customerEmail,
                confidence = args.confidence,
                reasoning = args.reasoning,
            )
        )
    }
}

private val ExtractDocumentInput.proFormaPrompt: String
    get() = """
    You will receive pro forma pages as images in context.

    Task: extract fields for a PRO FORMA INVOICE ("Pro forma", "Proforma").
    Output MUST be submitted via tool: submit_proforma_extraction.

    ## HARD RULES
    - Do NOT guess. If not visible, return null.
    - Amount fields must be numeric strings using '.' as decimal separator (e.g., "1234.56").
    - totalAmount = gross total if present.
    - Pro forma is informational; still extract number/date/totals as shown.

    ## IDENTIFIERS
    Extract proFormaNumber if visible ("Pro forma nr", "Proforma #").

    ## CUSTOMER
    For outgoing pro forma, issuer is the tenant; customer is billed-to/recipient party.
    Extract customerName/customerVat/customerEmail if present.

    Language hint: $language
    """.trimIndent()