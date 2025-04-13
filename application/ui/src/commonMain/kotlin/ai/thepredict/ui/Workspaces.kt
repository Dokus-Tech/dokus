package ai.thepredict.ui

import ai.thepredict.domain.model.Workspace
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.Briefcase
import compose.icons.feathericons.Edit2

@Composable
fun WorkspacesList(
    workspaces: List<Workspace>,
    modifier: Modifier = Modifier,
    onClick: ((Workspace) -> Unit)?,
) {
    LazyColumn(
        modifier,
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(workspaces) { workspace ->
            WorkspaceListItem(workspace, clickEnabled = onClick != null) {
                onClick?.invoke(workspace)
            }
        }
    }
}

@Composable
fun WorkspaceListItem(
    workspace: Workspace,
    clickEnabled: Boolean = false,
    onClick: () -> Unit = {},
) {
    Card(modifier = Modifier.clickable { if (clickEnabled) onClick.invoke() }) {
        Row(
            modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            WorkspaceIcon(workspace.url, modifier = Modifier.weight(1f))
            Column(
                modifier = Modifier.weight(4f),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.SpaceAround
            ) {
                WorkspaceName(workspace.name)
                WorkspaceLegalName(workspace.legalName, workspace.taxNumber)
            }
            WorkspaceNavigation(Modifier.weight(1f), if (clickEnabled) onClick else null)
        }
    }
}

@Composable
fun WorkspaceIcon(url: String?, modifier: Modifier = Modifier) {
    Icon(imageVector = FeatherIcons.Briefcase, contentDescription = "logo", modifier = modifier)
}

@Composable
fun WorkspaceName(name: String, modifier: Modifier = Modifier) {
    Text(text = name, modifier = modifier, style = MaterialTheme.typography.titleMedium)
}

@Composable
fun WorkspaceLegalName(legalName: String?, taxNumber: String?, modifier: Modifier = Modifier) {
    Text(
        text = taxNumber ?: legalName.orEmpty(),
        modifier = modifier,
        style = MaterialTheme.typography.labelMedium
    )
}

@Composable
private fun WorkspaceNavigation(modifier: Modifier = Modifier, onClick: (() -> Unit)?) {
    if (onClick == null) {
        Spacer(modifier)
        return
    }
    Icon(
        imageVector = FeatherIcons.Edit2,
        contentDescription = "edit",
        modifier = modifier
    )
}