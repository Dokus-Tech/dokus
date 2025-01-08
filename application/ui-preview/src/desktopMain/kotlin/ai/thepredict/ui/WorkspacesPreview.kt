package ai.thepredict.ui

import ai.thepredict.data.Workspace
import ai.thepredict.ui.tooling.mockedIv
import ai.thepredict.ui.tooling.mockedPredict
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.runtime.Composable

@Composable
@Preview
fun WorkspacePreview() {
    PreviewWrapper {
        WorkspaceListItem(Workspace.mockedIv) {}
    }
}

@Composable
@Preview
fun WorkspacesListPreview() {
    PreviewWrapper {
        WorkspacesList(listOf(Workspace.mockedIv, Workspace.mockedPredict)) {}
    }
}