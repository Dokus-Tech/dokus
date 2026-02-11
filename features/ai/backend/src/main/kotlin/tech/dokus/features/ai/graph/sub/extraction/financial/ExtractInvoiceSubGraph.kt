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
import tech.dokus.domain.Email
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

@Serializable
@SerialName("InvoiceExtractionResult")
data class InvoiceExtractionResult(
    val invoiceNumber: String?,

    // Dates
    val issueDate: LocalDate?,
    val dueDate: LocalDate?,

    // Amounts
    val currency: Currency,
    val subtotalAmount: Money?,
    val vatAmount: Money?,
    val totalAmount: Money?,

    // Line items & VAT breakdown
    val lineItems: List<FinancialLineItem> = emptyList(),
    val vatBreakdown: List<VatBreakdownEntry> = emptyList(),

    // Parties (facts only)
    val customerName: String?,
    val customerVat: VatNumber?,
    val customerEmail: Email? = null,

    // Payment hints
    val iban: Iban? = null,
    val payment: CanonicalPayment? = null,

    // Evidence/quality
    val confidence: Double,
    val reasoning: String? = null
)

fun AIAgentSubgraphBuilderBase<*, *>.extractInvoiceSubGraph(
    aiConfig: AIConfig,
    tools: List<Tool<*, *>>
): AIAgentSubgraphDelegate<ExtractDocumentInput, FinancialExtractionResult.Invoice> {
    return subgraphWithTask(
        name = "Extract invoice information",
        llmModel = aiConfig.mode.asVisionModel,
        tools = tools,
        llmParams = LLMParams.documentProcessing,
        assistantResponseRepeatMax = assistantResponseRepeatMax,
        finishTool = InvoiceExtractionFinishTool()
    ) { it.prompt }
}

@Serializable
data class InvoiceExtractionToolInput(
    @property:LLMDescription(ExtractionToolDescriptions.InvoiceNumber)
    val invoiceNumber: String?,
    @property:LLMDescription(ExtractionToolDescriptions.IssueDate)
    val issueDate: LocalDate?,
    @property:LLMDescription(ExtractionToolDescriptions.DueDate)
    val dueDate: LocalDate?,
    @property:LLMDescription(ExtractionToolDescriptions.Currency)
    val currency: String = "EUR",
    @property:LLMDescription(ExtractionToolDescriptions.SubtotalAmount)
    val subtotalAmount: String?,
    @property:LLMDescription(ExtractionToolDescriptions.VatAmount)
    val vatAmount: String?,
    @property:LLMDescription(ExtractionToolDescriptions.TotalAmount)
    val totalAmount: String?,
    @property:LLMDescription(ExtractionToolDescriptions.LineItems)
    val lineItems: List<LineItemToolInput>? = null,
    @property:LLMDescription(ExtractionToolDescriptions.VatBreakdown)
    val vatBreakdown: List<VatBreakdownToolInput>? = null,
    @property:LLMDescription(ExtractionToolDescriptions.CustomerName)
    val customerName: String?,
    @property:LLMDescription(ExtractionToolDescriptions.CustomerVat)
    val customerVat: String?,
    @property:LLMDescription(ExtractionToolDescriptions.CustomerEmail)
    val customerEmail: String? = null,
    @property:LLMDescription(ExtractionToolDescriptions.Iban)
    val iban: String? = null,
    @property:LLMDescription(ExtractionToolDescriptions.PaymentReference)
    val paymentReference: String? = null,
    @property:LLMDescription(ExtractionToolDescriptions.Confidence)
    val confidence: Double,
    @property:LLMDescription(ExtractionToolDescriptions.Reasoning)
    val reasoning: String? = null
)

private class InvoiceExtractionFinishTool : Tool<InvoiceExtractionToolInput, FinancialExtractionResult.Invoice>(
    argsSerializer = InvoiceExtractionToolInput.serializer(),
    resultSerializer = FinancialExtractionResult.Invoice.serializer(),
    name = "submit_invoice_extraction",
    description = "Submit extracted invoice fields from the document. Only include values you can see."
) {
    override suspend fun execute(args: InvoiceExtractionToolInput): FinancialExtractionResult.Invoice {
        return FinancialExtractionResult.Invoice(
            InvoiceExtractionResult(
                invoiceNumber = args.invoiceNumber,
                issueDate = args.issueDate,
                dueDate = args.dueDate,
                currency = Currency.from(args.currency),
                subtotalAmount = Money.from(args.subtotalAmount),
                vatAmount = Money.from(args.vatAmount),
                totalAmount = Money.from(args.totalAmount),
                lineItems = args.lineItems.orEmpty().mapNotNull { it.toDomain() },
                vatBreakdown = args.vatBreakdown.orEmpty().mapNotNull { it.toDomain() },
                customerName = args.customerName,
                customerVat = VatNumber.from(args.customerVat),
                customerEmail = Email.from(args.customerEmail),
                iban = Iban.from(args.iban),
                payment = CanonicalPayment.from(args.paymentReference),
                confidence = args.confidence,
                reasoning = args.reasoning
            )
        )
    }
}

private val ExtractDocumentInput.prompt
    get() = """
    You will receive invoice pages as images in context.

    Task: extract invoice fields for an OUTGOING INVOICE (issued by the tenant).
    Output MUST be submitted via tool: submit_invoice_extraction.

    ## HARD RULES
    - Do NOT guess. If not visible, return null.
    - Amount fields must be numeric strings using '.' as decimal separator (e.g., "1234.56").
    - If multiple totals exist, prefer the "Total" / "Totaal" / "Total TTC" style final payable amount.

    ## PARTY DETECTION
    OUTGOING INVOICE means:
    - Issuer is the tenant (your company) in header/logo area.
    - Customer is the billed-to party ("Client", "Klant", "Bill to", etc).

    ## DATE RULES
    Identify issue date ("Factuurdatum", "Date de facture", "Invoice date") and due date ("Vervaldatum", "Échéance", "Due date").
    If only one date is present and it clearly is the invoice date, set issueDate and leave dueDate null.

    ## PAYMENT FIELDS
    If IBAN or structured reference is present, extract it. Otherwise null.

    ## LINE ITEMS
    If an itemized table is present, extract lineItems with description, quantity, unitPrice, vatRate, netAmount (line total excl VAT).
    If no clear itemization, return an empty list.

    ## VAT BREAKDOWN
    If a VAT breakdown table is present, extract vatBreakdown rows (rate, base, amount).
    If reverse charge is indicated, use rate "0" and VAT amount "0".
    If not shown, return an empty list.

    ## LANGUAGE HINT
    Detected language hint: $language
    """.trimIndent()
