package tech.dokus.backend.services.banking

import java.security.MessageDigest
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import tech.dokus.backend.services.banking.AccountResolutionService.AccountResolution
import tech.dokus.backend.services.banking.StatementDedupService.StatementDedupOutcome
import tech.dokus.database.repository.banking.BankStatementRepository
import tech.dokus.database.repository.banking.BankTransactionCreate
import tech.dokus.database.repository.banking.BankTransactionRepository
import tech.dokus.database.repository.cashflow.DocumentSourceRepository
import tech.dokus.database.repository.cashflow.DocumentSourceSummary
import tech.dokus.domain.Money
import tech.dokus.domain.enums.BankTransactionSource
import tech.dokus.domain.enums.StatementTrust
import tech.dokus.domain.ids.BankAccountId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.DocumentSourceId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.BankStatementDraftData
import tech.dokus.domain.model.BankStatementTransactionDraftRow
import tech.dokus.domain.model.TransactionCommunication
import tech.dokus.foundation.backend.utils.loggerFor

private const val RowConfidenceThreshold = 0.90

data class DiscardedBankStatementRow(
    val index: Int,
    val reason: String,
)

data class BankStatementProcessingResult(
    val sanitizedDraft: BankStatementDraftData,
    val validRows: Int,
    val discardedRows: List<DiscardedBankStatementRow>,
    val statementTrust: StatementTrust,
    val dedupOutcome: StatementDedupOutcome,
    val accountId: BankAccountId?,
)

/**
 * Orchestrates the full bank statement processing pipeline:
 * 1. Statement dedup (skip if exact file hash match)
 * 2. Row validation (discard future dates, zero amounts, low confidence)
 * 3. Account resolution (find or auto-create account from IBAN)
 * 4. Trust calculation (reconcile opening + txns = closing)
 * 5. Create statement record
 * 6. Persist transactions
 */
