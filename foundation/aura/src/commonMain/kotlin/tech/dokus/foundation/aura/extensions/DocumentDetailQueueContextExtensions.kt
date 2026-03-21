package tech.dokus.foundation.aura.extensions

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.cashflow_review_back
import tech.dokus.aura.resources.cashflow_review_back_to_contact
import tech.dokus.aura.resources.cashflow_review_back_to_documents
import tech.dokus.aura.resources.cashflow_review_back_to_search
import tech.dokus.navigation.destinations.CashFlowDestination.DocumentDetailQueueContext

val DocumentDetailQueueContext.localized: String
    @Composable get() = when (this) {
        is DocumentDetailQueueContext.DocumentList -> stringResource(Res.string.cashflow_review_back_to_documents)
        is DocumentDetailQueueContext.Contact -> stringResource(Res.string.cashflow_review_back_to_contact)
        is DocumentDetailQueueContext.Search -> stringResource(Res.string.cashflow_review_back_to_search)
        is DocumentDetailQueueContext.Recent -> stringResource(Res.string.cashflow_review_back)
    }
