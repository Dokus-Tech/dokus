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
import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.model.FinancialLineItem
import tech.dokus.domain.model.VatBreakdownEntry
import tech.dokus.features.ai.config.asVisionModel
import tech.dokus.features.ai.config.assistantResponseRepeatMax
import tech.dokus.features.ai.config.documentProcessing
import tech.dokus.features.ai.models.ExtractDocumentInput
import tech.dokus.features.ai.models.FinancialExtractionResult
import tech.dokus.features.ai.models.ExtractionToolDescriptions
import tech.dokus.features.ai.models.LineItemToolInput
import tech.dokus.features.ai.models.CounterpartyExtraction
import tech.dokus.features.ai.models.CounterpartyFields
import tech.dokus.features.ai.models.CounterpartyRole
import tech.dokus.features.ai.models.toCounterpartyExtraction
import tech.dokus.features.ai.models.VatBreakdownToolInput
import tech.dokus.features.ai.models.toDomain
import tech.dokus.foundation.backend.config.AIConfig

fun AIAgentSubgraphBuilderBase<*, *>.extractCreditNoteSubGraph(
    aiConfig: AIConfig,
): AIAgentSubgraphDelegate<ExtractDocumentInput, FinancialExtractionResult.CreditNote> {
    return subgraphWithTask(
        name = "Extract credit note information",
        llmModel = aiConfig.mode.asVisionModel,
        tools = emptyList(),
        llmParams = LLMParams.documentProcessing,
        assistantResponseRepeatMax = assistantResponseRepeatMax,
        finishTool = CreditNoteExtractionFinishTool(),
    ) { it.creditNotePrompt }
}

@Serializable
@SerialName("CreditNoteExtractionResult")
data class CreditNoteExtractionResult(
    val creditNoteNumber: String?,

    val issueDate: LocalDate?,

    val currency: Currency,
    val subtotalAmount: Money?,
    val vatAmount: Money?,
    val totalAmount: Money?,

    val lineItems: List<FinancialLineItem> = emptyList(),
    val vatBreakdown: List<VatBreakdownEntry> = emptyList(),

    // Neutral parties (facts only)
    val sellerName: String?,
    val sellerVat: VatNumber?,
    val buyerName: String?,
    val buyerVat: VatNumber?,

    // Authoritative counterparty identity (used by deterministic contact resolution)
    val counterparty: CounterpartyExtraction? = null,

    // Optional tie-breaker hint (never overrides VAT evidence)
    val directionHint: DocumentDirection = DocumentDirection.Unknown,
    val directionHintConfidence: Double? = null,

    val originalInvoiceNumber: String?,   // if referenced
    val reason: String?,                  // if explicit (e.g. "Remboursement", "Retour")

    val confidence: Double,
    val reasoning: String?,
)

@Serializable
data class CreditNoteExtractionToolInput(
    @property:LLMDescription(ExtractionToolDescriptions.CreditNoteNumber)
    val creditNoteNumber: String?,
    @property:LLMDescription(ExtractionToolDescriptions.IssueDate)
    val issueDate: LocalDate?,
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
    @property:LLMDescription(ExtractionToolDescriptions.BuyerName)
    val buyerName: String?,
    @property:LLMDescription(ExtractionToolDescriptions.BuyerVat)
    val buyerVat: String?,
    @property:LLMDescription(ExtractionToolDescriptions.CounterpartyName)
    override val counterpartyName: String? = null,
    @property:LLMDescription(ExtractionToolDescriptions.CounterpartyVat)
    override val counterpartyVat: String? = null,
    @property:LLMDescription(ExtractionToolDescriptions.CounterpartyEmail)
    override val counterpartyEmail: String? = null,
    @property:LLMDescription(ExtractionToolDescriptions.CounterpartyStreet)
    override val counterpartyStreet: String? = null,
    @property:LLMDescription(ExtractionToolDescriptions.CounterpartyPostalCode)
    override val counterpartyPostalCode: String? = null,
    @property:LLMDescription(ExtractionToolDescriptions.CounterpartyCity)
    override val counterpartyCity: String? = null,
    @property:LLMDescription(ExtractionToolDescriptions.CounterpartyCountry)
    override val counterpartyCountry: String? = null,
    @property:LLMDescription(ExtractionToolDescriptions.CounterpartyRole)
    override val counterpartyRole: CounterpartyRole = CounterpartyRole.Unknown,
    @property:LLMDescription(ExtractionToolDescriptions.CounterpartyReasoning)
    override val counterpartyReasoning: String? = null,
    @property:LLMDescription(ExtractionToolDescriptions.DirectionHint)
    val directionHint: DocumentDirection = DocumentDirection.Unknown,
    @property:LLMDescription(ExtractionToolDescriptions.DirectionHintConfidence)
    val directionHintConfidence: Double? = null,
    @property:LLMDescription(ExtractionToolDescriptions.OriginalInvoiceNumber)
    val originalInvoiceNumber: String? = null,
    @property:LLMDescription(ExtractionToolDescriptions.CreditNoteReason)
    val reason: String? = null,
    @property:LLMDescription(ExtractionToolDescriptions.Confidence)
    val confidence: Double,
    @property:LLMDescription(ExtractionToolDescriptions.Reasoning)
    val reasoning: String? = null,
) : CounterpartyFields

