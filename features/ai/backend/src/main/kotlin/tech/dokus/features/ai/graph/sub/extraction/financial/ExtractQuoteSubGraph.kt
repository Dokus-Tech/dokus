package tech.dokus.features.ai.graph.sub.extraction.financial

import ai.koog.agents.core.dsl.builder.AIAgentSubgraphBuilderBase
import ai.koog.agents.core.dsl.builder.AIAgentSubgraphDelegate
import ai.koog.agents.core.tools.Tool
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
import tech.dokus.features.ai.config.asVisionModel
import tech.dokus.features.ai.models.ExtractDocumentInput
import tech.dokus.features.ai.models.FinancialExtractionResult
import tech.dokus.foundation.backend.config.AIConfig

fun AIAgentSubgraphBuilderBase<*, *>.extractQuoteSubGraph(
    aiConfig: AIConfig,
): AIAgentSubgraphDelegate<ExtractDocumentInput, FinancialExtractionResult.Quote> {
    return subgraphWithTask(
        name = "Extract quote information",
        llmModel = aiConfig.mode.asVisionModel,
        tools = emptyList(),
        llmParams = LLMParams(temperature = 0.1),
        finishTool = QuoteExtractionFinishTool(),
    ) { it.quotePrompt }
}

@Serializable
@SerialName("QuoteExtractionResult")
data class QuoteExtractionResult(
    val quoteNumber: String?,
    val issueDate: LocalDate?,
    val validUntil: LocalDate?,

    val currency: Currency,
    val subtotalAmount: Money?,
    val vatAmount: Money?,
    val totalAmount: Money?,

    val customerName: String?,
    val customerVat: VatNumber?,
    val customerEmail: Email?,

    val iban: Iban?,
    val payment: CanonicalPayment?,

    val confidence: Double,
    val reasoning: String?,
)

@Serializable
data class QuoteExtractionToolInput(
    val quoteNumber: String?,
    val issueDate: LocalDate?,
    val validUntil: LocalDate?,
    val currency: String = "EUR",
    val subtotalAmount: String?,
    val vatAmount: String?,
    val totalAmount: String?,
    val customerName: String?,
    val customerVat: String?,
    val customerEmail: String? = null,
    val iban: String? = null,
    val paymentReference: String? = null,
    val confidence: Double,
    val reasoning: String? = null,
)

private class QuoteExtractionFinishTool : Tool<QuoteExtractionToolInput, FinancialExtractionResult.Quote>(
    argsSerializer = QuoteExtractionToolInput.serializer(),
    resultSerializer = FinancialExtractionResult.Quote.serializer(),
    name = "submit_quote_extraction",
    description = "Submit extracted quote fields from the document. Only include values you can see.",
) {
    override suspend fun execute(args: QuoteExtractionToolInput): FinancialExtractionResult.Quote {
        return FinancialExtractionResult.Quote(
            QuoteExtractionResult(
                quoteNumber = args.quoteNumber,
                issueDate = args.issueDate,
                validUntil = args.validUntil,
                currency = Currency.from(args.currency),
                subtotalAmount = Money.from(args.subtotalAmount),
                vatAmount = Money.from(args.vatAmount),
                totalAmount = Money.from(args.totalAmount),
                customerName = args.customerName,
                customerVat = VatNumber.from(args.customerVat),
                customerEmail = Email.from(args.customerEmail),
                iban = Iban.from(args.iban),
                payment = CanonicalPayment.from(args.paymentReference),
                confidence = args.confidence,
                reasoning = args.reasoning,
            )
        )
    }
}

private val ExtractDocumentInput.quotePrompt: String
    get() = """
    You will receive quote pages as images in context.

    Task: extract fields for a SALES QUOTE / OFFER (Offerte / Devis / Quotation).
    Output MUST be submitted via tool: submit_quote_extraction.

    ## HARD RULES
    - Do NOT guess. If not visible, return null.
    - Amount fields must be numeric strings using '.' as decimal separator (e.g., "1234.56").
    - totalAmount = gross total (if present). If only subtotal is shown, set totalAmount null.

    ## IDENTIFIERS
    Extract quoteNumber ("Offerte nr", "Devis N°", "Quotation #") if visible.

    ## DATE RULES
    - issueDate: date of quote
    - validUntil: expiration/validity date ("Geldig tot", "Valable jusqu'au", "Valid until")

    ## CUSTOMER
    For outgoing quotes, issuer is the tenant; customer is billed-to/recipient party.
    Extract customerName/customerVat/customerEmail if present.

    Language hint: $language
    """.trimIndent()
