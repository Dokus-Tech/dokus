package ai.thepredict.app.onboarding.workspaces

import ai.thepredict.ui.PButton
import ai.thepredict.ui.PTitle
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen

internal class WorkspacesScreen : Screen {

    @Composable
    override fun Content() {
        val viewModel = rememberScreenModel { WorkspacesViewModel() }
        val data = viewModel.state.collectAsState()

        LaunchedEffect("workspace-selection") {
            viewModel.fetch()
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        ) {
            PTitle("Workspace selection screen")

            PButton("Refresh") {
                viewModel.fetch()
            }

            when (val state = data.value) {
                is WorkspacesViewModel.State.Loading -> {
                    PTitle("Loading")
                }

                is WorkspacesViewModel.State.Loaded -> {
                    state.workspaces.forEach {
                        PTitle(it.name)
                    }
                }

                is WorkspacesViewModel.State.Error -> {
                    PTitle("Error happened")
                }
            }
        }
    }
}