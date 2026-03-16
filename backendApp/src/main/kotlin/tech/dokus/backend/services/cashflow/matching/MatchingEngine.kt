package tech.dokus.backend.services.cashflow.matching

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import tech.dokus.database.repository.banking.BankTransactionRepository
import tech.dokus.database.repository.contacts.ContactRepository
import tech.dokus.domain.enums.AutoPaymentTriggerSource
import tech.dokus.domain.enums.BankTransactionStatus
import tech.dokus.domain.ids.CashflowEntryId
import tech.dokus.domain.enums.CashflowSourceType
import tech.dokus.domain.enums.MatchedBy
import tech.dokus.domain.enums.ResolutionType
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.BankTransactionDto
import tech.dokus.domain.model.CashflowEntry
import tech.dokus.backend.services.banking.sse.BankingSsePublisher
import tech.dokus.backend.services.cashflow.AutoPaymentService
import tech.dokus.foundation.backend.utils.loggerFor
import tech.dokus.foundation.backend.utils.runSuspendCatching
import java.util.UUID

/**
 * Orchestrates the full matching pipeline:
 *
 * 1. Pre-classify transactions (skip known non-matchable)
 * 2. Load and filter candidate entries (direction + amount range)
 * 3. Score each (tx, candidate) pair via Bayesian model
 * 4. Resolve conflicts (margin between best and second-best)
 * 5. Classify decision (AutoMatch / NeedsReview / Discard)
 * 6. Persist match links and trigger auto-payment
 */
