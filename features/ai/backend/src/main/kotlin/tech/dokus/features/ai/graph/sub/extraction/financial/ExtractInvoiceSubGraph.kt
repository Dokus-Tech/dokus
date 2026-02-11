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
import tech.dokus.domain.enums.DocumentDirection
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

    // Parties (facts only, neutral roles)
    val sellerName: String?,
    val sellerVat: VatNumber?,
    val sellerEmail: Email? = null,
    val sellerStreet: String? = null,
    val sellerPostalCode: String? = null,
    val sellerCity: String? = null,
    val sellerCountry: String? = null,
    val buyerName: String?,
    val buyerVat: VatNumber?,
    val buyerEmail: Email? = null,
    val buyerStreet: String? = null,
    val buyerPostalCode: String? = null,
    val buyerCity: String? = null,
    val buyerCountry: String? = null,

    // Payment hints
    val iban: Iban? = null,
    val payment: CanonicalPayment? = null,

    // Optional hint used only as tie-breaker after deterministic matching.
    val directionHint: DocumentDirection = DocumentDirection.Unknown,
    val directionHintConfidence: Double? = null,

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
    @property:LLMDescription(ExtractionToolDescriptions.SellerName)
    val sellerName: String?,
    @property:LLMDescription(ExtractionToolDescriptions.SellerVat)
    val sellerVat: String?,
    @property:LLMDescription(ExtractionToolDescriptions.SellerEmail)
    val sellerEmail: String? = null,
    @property:LLMDescription(ExtractionToolDescriptions.SellerStreet)
    val sellerStreet: String? = null,
    @property:LLMDescription(ExtractionToolDescriptions.SellerPostalCode)
    val sellerPostalCode: String? = null,
    @property:LLMDescription(ExtractionToolDescriptions.SellerCity)
    val sellerCity: String? = null,
    @property:LLMDescription(ExtractionToolDescriptions.SellerCountry)
    val sellerCountry: String? = null,
    @property:LLMDescription(ExtractionToolDescriptions.BuyerName)
    val buyerName: String?,
    @property:LLMDescription(ExtractionToolDescriptions.BuyerVat)
    val buyerVat: String?,
    @property:LLMDescription(ExtractionToolDescriptions.BuyerEmail)
    val buyerEmail: String? = null,
    @property:LLMDescription(ExtractionToolDescriptions.BuyerStreet)
    val buyerStreet: String? = null,
    @property:LLMDescription(ExtractionToolDescriptions.BuyerPostalCode)
    val buyerPostalCode: String? = null,
    @property:LLMDescription(ExtractionToolDescriptions.BuyerCity)
    val buyerCity: String? = null,
    @property:LLMDescription(ExtractionToolDescriptions.BuyerCountry)
    val buyerCountry: String? = null,
    @property:LLMDescription(ExtractionToolDescriptions.Iban)
    val iban: String? = null,
    @property:LLMDescription(ExtractionToolDescriptions.PaymentReference)
    val paymentReference: String? = null,
    @property:LLMDescription(ExtractionToolDescriptions.DirectionHint)
    val directionHint: DocumentDirection = DocumentDirection.Unknown,
    @property:LLMDescription(ExtractionToolDescriptions.DirectionHintConfidence)
    val directionHintConfidence: Double? = null,
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
                sellerName = args.sellerName,
                sellerVat = VatNumber.from(args.sellerVat),
                sellerEmail = Email.from(args.sellerEmail),
                sellerStreet = args.sellerStreet,
                sellerPostalCode = args.sellerPostalCode,
                sellerCity = args.sellerCity,
                sellerCountry = args.sellerCountry,
                buyerName = args.buyerName,
                buyerVat = VatNumber.from(args.buyerVat),
                buyerEmail = Email.from(args.buyerEmail),
                buyerStreet = args.buyerStreet,
                buyerPostalCode = args.buyerPostalCode,
                buyerCity = args.buyerCity,
                buyerCountry = args.buyerCountry,
                iban = Iban.from(args.iban),
                payment = CanonicalPayment.from(args.paymentReference),
                directionHint = args.directionHint,
                directionHintConfidence = args.directionHintConfidence,
                confidence = args.confidence,
                reasoning = args.reasoning
            )
        )
    }
}

private val ExtractDocumentInput.prompt
    get() = """
    You will receive invoice pages as images in context.

    Task: extract invoice fields as neutral facts from the document.
    Do NOT decide incoming/outgoing business direction here.
    Output MUST be submitted via tool: submit_invoice_extraction.

    ## HARD RULES
    - Do NOT guess. If not visible, return null.
    - Amount fields must be numeric strings using '.' as decimal separator (e.g., "1234.56").
    - If multiple totals exist, prefer the "Total" / "Totaal" / "Total TTC" style final payable amount.

    ## PARTY EXTRACTION (CRITICAL)
    - `seller*`: entity that ISSUED the invoice (header/logo issuer area).
    - `buyer*`: billed-to/recipient entity ("Recipient", "Client", "Klant", etc).
    - Always extract both seller and buyer when visible.
    - Do not swap seller/buyer based on tenant context.

    ## OPTIONAL DIRECTION HINT
    - Provide `directionHint` only if direction is explicit from the paper (e.g., clear billed-to vs issuer roles).
    - Do not infer from assumptions; use UNKNOWN when unsure.
    - If you provide `directionHint`, provide `directionHintConfidence` in range 0.0-1.0.

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
