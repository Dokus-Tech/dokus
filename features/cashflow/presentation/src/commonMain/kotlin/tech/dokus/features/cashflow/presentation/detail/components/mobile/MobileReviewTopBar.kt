package tech.dokus.features.cashflow.presentation.detail.components.mobile

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
import tech.dokus.aura.resources.action_download_pdf
import tech.dokus.aura.resources.nav_documents
import tech.dokus.features.cashflow.presentation.common.components.chips.DokusStatusChip
import tech.dokus.features.cashflow.presentation.detail.DocumentDetailState
import tech.dokus.features.cashflow.presentation.detail.components.DownloadPdfButton
import tech.dokus.features.cashflow.presentation.detail.SourceEvidenceViewerState
import tech.dokus.features.cashflow.presentation.detail.statusBadgeLocalized
import tech.dokus.foundation.aura.components.PButton
import tech.dokus.foundation.aura.components.PButtonVariant
import tech.dokus.foundation.aura.components.common.PTopAppBar
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.extensions.colorized
import tech.dokus.foundation.aura.extensions.localizedUppercase
import tech.dokus.features.cashflow.presentation.detail.colorized as financialStatusColorized

@Composable
internal fun MobileDocumentDetailTopBar(
    state: DocumentDetailState,
    onBackClick: () -> Unit,
    onDownloadPdf: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PTopAppBar(
        title = stringResource(Res.string.nav_documents),
        navController = null,
        onBackClick = onBackClick,
        actions = {
            if (state.hasContent) {
                DownloadPdfButton(
                    downloadState = state.downloadState,
                    onClick = onDownloadPdf,
                    formatLabel = state.downloadFormatLabel,
                )
            }
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
