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
import tech.dokus.domain.enums.Currency
import tech.dokus.features.ai.config.asVisionModel
import tech.dokus.features.ai.models.ExtractDocumentInput
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
    val supplierVat: String?,
    val supplierEmail: String?,

    val currency: Currency,
    val subtotalAmount: Money?,
    val vatAmount: Money?,
    val totalAmount: Money?,

    val iban: String?,
    val paymentReference: String?,

    val confidence: Double,
    val reasoning: String?,
)

@Serializable
data class PurchaseOrderExtractionToolInput(
    val poNumber: String?,
    val orderDate: LocalDate?,
    val expectedDeliveryDate: LocalDate? = null,
    val supplierName: String?,
    val supplierVat: String? = null,
    val supplierEmail: String? = null,
    val currency: String = "EUR",
    val subtotalAmount: String?,
    val vatAmount: String?,
    val totalAmount: String?,
    val iban: String? = null,
    val paymentReference: String? = null,
    val confidence: Double,
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
                supplierVat = args.supplierVat,
                supplierEmail = args.supplierEmail,
                currency = Currency.from(args.currency),
                subtotalAmount = Money.from(args.subtotalAmount),
                vatAmount = Money.from(args.vatAmount),
                totalAmount = Money.from(args.totalAmount),
                iban = args.iban,
                paymentReference = args.paymentReference,
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
    - subtotalAmount = net total before VAT (if shown).
    - vatAmount = total VAT amount (if shown).
    - totalAmount = gross total (if shown). If no totals exist, keep amounts null.

    ## IDENTIFIERS
    Extract poNumber ("PO", "Bestelbon nr", "Bon de commande N°") if visible.

    ## DATES
    - orderDate: order date ("Orderdatum", "Date de commande")
    - expectedDeliveryDate: delivery/expected date if stated

    ## SUPPLIER
    Supplier/vendor is usually the recipient/supplier party. Extract supplierName/supplierVat if present.

    Language hint: $language
    """.trimIndent()
