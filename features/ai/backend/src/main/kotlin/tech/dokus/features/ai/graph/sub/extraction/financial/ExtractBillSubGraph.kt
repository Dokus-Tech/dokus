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
import tech.dokus.domain.ids.Iban
import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.model.CanonicalPayment
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

fun AIAgentSubgraphBuilderBase<*, *>.extractBillSubGraph(
    aiConfig: AIConfig,
): AIAgentSubgraphDelegate<ExtractDocumentInput, FinancialExtractionResult.Bill> {
    return subgraphWithTask(
        name = "Extract bill information",
        llmModel = aiConfig.mode.asVisionModel,
        tools = emptyList(),
        llmParams = LLMParams.documentProcessing,
        assistantResponseRepeatMax = assistantResponseRepeatMax,
        finishTool = BillExtractionFinishTool(),
    ) { it.billPrompt }
}

@Serializable
@SerialName("BillExtractionResult")
data class BillExtractionResult(
    val supplierName: String?,
    val supplierVat: VatNumber?,
    val invoiceNumber: String?,
    val issueDate: LocalDate?,
    val dueDate: LocalDate?,
    val currency: Currency,
    val totalAmount: Money?,
    val vatAmount: Money?,
    val lineItems: List<FinancialLineItem> = emptyList(),
    val vatBreakdown: List<VatBreakdownEntry> = emptyList(),
    val iban: Iban?,
    val payment: CanonicalPayment?,
    val confidence: Double,
    val reasoning: String?,
)

@Serializable
data class BillExtractionToolInput(
    @property:LLMDescription(ExtractionToolDescriptions.SupplierName)
    val supplierName: String?,
    @property:LLMDescription(ExtractionToolDescriptions.SupplierVat)
    val supplierVat: String?,
    @property:LLMDescription(ExtractionToolDescriptions.InvoiceNumber)
    val invoiceNumber: String?,
    @property:LLMDescription(ExtractionToolDescriptions.IssueDate)
    val issueDate: LocalDate?,
    @property:LLMDescription(ExtractionToolDescriptions.DueDate)
    val dueDate: LocalDate?,
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
    @property:LLMDescription(ExtractionToolDescriptions.Iban)
    val iban: String? = null,
    @property:LLMDescription(ExtractionToolDescriptions.PaymentReference)
    val paymentReference: String? = null,
    @property:LLMDescription(ExtractionToolDescriptions.Confidence)
    val confidence: Double,
    @property:LLMDescription(ExtractionToolDescriptions.Reasoning)
    val reasoning: String? = null,
)

private class BillExtractionFinishTool : Tool<BillExtractionToolInput, FinancialExtractionResult.Bill>(
    argsSerializer = BillExtractionToolInput.serializer(),
    resultSerializer = FinancialExtractionResult.Bill.serializer(),
    name = "submit_bill_extraction",
    description = "Submit extracted bill fields from the document. Only include values you can see.",
) {
    override suspend fun execute(args: BillExtractionToolInput): FinancialExtractionResult.Bill {
        return FinancialExtractionResult.Bill(
            BillExtractionResult(
                supplierName = args.supplierName,
                supplierVat = VatNumber.from(args.supplierVat),
                invoiceNumber = args.invoiceNumber,
                issueDate = args.issueDate,
                dueDate = args.dueDate,
                currency = Currency.from(args.currency),
                totalAmount = Money.from(args.totalAmount),
                vatAmount = Money.from(args.vatAmount),
                lineItems = args.lineItems.orEmpty().mapNotNull { it.toDomain() },
                vatBreakdown = args.vatBreakdown.orEmpty().mapNotNull { it.toDomain() },
                iban = Iban.from(args.iban),
                payment = CanonicalPayment.from(args.paymentReference),
                confidence = args.confidence,
                reasoning = args.reasoning,
            )
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
    - If VAT breakdown is shown, extract it as vatBreakdown (rate/base/amount).

    ## PARTY DETECTION
    BILL means:
    - Supplier/vendor is the ISSUER (header/logo).
    - We are the customer (often in "Klant/Client" section).

    ## DATE RULES
    Identify issue date ("Factuurdatum", "Date de facture", "Invoice date") and due date ("Vervaldatum", "Échéance", "Due date").
    If due date is not present, keep null.

    ## PAYMENT
    Extract IBAN and structured reference/communication if present.

    ## LINE ITEMS
    If an itemized table is present, extract lineItems with description, quantity, unitPrice, vatRate, netAmount.
    If no clear itemization, return an empty list.

    ## VAT BREAKDOWN
    If a VAT breakdown table is present, extract vatBreakdown rows (rate, base, amount).
    If reverse charge is indicated, use rate "0" and VAT amount "0".
    If not shown, return an empty list.

    Language hint: $language
    """.trimIndent()
