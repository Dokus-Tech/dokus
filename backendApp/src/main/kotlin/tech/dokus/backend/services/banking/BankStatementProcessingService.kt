package tech.dokus.backend.services.banking

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import tech.dokus.backend.services.banking.AccountResolutionService.AccountResolution
import tech.dokus.backend.services.banking.StatementDedupService.StatementDedupOutcome
import tech.dokus.database.entity.DocumentSourceEntity
import tech.dokus.database.repository.banking.BankStatementRepository
import tech.dokus.database.repository.banking.BankTransactionCreate
import tech.dokus.database.repository.banking.BankTransactionRepository
import tech.dokus.database.repository.cashflow.DocumentSourceRepository
import tech.dokus.domain.Money
import tech.dokus.domain.enums.BankTransactionSource
import tech.dokus.domain.enums.StatementTrust
import tech.dokus.domain.ids.BankAccountId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.DocumentSourceId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.BankStatementDraftData
import tech.dokus.domain.model.BankStatementTransactionDraftRowDto
import tech.dokus.domain.model.TransactionCommunicationDto
import tech.dokus.foundation.backend.utils.loggerFor
import java.security.MessageDigest

private const val RowConfidenceThreshold = 0.90

data class DiscardedBankStatementRow(
    val index: Int,
    val reason: String,
)

data class BankStatementPrepareResult(
    val sanitizedDraft: BankStatementDraftData,
    val validRows: Int,
    val discardedRows: List<DiscardedBankStatementRow>,
    val statementTrust: StatementTrust,
    val dedupOutcome: StatementDedupOutcome,
    val accountId: BankAccountId?,
    val effectiveSource: BankTransactionSource,
    val fileHash: String?,
    val hasDuplicates: Boolean,
)

