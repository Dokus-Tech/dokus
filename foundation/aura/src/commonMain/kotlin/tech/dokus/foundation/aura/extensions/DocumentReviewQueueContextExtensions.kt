package tech.dokus.foundation.aura.extensions

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.cashflow_review_back
import tech.dokus.aura.resources.cashflow_review_back_to_contact
import tech.dokus.aura.resources.cashflow_review_back_to_documents
import tech.dokus.aura.resources.cashflow_review_back_to_search
import tech.dokus.navigation.destinations.CashFlowDestination.DocumentReviewQueueContext

val DocumentReviewQueueContext.localized: String
    @Composable get() = when (this) {
        is DocumentReviewQueueContext.DocumentList -> stringResource(Res.string.cashflow_review_back_to_documents)
        is DocumentReviewQueueContext.Contact -> stringResource(Res.string.cashflow_review_back_to_contact)
        is DocumentReviewQueueContext.Search -> stringResource(Res.string.cashflow_review_back_to_search)
        is DocumentReviewQueueContext.Recent -> stringResource(Res.string.cashflow_review_back)
    }
