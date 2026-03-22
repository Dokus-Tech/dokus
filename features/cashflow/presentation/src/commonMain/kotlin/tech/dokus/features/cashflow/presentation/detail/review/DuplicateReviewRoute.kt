package tech.dokus.features.cashflow.presentation.detail.review

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import org.koin.core.parameter.parametersOf
import pro.respawn.flowmvi.compose.dsl.DefaultLifecycle
import pro.respawn.flowmvi.compose.dsl.subscribe
import tech.dokus.domain.enums.ReviewReason
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.DocumentMatchReviewId
import tech.dokus.features.cashflow.presentation.detail.DuplicateReviewAction
import tech.dokus.features.cashflow.presentation.detail.DuplicateReviewContainer
import tech.dokus.foundation.app.mvi.container

/**
 * Route composable for the duplicate review flow.
 *
 * Creates a [DuplicateReviewContainer] that loads both documents,
 * computes diffs, and handles Same/Different resolution.
 */
@Composable
internal fun DuplicateReviewRoute(
    existingDocumentId: DocumentId,
    incomingDocumentId: DocumentId,
    reviewId: DocumentMatchReviewId,
    reasonType: ReviewReason,
    contentPadding: PaddingValues,
    onResolved: () -> Unit,
    onSwitchToDetail: () -> Unit,
    container: DuplicateReviewContainer = container {
        parametersOf(existingDocumentId, incomingDocumentId, reviewId, reasonType)
    },
    modifier: Modifier = Modifier,
) {
    val state by container.store.subscribe(DefaultLifecycle) { action ->
        when (action) {
            DuplicateReviewAction.Resolved -> onResolved()
        }
    }

    DesktopDuplicateReviewSurface(
        state = state,
        contentPadding = contentPadding,
        onResolveSame = { container.store.intent(tech.dokus.features.cashflow.presentation.detail.DuplicateReviewIntent.ResolveSame) },
        onResolveDifferent = { container.store.intent(tech.dokus.features.cashflow.presentation.detail.DuplicateReviewIntent.ResolveDifferent) },
        onSwitchToDetail = onSwitchToDetail,
        modifier = modifier,
    )
}
