package ai.thepredict.ui

import ai.thepredict.domain.model.Company
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview
@Composable
fun WorkspaceItemNewPreview() {
    Themed {
        WorkspaceItemCreateNew { }
    }
}

@Preview
@Composable
fun WorkspaceItemPreview() {
    Themed {
        WorkspaceItem(
            workspace = mockWorkspaces.first(),
            modifier = Modifier.padding(8.dp),
            onClick = onAddWorkspaceClick,
        )
    }
}

private val mockWorkspaces = listOf(
    Company(id = "1", name = "Acme Corp", taxId = "123456789", isOwner = true),
    Company(id = "2", name = "Tech Solutions", taxId = "987654321", isOwner = false),
    Company(id = "3", name = "Design Studio", taxId = "45678", isOwner = false),
    Company(id = "4", name = "Marketing Plus", taxId = "789123456", isOwner = false)
)

private val modifier = Modifier.fillMaxWidth()
private val onWorkspaceClick: (Company) -> Unit = {}
private val onAddWorkspaceClick: () -> Unit = {}

@Preview
@Composable
fun WorkspacesGridPreview() {
    Themed {
        WorkspacesGrid(
            workspaces = mockWorkspaces,
            onWorkspaceClick = onWorkspaceClick,
            onAddWorkspaceClick = onAddWorkspaceClick
        )
    }
}
