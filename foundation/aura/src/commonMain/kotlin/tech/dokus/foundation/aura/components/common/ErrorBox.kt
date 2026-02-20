package tech.dokus.foundation.aura.components.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.ui.tooling.preview.PreviewParameter
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.exception_connection_error
import tech.dokus.aura.resources.state_error
import tech.dokus.aura.resources.state_retry
import tech.dokus.domain.asbtractions.RetryHandler
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.foundation.aura.components.POutlinedButton
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.extensions.localized
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

@Composable
fun DokusErrorText(
    text: String,
    modifier: Modifier = Modifier.padding(all = Constraints.Spacing.large)
) {
    Text(
        text,
        modifier = modifier,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.error
    )
}

@Composable
fun DokusErrorContent(
    text: String,
    retryHandler: RetryHandler?,
    title: String? = null,
    compact: Boolean = false,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    if (compact) {
        DokusErrorContentCompact(
            text = text,
            retryHandler = retryHandler,
            modifier = modifier
        )
    } else {
        DokusErrorContentFull(
            text = text,
            retryHandler = retryHandler,
            title = title,
            modifier = modifier
        )
    }
}

@Composable
private fun DokusErrorContentFull(
    text: String,
    retryHandler: RetryHandler?,
    title: String?,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
    ) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = null,
            modifier = Modifier.size(Constraints.IconSize.xxLarge),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(Constraints.Spacing.xLarge))
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(Constraints.Spacing.small))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (retryHandler != null) {
            Spacer(modifier = Modifier.height(Constraints.Spacing.large))
            POutlinedButton(
                text = stringResource(Res.string.state_retry),
                onClick = { retryHandler.retry() }
            )
        }
    }
}

/**
 * Compact error content for inline display in cards.
 * Shows icon, text, and retry button in a horizontal layout.
 */
@Composable
private fun DokusErrorContentCompact(
    text: String,
    retryHandler: RetryHandler?,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
        modifier = modifier
    ) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = null,
            modifier = Modifier.size(Constraints.IconSize.medium),
            tint = MaterialTheme.colorScheme.error
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (retryHandler != null) {
            POutlinedButton(
                text = stringResource(Res.string.state_retry),
                onClick = { retryHandler.retry() }
            )
        }
    }
}

@Composable
fun DokusErrorContent(
    exception: DokusException,
    retryHandler: RetryHandler?,
    title: String? = null,
    compact: Boolean = false,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    DokusErrorContent(
        title = title,
        text = exception.localized,
        retryHandler = retryHandler.takeIf { exception.recoverable },
        compact = compact,
        modifier = modifier
    )
}

@Composable
fun DokusErrorText(exception: DokusException, modifier: Modifier = Modifier.padding(all = Constraints.Spacing.large)) {
    DokusErrorText(text = exception.localized, modifier)
}

@Preview
@Composable
private fun PulseErrorTextPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        DokusErrorText(text = stringResource(Res.string.state_error))
    }
}

@Preview
@Composable
private fun PulseErrorContentPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        DokusErrorContent(
            title = stringResource(Res.string.state_error),
            text = stringResource(Res.string.exception_connection_error),
            retryHandler = RetryHandler { }
        )
    }
}
