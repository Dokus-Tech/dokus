package tech.dokus.features.ai.graph.sub.extraction.financial

import ai.koog.agents.core.dsl.builder.AIAgentSubgraphBuilderBase
import ai.koog.agents.core.dsl.builder.AIAgentSubgraphDelegate
import ai.koog.agents.core.tools.Tool
import ai.koog.agents.ext.agent.subgraphWithTask
import ai.koog.prompt.params.LLMParams
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import tech.dokus.domain.Money
import tech.dokus.domain.VatRate
import tech.dokus.domain.enums.Currency
import tech.dokus.domain.ids.Iban
import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.model.CanonicalPayment
import tech.dokus.features.ai.config.asVisionModel
import tech.dokus.features.ai.models.ExtractDocumentInput
import tech.dokus.features.ai.models.FinancialExtractionResult
import tech.dokus.foundation.backend.config.AIConfig

fun AIAgentSubgraphBuilderBase<*, *>.extractBillSubGraph(
    aiConfig: AIConfig,
): AIAgentSubgraphDelegate<ExtractDocumentInput, FinancialExtractionResult.Bill> {
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
    val supplierVat: VatNumber?,
    val invoiceNumber: String?,
    val issueDate: LocalDate?,
    val dueDate: LocalDate?,
    val currency: Currency,
    val totalAmount: Money?,
    val vatAmount: Money?,
    val vatRate: VatRate?,
    val iban: Iban?,
    val payment: CanonicalPayment?,
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
    val vatRate: String? = null,
    val iban: String? = null,
    val paymentReference: String? = null,
    val confidence: Double,
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
                vatRate = VatRate.from(args.vatRate),
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
    - totalAmount = gross total payable (final total).
    - vatAmount = total VAT amount, if present.
    - vatRate: if there is a single clear VAT rate, return it like "21". If multiple rates or unclear, return null.

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
