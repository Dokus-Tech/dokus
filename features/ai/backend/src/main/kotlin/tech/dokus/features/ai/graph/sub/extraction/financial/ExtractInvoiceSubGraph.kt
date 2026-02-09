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
import tech.dokus.features.ai.config.asVisionModel
import tech.dokus.features.ai.models.ExtractDocumentInput
import tech.dokus.features.ai.models.FinancialExtractionResult
import tech.dokus.foundation.backend.config.AIConfig
import tech.dokus.domain.Money
import tech.dokus.domain.enums.Currency
import tech.dokus.domain.ids.Iban
import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.model.CanonicalPayment

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

    // Parties (facts only)
    val customerName: String?,
    val customerVat: String?,
    val customerEmail: String? = null,

    // Payment hints
    val iban: String? = null,
    val payment: CanonicalPayment? = null,

    // Evidence/quality
    val confidence: Double,
    val reasoning: String? = null
)

fun AIAgentSubgraphBuilderBase<*, *>.extractInvoiceSubGraph(
    aiConfig: AIConfig,
): AIAgentSubgraphDelegate<ExtractDocumentInput, FinancialExtractionResult.Invoice> {
    return subgraphWithTask(
        name = "Extract invoice information",
        llmModel = aiConfig.mode.asVisionModel,
        tools = emptyList(),
        llmParams = LLMParams(temperature = 0.1),
        finishTool = InvoiceExtractionFinishTool()
    ) { it.prompt }
}

@Serializable
data class InvoiceExtractionToolInput(
    @property:LLMDescription("Invoice number (e.g. 2025-001). Null if not visible.")
    val invoiceNumber: String?,
    @property:LLMDescription("Issue date. Null if not visible.")
    val issueDate: LocalDate?,
    @property:LLMDescription("Due date. Null if not visible.")
    val dueDate: LocalDate?,
    @property:LLMDescription("Currency code like EUR. If symbol only, infer best guess.")
    val currency: String = "EUR",
    @property:LLMDescription("Subtotal/net amount as it appears. Use plain number string (e.g. 1234.56). Null if not present.")
    val subtotalAmount: String?,
    @property:LLMDescription("Total VAT amount. Use plain number string. Null if not present.")
    val vatAmount: String?,
    @property:LLMDescription("Total/gross amount. Use plain number string. Null if not present.")
    val totalAmount: String?,
    @property:LLMDescription("Customer name (the billed-to party). Null if unclear.")
    val customerName: String?,
    @property:LLMDescription("Customer VAT number if shown (e.g. BE0123456789). Null if not visible.")
    val customerVat: String?,
    @property:LLMDescription("Customer email if visible.")
    val customerEmail: String? = null,
    @property:LLMDescription("IBAN for payment if visible.")
    val iban: String? = null,
    @property:LLMDescription("Payment reference / structured communication if visible.")
    val paymentReference: String? = null,
    @property:LLMDescription("Confidence score 0.0-1.0 for the extraction quality.")
    val confidence: Double,
    @property:LLMDescription("Short reasoning: what you used to extract totals/dates/number.")
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
                customerName = args.customerName,
                customerVat = VatNumber.from(args.customerVat)?.value,
                customerEmail = args.customerEmail,
                iban = Iban.from(args.iban)?.value,
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
    - totalAmount = gross total payable (not subtotal).
    - subtotalAmount = net total before VAT (if shown).
    - vatAmount = total VAT amount (if shown).
    - If multiple totals exist, prefer the "Total" / "Totaal" / "Total TTC" style final payable amount.

    ## PARTY DETECTION
    OUTGOING INVOICE means:
    - Issuer is the tenant (your company) in header/logo area.
    - Customer is the billed-to party ("Client", "Klant", "Bill to", etc).
    Extract CUSTOMER fields only (customerName, customerVat, customerEmail).

    ## DATE RULES
    - issueDate: date of invoice issuance ("Factuurdatum", "Date de facture", "Invoice date")
    - dueDate: payment due ("Vervaldatum", "Échéance", "Due date")
    If only one date is present and it clearly is the invoice date, set issueDate and leave dueDate null.

    ## PAYMENT FIELDS
    If IBAN or structured reference is present, extract it. Otherwise null.

    ## LANGUAGE HINT
    Detected language hint: $language
    """.trimIndent()
