package ai.thepredict.ui

import ai.thepredict.domain.model.Company
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

private val workspaceItemSize = 80.dp

@Composable
fun WorkspaceItemCreateNew(
    modifier: Modifier = Modifier,
    onAddClick: () -> Unit,
) {
    Column(
        modifier = modifier.clickable { onAddClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Avatar box
        POutlinedCard(
            modifier = Modifier.size(workspaceItemSize),
        ) {
            Text(
                modifier = Modifier.align(Alignment.Center),
                text = "+",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        // Workspace name
        Text(
            text = "Create new workspace",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun WorkspaceItem(
    workspace: Company,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier.clickable { onClick?.invoke() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Avatar box
        PCard(
            modifier = Modifier.size(workspaceItemSize),
        ) {
            Text(
                modifier = Modifier.align(Alignment.Center),
                text = workspace.name.firstOrNull()?.uppercaseChar()?.toString() ?: "",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
        }

        // Workspace name
        Text(
            text = workspace.name,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
    }
}

private val workspaceItemWidth = 100.dp

@Composable
fun WorkspacesGrid(
    workspaces: List<Company>,
    onWorkspaceClick: ((Company) -> Unit),
    onAddWorkspaceClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
    ) {
        workspaces.forEach { workspace ->
            WorkspaceItem(
                workspace = workspace,
                modifier = Modifier.width(workspaceItemWidth),
                onClick = { onWorkspaceClick(workspace) }
            )
        }
        if (onAddWorkspaceClick != null) {
            WorkspaceItemCreateNew(
                modifier = Modifier.width(workspaceItemWidth),
                onAddClick = onAddWorkspaceClick
            )
        }
    }
}