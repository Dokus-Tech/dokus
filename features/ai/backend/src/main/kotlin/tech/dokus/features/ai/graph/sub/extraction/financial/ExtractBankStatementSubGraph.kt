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
import tech.dokus.domain.ids.Iban
import tech.dokus.features.ai.config.asVisionModel
import tech.dokus.features.ai.config.finishToolOnly
import tech.dokus.features.ai.config.finishToolVisionAssistantResponseRepeatMax
import tech.dokus.features.ai.models.ExtractDocumentInput
import tech.dokus.features.ai.models.ExtractionToolDescriptions
import tech.dokus.features.ai.models.FinancialExtractionResult
import tech.dokus.foundation.backend.config.AIConfig

@Serializable
data class BankStatementTransactionExtractionRow(
    val transactionDate: LocalDate? = null,
    val signedAmount: Money? = null,
    val counterpartyName: String? = null,
    val counterpartyIban: Iban? = null,
    val structuredCommunicationRaw: String? = null,
    val descriptionRaw: String? = null,
    val rowConfidence: Double = 0.0,
)

@Serializable
@SerialName("BankStatementExtractionResult")
data class BankStatementExtractionResult(
    val rows: List<BankStatementTransactionExtractionRow> = emptyList(),
    val confidence: Double,
    val reasoning: String? = null,
)

fun AIAgentSubgraphBuilderBase<*, *>.extractBankStatementSubGraph(
    aiConfig: AIConfig,
): AIAgentSubgraphDelegate<ExtractDocumentInput, FinancialExtractionResult.BankStatement> {
    return subgraphWithTask(
        name = "Extract bank statement transaction rows",
        llmModel = aiConfig.mode.asVisionModel,
        tools = emptyList(),
        llmParams = LLMParams.finishToolOnly("submit_bank_statement_extraction"),
        assistantResponseRepeatMax = finishToolVisionAssistantResponseRepeatMax,
        finishTool = BankStatementExtractionFinishTool()
    ) { it.bankStatementPrompt }
}

@Serializable
data class BankStatementTransactionToolInput(
    @property:LLMDescription(ExtractionToolDescriptions.BankTransactionDate)
    val transactionDate: LocalDate? = null,
    @property:LLMDescription(ExtractionToolDescriptions.BankSignedAmount)
    val signedAmount: String? = null,
    @property:LLMDescription(ExtractionToolDescriptions.BankCounterpartyName)
    val counterpartyName: String? = null,
    @property:LLMDescription(ExtractionToolDescriptions.BankCounterpartyIban)
    val counterpartyIban: String? = null,
    @property:LLMDescription(ExtractionToolDescriptions.BankStructuredCommunicationRaw)
    val structuredCommunicationRaw: String? = null,
    @property:LLMDescription(ExtractionToolDescriptions.BankDescriptionRaw)
    val descriptionRaw: String? = null,
    @property:LLMDescription(ExtractionToolDescriptions.BankRowConfidence)
    val rowConfidence: Double = 0.0,
)

@Serializable
data class BankStatementExtractionToolInput(
    val rows: List<BankStatementTransactionToolInput> = emptyList(),
    @property:LLMDescription(ExtractionToolDescriptions.Confidence)
    val confidence: Double,
    @property:LLMDescription(ExtractionToolDescriptions.Reasoning)
    val reasoning: String? = null,
)

private class BankStatementExtractionFinishTool :
    Tool<BankStatementExtractionToolInput, FinancialExtractionResult.BankStatement>(
        argsSerializer = BankStatementExtractionToolInput.serializer(),
        resultSerializer = FinancialExtractionResult.BankStatement.serializer(),
        name = "submit_bank_statement_extraction",
        description = "Submit extracted transaction rows from a bank statement document."
    ) {
    override suspend fun execute(args: BankStatementExtractionToolInput): FinancialExtractionResult.BankStatement {
        return FinancialExtractionResult.BankStatement(
            BankStatementExtractionResult(
                rows = args.rows.map { row ->
                    BankStatementTransactionExtractionRow(
                        transactionDate = row.transactionDate,
                        signedAmount = Money.from(row.signedAmount),
                        counterpartyName = row.counterpartyName,
                        counterpartyIban = Iban.from(row.counterpartyIban)?.takeIf { it.isValid },
                        structuredCommunicationRaw = row.structuredCommunicationRaw,
                        descriptionRaw = row.descriptionRaw,
                        rowConfidence = row.rowConfidence.coerceIn(0.0, 1.0),
                    )
                },
                confidence = args.confidence.coerceIn(0.0, 1.0),
                reasoning = args.reasoning
            )
        )
    }
}

private val ExtractDocumentInput.bankStatementPrompt
    get() = """
    You will receive a bank statement document in context.

    Task: extract transaction rows only.
    Output MUST be submitted via tool: submit_bank_statement_extraction.

    HARD RULES
    - Do not guess: if a field is not visible for a row, set null.
    - signedAmount MUST keep sign:
      - positive = money received
      - negative = money sent
    - structuredCommunicationRaw must preserve exact formatting shown in the document.
      Example: +++123/4567/89012+++
    - descriptionRaw should contain the raw line text for that transaction.
    - rowConfidence must be in range 0.0..1.0.

    Include all visible transaction rows on the page(s). Return empty list if no transaction table is visible.

    LANGUAGE HINT
    Detected language hint: ${language.take(8).replace(Regex("[^a-zA-Z]"), "")}
    """.trimIndent()
