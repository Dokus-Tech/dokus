package ai.thepredict.ui

import ai.thepredict.domain.model.Company
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun WorkspaceItem(
    workspace: Company,
    modifier: Modifier = Modifier,
    onAddClick: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier.clickable { onClick?.invoke() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Avatar box
        Card(
            modifier = Modifier.size(80.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (onAddClick != null) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (onAddClick != null) {
                    Text(
                        modifier = Modifier.align(Alignment.Center),
                        text = "+",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                } else {
                    Text(
                        modifier = Modifier.align(Alignment.Center),
                        text = workspace.name.firstOrNull()?.uppercaseChar()?.toString() ?: "",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Workspace name
        Text(
            text = if (onAddClick != null) "Add workspace" else workspace.name,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun WorkspacesGrid(
    workspaces: List<Company>,
    onWorkspaceClick: ((Company) -> Unit),
    onAddWorkspaceClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        modifier = modifier,
        columns = GridCells.Fixed(3),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(workspaces) { workspace ->
            WorkspaceItem(
                workspace = workspace,
                onClick = { onWorkspaceClick(workspace) }
            )
        }

        if (onAddWorkspaceClick != null) {
            item {
                WorkspaceItem(
                    workspace = Company(
                        id = "",
                        name = "",
                        taxId = "",
                        isOwner = false
                    ),
                    onAddClick = onAddWorkspaceClick
                )
            }
        }
    }
}