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
import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.model.FinancialLineItem
import tech.dokus.domain.model.VatBreakdownEntry
import tech.dokus.features.ai.config.asVisionModel
import tech.dokus.features.ai.models.ExtractDocumentInput
import tech.dokus.features.ai.models.ExtractionToolDescriptions
import tech.dokus.features.ai.models.FinancialExtractionResult
import tech.dokus.features.ai.models.LineItemToolInput
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
        llmParams = LLMParams(temperature = 0.1),
        finishTool = CreditNoteExtractionFinishTool(),
    ) { it.creditNotePrompt }
}

@Serializable
enum class CreditNoteDirection {
    SALES,     // we issued it to a customer
    PURCHASE,  // supplier issued it to us
    UNKNOWN
}

@Serializable
@SerialName("CreditNoteExtractionResult")
data class CreditNoteExtractionResult(
    val creditNoteNumber: String?,
    val direction: CreditNoteDirection,   // SALES / PURCHASE / UNKNOWN

    val issueDate: LocalDate?,

    val currency: Currency,
    val subtotalAmount: Money?,
    val vatAmount: Money?,
    val totalAmount: Money?,

    val lineItems: List<FinancialLineItem> = emptyList(),
    val vatBreakdown: List<VatBreakdownEntry> = emptyList(),

    val counterpartyName: String?,        // customer or supplier depending on direction
    val counterpartyVat: VatNumber?,

    val originalInvoiceNumber: String?,   // if referenced
    val reason: String?,                  // if explicit (e.g. "Remboursement", "Retour")

    val confidence: Double,
    val reasoning: String?,
)

@Serializable
data class CreditNoteExtractionToolInput(
    @property:LLMDescription(ExtractionToolDescriptions.CreditNoteNumber)
    val creditNoteNumber: String?,
    @property:LLMDescription(ExtractionToolDescriptions.CreditNoteDirection)
    val direction: CreditNoteDirection = CreditNoteDirection.UNKNOWN,
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
    @property:LLMDescription(ExtractionToolDescriptions.CounterpartyName)
    val counterpartyName: String?,
    @property:LLMDescription(ExtractionToolDescriptions.CounterpartyVat)
    val counterpartyVat: String?,
    @property:LLMDescription(ExtractionToolDescriptions.OriginalInvoiceNumber)
    val originalInvoiceNumber: String? = null,
    @property:LLMDescription(ExtractionToolDescriptions.CreditNoteReason)
    val reason: String? = null,
    @property:LLMDescription(ExtractionToolDescriptions.Confidence)
    val confidence: Double,
    @property:LLMDescription(ExtractionToolDescriptions.Reasoning)
    val reasoning: String? = null,
)

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
                direction = args.direction,
                issueDate = args.issueDate,
                currency = Currency.from(args.currency),
                subtotalAmount = Money.from(args.subtotalAmount),
                vatAmount = Money.from(args.vatAmount),
                totalAmount = Money.from(args.totalAmount),
                lineItems = args.lineItems.orEmpty().mapNotNull { it.toDomain() },
                vatBreakdown = args.vatBreakdown.orEmpty().mapNotNull { it.toDomain() },
                counterpartyName = args.counterpartyName,
                counterpartyVat = VatNumber.from(args.counterpartyVat),
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

    ## DIRECTION (SALES vs PURCHASE)
    Determine direction if possible:
    - SALES: we are the issuer (our company in header/logo), crediting a customer
    - PURCHASE: supplier is issuer, crediting us
    If unclear, set direction = UNKNOWN.

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