class BankStatementProcessingService(
    private val statementDedupService: StatementDedupService,
    private val accountResolutionService: AccountResolutionService,
    private val trustCalculator: StatementTrustCalculator,
    private val bankStatementRepository: BankStatementRepository,
    private val bankTransactionRepository: BankTransactionRepository,
    private val documentSourceRepository: DocumentSourceRepository,
) {
    private val logger = loggerFor()

    suspend fun process(
        tenantId: TenantId,
        documentId: DocumentId,
        sourceId: DocumentSourceId?,
        draftData: BankStatementDraftData,
        source: BankTransactionSource = BankTransactionSource.PdfStatement,
    ): BankStatementProcessingResult {
        // Resolve source metadata (file hash + content type for source detection)
        val resolvedSource = resolveSource(tenantId, documentId, sourceId)
        val fileHash = resolvedSource?.inputHash
        val effectiveSource = if (resolvedSource?.contentType == "text/csv") {
            BankTransactionSource.CsvStatement
        } else {
            source
        }

        // 1. Statement dedup
        val dedupOutcome = if (fileHash != null) {
            statementDedupService.checkDedup(
                tenantId = tenantId,
                fileHash = fileHash,
                accountIban = draftData.accountIban,
                periodEnd = draftData.periodEnd,
                closingBalance = draftData.closingBalance,
            ).getOrThrow()
        } else {
            logger.warn("No file hash for document {}, skipping dedup", documentId)
            StatementDedupOutcome.New
        }

        if (dedupOutcome is StatementDedupOutcome.Skip) {
            logger.info("Statement skipped (dedup) for document {} tenant {}", documentId, tenantId)
            return BankStatementProcessingResult(
                sanitizedDraft = draftData,
                validRows = 0,
                discardedRows = emptyList(),
                statementTrust = StatementTrust.Low,
                dedupOutcome = dedupOutcome,
                accountId = null,
            )
        }

        // 2. Validate rows
        val today = Clock.System.now().toLocalDateTime(TimeZone.UTC).date
        val discarded = mutableListOf<DiscardedBankStatementRow>()
        val validRows = mutableListOf<BankStatementTransactionDraftRow>()

        draftData.transactions.forEachIndexed { index, row ->
            val date = row.transactionDate
            if (date == null) {
                discarded += DiscardedBankStatementRow(index, "missing_date")
                return@forEachIndexed
            }
            if (date > today) {
                discarded += DiscardedBankStatementRow(index, "date_in_future")
                return@forEachIndexed
            }

            val amount = row.signedAmount
            if (amount == null || amount.isZero) {
                discarded += DiscardedBankStatementRow(index, "invalid_or_zero_amount")
                return@forEachIndexed
            }

            if (row.rowConfidence < RowConfidenceThreshold) {
                discarded += DiscardedBankStatementRow(index, "low_confidence")
                return@forEachIndexed
            }

            validRows += row
        }

        // 3. Account resolution
        val validRowAmounts = validRows.sumOf { it.signedAmount?.minor ?: 0L }
        val accountResolution = accountResolutionService.resolve(
            tenantId = tenantId,
            draftData = draftData,
            validRowAmounts = validRowAmounts,
        ).getOrThrow()

        val accountId = (accountResolution as? AccountResolution.Resolved)?.accountId
        val accountStatus = (accountResolution as? AccountResolution.Resolved)?.accountStatus

        if (accountId == null) {
            logger.warn(
                "No bank account resolved for document {}: IBAN={}, tenant={}",
                documentId, draftData.accountIban?.value, tenantId
            )
        }

        // 4. Trust calculation
        val trustResult = trustCalculator.calculate(
            draftData = draftData,
            validRowAmounts = validRowAmounts,
            accountStatus = accountStatus,
        )

        // 5. Create statement record
        bankStatementRepository.create(
            tenantId = tenantId,
            bankAccountId = accountId,
            documentId = documentId,
            source = effectiveSource,
            statementTrust = trustResult.trust,
            fileHash = fileHash,
            accountIban = draftData.accountIban,
            periodStart = draftData.periodStart,
            periodEnd = draftData.periodEnd,
            openingBalance = draftData.openingBalance,
            closingBalance = draftData.closingBalance,
            transactionCount = validRows.size,
        )

        // 6. Persist transactions
        val inserts = validRows.map { row ->
            val date = requireNotNull(row.transactionDate)
            val amount = requireNotNull(row.signedAmount)
            val structured = row.communication as? TransactionCommunication.Structured
            val structuredRaw = structured?.raw
            val normalizedComm = structured?.normalized?.value
            val freeComm = (row.communication as? TransactionCommunication.FreeForm)?.text
            BankTransactionCreate(
                dedupHash = hashRow(
                    date = date,
                    amount = amount,
                    description = row.descriptionRaw,
                    structuredCommunication = structuredRaw,
                    counterpartyName = row.counterparty.name,
                ),
                bankAccountId = accountId,
                transactionDate = date,
                signedAmount = amount,
                counterpartyName = row.counterparty.name,
                counterpartyIban = row.counterparty.iban?.value,
                counterpartyBic = row.counterparty.bic?.value,
                structuredCommunicationRaw = structuredRaw,
                normalizedStructuredCommunication = normalizedComm,
                freeCommunication = freeComm,
                descriptionRaw = row.descriptionRaw,
                statementTrust = trustResult.trust,
            )
        }

        bankTransactionRepository.replaceForDocument(
            tenantId = tenantId,
            documentId = documentId,
            rows = inserts,
        )

        discarded.forEach {
            logger.info(
                "Discarded bank statement row for document {} at index {} due to {}",
                documentId,
                it.index,
                it.reason,
            )
        }

        return BankStatementProcessingResult(
            sanitizedDraft = draftData.copy(transactions = validRows),
            validRows = validRows.size,
            discardedRows = discarded,
            statementTrust = trustResult.trust,
            dedupOutcome = dedupOutcome,
            accountId = accountId,
        )
    }

    private suspend fun resolveSource(
        tenantId: TenantId,
        documentId: DocumentId,
        sourceId: DocumentSourceId?,
    ): DocumentSourceSummary? {
        return if (sourceId != null) {
            documentSourceRepository.getById(tenantId, sourceId)
        } else {
            documentSourceRepository.selectPreferredSource(tenantId, documentId)
        }
    }

    companion object {
        private val WhitespaceRegex = Regex("\\s+")

        fun normalizeStructuredCommunication(raw: String?): String? {
            if (raw.isNullOrBlank()) return null
            return raw.trim().uppercase().replace(WhitespaceRegex, "")
        }

        fun hashRow(
            date: kotlinx.datetime.LocalDate,
            amount: Money,
            description: String?,
            structuredCommunication: String?,
            counterpartyName: String?,
        ): String {
            val raw = listOf(
                date.toString(),
                amount.minor.toString(),
                description?.trim().orEmpty(),
                structuredCommunication?.trim().orEmpty(),
                counterpartyName?.trim().orEmpty(),
            ).joinToString("|")
            return MessageDigest.getInstance("SHA-256")
                .digest(raw.toByteArray())
                .joinToString("") { "%02x".format(it) }
        }
    }
}
