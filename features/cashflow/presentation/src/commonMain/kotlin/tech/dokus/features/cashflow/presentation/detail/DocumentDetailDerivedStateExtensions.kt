package tech.dokus.features.cashflow.presentation.detail

import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import tech.dokus.domain.enums.SourceMatchKind
import tech.dokus.domain.enums.DocumentSourceStatus
import tech.dokus.domain.model.CashflowEntryDto
import tech.dokus.foundation.app.state.DokusState

val DocumentDetailState.paidAtDate: LocalDate?
    get() = (cashflowEntryState as? DokusState.Success<CashflowEntryDto>)?.data?.paidAt?.date

val DocumentDetailState.overdueDays: Int?
    get() {
        if (financialStatus != ReviewFinancialStatus.Overdue) return null
        val dueDate = resolvedDueDate ?: return null
        return if (dueDate < today) dueDate.daysUntil(today) else null
    }

val DocumentDetailState.hasCrossMatchedSources: Boolean
    get() = documentRecord?.pendingMatchReview == null &&
        (documentRecord?.sources?.any { source ->
            source.status == DocumentSourceStatus.Linked &&
                source.matchType in setOf(
                    SourceMatchKind.ExactFile,
                    SourceMatchKind.SameContent,
                    SourceMatchKind.SameDocument,
                )
        } == true)
