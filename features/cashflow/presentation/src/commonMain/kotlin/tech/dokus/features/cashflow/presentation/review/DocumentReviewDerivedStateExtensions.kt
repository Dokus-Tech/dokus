package tech.dokus.features.cashflow.presentation.review

import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import tech.dokus.domain.enums.SourceMatchKind
import tech.dokus.domain.enums.DocumentSourceStatus
import tech.dokus.domain.model.CashflowEntry
import tech.dokus.foundation.app.state.DokusState

val DocumentReviewState.paidAtDate: LocalDate?
    get() = (cashflowEntryState as? DokusState.Success<CashflowEntry>)?.data?.paidAt?.date

val DocumentReviewState.overdueDays: Int?
    get() {
        if (financialStatus != ReviewFinancialStatus.Overdue) return null
        val dueDate = resolvedDueDate ?: return null
        return if (dueDate < today) dueDate.daysUntil(today) else null
    }

val DocumentReviewState.hasCrossMatchedSources: Boolean
    get() = documentRecord?.pendingMatchReview == null &&
        (documentRecord?.sources?.any { source ->
            source.status == DocumentSourceStatus.Linked &&
                source.matchType in setOf(
                    SourceMatchKind.ExactFile,
                    SourceMatchKind.SameContent,
                    SourceMatchKind.SameDocument,
                )
        } == true)
