package tech.dokus.foundation.aura.components.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.CircleAlert
import com.composables.icons.lucide.CloudOff
import com.composables.icons.lucide.Lucide
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.state_offline
import tech.dokus.aura.resources.state_retry
import tech.dokus.domain.asbtractions.RetryHandler
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.foundation.aura.components.POutlinedButton
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.extensions.localized
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

/**
 * Overlay that blurs content and shows an error indicator when an exception is present.
 *
 * Preserves the underlying layout (showing last-known data or skeleton behind blur)
 * while communicating that the content is stale or unavailable.
 *
 * @param exception The error to display, or null to show content normally
 * @param retryHandler Retry callback, shown only when exception is recoverable
 * @param modifier Optional modifier for the root container
 * @param content The content to display (blurred when error is present)
 */
@Composable
fun ErrorOverlay(
    exception: DokusException?,
    retryHandler: RetryHandler?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier) {
        Box(
            modifier = if (exception != null) {
                Modifier.blur(6.dp).clearAndSetSemantics {}
            } else {
                Modifier
            },
        ) {
            content()
        }

        AnimatedVisibility(
            visible = exception != null,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center,
            ) {
                if (exception != null) {
                    ErrorOverlayContent(
                        exception = exception,
                        retryHandler = retryHandler,
                    )
                }
            }
        }
    }
}

/**
 * Convenience overload for offline state.
 *
 * Shows a [ConnectionError][DokusException.ConnectionError] overlay when [isOffline] is true.
 */
@Composable
fun ErrorOverlay(
    isOffline: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) = ErrorOverlay(
    exception = if (isOffline) DokusException.ConnectionError() else null,
    retryHandler = null,
    modifier = modifier,
    content = content,
)

@Composable
private fun ErrorOverlayContent(
    exception: DokusException,
    retryHandler: RetryHandler?,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = exception.overlayIcon,
            contentDescription = null,
            modifier = Modifier.size(Constraints.IconSize.large),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(Constraints.Spacing.medium))
        Text(
            text = exception.localized,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (exception.recoverable && retryHandler != null) {
            Spacer(modifier = Modifier.height(Constraints.Spacing.medium))
            POutlinedButton(
                text = stringResource(Res.string.state_retry),
                onClick = { retryHandler.retry() },
            )
        }
    }
}

private val DokusException.overlayIcon
    get() = when (this) {
        is DokusException.ConnectionError,
        is DokusException.PeppolDirectoryUnavailable,
        -> Lucide.CloudOff
        else -> Lucide.CircleAlert
    }

@Preview
@Composable
private fun ErrorOverlayPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        ErrorOverlay(
            exception = DokusException.ConnectionError(),
            retryHandler = RetryHandler { },
        ) {
            Text("Content behind overlay")
        }
    }
}

@Preview
@Composable
private fun ErrorOverlayOfflinePreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        ErrorOverlay(isOffline = true) {
            Text(stringResource(Res.string.state_offline))
        }
    }
}
