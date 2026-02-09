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
import tech.dokus.domain.VatRate
import tech.dokus.domain.enums.Currency
import tech.dokus.domain.enums.PaymentMethod
import tech.dokus.features.ai.config.asVisionModel
import tech.dokus.features.ai.models.ExtractDocumentInput
import tech.dokus.features.ai.models.FinancialExtractionResult
import tech.dokus.foundation.backend.config.AIConfig

@Serializable
@SerialName("ReceiptExtractionResult")
data class ReceiptExtractionResult(
    val merchantName: String?,
    val date: LocalDate?,
    val currency: Currency,
    val totalAmount: Money?,
    val vatAmount: Money?,
    val vatRate: VatRate?,
    val receiptNumber: String?,
    val paymentMethod: PaymentMethod?,
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
        llmParams = LLMParams(temperature = 0.1),
        finishTool = ReceiptExtractionFinishTool()
    ) { it.receiptPrompt }
}

@Serializable
data class ReceiptExtractionToolInput(
    @property:LLMDescription("Merchant/store name from the receipt header. Null if not visible.")
    val merchantName: String?,
    @property:LLMDescription("Transaction date. Null if not visible.")
    val date: LocalDate?,
    @property:LLMDescription("Currency code like EUR. If symbol only, infer best guess.")
    val currency: String = "EUR",
    @property:LLMDescription("Total amount paid. Use plain number string (e.g. 12.50). Null if not present.")
    val totalAmount: String?,
    @property:LLMDescription("VAT amount if shown separately. Use plain number string. Null if not present.")
    val vatAmount: String?,
    @property:LLMDescription("VAT rate percentage if visible (e.g. '6', '12', '21'). Null if unclear or not shown.")
    val vatRate: String?,
    @property:LLMDescription("Receipt/ticket number for identification. Null if not visible.")
    val receiptNumber: String?,
    @property:LLMDescription("Payment method if visible on receipt.")
    val paymentMethod: PaymentMethod? = null,
    @property:LLMDescription("Confidence score 0.0-1.0 for the extraction quality.")
    val confidence: Double,
    @property:LLMDescription("Short reasoning: what you used to extract the total/date/merchant.")
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
                date = args.date,
                currency = Currency.from(args.currency),
                totalAmount = Money.from(args.totalAmount),
                vatAmount = Money.from(args.vatAmount),
                vatRate = VatRate.from(args.vatRate),
                receiptNumber = args.receiptNumber,
                paymentMethod = args.paymentMethod,
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
    - totalAmount = final amount paid (the largest total, often at bottom).

    ## MERCHANT
    - Look for the store/merchant name at the TOP of the receipt (header/logo area).
    - Extract the actual business name, not taglines or slogans.

    ## DATE
    - Extract transaction date if printed on receipt.
    - Often appears near top or bottom with a timestamp.

    ## VAT
    - If VAT breakdown is shown, extract vatAmount and vatRate.
    - Belgian receipts often show "BTW" with 6%, 12%, or 21% rates.
    - If multiple VAT rates shown, return null for vatRate.

    ## RECEIPT NUMBER
    - Look for ticket/receipt number, transaction ID, or similar identifier.
    - Helps with deduplication.

    ## PAYMENT METHOD
    - If "CASH", "CARTE", "CARD", "BANCONTACT", "VISA", "MASTERCARD" etc. is shown, extract it.
    - Map to: BankTransfer, CreditCard, DebitCard, PayPal, Cash, Crypto, DirectDebit, Cheque, Other, Unknown

    ## LANGUAGE HINT
    Detected language hint: $language
    """.trimIndent()
