package ai.thepredict.app.onboarding.workspaces.create

import ai.thepredict.app.navigation.CoreNavigation
import ai.thepredict.domain.exceptions.PredictException
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.registry.rememberScreen
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.launch

private val WorkspaceCreateViewModel.State.exceptionOrNull: PredictException?
    get() = when (this) {
        is WorkspaceCreateViewModel.State.Error -> exception
        else -> null
    }

internal class WorkspaceCreateScreen : Screen {

    @Composable
    override fun Content() {
        val viewModel = rememberScreenModel { WorkspaceCreateViewModel() }
        val data = viewModel.state.collectAsState()

        val navigator = LocalNavigator.currentOrThrow
        val focusManager = LocalFocusManager.current
        val scope = rememberCoroutineScope()

        val fieldsError: PredictException? = data.value.exceptionOrNull

        var workspaceName by remember { mutableStateOf("") }
        var legalName by remember { mutableStateOf("") }
        var taxNumber by remember { mutableStateOf("") }
        val mutableInteractionSource = remember { MutableInteractionSource() }

        val homeScreen = rememberScreen(CoreNavigation.Core)

        val handleEffect = { effect: WorkspaceCreateViewModel.Effect ->
            when (effect) {
                is WorkspaceCreateViewModel.Effect.NavigateToHome -> navigator.replaceAll(
                    homeScreen
                )
            }
        }

        LaunchedEffect("workspace-create") {
            scope.launch { viewModel.effect.collect(handleEffect) }
        }

        Scaffold { contentPadding ->
            Box(
                Modifier
                    .padding(contentPadding)
                    .clickable(
                        indication = null,
                        interactionSource = mutableInteractionSource
                    ) {
                        focusManager.clearFocus()
                    }
            ) {
            }
        }
    }
}