class MatchingEngine(
    private val matchingRepository: MatchingRepository,
    private val matchScorer: MatchScorer,
    private val matchFeedbackStore: MatchFeedbackStore,
    private val bankTransactionRepository: BankTransactionRepository,
    private val contactRepository: ContactRepository,
    private val autoPaymentService: AutoPaymentService,
    private val bankingSsePublisher: BankingSsePublisher,
    private val transferDetector: TransferDetector,
) {
    private val logger = loggerFor()

    /**
     * Returns candidate transactions for a cashflow entry.
     * Combines NeedsReview suggestions with all selectable (unmatched) transactions.
     */
    suspend fun getPaymentCandidates(
        tenantId: TenantId,
        cashflowEntryId: CashflowEntryId,
    ): List<BankTransactionDto> {
        val candidates = bankTransactionRepository.listCandidatesForEntry(tenantId, cashflowEntryId)
        val selectable = bankTransactionRepository.listSelectable(tenantId)
        val candidateIds = candidates.map { it.id }.toSet()
        return candidates + selectable.filter { it.id !in candidateIds }
    }

    /**
     * Run matching for all unmatched transactions from a bank statement document.
     */
    suspend fun matchBankStatement(
        tenantId: TenantId,
        documentId: DocumentId,
    ) {
        val transactions = bankTransactionRepository.listByDocument(tenantId, documentId)
            .filter { it.status == BankTransactionStatus.Unmatched }
        if (transactions.isEmpty()) return

        matchTransactions(tenantId, transactions, AutoPaymentTriggerSource.BankImport)
    }

    /**
     * Run matching for a specific set of transactions.
     * Reusable for all trigger sources (bank import, invoice confirmed, contact updated).
     */
    suspend fun matchTransactions(
        tenantId: TenantId,
        transactions: List<BankTransactionDto>,
        triggerSource: AutoPaymentTriggerSource,
    ) {
        if (transactions.isEmpty()) return

        // Load contact cache (shared across all transaction scoring)
        val contactCache = mutableMapOf<String, tech.dokus.domain.model.contact.ContactDto?>()
        suspend fun resolveContact(entry: CashflowEntry): tech.dokus.domain.model.contact.ContactDto? {
            val contactId = entry.contactId ?: return null
            val key = contactId.toString()
            if (contactCache.containsKey(key)) return contactCache[key]
            val resolved = contactRepository.getContact(contactId, tenantId).getOrNull()
            contactCache[key] = resolved
            return resolved
        }

        for (tx in transactions) {
            // Step 0: Transfer detection — runs before document matching
            val transferResult = runSuspendCatching { transferDetector.detect(tenantId, tx) }.getOrNull()
            when (transferResult) {
                is TransferResult.ClearPair -> {
                    applyAutoTransfer(tenantId, tx, transferResult)
                    continue
                }
                is TransferResult.LikelyTransfer -> {
                    applySuggestTransfer(tenantId, tx, transferResult)
                    continue
                }
                null -> { /* Not a transfer — continue to document matching */ }
            }

            // Step 1: Pre-classify
            val preClass = TransactionPreClassifier.classify(tx)
            if (preClass.shouldSkip) {
                logger.debug("Pre-classified tx {} as skip: {}", tx.id, preClass.reason)
                continue
            }

            // Step 2: Determine direction and load candidates
            val direction = MatchCandidateBlocker.inferDirection(tx.signedAmount)
            if (direction == tech.dokus.domain.enums.CashflowDirection.Neutral) continue

            val allEntries = matchingRepository.loadCandidateEntries(tenantId, direction)
            if (allEntries.isEmpty()) continue

            // Step 2b: Filter by amount range
            val filtered = MatchCandidateBlocker.filterByAmountRange(allEntries, tx.signedAmount)
            if (filtered.isEmpty()) continue

            // Step 3: Build candidates with contact + invoice metadata
            val invoiceSourceIds = filtered
                .filter { it.sourceType == CashflowSourceType.Invoice }
                .mapNotNull { runCatching { UUID.fromString(it.sourceId) }.getOrNull() }
                .toSet()
            val invoiceMeta = matchingRepository.loadInvoiceMeta(tenantId, invoiceSourceIds)

            // Load rejected pairs for this transaction
            val documentIds = filtered.mapNotNull { it.documentId }
            val rejectedDocIds = matchingRepository.loadRejectedDocumentIds(tenantId, tx.id, documentIds)

            // Step 4: Score all candidates
            val scored = filtered.mapNotNull { entry ->
                val contact = resolveContact(entry)
                val meta = invoiceMeta[entry.sourceId]

                val candidate = MatchCandidate(
                    entry = entry,
                    contactIban = contact?.iban?.value,
                    contactName = contact?.name?.value ?: entry.contactName,
                    contactId = entry.contactId,
                    invoiceReference = meta?.structuredReference,
                )

                matchScorer.score(tx, candidate, meta, rejectedDocIds)
            }.sortedByDescending { it.score }

            val best = scored.firstOrNull() ?: continue

            // Step 5: Resolve conflicts
            val margin = MatchConflictResolver.computeMargin(scored)

            // Step 6: Classify
            val decision = MatchClassifier.classify(best, margin, tx.statementTrust)

            // Step 7: Apply decision
            when (decision) {
                MatchDecisionType.AutoMatch -> applyAutoMatch(tenantId, best, margin, triggerSource)
                MatchDecisionType.NeedsReview -> applyNeedsReview(tenantId, best)
                MatchDecisionType.Discard -> { /* Drop silently */ }
            }
        }
    }

    // ─── Apply decisions ───────────────────────────────────────────────

    private suspend fun applyAutoMatch(
        tenantId: TenantId,
        best: ScoredCandidate,
        margin: Double,
        triggerSource: AutoPaymentTriggerSource,
    ) {
        bankTransactionRepository.markMatched(
            tenantId = tenantId,
            transactionId = best.transaction.id,
            cashflowEntryId = best.entryId,
            matchedBy = MatchedBy.Auto,
            resolutionType = ResolutionType.Document,
            score = best.score,
            evidence = best.evidenceStrings,
        )

        // Record learned pattern
        runSuspendCatching {
            matchFeedbackStore.recordConfirmedMatch(
                tenantId = tenantId,
                counterpartyIban = best.transaction.counterparty.iban?.value,
                contactId = best.candidate.contactId,
            )
        }

        // Trigger auto-payment
        runSuspendCatching {
            val reasonsJson = Json.encodeToString(best.evidenceStrings)
            val rulesJson = Json.encodeToString(
                mapOf("model" to "bayesian", "trust" to best.transaction.statementTrust.dbValue),
            )
            autoPaymentService.applyAutoPayment(
                tenantId = tenantId,
                entry = best.candidate.entry,
                transaction = best.transaction,
                confidenceScore = best.score,
                scoreMargin = margin,
                reasonsJson = reasonsJson,
                rulesJson = rulesJson,
                triggerSource = triggerSource,
            )
        }.onFailure { e ->
            logger.warn(
                "Auto-payment failed for tx {} → entry {}: {}",
                best.transaction.id, best.entryId, e.message,
            )
        }

        bankingSsePublisher.publishMatchUpdated(tenantId, best.transaction.id)

        logger.info(
            "Auto-matched tx {} → entry {} (score={}, margin={}, signals={})",
            best.transaction.id, best.entryId, "%.4f".format(best.score), "%.4f".format(margin), best.evidenceStrings,
        )
    }

    private suspend fun applyNeedsReview(tenantId: TenantId, best: ScoredCandidate) {
        bankTransactionRepository.setMatchCandidate(
            tenantId = tenantId,
            transactionId = best.transaction.id,
            cashflowEntryId = best.entryId,
            score = best.score,
            evidence = best.evidenceStrings,
        )

        bankingSsePublisher.publishMatchUpdated(tenantId, best.transaction.id)

        logger.debug(
            "Needs-review tx {} → entry {} (score={}, signals={})",
            best.transaction.id, best.entryId, "%.4f".format(best.score), best.evidenceStrings,
        )
    }

    // ─── Transfer resolution ────────────────────────────────────────────

    @OptIn(kotlin.uuid.ExperimentalUuidApi::class)
    private suspend fun applyAutoTransfer(
        tenantId: TenantId,
        tx: BankTransactionDto,
        result: TransferResult.ClearPair,
    ) {
        val pairId = kotlin.uuid.Uuid.random()

        // Mark both sides
        bankTransactionRepository.markTransfer(
            tenantId = tenantId,
            transactionId = tx.id,
            transferPairId = pairId,
            matchedBy = MatchedBy.Auto,
        )
        bankTransactionRepository.markTransfer(
            tenantId = tenantId,
            transactionId = result.counterpartTransactionId,
            transferPairId = pairId,
            matchedBy = MatchedBy.Auto,
        )

        bankingSsePublisher.publishMatchUpdated(tenantId, tx.id)
        bankingSsePublisher.publishMatchUpdated(tenantId, result.counterpartTransactionId)

        logger.info(
            "Auto-transfer: {} ↔ {} (pairId={}, dest={})",
            tx.id, result.counterpartTransactionId, pairId, result.destinationAccountId,
        )
    }

    private suspend fun applySuggestTransfer(
        tenantId: TenantId,
        tx: BankTransactionDto,
        result: TransferResult.LikelyTransfer,
    ) {
        // Set to NEEDS_REVIEW — do NOT auto-match one-sided transfers.
        // The transfer suggestion is stored in matchEvidence for UI to display.
        bankTransactionRepository.suggestTransfer(
            tenantId = tenantId,
            transactionId = tx.id,
            evidence = listOf("transfer_suggestion:${result.destinationAccountId}", result.reason),
        )

        bankingSsePublisher.publishMatchUpdated(tenantId, tx.id)

        logger.info(
            "Likely transfer (one-sided): {} → account {} ({})",
            tx.id, result.destinationAccountId, result.reason,
        )
    }
}
