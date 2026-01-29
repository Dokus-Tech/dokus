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

    val currency: String,
    val subtotalAmount: String?,
    val vatAmount: String?,
    val totalAmount: String?,

    val counterpartyName: String?,        // customer or supplier depending on direction
    val counterpartyVat: String?,

    val originalInvoiceNumber: String?,   // if referenced
    val reason: String?,                  // if explicit (e.g. "Remboursement", "Retour")

    val confidence: Double,
    val reasoning: String?,
)

@Serializable
data class CreditNoteExtractionToolInput(
    val creditNoteNumber: String?,
    val direction: CreditNoteDirection = CreditNoteDirection.UNKNOWN,
    val issueDate: LocalDate?,
    val currency: String = "EUR",
    val subtotalAmount: String?,
    val vatAmount: String?,
    val totalAmount: String?,
    val counterpartyName: String?,
    val counterpartyVat: String?,
    val originalInvoiceNumber: String? = null,
    val reason: String? = null,
    val confidence: Double,
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
                currency = args.currency,
                subtotalAmount = args.subtotalAmount,
                vatAmount = args.vatAmount,
                totalAmount = args.totalAmount,
                counterpartyName = args.counterpartyName,
                counterpartyVat = args.counterpartyVat,
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
    - subtotalAmount = net total before VAT (if shown).
    - vatAmount = total VAT amount (if shown).
    - totalAmount = gross total of the credit note.

    ## DIRECTION (SALES vs PURCHASE)
    Determine direction if possible:
    - SALES: we are the issuer (our company in header/logo), crediting a customer
    - PURCHASE: supplier is issuer, crediting us
    If unclear, set direction = UNKNOWN.
    Extract counterpartyName/counterpartyVat for the other party (customer/supplier).

    ## REFERENCES
    If the credit note references an original invoice number/date, extract originalInvoiceNumber.

    ## REASON
    If a clear reason is written (return, correction, discount), extract it; else null.

    Language hint: $language
    """.trimIndent()