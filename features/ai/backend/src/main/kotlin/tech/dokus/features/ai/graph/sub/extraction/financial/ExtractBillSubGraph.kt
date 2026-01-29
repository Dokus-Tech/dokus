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
import tech.dokus.foundation.backend.config.AIConfig

fun AIAgentSubgraphBuilderBase<*, *>.extractBillSubGraph(
    aiConfig: AIConfig,
): AIAgentSubgraphDelegate<ExtractDocumentInput, BillExtractionResult> {
    return subgraphWithTask(
        name = "Extract bill information",
        llmModel = aiConfig.mode.asVisionModel,
        tools = emptyList(),
        llmParams = LLMParams(temperature = 0.1),
        finishTool = BillExtractionFinishTool(),
    ) { it.billPrompt }
}

@Serializable
@SerialName("BillExtractionResult")
data class BillExtractionResult(
    val supplierName: String?,
    val supplierVat: String?,
    val invoiceNumber: String?,
    val issueDate: LocalDate?,
    val dueDate: LocalDate?,
    val currency: String,          // "EUR"
    val totalAmount: String?,      // gross total payable
    val vatAmount: String?,        // total VAT, if present
    val vatRatePercent: String?,   // e.g. "21", "6", or null if unclear/mixed
    val iban: String?,
    val paymentReference: String?,
    val confidence: Double,
    val reasoning: String?,
)

@Serializable
data class BillExtractionToolInput(
    val supplierName: String?,
    val supplierVat: String?,
    val invoiceNumber: String?,
    val issueDate: LocalDate?,
    val dueDate: LocalDate?,
    val currency: String = "EUR",
    val totalAmount: String?,
    val vatAmount: String?,
    val vatRatePercent: String? = null,
    val iban: String? = null,
    val paymentReference: String? = null,
    val confidence: Double,
    val reasoning: String? = null,
)

private class BillExtractionFinishTool : Tool<BillExtractionToolInput, BillExtractionResult>(
    argsSerializer = BillExtractionToolInput.serializer(),
    resultSerializer = BillExtractionResult.serializer(),
    name = "submit_bill_extraction",
    description = "Submit extracted bill fields from the document. Only include values you can see.",
) {
    override suspend fun execute(args: BillExtractionToolInput): BillExtractionResult {
        return BillExtractionResult(
            supplierName = args.supplierName,
            supplierVat = args.supplierVat,
            invoiceNumber = args.invoiceNumber,
            issueDate = args.issueDate,
            dueDate = args.dueDate,
            currency = args.currency,
            totalAmount = args.totalAmount,
            vatAmount = args.vatAmount,
            vatRatePercent = args.vatRatePercent,
            iban = args.iban,
            paymentReference = args.paymentReference,
            confidence = args.confidence,
            reasoning = args.reasoning,
        )
    }
}

private val ExtractDocumentInput.billPrompt: String
    get() = """
    You will receive bill pages as images in context.

    Task: extract fields for an INCOMING supplier invoice / BILL (we receive it from a supplier).
    Output MUST be submitted via tool: submit_bill_extraction.

    ## HARD RULES
    - Do NOT guess. If not visible, return null.
    - Amount fields must be numeric strings using '.' as decimal separator (e.g., "1234.56").
    - totalAmount = gross total payable (final total).
    - vatAmount = total VAT amount, if present.
    - vatRatePercent: if there is a single clear VAT rate, return it like "21". If multiple rates or unclear, return null.

    ## PARTY DETECTION
    BILL means:
    - Supplier/vendor is the ISSUER (header/logo).
    - We are the customer (often in "Klant/Client" section).
    Extract SUPPLIER fields only (supplierName, supplierVat).

    ## DATE RULES
    - issueDate: invoice issue date ("Factuurdatum", "Date de facture", "Invoice date")
    - dueDate: payment due ("Vervaldatum", "Échéance", "Due date")
    If due date is not present, keep null.

    ## PAYMENT
    Extract IBAN and structured reference/communication if present.

    Language hint: $language
    """.trimIndent()