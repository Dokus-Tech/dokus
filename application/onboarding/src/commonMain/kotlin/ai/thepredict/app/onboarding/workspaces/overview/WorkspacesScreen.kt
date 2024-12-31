package ai.thepredict.app.onboarding.workspaces.overview

import ai.thepredict.app.navigation.OnboardingNavigation
import ai.thepredict.ui.PButton
import ai.thepredict.ui.PTitle
import ai.thepredict.ui.PTopAppBar
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.registry.rememberScreen
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.mohamedrejeb.calf.ui.progress.AdaptiveCircularProgressIndicator

internal class WorkspacesScreen : Screen {

    @Composable
    override fun Content() {
        val viewModel = rememberScreenModel { WorkspacesViewModel() }
        val data = viewModel.state.collectAsState()

        val navigator = LocalNavigator.currentOrThrow
        val workspaceCreate = rememberScreen(OnboardingNavigation.Workspaces.Create)

        LaunchedEffect("workspace-selection") {
            viewModel.fetch()
        }

        Scaffold(
            topBar = { PTopAppBar("Your workspaces") }
        ) { innerPadding ->
            Column(
                modifier = Modifier.fillMaxWidth().padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly,
            ) {
                PButton("Refresh") {
                    viewModel.fetch()
                }

                when (val state = data.value) {
                    is WorkspacesViewModel.State.Loading -> {
                        AdaptiveCircularProgressIndicator()
                    }

                    is WorkspacesViewModel.State.Loaded -> {
                        state.workspaces.forEach {
                            PTitle(it.name)
                        }
                    }

                    is WorkspacesViewModel.State.Error -> {
                        PTitle(state.exception.message)
                    }
                }

                PButton("Continue") {
                }

                OutlinedButton(
                    onClick = {
                        navigator.push(workspaceCreate)
                    }
                ) {
                    Text("Create a workspace")
                }
            }
        }
    }
}