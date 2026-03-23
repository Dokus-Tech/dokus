package tech.dokus.backend.services.cashflow.matching

import tech.dokus.database.entity.BankTransactionEntity
import tech.dokus.database.entity.CashflowEntryEntity
import tech.dokus.domain.Money
import tech.dokus.domain.enums.CashflowDirection
import tech.dokus.domain.enums.MatchSignalType
import tech.dokus.domain.enums.StatementTrust
import tech.dokus.domain.ids.BankTransactionId
import tech.dokus.domain.ids.CashflowEntryId
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.TenantId

/**
 * Result of a single signal evaluation during scoring.
 */
data class SignalResult(
    val signal: MatchSignalType,
    val fired: Boolean,
    val logOdds: Double,
    val detail: String? = null,
)

/**
 * A candidate document/entry that could match a transaction,
 * before scoring.
 */
data class MatchCandidate(
    val entry: CashflowEntryEntity,
    val contactIban: String?,
    val contactName: String?,
    val contactId: ContactId?,
    val invoiceReference: String?,
)

/**
 * A scored candidate with full signal breakdown.
 */
data class ScoredCandidate(
    val candidate: MatchCandidate,
    val transaction: BankTransactionEntity,
    val score: Double,
    val signals: List<SignalResult>,
    val hasHardSignal: Boolean,
) {
    val entryId: CashflowEntryId get() = candidate.entry.id
    val evidenceStrings: List<String>
        get() = signals.filter { it.fired }.map { it.signal.dbValue }
}

/**
 * Classification of a match decision.
 */
enum class MatchDecisionType {
    /** Strong enough for auto-match + auto-pay. */
    AutoMatch,

    /** Decent score but needs human review. */
    NeedsReview,

    /** Below threshold — discard. */
    Discard,
}

/**
 * Final decision for a transaction after conflict resolution.
 */
data class MatchDecision(
    val transactionId: BankTransactionId,
    val tenantId: TenantId,
    val type: MatchDecisionType,
    val winner: ScoredCandidate?,
    val margin: Double,
    val trust: StatementTrust,
)

/**
 * Lightweight candidate filter criteria for SQL-level blocking.
 */
data class CandidateBlockCriteria(
    val tenantId: TenantId,
    val direction: CashflowDirection,
    val absoluteAmount: Money,
    val toleranceMinor: Long,
)
