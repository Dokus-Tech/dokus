package tech.dokus.features.cashflow.presentation.review.components.mobile

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.action_back
import tech.dokus.aura.resources.nav_documents
import tech.dokus.features.cashflow.presentation.common.components.chips.DokusStatusChip
import tech.dokus.features.cashflow.presentation.review.DocumentReviewState
import tech.dokus.features.cashflow.presentation.review.SourceEvidenceViewerState
import tech.dokus.features.cashflow.presentation.review.statusBadgeLocalized
import tech.dokus.foundation.aura.components.common.PTopAppBar
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.extensions.colorized
import tech.dokus.foundation.aura.extensions.localizedUppercase
import tech.dokus.features.cashflow.presentation.review.colorized as financialStatusColorized

@Composable
internal fun MobileDocumentDetailTopBar(
    state: DocumentReviewState.Content,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PTopAppBar(
        title = stringResource(Res.string.nav_documents),
        navController = null,
        onBackClick = onBackClick,
        actions = {
            DokusStatusChip(
                label = state.statusBadgeLocalized,
                color = state.financialStatus.financialStatusColorized,
            )
        },
        modifier = modifier,
    )
}

@Composable
internal fun MobileSourceViewerTopBar(
    viewerState: SourceEvidenceViewerState,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PTopAppBar(
        title = stringResource(Res.string.action_back),
        navController = null,
        onBackClick = onBackClick,
        actions = {
            Surface(
                color = viewerState.sourceType.colorized.copy(alpha = 0.12f),
                shape = RoundedCornerShape(8.dp),
            ) {
                Text(
                    text = viewerState.sourceType.localizedUppercase,
                    style = MaterialTheme.typography.labelMedium,
                    color = viewerState.sourceType.colorized,
                    modifier = Modifier.padding(
                        horizontal = Constraints.Spacing.small,
                        vertical = Constraints.Spacing.xSmall,
                    ),
                )
            }
        },
        modifier = modifier,
    )
}
