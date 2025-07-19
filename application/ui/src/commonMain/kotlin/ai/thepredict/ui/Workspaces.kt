package ai.thepredict.ui

import ai.thepredict.domain.model.Company
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (onAddClick != null) MaterialTheme.colorScheme.surface
                    else MaterialTheme.colorScheme.primaryContainer
                )
                .then(
                    if (onAddClick != null)
                        Modifier.border(
                            1.dp,
                            MaterialTheme.colorScheme.outline,
                            RoundedCornerShape(8.dp)
                        )
                    else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            if (onAddClick != null) {
                // Plus icon for add workspace
                Text(
                    text = "+",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            } else {
                // First letter of workspace name
                Text(
                    text = workspace.name.firstOrNull()?.uppercaseChar()?.toString() ?: "",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
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
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
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