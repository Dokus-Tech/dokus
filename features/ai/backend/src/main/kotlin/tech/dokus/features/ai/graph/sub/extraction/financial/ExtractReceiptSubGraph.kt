package tech.dokus.features.ai.graph.sub.extraction.financial

import ai.koog.agents.core.dsl.builder.AIAgentSubgraphBuilderBase
import ai.koog.agents.core.dsl.builder.AIAgentSubgraphDelegate
import ai.koog.agents.core.tools.Tool
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.ext.agent.subgraphWithTask
import ai.koog.prompt.params.LLMParams
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import tech.dokus.domain.Money
import tech.dokus.domain.enums.Currency
import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.enums.PaymentMethod
import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.model.FinancialLineItem
import tech.dokus.domain.model.VatBreakdownEntry
import tech.dokus.features.ai.config.asVisionModel
import tech.dokus.features.ai.config.assistantResponseRepeatMax
import tech.dokus.features.ai.config.documentProcessing
import tech.dokus.features.ai.models.ExtractDocumentInput
import tech.dokus.features.ai.models.ExtractionToolDescriptions
import tech.dokus.features.ai.models.FinancialExtractionResult
import tech.dokus.features.ai.models.LineItemToolInput
import tech.dokus.features.ai.models.VatBreakdownToolInput
import tech.dokus.features.ai.models.toDomain
import tech.dokus.foundation.backend.config.AIConfig

@Serializable
@SerialName("ReceiptExtractionResult")
data class ReceiptExtractionResult(
    val merchantName: String?,
    val merchantVat: VatNumber?,
    val date: LocalDate?,
    val currency: Currency,
    val totalAmount: Money?,
    val vatAmount: Money?,
    val lineItems: List<FinancialLineItem> = emptyList(),
    val vatBreakdown: List<VatBreakdownEntry> = emptyList(),
    val receiptNumber: String?,
    val paymentMethod: PaymentMethod?,
    val directionHint: DocumentDirection = DocumentDirection.Unknown,
    val directionHintConfidence: Double? = null,
    val confidence: Double,
    val reasoning: String?
)

fun AIAgentSubgraphBuilderBase<*, *>.extractReceiptSubGraph(
    aiConfig: AIConfig,
): AIAgentSubgraphDelegate<ExtractDocumentInput, FinancialExtractionResult.Receipt> {
    return subgraphWithTask(
        name = "Extract receipt information",
        llmModel = aiConfig.mode.asVisionModel,
        tools = emptyList(),
        llmParams = LLMParams.documentProcessing,
        assistantResponseRepeatMax = assistantResponseRepeatMax,
        finishTool = ReceiptExtractionFinishTool()
    ) { it.receiptPrompt }
}

@Serializable
data class ReceiptExtractionToolInput(
    @property:LLMDescription(ExtractionToolDescriptions.MerchantName)
    val merchantName: String?,
    @property:LLMDescription(ExtractionToolDescriptions.MerchantVat)
    val merchantVat: String?,
    @property:LLMDescription(ExtractionToolDescriptions.ReceiptDate)
    val date: LocalDate?,
    @property:LLMDescription(ExtractionToolDescriptions.Currency)
    val currency: String = "EUR",
    @property:LLMDescription(ExtractionToolDescriptions.TotalAmount)
    val totalAmount: String?,
    @property:LLMDescription(ExtractionToolDescriptions.VatAmount)
    val vatAmount: String?,
    @property:LLMDescription(ExtractionToolDescriptions.LineItems)
    val lineItems: List<LineItemToolInput>? = null,
    @property:LLMDescription(ExtractionToolDescriptions.VatBreakdown)
    val vatBreakdown: List<VatBreakdownToolInput>? = null,
    @property:LLMDescription(ExtractionToolDescriptions.ReceiptNumber)
    val receiptNumber: String?,
    @property:LLMDescription(ExtractionToolDescriptions.PaymentMethod)
    val paymentMethod: PaymentMethod? = null,
    @property:LLMDescription(ExtractionToolDescriptions.DirectionHint)
    val directionHint: DocumentDirection = DocumentDirection.Unknown,
    @property:LLMDescription(ExtractionToolDescriptions.DirectionHintConfidence)
    val directionHintConfidence: Double? = null,
    @property:LLMDescription(ExtractionToolDescriptions.Confidence)
    val confidence: Double,
    @property:LLMDescription(ExtractionToolDescriptions.Reasoning)
    val reasoning: String? = null
)

private class ReceiptExtractionFinishTool : Tool<ReceiptExtractionToolInput, FinancialExtractionResult.Receipt>(
    argsSerializer = ReceiptExtractionToolInput.serializer(),
    resultSerializer = FinancialExtractionResult.Receipt.serializer(),
    name = "submit_receipt_extraction",
    description = "Submit extracted receipt fields from the document. Only include values you can see."
) {
    override suspend fun execute(args: ReceiptExtractionToolInput): FinancialExtractionResult.Receipt {
        return FinancialExtractionResult.Receipt(
            ReceiptExtractionResult(
                merchantName = args.merchantName,
                merchantVat = VatNumber.from(args.merchantVat),
                date = args.date,
                currency = Currency.from(args.currency),
                totalAmount = Money.from(args.totalAmount),
                vatAmount = Money.from(args.vatAmount),
                lineItems = args.lineItems.orEmpty().mapNotNull { it.toDomain() },
                vatBreakdown = args.vatBreakdown.orEmpty().mapNotNull { it.toDomain() },
                receiptNumber = args.receiptNumber,
                paymentMethod = args.paymentMethod,
                directionHint = args.directionHint,
                directionHintConfidence = args.directionHintConfidence,
                confidence = args.confidence,
                reasoning = args.reasoning
            )
        )
    }
}

private val ExtractDocumentInput.receiptPrompt
    get() = """
    You will receive receipt/ticket images in context.

    Task: extract fields from a THERMAL RECEIPT / POS TICKET (point-of-sale purchase).
    Output MUST be submitted via tool: submit_receipt_extraction.

    ## HARD RULES
    - Do NOT guess. If not visible, return null.
    - Amount fields must be numeric strings using '.' as decimal separator (e.g., "12.50").

    ## MERCHANT
    - Look for the store/merchant name at the TOP of the receipt (header/logo area).
    - Extract the actual business name, not taglines or slogans.
    - If visible, extract merchant VAT number.

    ## DATE
    - Extract transaction date if printed on receipt.
    - Often appears near top or bottom with a timestamp.

    ## VAT
    - If VAT breakdown is shown, extract vatBreakdown rows (rate, base, amount).
    - If reverse charge is indicated, use rate "0" and VAT amount "0".

    ## RECEIPT NUMBER
    - Look for ticket/receipt number, transaction ID, or similar identifier.
    - Helps with deduplication.

    ## PAYMENT METHOD
    - If "CASH", "CARTE", "CARD", "BANCONTACT", "VISA", "MASTERCARD" etc. is shown, extract it.
    - Map to: BankTransfer, CreditCard, DebitCard, PayPal, Cash, Crypto, DirectDebit, Cheque, Other, Unknown

    ## LINE ITEMS
    - Only extract lineItems if the receipt is clearly itemized; otherwise return an empty list.

    ## OPTIONAL DIRECTION HINT
    - Provide `directionHint` only when the receipt clearly indicates whether tenant is merchant or buyer.
    - Use UNKNOWN if not explicit.
    - If you provide `directionHint`, provide `directionHintConfidence` in range 0.0-1.0.

    ## LANGUAGE HINT
    Detected language hint: $language
    """.trimIndent()
