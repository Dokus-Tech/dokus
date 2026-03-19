package tech.dokus.features.ai.graph.sub.extraction.financial

import ai.koog.agents.core.agent.entity.createStorageKey
import ai.koog.agents.core.dsl.builder.AIAgentSubgraphBuilderBase
import ai.koog.agents.core.dsl.builder.AIAgentSubgraphDelegate
import ai.koog.agents.core.tools.Tool
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.ext.agent.subgraphWithTask
import ai.koog.prompt.params.LLMParams
import kotlinx.serialization.Serializable
import tech.dokus.features.ai.config.asVisionModel
import tech.dokus.features.ai.config.finishToolOnly
import tech.dokus.features.ai.config.finishToolVisionAssistantResponseRepeatMax
import tech.dokus.features.ai.graph.nodes.CSV_BYTES_STORAGE_KEY
import tech.dokus.features.ai.models.ExtractDocumentInput
import tech.dokus.features.ai.models.FinancialExtractionResult
import tech.dokus.features.ai.services.CsvBankStatementParser
import tech.dokus.foundation.backend.config.AIConfig

// ── Data classes ──

@Serializable
data class CsvColumnMapping(
    @property:LLMDescription("True if this CSV contains bank transactions. False if it's a different type of data (e.g. product list, contacts, etc.)")
    val isBankStatement: Boolean,
    @property:LLMDescription("If not a bank statement, explain why (e.g. 'This appears to be a product inventory list')")
    val rejectionReason: String? = null,
    @property:LLMDescription("Column name/header for the transaction date")
    val dateColumn: String? = null,
    @property:LLMDescription("Column name for a single signed amount (positive=received, negative=sent). Null if the bank uses separate debit/credit columns instead.")
    val amountColumn: String? = null,
    @property:LLMDescription("Column name for debit amounts (money sent). Only if the bank uses separate columns for debit and credit.")
    val debitColumn: String? = null,
    @property:LLMDescription("Column name for credit amounts (money received). Only if the bank uses separate columns for debit and credit.")
    val creditColumn: String? = null,
    @property:LLMDescription("Column name for the counterparty/beneficiary/sender name")
    val counterpartyNameColumn: String? = null,
    @property:LLMDescription("Column name for the counterparty IBAN")
    val counterpartyIbanColumn: String? = null,
    @property:LLMDescription("Column name for payment communication/reference (structured or free text)")
    val communicationColumn: String? = null,
    @property:LLMDescription("Column name for transaction description or details")
    val descriptionColumn: String? = null,
    @property:LLMDescription("Date format used in the date column, e.g. 'dd/MM/yyyy', 'yyyy-MM-dd', 'dd-MM-yyyy', 'MM/dd/yyyy'")
    val dateFormat: String? = null,
    @property:LLMDescription("Decimal separator used in amount columns: '.' (period) or ',' (comma). European banks often use comma.")
    val decimalSeparator: String = ".",
    @property:LLMDescription("0-based row index of the header row (usually 0, but some CSVs have metadata rows before the header)")
    val headerRowIndex: Int = 0,
    @property:LLMDescription("0-based row index where data rows start (usually headerRowIndex + 1)")
    val dataStartRowIndex: Int = 1,
    @property:LLMDescription("Column delimiter: ',' (comma) or ';' (semicolon). European bank exports often use semicolons.")
    val delimiter: String = ",",
    @property:LLMDescription("Detected file encoding: UTF-8, ISO-8859-1, or Windows-1252. Look for garbled characters that suggest wrong encoding.")
    val encoding: String = "UTF-8",
    @property:LLMDescription("Account IBAN if visible in metadata rows above the header. Null if not found.")
    val accountIban: String? = null,
    @property:LLMDescription("Bank/institution name if visible in metadata rows or header. Null if not found.")
    val institutionName: String? = null,
    @property:LLMDescription("Confidence score 0.0-1.0 for the mapping quality.")
    val confidence: Double,
    @property:LLMDescription("Short reasoning: what you used to determine the column mapping.")
    val reasoning: String? = null,
)

// ── Finish tool ──

private class CsvMappingFinishTool :
    Tool<CsvColumnMapping, CsvColumnMapping>(
        argsSerializer = CsvColumnMapping.serializer(),
        resultSerializer = CsvColumnMapping.serializer(),
        name = "submit_csv_column_mapping",
        description = "Submit the column mapping for a CSV bank statement file."
    ) {
    override suspend fun execute(args: CsvColumnMapping): CsvColumnMapping {
        return args.copy(confidence = args.confidence.coerceIn(0.0, 1.0))
    }
}

