package tech.dokus.app.share

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import tech.dokus.app.share.ShareImportIntent.NavigateToLogin
import tech.dokus.app.share.ShareImportIntent.Retry
import tech.dokus.app.share.ShareImportIntent.SelectWorkspace
import tech.dokus.features.auth.presentation.auth.components.WorkspaceSelectionBody
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.aura.components.POutlinedButton
import tech.dokus.foundation.aura.components.common.AnimatedCheck
import tech.dokus.foundation.aura.components.common.PTopAppBar

@Composable
internal fun ShareImportScreen(
    state: ShareImportState,
    onIntent: (ShareImportIntent) -> Unit
) {
    ShareImportBackHandler(
        enabled = state is ShareImportState.Uploading || state is ShareImportState.Success
    )

    Scaffold(
        topBar = {
            PTopAppBar(
                title = "Import Shared PDF",
                showBackButton = false
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (state) {
                ShareImportState.LoadingContext -> LoadingContextContent()
                is ShareImportState.SelectWorkspace -> SelectWorkspaceContent(state, onIntent)
                is ShareImportState.Uploading -> UploadingContent(state)
                is ShareImportState.Success -> SuccessContent(state)
                is ShareImportState.Error -> ErrorContent(state, onIntent)
            }
        }
    }
}

@Composable
private fun LoadingContextContent() {
    CircularProgressIndicator()
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = "Preparing import…",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun SelectWorkspaceContent(
    state: ShareImportState.SelectWorkspace,
    onIntent: (ShareImportIntent) -> Unit
) {
    Text(
        text = "Choose workspace",
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onSurface
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "Select where \"${state.fileName}\" should be uploaded.",
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(20.dp))

    WorkspaceSelectionBody(
        state = DokusState.success(state.workspaces),
        onTenantClick = { tenant ->
            if (!state.isSwitchingWorkspace) {
                onIntent(SelectWorkspace(tenant.id))
            }
        },
        onAddTenantClick = {}
    )

    if (state.isSwitchingWorkspace) {
        Spacer(modifier = Modifier.height(20.dp))
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Switching workspace…",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun UploadingContent(state: ShareImportState.Uploading) {
    CircularProgressIndicator(progress = { state.progress })
    Spacer(modifier = Modifier.height(20.dp))
    Text(
        text = "Uploading to ${state.workspaceName}",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = state.fileName,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "${(state.progress * 100).toInt()}%",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun SuccessContent(state: ShareImportState.Success) {
    AnimatedCheck(play = true)
    Spacer(modifier = Modifier.height(18.dp))
    Text(
        text = "Upload complete",
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onSurface
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = state.fileName,
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
        text = state.title,
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.error
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = state.message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(24.dp))

    if (state.canRetry) {
        POutlinedButton(
            text = "Try again",
            modifier = Modifier.fillMaxWidth(),
            onClick = { onIntent(Retry) }
        )
    }
    if (state.canNavigateToLogin) {
        if (state.canRetry) {
            Spacer(modifier = Modifier.height(12.dp))
        }
        POutlinedButton(
            text = "Go to login",
            modifier = Modifier.fillMaxWidth(),
            onClick = { onIntent(NavigateToLogin) }
        )
    }
}
