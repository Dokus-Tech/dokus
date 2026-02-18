package tech.dokus.app.share

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import tech.dokus.foundation.aura.components.common.DokusLoader
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.share_import_file_progress
import tech.dokus.aura.resources.share_import_go_to_login
import tech.dokus.aura.resources.share_import_needs_review_count
import tech.dokus.aura.resources.share_import_open_app
import tech.dokus.aura.resources.share_import_overall_progress
import tech.dokus.aura.resources.share_import_preparing
import tech.dokus.aura.resources.share_import_success_multiple
import tech.dokus.aura.resources.share_import_success_single
import tech.dokus.aura.resources.share_import_success_summary_multiple
import tech.dokus.aura.resources.share_import_title
import tech.dokus.aura.resources.share_import_uploading_to
import tech.dokus.aura.resources.share_import_workspace_switch_hint
import tech.dokus.aura.resources.state_error
import tech.dokus.aura.resources.state_retry
import tech.dokus.app.share.ShareImportIntent.NavigateToLogin
import tech.dokus.app.share.ShareImportIntent.OpenApp
import tech.dokus.foundation.aura.components.POutlinedButton
import tech.dokus.foundation.aura.components.common.AnimatedCheck
import tech.dokus.foundation.aura.components.common.PTopAppBar
import tech.dokus.foundation.aura.extensions.localized
import tech.dokus.domain.exceptions.DokusException

@Composable
internal fun ShareImportScreen(
    state: ShareImportState,
    onIntent: (ShareImportIntent) -> Unit
) {
    ShareImportBackHandler(
        enabled = state is ShareImportState.Uploading || state is ShareImportState.SuccessPulse
    )

    Scaffold(
        topBar = {
            PTopAppBar(
                title = Res.string.share_import_title,
                showBackButton = false
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp)
                .widthIn(max = 720.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (state) {
                ShareImportState.LoadingContext -> LoadingContextContent()
                is ShareImportState.Uploading -> UploadingContent(state)
                is ShareImportState.SuccessPulse -> SuccessContent(state)
                is ShareImportState.Error -> ErrorContent(state, onIntent)
            }
        }
    }
}

@Composable
private fun LoadingContextContent() {
    DokusLoader()
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = stringResource(Res.string.share_import_preparing),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun UploadingContent(state: ShareImportState.Uploading) {
    Text(
        text = stringResource(Res.string.share_import_uploading_to, state.workspaceName),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface
    )
    Spacer(modifier = Modifier.height(6.dp))
    Text(
        text = stringResource(
            Res.string.share_import_file_progress,
            state.currentFileIndex,
            state.totalFiles
        ),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(18.dp))

    LinearProgressIndicator(
        progress = { state.overallProgress },
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(14.dp))
    Text(
        text = state.currentFileName,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(6.dp))
    Text(
        text = stringResource(
            Res.string.share_import_overall_progress,
            (state.overallProgress * 100).toInt()
        ),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun SuccessContent(state: ShareImportState.SuccessPulse) {
    AnimatedCheck(play = true)
    Spacer(modifier = Modifier.height(18.dp))

    val uploadedLabel = if (state.uploadedCount == 1) {
        stringResource(Res.string.share_import_success_single)
    } else {
        stringResource(Res.string.share_import_success_multiple, state.uploadedCount)
    }

    Text(
        text = uploadedLabel,
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onSurface
    )
    if (state.needsReviewCount > 0) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(Res.string.share_import_needs_review_count, state.needsReviewCount),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = sharedFilesSummary(state.primaryFileName, state.additionalFileCount),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun ErrorContent(
    state: ShareImportState.Error,
    onIntent: (ShareImportIntent) -> Unit
) {
    Text(
        text = stringResource(Res.string.state_error),
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.error
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = state.exception.localized,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(24.dp))

    if (state.retryHandler != null) {
        POutlinedButton(
            text = stringResource(Res.string.state_retry),
            modifier = Modifier.fillMaxWidth(),
            onClick = { state.retryHandler.retry() }
        )
    }
    if (state.canOpenApp) {
        if (state.retryHandler != null) {
            Spacer(modifier = Modifier.height(12.dp))
        }
        POutlinedButton(
            text = stringResource(Res.string.share_import_open_app),
            modifier = Modifier.fillMaxWidth(),
            onClick = { onIntent(OpenApp) }
        )
        if (state.exception is DokusException.WorkspaceContextUnavailable) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(Res.string.share_import_workspace_switch_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
    if (state.canNavigateToLogin) {
        if (state.retryHandler != null) {
            Spacer(modifier = Modifier.height(12.dp))
        }
        POutlinedButton(
            text = stringResource(Res.string.share_import_go_to_login),
            modifier = Modifier.fillMaxWidth(),
            onClick = { onIntent(NavigateToLogin) }
        )
    }
}

@Composable
private fun sharedFilesSummary(primaryFileName: String, additionalFileCount: Int): String {
    return if (additionalFileCount == 0) {
        primaryFileName
    } else {
        stringResource(Res.string.share_import_success_summary_multiple, primaryFileName, additionalFileCount)
    }
}