/**
 * Orchestrates the bank statement processing pipeline:
 * 1. Statement dedup (skip if exact file hash match)
 * 2. Row validation (discard future dates, zero amounts, low confidence)
 * 3. Account resolution (find or auto-create account from IBAN)
 * 4. Trust calculation (reconcile opening + txns = closing)
 * 5. Duplicate detection (flag rows matching existing transactions)
 *
 * Persistence is separate via [persistTransactions] — called immediately
 * when no duplicates, or deferred until user confirms when duplicates exist.
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

    /**
     * Validates rows, resolves account, calculates trust, detects duplicates.
     * Does NOT persist transactions — call [persistTransactions] for that.
     */
    suspend fun prepare(
        tenantId: TenantId,
        documentId: DocumentId,
        sourceId: DocumentSourceId?,
        draftData: BankStatementDraftData,
        source: BankTransactionSource = BankTransactionSource.PdfStatement,
    ): BankStatementPrepareResult {
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
            return BankStatementPrepareResult(
                sanitizedDraft = draftData,
                validRows = 0,
                discardedRows = emptyList(),
                statementTrust = StatementTrust.Low,
                dedupOutcome = dedupOutcome,
                accountId = null,
                effectiveSource = effectiveSource,
                fileHash = fileHash,
                hasDuplicates = false,
            )
        }

        // 2. Validate rows
        val today = Clock.System.now().toLocalDateTime(TimeZone.UTC).date
        val discarded = mutableListOf<DiscardedBankStatementRow>()
        val validRows = mutableListOf<BankStatementTransactionDraftRowDto>()

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
                documentId,
                draftData.accountIban?.value,
                tenantId
            )
        }

        // 4. Trust calculation
        val trustResult = trustCalculator.calculate(
            draftData = draftData,
            validRowAmounts = validRowAmounts,
            accountStatus = accountStatus,
        )

        // 5. Duplicate detection — check (date, amount) against existing transactions for same account
        val annotatedRows: List<BankStatementTransactionDraftRowDto>
        var hasDuplicates = false

        if (accountId != null) {
            val existingTransactions = bankTransactionRepository.findByDateAndAmount(
                tenantId = tenantId,
                bankAccountId = accountId,
                datesToCheck = validRows.mapNotNull { row ->
                    val date = row.transactionDate ?: return@mapNotNull null
                    val amount = row.signedAmount ?: return@mapNotNull null
                    BankTransactionRepository.DateAmountKey(date, amount)
                },
            )

            annotatedRows = validRows.map { row ->
                val isDuplicate = row.transactionDate != null && row.signedAmount != null &&
                    BankTransactionRepository.DateAmountKey(row.transactionDate!!, row.signedAmount!!) in existingTransactions
                if (isDuplicate) hasDuplicates = true
                row.copy(potentialDuplicate = isDuplicate)
            }
        } else {
            annotatedRows = validRows
        }

        discarded.forEach {
            logger.info(
                "Discarded bank statement row for document {} at index {} due to {}",
                documentId,
                it.index,
                it.reason,
            )
        }

        return BankStatementPrepareResult(
            sanitizedDraft = draftData.copy(transactions = annotatedRows),
            validRows = annotatedRows.size,
            discardedRows = discarded,
            statementTrust = trustResult.trust,
            dedupOutcome = dedupOutcome,
            accountId = accountId,
            effectiveSource = effectiveSource,
            fileHash = fileHash,
            hasDuplicates = hasDuplicates,
        )
    }

    /**
     * Persists validated transactions to the database.
     * Creates the bank_statements record and inserts bank_transactions rows.
     * Only call after [prepare] returns successfully.
     */
    suspend fun persistTransactions(
        tenantId: TenantId,
        documentId: DocumentId,
        prepareResult: BankStatementPrepareResult,
    ) {
        val rows = prepareResult.sanitizedDraft.transactions.filter { !it.excluded }

        // Create statement record
        bankStatementRepository.create(
            tenantId = tenantId,
            bankAccountId = prepareResult.accountId,
            documentId = documentId,
            source = prepareResult.effectiveSource,
            statementTrust = prepareResult.statementTrust,
            fileHash = prepareResult.fileHash,
            accountIban = prepareResult.sanitizedDraft.accountIban,
            periodStart = prepareResult.sanitizedDraft.periodStart,
            periodEnd = prepareResult.sanitizedDraft.periodEnd,
            openingBalance = prepareResult.sanitizedDraft.openingBalance,
            closingBalance = prepareResult.sanitizedDraft.closingBalance,
            transactionCount = rows.size,
        )

        // Persist transactions
        val inserts = rows.mapNotNull { row ->
            val date = row.transactionDate ?: return@mapNotNull null
            val amount = row.signedAmount ?: return@mapNotNull null
            val structured = row.communication as? TransactionCommunicationDto.Structured
            val structuredRaw = structured?.raw
            val normalizedComm = structured?.normalized?.value
            val freeComm = (row.communication as? TransactionCommunicationDto.FreeForm)?.text
            BankTransactionCreate(
                dedupHash = hashRow(
                    date = date,
                    amount = amount,
                    description = row.descriptionRaw,
                    structuredCommunication = structuredRaw,
                    counterpartyName = row.counterparty.name,
                ),
                source = prepareResult.effectiveSource,
                bankAccountId = prepareResult.accountId,
                transactionDate = date,
                signedAmount = amount,
                counterpartyName = row.counterparty.name,
                counterpartyIban = row.counterparty.iban?.value,
                counterpartyBic = row.counterparty.bic?.value,
                structuredCommunicationRaw = structuredRaw,
                normalizedStructuredCommunication = normalizedComm,
                freeCommunication = freeComm,
                descriptionRaw = row.descriptionRaw,
                statementTrust = prepareResult.statementTrust,
            )
        }

        bankTransactionRepository.replaceForDocument(
            tenantId = tenantId,
            documentId = documentId,
            rows = inserts,
        )
    }

    private suspend fun resolveSource(
        tenantId: TenantId,
        documentId: DocumentId,
        sourceId: DocumentSourceId?,
    ): DocumentSourceEntity? {
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
