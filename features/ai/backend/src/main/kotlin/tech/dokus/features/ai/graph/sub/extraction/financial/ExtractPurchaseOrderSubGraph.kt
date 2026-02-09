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
import tech.dokus.features.ai.config.asVisionModel
import tech.dokus.features.ai.models.ExtractDocumentInput
import tech.dokus.features.ai.models.ExtractionToolDescriptions
import tech.dokus.features.ai.models.FinancialExtractionResult
import tech.dokus.foundation.backend.config.AIConfig

fun AIAgentSubgraphBuilderBase<*, *>.extractPurchaseOrderSubGraph(
    aiConfig: AIConfig,
): AIAgentSubgraphDelegate<ExtractDocumentInput, FinancialExtractionResult.PurchaseOrder> {
    return subgraphWithTask(
        name = "Extract purchase order information",
        llmModel = aiConfig.mode.asVisionModel,
        tools = emptyList(),
        llmParams = LLMParams(temperature = 0.1),
        finishTool = PurchaseOrderExtractionFinishTool(),
    ) { it.purchaseOrderPrompt }
}

@Serializable
@SerialName("PurchaseOrderExtractionResult")
data class PurchaseOrderExtractionResult(
    val poNumber: String?,
    val orderDate: LocalDate?,
    val expectedDeliveryDate: LocalDate?,

    val supplierName: String?,
    val supplierVat: VatNumber?,
    val supplierEmail: Email?,

    val currency: Currency,
    val subtotalAmount: Money?,
    val vatAmount: Money?,
    val totalAmount: Money?,

    val iban: Iban?,
    val payment: CanonicalPayment?,

    val confidence: Double,
    val reasoning: String?,
)

@Serializable
data class PurchaseOrderExtractionToolInput(
    @property:LLMDescription(ExtractionToolDescriptions.PurchaseOrderNumber)
    val poNumber: String?,
    @property:LLMDescription(ExtractionToolDescriptions.OrderDate)
    val orderDate: LocalDate?,
    @property:LLMDescription(ExtractionToolDescriptions.ExpectedDeliveryDate)
    val expectedDeliveryDate: LocalDate? = null,
    @property:LLMDescription(ExtractionToolDescriptions.SupplierName)
    val supplierName: String?,
    @property:LLMDescription(ExtractionToolDescriptions.SupplierVat)
    val supplierVat: String? = null,
    @property:LLMDescription(ExtractionToolDescriptions.SupplierEmail)
    val supplierEmail: String? = null,
    @property:LLMDescription(ExtractionToolDescriptions.Currency)
    val currency: String = "EUR",
    @property:LLMDescription(ExtractionToolDescriptions.SubtotalAmount)
    val subtotalAmount: String?,
    @property:LLMDescription(ExtractionToolDescriptions.VatAmount)
    val vatAmount: String?,
    @property:LLMDescription(ExtractionToolDescriptions.TotalAmount)
    val totalAmount: String?,
    @property:LLMDescription(ExtractionToolDescriptions.Iban)
    val iban: String? = null,
    @property:LLMDescription(ExtractionToolDescriptions.PaymentReference)
    val paymentReference: String? = null,
    @property:LLMDescription(ExtractionToolDescriptions.Confidence)
    val confidence: Double,
    @property:LLMDescription(ExtractionToolDescriptions.Reasoning)
    val reasoning: String? = null,
)

private class PurchaseOrderExtractionFinishTool :
    Tool<PurchaseOrderExtractionToolInput, FinancialExtractionResult.PurchaseOrder>(
        argsSerializer = PurchaseOrderExtractionToolInput.serializer(),
        resultSerializer = FinancialExtractionResult.PurchaseOrder.serializer(),
        name = "submit_purchase_order_extraction",
        description = "Submit extracted purchase order fields from the document. Only include values you can see.",
    ) {
    override suspend fun execute(args: PurchaseOrderExtractionToolInput): FinancialExtractionResult.PurchaseOrder {
        return FinancialExtractionResult.PurchaseOrder(
            PurchaseOrderExtractionResult(
                poNumber = args.poNumber,
                orderDate = args.orderDate,
                expectedDeliveryDate = args.expectedDeliveryDate,
                supplierName = args.supplierName,
                supplierVat = VatNumber.from(args.supplierVat),
                supplierEmail = Email.from(args.supplierEmail),
                currency = Currency.from(args.currency),
                subtotalAmount = Money.from(args.subtotalAmount),
                vatAmount = Money.from(args.vatAmount),
                totalAmount = Money.from(args.totalAmount),
                iban = Iban.from(args.iban),
                payment = CanonicalPayment.from(args.paymentReference),
                confidence = args.confidence,
                reasoning = args.reasoning,
            )
        )
    }
}

private val ExtractDocumentInput.purchaseOrderPrompt: String
    get() = """
    You will receive purchase order pages as images in context.

    Task: extract fields for a PURCHASE ORDER (PO / Bon de commande / Bestelbon).
    Output MUST be submitted via tool: submit_purchase_order_extraction.

    ## HARD RULES
    - Do NOT guess. If not visible, return null.
    - Amount fields must be numeric strings using '.' as decimal separator (e.g., "1234.56").

    ## IDENTIFIERS
    Extract poNumber ("PO", "Bestelbon nr", "Bon de commande N°") if visible.

    ## DATES
    Identify order date ("Orderdatum", "Date de commande") and expected delivery date if stated.

    ## SUPPLIER
    Supplier/vendor is usually the recipient/supplier party.

    Language hint: $language
    """.trimIndent()
