package ai.thepredict.app.onboarding.workspaces.overview

import ai.thepredict.app.core.constrains.isLargeScreen
import ai.thepredict.app.navigation.CoreNavigation
import ai.thepredict.app.navigation.OnboardingNavigation
import ai.thepredict.domain.model.Company
import ai.thepredict.ui.WorkspacesGrid
import ai.thepredict.ui.common.ErrorBox
import ai.thepredict.ui.text.AppNameText
import ai.thepredict.ui.text.CopyRightText
import ai.thepredict.ui.text.SectionTitle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.registry.rememberScreen
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.mohamedrejeb.calf.ui.progress.AdaptiveCircularProgressIndicator
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

        Scaffold { contentPadding ->
            Box(Modifier.padding(contentPadding)) {
                if (isLargeScreen) {
                    WorkspacesSelectionDesktopContent(
                        state = state,
                        onWorkspaceSelect = viewModel::selectWorkspace,
                        onNewWorkspaceClick = viewModel::createWorkspace,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    WorkspacesSelectionMobileContent(
                        state = state,
                        onWorkspaceSelect = viewModel::selectWorkspace,
                        onNewWorkspaceClick = viewModel::createWorkspace,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
internal fun WorkspacesSelectionDesktopContent(
    state: WorkspacesViewModel.State,
    onWorkspaceSelect: (Company) -> Unit,
    onNewWorkspaceClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        AppNameText()

        when (state) {
            is WorkspacesViewModel.State.Loading -> {
                Box(modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                    AdaptiveCircularProgressIndicator()
                }
            }

            is WorkspacesViewModel.State.Loaded -> {
                Row(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Spacer(Modifier.weight(1f))
                    WorkspacesSelection(
                        workspaces = state.workspaces,
                        onWorkspaceSelect = onWorkspaceSelect,
                        onNewWorkspaceClick = onNewWorkspaceClick,
                        modifier = Modifier.weight(3f)
                    )
                    Spacer(Modifier.weight(1f))
                }
            }

            is WorkspacesViewModel.State.Error -> {
                ErrorBox(
                    exception = state.exception,
                    modifier = Modifier.fillMaxWidth(),
                    onRetry = {}
                )
            }
        }

        CopyRightText()
        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
internal fun WorkspacesSelectionMobileContent(
    state: WorkspacesViewModel.State,
    onWorkspaceSelect: (Company) -> Unit,
    onNewWorkspaceClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        AppNameText()

        when (state) {
            is WorkspacesViewModel.State.Loading -> {
                Box(modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                    AdaptiveCircularProgressIndicator()
                }
            }

            is WorkspacesViewModel.State.Loaded -> {
                WorkspacesSelection(
                    workspaces = state.workspaces,
                    onWorkspaceSelect = onWorkspaceSelect,
                    onNewWorkspaceClick = onNewWorkspaceClick,
                    modifier = Modifier.fillMaxSize()
                )
            }

            is WorkspacesViewModel.State.Error -> {
                ErrorBox(
                    exception = state.exception,
                    modifier = Modifier.fillMaxWidth(),
                    onRetry = {}
                )
            }
        }
    }
}

@Composable
internal fun WorkspacesSelection(
    workspaces: List<Company>,
    onWorkspaceSelect: (Company) -> Unit,
    onNewWorkspaceClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = if (isLargeScreen) Alignment.CenterHorizontally else Alignment.Start,
        verticalArrangement = Arrangement.Center,
    ) {
        SectionTitle(
            "Select workspace",
            horizontalArrangement = Arrangement.Center,
        )

        Spacer(modifier = Modifier.height(32.dp))

        WorkspacesGrid(
            workspaces = workspaces,
            onWorkspaceClick = onWorkspaceSelect,
            onAddWorkspaceClick = onNewWorkspaceClick,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}