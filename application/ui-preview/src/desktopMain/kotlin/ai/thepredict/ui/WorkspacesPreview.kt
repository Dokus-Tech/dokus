package ai.thepredict.ui

import ai.thepredict.data.Workspace
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.runtime.Composable

private val workspaces = listOf(
    Workspace(Workspace.Id.random, "Invoid Vision", "Invoid Vision BV", "BE0777887045"),
    Workspace(Workspace.Id.random, "Predict", "The Predict SRL"),
)

@Composable
@Preview
fun WorkspacePreview() {
    PreviewWrapper {
        WorkspaceListItem(workspaces.first()) {}
    }
}

@Composable
@Preview
fun WorkspacesListPreview() {
    PreviewWrapper {
        WorkspacesList(workspaces) {}
    }
}