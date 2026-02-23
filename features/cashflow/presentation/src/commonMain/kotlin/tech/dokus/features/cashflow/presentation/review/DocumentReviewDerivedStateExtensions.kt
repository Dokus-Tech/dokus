package tech.dokus.features.cashflow.presentation.review

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.todayIn
import tech.dokus.domain.enums.DocumentMatchType
import tech.dokus.domain.enums.DocumentSourceStatus
import tech.dokus.domain.model.CashflowEntry
import tech.dokus.domain.model.FinancialDocumentDto
import tech.dokus.domain.model.InvoiceDraftData
import tech.dokus.foundation.app.state.DokusState

val DocumentReviewState.Content.paidAtDate: LocalDate?
    get() = (cashflowEntryState as? DokusState.Success<CashflowEntry>)?.data?.paidAt?.date

val DocumentReviewState.Content.resolvedDueDate: LocalDate?
    get() = when (val data = draftData) {
        is InvoiceDraftData -> data.dueDate
        else -> (document.confirmedEntity as? FinancialDocumentDto.InvoiceDto)?.dueDate
    }

val DocumentReviewState.Content.overdueDays: Int?
    get() {
        if (financialStatus != ReviewFinancialStatus.Overdue) return null
        val dueDate = resolvedDueDate ?: return null
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        return if (dueDate < today) dueDate.daysUntil(today) else null
    }

val DocumentReviewState.Content.hasCrossMatchedSources: Boolean
    get() = document.pendingMatchReview == null &&
        document.sources.any { source ->
            source.status == DocumentSourceStatus.Linked &&
                source.matchType in setOf(
                    DocumentMatchType.ExactFile,
                    DocumentMatchType.SameContent,
                    DocumentMatchType.SameDocument,
                )
        }
