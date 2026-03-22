package tech.dokus.features.cashflow.presentation.detail.components

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.action_download_retry
import tech.dokus.features.cashflow.presentation.detail.DownloadState
import tech.dokus.foundation.aura.components.PButton
import tech.dokus.foundation.aura.components.PButtonVariant

@Composable
internal fun DownloadPdfButton(
    downloadState: DownloadState,
    onClick: () -> Unit,
    formatLabel: String = "PDF",
) {
    val downloadText = "↓ $formatLabel"
    when (downloadState) {
        DownloadState.Idle -> PButton(
            text = downloadText,
            variant = PButtonVariant.OutlineMuted,
            onClick = onClick,
        )

        DownloadState.Downloading -> PButton(
            text = downloadText,
            variant = PButtonVariant.OutlineMuted,
            isLoading = true,
            onClick = {},
        )

        DownloadState.Failed -> PButton(
            text = stringResource(Res.string.action_download_retry),
            variant = PButtonVariant.Outline,
            onClick = onClick,
        )
    }
}