private class CreditNoteExtractionFinishTool :
    Tool<CreditNoteExtractionToolInput, FinancialExtractionResult.CreditNote>(
        argsSerializer = CreditNoteExtractionToolInput.serializer(),
        resultSerializer = FinancialExtractionResult.CreditNote.serializer(),
        name = "submit_credit_note_extraction",
        description = "Submit extracted credit note fields from the document. Only include values you can see.",
    ) {
    override suspend fun execute(args: CreditNoteExtractionToolInput): FinancialExtractionResult.CreditNote {
        return FinancialExtractionResult.CreditNote(
            CreditNoteExtractionResult(
                creditNoteNumber = args.creditNoteNumber,
                issueDate = args.issueDate,
                currency = Currency.from(args.currency),
                subtotalAmount = Money.from(args.subtotalAmount),
                vatAmount = Money.from(args.vatAmount),
                totalAmount = Money.from(args.totalAmount),
                lineItems = args.lineItems.orEmpty().mapNotNull { it.toDomain() },
                vatBreakdown = args.vatBreakdown.orEmpty().mapNotNull { it.toDomain() },
                sellerName = args.sellerName,
                sellerVat = VatNumber.from(args.sellerVat),
                buyerName = args.buyerName,
                buyerVat = VatNumber.from(args.buyerVat),
                counterparty = args.toCounterpartyExtraction(),
                directionHint = args.directionHint,
                directionHintConfidence = args.directionHintConfidence,
                originalInvoiceNumber = args.originalInvoiceNumber,
                reason = args.reason,
                confidence = args.confidence,
                reasoning = args.reasoning,
            )
        )
    }
}

private val ExtractDocumentInput.creditNotePrompt: String
    get() = """
    You will receive credit note pages as images in context.

    Task: extract fields for a CREDIT NOTE (Avoir / Note de crédit / Creditnota).
    Output MUST be submitted via tool: submit_credit_note_extraction.

    ## HARD RULES
    - Do NOT guess. If not visible, return null.
    - Amount fields must be numeric strings using '.' as decimal separator (e.g., "1234.56").

    ## PARTY EXTRACTION (CRITICAL)
    - `seller*`: entity that ISSUED the credit note.
    - Prioritize issuer evidence from logo/header legal block and footer issuer/contact/VAT block.
    - `buyer*`: credited-to/recipient entity.
    - Prioritize credited-to/client block for buyer.
    - If one side is not visible, keep it null.
    - Prefer null over duplicating seller and buyer values.
    - Do not swap seller/buyer based on assumptions.

    ## AUTHORITATIVE COUNTERPARTY (CRITICAL)
    - Provide a single authoritative `counterparty*` block for downstream deterministic matching.
    - Counterparty is the other business party of this credit note.
    - Payment instruments (Visa/Mastercard/Apple Pay/etc.) are never counterparties.
    - If VAT-like text is a payment token, set `counterpartyVat` to null.
    - Always provide short `counterpartyReasoning`.

    ### Example
    - Credit note issued by seller to the buyer:
      - seller = issuer, buyer = credited customer, counterparty = seller, role = SELLER.

    ## OPTIONAL DIRECTION HINT
    - Provide `directionHint` only when explicit from the paper.
    - Use UNKNOWN when unsure.
    - If you provide `directionHint`, provide `directionHintConfidence` in range 0.0-1.0.

    ## REFERENCES
    If the credit note references an original invoice number/date, extract originalInvoiceNumber.

    ## REASON
    If a clear reason is written (return, correction, discount), extract it; else null.

    ## LINE ITEMS
    If an itemized table is present, extract lineItems with description, quantity, unitPrice, vatRate, netAmount.
    If no clear itemization, return an empty list.

    ## VAT BREAKDOWN
    If a VAT breakdown table is present, extract vatBreakdown rows (rate, base, amount).
    If reverse charge is indicated, use rate "0" and VAT amount "0".
    If not shown, return an empty list.

    Language hint: $language
    """.trimIndent()
