package ai.thepredict.app.onboarding.workspaces.overview

import ai.thepredict.app.navigation.CoreNavigation
import ai.thepredict.app.navigation.OnboardingNavigation
import ai.thepredict.ui.PButton
import ai.thepredict.ui.PButtonVariant
import ai.thepredict.ui.WorkspacesList
import ai.thepredict.ui.common.ErrorBox
import ai.thepredict.ui.common.PTopAppBar
import ai.thepredict.ui.common.limitedWidth
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.registry.rememberScreen
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.mohamedrejeb.calf.ui.progress.AdaptiveCircularProgressIndicator
import compose.icons.FeatherIcons
import compose.icons.feathericons.Plus
import kotlinx.coroutines.launch

internal class WorkspacesScreen : Screen {

    @Composable
    override fun Content() {
        val viewModel = rememberScreenModel { WorkspacesViewModel() }
        val scope = rememberCoroutineScope()

        val data = viewModel.state.collectAsState()
        val state = data.value

        val navigator = LocalNavigator.currentOrThrow
        val workspaceCreate = rememberScreen(OnboardingNavigation.Workspaces.Create)
        val homeScreen = rememberScreen(CoreNavigation.Core)

        val handleEffect = { effect: WorkspacesViewModel.Effect ->
            when (effect) {
                is WorkspacesViewModel.Effect.NavigateCreateWorkspace -> navigator.push(
                    workspaceCreate
                )

                is WorkspacesViewModel.Effect.NavigateHome -> navigator.replaceAll(
                    homeScreen
                )
            }
        }

        LaunchedEffect("workspaces-overview") {
            scope.launch { viewModel.effect.collect(handleEffect) }
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
                Workspaces(
                    state = state,
                    onCreateClick = { viewModel.createWorkspace() },
                    onRefreshClick = { viewModel.fetch() },
                    modifier = Modifier.limitedWidth()
                )
                Spacer(Modifier.weight(1f))
                Actions(
                    state = state,
                    onCreateClick = { viewModel.createWorkspace() },
                    onContinueClick = { viewModel.continueToHome() }
                )
                Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun Workspaces(
    state: WorkspacesViewModel.State,
    onCreateClick: () -> Unit,
    onRefreshClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier.padding(horizontal = 16.dp)) {
        when (state) {
            is WorkspacesViewModel.State.Loading -> {
                Box(modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                    AdaptiveCircularProgressIndicator()
                }
            }

            is WorkspacesViewModel.State.Loaded -> {
                if (state.workspaces.isEmpty()) {
                    NoWorkspaces(Modifier.fillMaxWidth().heightIn(min = 120.dp), onCreateClick)
                    return@Card
                }
                WorkspacesList(state.workspaces, onClick = null)
            }

            is WorkspacesViewModel.State.Error -> {
                ErrorBox(
                    exception = state.exception,
                    modifier = Modifier.fillMaxWidth(),
                    onRetry = onRefreshClick
                )
            }
        }
    }
}

@Composable
private fun NoWorkspaces(modifier: Modifier = Modifier, onCreateClick: () -> Unit) {
    Column(
        modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Seems like you don't have any workspaces yet.",
            style = MaterialTheme.typography.titleSmall
        )
        Spacer(Modifier.padding(vertical = 8.dp))
        Text("Let's create one?", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.padding(vertical = 8.dp))
        PButton(
            "Create a workspace",
            variant = PButtonVariant.Outline,
            icon = FeatherIcons.Plus,
            onClick = onCreateClick
        )
    }
}

@Composable
private fun Actions(
    modifier: Modifier = Modifier,
    state: WorkspacesViewModel.State,
    onCreateClick: () -> Unit,
    onContinueClick: () -> Unit,
) {
    if (state is WorkspacesViewModel.State.Loaded && state.workspaces.isNotEmpty()) {
        Column(
            modifier.padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            PButton(
                "Create a workspace",
                variant = PButtonVariant.Outline,
                icon = FeatherIcons.Plus,
                onClick = onCreateClick
            )
            Text(
                "OR",
                modifier = Modifier.padding(vertical = 16.dp),
                style = MaterialTheme.typography.titleMedium
            )
            PButton(text = "Continue", onClick = onContinueClick)
        }
    }
}