// ── Subgraph: AI column mapping → deterministic parse ──

fun AIAgentSubgraphBuilderBase<*, *>.extractCsvBankStatementSubGraph(
    aiConfig: AIConfig,
): AIAgentSubgraphDelegate<ExtractDocumentInput, FinancialExtractionResult> {
    return subgraph(name = "csv-bank-statement-extraction") {
        val mappingKey = createStorageKey<CsvColumnMapping>("csv-column-mapping")

        // Step 1: AI determines column mapping from the CSV preview (already in prompt)
        val aiMapping by subgraphWithTask(
            name = "Determine CSV column mapping",
            llmModel = aiConfig.mode.asVisionModel,
            tools = emptyList(),
            llmParams = LLMParams.finishToolOnly("submit_csv_column_mapping"),
            assistantResponseRepeatMax = finishToolVisionAssistantResponseRepeatMax,
            finishTool = CsvMappingFinishTool()
        ) { _: ExtractDocumentInput -> csvMappingPrompt }

        // Step 2: Store AI mapping result
        val storeMapping by node<CsvColumnMapping, CsvColumnMapping>("store-csv-mapping") { mapping ->
            storage.set(mappingKey, mapping)
            mapping
        }

        // Step 3: Parse CSV deterministically using the AI-provided mapping
        val parseCsv by node<CsvColumnMapping, FinancialExtractionResult>("parse-csv-bank-statement") { mapping ->
            if (!mapping.isBankStatement) {
                return@node FinancialExtractionResult.Unsupported(
                    documentType = "BankStatement",
                    reason = mapping.rejectionReason ?: "CSV content is not a bank statement"
                )
            }

            val csvBytesKey = createStorageKey<ByteArray>(CSV_BYTES_STORAGE_KEY)
            val csvBytes = storage.getValue(csvBytesKey)

            CsvBankStatementParser.parse(csvBytes, mapping)
        }

        nodeStart then aiMapping then storeMapping then parseCsv then nodeFinish
    }
}

private val csvMappingPrompt = """
    You are analyzing a CSV file that was uploaded. The CSV text preview is already in the conversation above.

    TASK: Determine if this CSV contains bank transactions, and if so, map its columns to standard fields.

    STEP 1 — CLASSIFICATION
    Look at the columns and data. Does this look like a bank transaction export?
    Bank transaction CSVs typically have columns for: date, amount, counterparty name, payment reference/communication.
    If this is NOT a bank transaction export, set isBankStatement=false with a reason and leave all column fields null.

    STEP 2 — ENCODING DETECTION
    Look for garbled characters that suggest the file was decoded with the wrong encoding:
    - "Ã©" instead of "é", "Ã¨" instead of "è" → the file is likely ISO-8859-1 or Windows-1252, decoded as UTF-8
    - "Ã¼" instead of "ü", "Ã¶" instead of "ö" → same encoding issue
    - Accented characters render correctly → encoding is UTF-8
    Belgian bank exports often use ISO-8859-1 for names with accents (é, è, ê, ë, etc.).

    STEP 3 — COLUMN MAPPING
    For each field, provide the EXACT column header name as it appears in the CSV:
    - dateColumn: the column with transaction dates
    - amountColumn: if the bank uses a single signed column (positive = received, negative = sent)
    - debitColumn + creditColumn: if the bank uses SEPARATE columns for money sent vs received
    - counterpartyNameColumn: counterparty / beneficiary / sender name
    - counterpartyIbanColumn: counterparty IBAN (may not exist in all exports)
    - communicationColumn: payment reference, structured communication (Belgian +++XXX/XXXX/XXXXX+++), or free text reference
    - descriptionColumn: transaction description or details

    STEP 4 — FORMAT DETECTION
    - dateFormat: the format used for dates (e.g. "dd/MM/yyyy", "yyyy-MM-dd")
    - decimalSeparator: "." or "," — check how amounts are formatted (European banks often use comma)
    - delimiter: "," or ";" — check what separates the columns
    - headerRowIndex: 0-based index of the header row (usually 0, but some CSVs have metadata lines first)
    - dataStartRowIndex: 0-based index where data rows begin (usually headerRowIndex + 1)

    STEP 5 — METADATA
    - accountIban: look in any metadata rows ABOVE the header for an IBAN
    - institutionName: look for a bank name in metadata rows or the filename

    Submit your analysis via the submit_csv_column_mapping tool.
    Set null for any column that doesn't exist in this CSV.
""".trimIndent()
