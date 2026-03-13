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
    val freeCommunication: String? = null,
    val descriptionRaw: String? = null,
    val rowConfidence: Double = 0.0,
)

@Serializable
@SerialName("BankStatementExtractionResult")
data class BankStatementExtractionResult(
    val rows: List<BankStatementTransactionExtractionRow> = emptyList(),
    val accountIban: Iban? = null,
    val openingBalance: Money? = null,
    val closingBalance: Money? = null,
    val periodStart: LocalDate? = null,
    val periodEnd: LocalDate? = null,
    val institutionName: String? = null,
    val institutionBic: String? = null,
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
    @property:LLMDescription(ExtractionToolDescriptions.BankFreeCommunication)
    val freeCommunication: String? = null,
    @property:LLMDescription(ExtractionToolDescriptions.BankDescriptionRaw)
    val descriptionRaw: String? = null,
    @property:LLMDescription(ExtractionToolDescriptions.BankRowConfidence)
    val rowConfidence: Double = 0.0,
)

@Serializable
data class BankStatementExtractionToolInput(
    val rows: List<BankStatementTransactionToolInput> = emptyList(),
    @property:LLMDescription(ExtractionToolDescriptions.BankAccountIban)
    val accountIban: String? = null,
    @property:LLMDescription(ExtractionToolDescriptions.BankOpeningBalance)
    val openingBalance: String? = null,
    @property:LLMDescription(ExtractionToolDescriptions.BankClosingBalance)
    val closingBalance: String? = null,
    @property:LLMDescription(ExtractionToolDescriptions.BankPeriodStart)
    val periodStart: LocalDate? = null,
    @property:LLMDescription(ExtractionToolDescriptions.BankPeriodEnd)
    val periodEnd: LocalDate? = null,
    @property:LLMDescription(ExtractionToolDescriptions.BankInstitutionName)
    val institutionName: String? = null,
    @property:LLMDescription(ExtractionToolDescriptions.BankInstitutionBic)
    val institutionBic: String? = null,
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
                        freeCommunication = row.freeCommunication,
                        descriptionRaw = row.descriptionRaw,
                        rowConfidence = row.rowConfidence.coerceIn(0.0, 1.0),
                    )
                },
                accountIban = Iban.from(args.accountIban)?.takeIf { it.isValid },
                openingBalance = Money.from(args.openingBalance),
                closingBalance = Money.from(args.closingBalance),
                periodStart = args.periodStart,
                periodEnd = args.periodEnd,
                institutionName = args.institutionName?.trim()?.takeIf { it.isNotEmpty() },
                institutionBic = args.institutionBic?.trim()?.takeIf { it.isNotEmpty() },
                confidence = args.confidence.coerceIn(0.0, 1.0),
                reasoning = args.reasoning
            )
        )
    }
}

private val ExtractDocumentInput.bankStatementPrompt
    get() = """
    You will receive a bank statement document in context.

    Task: extract statement header fields AND transaction rows.
    Output MUST be submitted via tool: submit_bank_statement_extraction.

    HEADER FIELDS (extract from statement header/footer if visible)
    - accountIban: the IBAN of the account this statement belongs to
    - openingBalance: the opening/previous balance shown on the statement (signed, e.g. "1234.56" or "-50.00")
    - closingBalance: the closing/new balance shown on the statement (signed)
    - periodStart: the start date of the statement period
    - periodEnd: the end date of the statement period
    - institutionName: the legal or trading name of the bank/financial institution that issued this statement (e.g. "Wise Europe SA", "KBC Bank NV"). Look in the header, logo, or footer.
    - institutionBic: the BIC/SWIFT code of the issuing bank if visible (e.g. "TRWIBEB1XXX")

    TRANSACTION FIELD RULES
    Bank statements often show a bold transfer type header (e.g. "SENDING MONEY TO", "EUROPEAN DIRECT DEBIT",
    "PAYMENT VIA BANCONTACT", "CREDIT TRANSFER FROM") followed by detail lines with the actual information.

    - counterpartyName: Extract the actual business/person entity name from the DETAIL lines, NOT the transfer
      type header. Examples: "TEAM INNING BV", "BE-MOBILE", "PROUNITY SA" — not "SENDING MONEY TO".
    - counterpartyIban: The IBAN of the counterparty if visible in the detail lines.
    - structuredCommunicationRaw: Belgian OGM structured communication (+++XXX/XXXX/XXXXX+++) if present. Preserve exact formatting.
    - freeCommunication: Any free-form payment reference that is NOT a structured communication. E.g. invoice numbers,
      reference codes, mandate references. Examples: "IV-063", "777887093469", "4411 PARKING AND MOBILITY".
    - descriptionRaw: The FULL raw text of the transaction — include the transfer type header AND all detail lines, joined with newlines.

    HARD RULES
    - Do not guess: if a field is not visible, set null.
    - signedAmount MUST keep sign:
      - positive = money received
      - negative = money sent
    - rowConfidence must be in range 0.0..1.0.
    - openingBalance and closingBalance must include sign (positive or negative).

    Include all visible transaction rows on the page(s). Return empty list if no transaction table is visible.

    LANGUAGE HINT
    Detected language hint: ${language.take(8).replace(Regex("[^a-zA-Z]"), "")}
    """.trimIndent()
