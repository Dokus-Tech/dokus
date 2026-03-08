package tech.dokus.features.cashflow.presentation.chat.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.chat_loading
import tech.dokus.features.cashflow.presentation.chat.ChatState
import tech.dokus.foundation.aura.components.common.DokusErrorBanner
import tech.dokus.foundation.aura.components.common.DokusLoader
import tech.dokus.foundation.aura.components.common.ShimmerLine
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

@Composable
internal fun LoadingContent(contentPadding: PaddingValues) {
    Box(
        modifier = androidx.compose.ui.Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.medium)
        ) {
            DokusLoader()
            Text(
                text = stringResource(Res.string.chat_loading),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
internal fun ErrorContent(
    error: ChatState.Error,
    contentPadding: PaddingValues,
) {
    Column(
        modifier = androidx.compose.ui.Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(Constraints.Spacing.large),
        verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.large),
    ) {
        DokusErrorBanner(
            exception = error.exception,
            retryHandler = error.retryHandler,
        )
        // Chat message skeleton
        repeat(3) {
            ShimmerLine(
                modifier = androidx.compose.ui.Modifier
                    .fillMaxWidth(if (it % 2 == 0) 0.7f else 0.5f),
            )
        }
    }
}

@Preview
@Composable
private fun LoadingContentPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        LoadingContent(contentPadding = PaddingValues(0.dp))
    }
}
