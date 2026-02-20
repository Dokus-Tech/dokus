package tech.dokus.features.cashflow.presentation.review.route

import tech.dokus.domain.enums.DocumentListFilter
import tech.dokus.features.cashflow.presentation.documents.mvi.DocumentFilter
import tech.dokus.features.cashflow.presentation.review.DocumentReviewRouteContext
import tech.dokus.navigation.destinations.CashFlowDestination

internal fun DocumentFilter.toRouteFilter(): CashFlowDestination.DocumentReviewSourceFilter = when (this) {
    DocumentFilter.All -> CashFlowDestination.DocumentReviewSourceFilter.All
    DocumentFilter.NeedsAttention -> CashFlowDestination.DocumentReviewSourceFilter.NeedsAttention
    DocumentFilter.Confirmed -> CashFlowDestination.DocumentReviewSourceFilter.Confirmed
}

internal fun DocumentFilter.toRouteFilterToken(): String = toRouteFilter().token

internal fun CashFlowDestination.DocumentReviewSourceFilter.toListFilter(): DocumentListFilter = when (this) {
    CashFlowDestination.DocumentReviewSourceFilter.All -> DocumentListFilter.All
    CashFlowDestination.DocumentReviewSourceFilter.NeedsAttention -> DocumentListFilter.NeedsAttention
    CashFlowDestination.DocumentReviewSourceFilter.Confirmed -> DocumentListFilter.Confirmed
}

internal fun CashFlowDestination.DocumentReview.toRouteContextOrNull(): DocumentReviewRouteContext? {
    val sourceFilter = CashFlowDestination.DocumentReviewSourceFilter.fromToken(sourceFilter) ?: return null
    val sourceSearch = sourceSearch?.trim()?.takeIf { it.isNotEmpty() }
    val sourceSort = CashFlowDestination.DocumentReviewSourceSort.fromToken(sourceSort)
    return DocumentReviewRouteContext(
        filter = sourceFilter,
        search = sourceSearch,
        sort = sourceSort,
    )
}
