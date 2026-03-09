package tech.dokus.foundation.aura.components.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.state_retry
import tech.dokus.domain.asbtractions.RetryHandler
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.components.POutlinedButton
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.extensions.localized
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

/**
 * Inline error banner for displaying errors within a screen layout.
 *
 * Unlike [DokusErrorContent] which is designed as a centered fullscreen error,
 * this banner sits inside the content flow alongside skeleton/placeholder UI.
 * It shows a compact card with error icon, message, and optional retry button.
 */
@Composable
fun DokusErrorBanner(
    exception: DokusException,
    retryHandler: RetryHandler?,
    modifier: Modifier = Modifier,
) {
    DokusCardSurface(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Constraints.Spacing.large),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.medium),
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = null,
                modifier = Modifier.size(Constraints.IconSize.medium),
                tint = MaterialTheme.colorScheme.error,
            )
            Text(
                text = exception.localized,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (retryHandler != null && exception.recoverable) {
                Spacer(Modifier.width(Constraints.Spacing.small))
                POutlinedButton(
                    text = stringResource(Res.string.state_retry),
                    onClick = { retryHandler.retry() },
                )
            }
        }
    }
}

@Preview
@Composable
private fun DokusErrorBannerPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        DokusErrorBanner(
            exception = DokusException.ConnectionError(),
            retryHandler = RetryHandler { },
        )
    }
}
