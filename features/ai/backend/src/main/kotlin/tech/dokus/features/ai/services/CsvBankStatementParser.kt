package tech.dokus.features.ai.services

import kotlinx.datetime.LocalDate
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import tech.dokus.domain.Money
import tech.dokus.domain.enums.Currency
import tech.dokus.domain.ids.Iban
import tech.dokus.features.ai.graph.sub.extraction.financial.BankStatementExtractionResult
import tech.dokus.features.ai.graph.sub.extraction.financial.BankStatementTransactionExtractionRow
import tech.dokus.features.ai.graph.sub.extraction.financial.CsvColumnMapping
import tech.dokus.features.ai.models.FinancialExtractionResult
import tech.dokus.foundation.backend.utils.loggerFor
import java.io.StringReader
import java.nio.charset.Charset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Deterministic CSV bank statement parser.
 *
 * Uses AI-determined [CsvColumnMapping] to parse CSV bytes into
 * [BankStatementExtractionResult] using Apache Commons CSV.
 * No AI is involved in the parsing itself — only the column mapping comes from AI.
 */
object CsvBankStatementParser {
    private val logger = loggerFor()

    private val OGM_PATTERN = Regex("""\+{3}\d{3}/\d{4}/\d{5}\+{3}""")

    fun parse(csvBytes: ByteArray, mapping: CsvColumnMapping): FinancialExtractionResult.BankStatement {
        val charset = runCatching { Charset.forName(mapping.encoding) }.getOrDefault(Charsets.UTF_8)
        val csvText = String(csvBytes, charset)
        val delimiter = mapping.delimiter.firstOrNull() ?: ','

        // Skip metadata rows before the header
        val lines = csvText.lines()
        val csvFromHeader = lines.drop(mapping.headerRowIndex).joinToString("\n")

        val format = CSVFormat.Builder.create(CSVFormat.DEFAULT)
            .setDelimiter(delimiter)
            .setHeader()
            .setSkipHeaderRecord(true)
            .setIgnoreEmptyLines(true)
            .setTrim(true)
            .build()

        val parser = CSVParser.parse(StringReader(csvFromHeader), format)

        val dateFormatter = mapping.dateFormat?.let { fmt ->
            runCatching { DateTimeFormatter.ofPattern(fmt) }.getOrNull()
        }

        val rows = mutableListOf<BankStatementTransactionExtractionRow>()

        for (record in parser) {
            try {
                val date = mapping.dateColumn?.let { col ->
                    val raw = record.safeGet(col)?.trim() ?: return@let null
                    parseDate(raw, dateFormatter)
                }

                val amount = parseAmount(record, mapping)

                val counterpartyName = mapping.counterpartyNameColumn?.let { col ->
                    record.safeGet(col)?.trim()?.takeIf { it.isNotBlank() }
                }
                val counterpartyIban = mapping.counterpartyIbanColumn?.let { col ->
                    record.safeGet(col)?.trim()?.let { raw ->
                        Iban.from(raw)?.takeIf { it.isValid }
                    }
                }

                val communication = mapping.communicationColumn?.let { col ->
                    record.safeGet(col)?.trim()?.takeIf { it.isNotBlank() }
                }
                val description = mapping.descriptionColumn?.let { col ->
                    record.safeGet(col)?.trim()?.takeIf { it.isNotBlank() }
                }

                val structuredComm = communication?.let { comm ->
                    OGM_PATTERN.find(comm)?.value
                }
                val freeComm = if (structuredComm != null) null else communication

                rows += BankStatementTransactionExtractionRow(
                    transactionDate = date,
                    signedAmount = amount,
                    counterpartyName = counterpartyName,
                    counterpartyIban = counterpartyIban,
                    structuredCommunicationRaw = structuredComm,
                    freeCommunication = freeComm,
                    descriptionRaw = description ?: communication,
                    rowConfidence = if (date != null && amount != null) 1.0 else 0.90,
                )
            } catch (e: Exception) {
                logger.warn("Skipping CSV row {}: {}", record.recordNumber, e.message)
            }
        }

        val result = BankStatementExtractionResult(
            rows = rows,
            accountIban = mapping.accountIban?.let { Iban.from(it) }?.takeIf { it.isValid },
            openingBalance = null,
            closingBalance = null,
            periodStart = rows.mapNotNull { it.transactionDate }.minOrNull(),
            periodEnd = rows.mapNotNull { it.transactionDate }.maxOrNull(),
            institutionName = mapping.institutionName?.trim()?.takeIf { it.isNotEmpty() },
            institutionBic = null,
            confidence = mapping.confidence,
            reasoning = "Parsed ${rows.size} rows from CSV using AI-determined column mapping",
        )

        return FinancialExtractionResult.BankStatement(result)
    }

    private fun parseAmount(
        record: org.apache.commons.csv.CSVRecord,
        mapping: CsvColumnMapping,
    ): Money? {
        if (mapping.amountColumn != null) {
            val raw = record.safeGet(mapping.amountColumn)?.trim() ?: return null
            return parseMoneyString(raw, mapping.decimalSeparator)
        }

        // Split debit/credit columns
        val debitRaw = mapping.debitColumn?.let { record.safeGet(it)?.trim() }
        val creditRaw = mapping.creditColumn?.let { record.safeGet(it)?.trim() }

        val debit = debitRaw?.takeIf { it.isNotBlank() }?.let { parseMoneyString(it, mapping.decimalSeparator) }
        val credit = creditRaw?.takeIf { it.isNotBlank() }?.let { parseMoneyString(it, mapping.decimalSeparator) }

        return when {
            debit != null && !debit.isZero -> -debit
            credit != null -> credit
            else -> null
        }
    }

    private fun parseMoneyString(raw: String, decimalSeparator: String): Money? {
        val cleaned = raw
            .replace(Regex("[^0-9.,+-]"), "")
            .let { str ->
                if (decimalSeparator == ",") {
                    str.replace(".", "").replace(",", ".")
                } else {
                    str.replace(",", "")
                }
            }
            .trim()
        if (cleaned.isBlank()) return null
        // Bank statements default to EUR (Belgian context)
        return Money.from(cleaned, Currency.Eur)
    }

    private fun parseDate(raw: String, formatter: DateTimeFormatter?): LocalDate? {
        if (formatter != null) {
            return try {
                val javaDate = java.time.LocalDate.parse(raw, formatter)
                LocalDate(javaDate.year, javaDate.monthValue, javaDate.dayOfMonth)
            } catch (_: DateTimeParseException) {
                null
            }
        }
        return runCatching { LocalDate.parse(raw) }.getOrNull()
    }

    private fun org.apache.commons.csv.CSVRecord.safeGet(columnName: String): String? {
        return try {
            if (isMapped(columnName)) get(columnName) else null
        } catch (_: Exception) {
            null
        }
    }
}
