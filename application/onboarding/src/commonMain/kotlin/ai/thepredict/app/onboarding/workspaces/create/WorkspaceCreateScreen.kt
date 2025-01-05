package ai.thepredict.app.onboarding.workspaces.create

import ai.thepredict.domain.exceptions.PredictException
import ai.thepredict.ui.PButton
import ai.thepredict.ui.common.PTopAppBar
import ai.thepredict.ui.common.limitedWidth
import ai.thepredict.ui.fields.PTextFieldFreeDefaults
import ai.thepredict.ui.fields.PTextFieldName
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.mohamedrejeb.calf.ui.progress.AdaptiveCircularProgressIndicator
import compose.icons.FeatherIcons
import compose.icons.feathericons.Award
import compose.icons.feathericons.Briefcase
import compose.icons.feathericons.Hash

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

        var workspaceName by remember { mutableStateOf("") }
        var legalName by remember { mutableStateOf("") }
        var taxNumber by remember { mutableStateOf("") }

        Scaffold(
            topBar = { PTopAppBar("Let's create your workspace") }
        ) { innerPadding ->
            Column(
                modifier = Modifier.fillMaxWidth().padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly,
            ) {
                CreateWorkspaceFields(
                    data.value,
                    workspaceName,
                    legalName,
                    taxNumber,
                    onSetWorkspaceName = { workspaceName = it },
                    onSetLegalName = { legalName = it },
                    onSetTaxNumber = { taxNumber = it }
                )
                Spacer(Modifier.weight(1f))
                Actions(
                    data.value,
                    onCreateClick = { viewModel.create(workspaceName, legalName, taxNumber) },
                    onSuccess = { navigator.pop() }
                )
                Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun CreateWorkspaceFields(
    state: WorkspaceCreateViewModel.State,
    workspaceName: String,
    legalName: String,
    taxNumber: String,
    onSetWorkspaceName: (String) -> Unit,
    onSetLegalName: (String) -> Unit,
    onSetTaxNumber: (String) -> Unit,
) {
    val fieldsError = state.exceptionOrNull
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier.fillMaxWidth().limitedWidth(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PTextFieldName(
            modifier = Modifier.padding(vertical = 16.dp),
            fieldName = "Company name",
            error = fieldsError.takeIf { it is PredictException.InvalidWorkspaceName },
            value = workspaceName,
            icon = FeatherIcons.Award,
            keyboardOptions = PTextFieldFreeDefaults.keyboardOptions.copy(imeAction = ImeAction.Next),
            onAction = { focusManager.moveFocus(FocusDirection.Next) },
            onValueChange = onSetWorkspaceName
        )

        PTextFieldName(
            modifier = Modifier.padding(vertical = 16.dp),
            fieldName = "Legal name (optional)",
            value = legalName,
            icon = FeatherIcons.Briefcase,
            keyboardOptions = PTextFieldFreeDefaults.keyboardOptions.copy(imeAction = ImeAction.Next),
            onAction = { focusManager.moveFocus(FocusDirection.Next) },
            onValueChange = onSetLegalName
        )

        PTextFieldName(
            modifier = Modifier.padding(vertical = 16.dp),
            fieldName = "Tax Number (optional)",
            error = fieldsError.takeIf { it is PredictException.InvalidTaxNumber },
            value = taxNumber,
            icon = FeatherIcons.Hash,
            keyboardOptions = PTextFieldFreeDefaults.keyboardOptions.copy(imeAction = ImeAction.Done),
            onAction = { focusManager.clearFocus() },
            onValueChange = onSetTaxNumber
        )
    }
}

@Composable
private fun Actions(
    state: WorkspaceCreateViewModel.State,
    onCreateClick: () -> Unit,
    onSuccess: () -> Unit,
) {
    when (state) {
        is WorkspaceCreateViewModel.State.Idle -> {
            PButton("Create", onClick = onCreateClick)
        }

        is WorkspaceCreateViewModel.State.Loading -> {
            AdaptiveCircularProgressIndicator()
        }

        is WorkspaceCreateViewModel.State.Loaded -> {
            onSuccess()
        }

        is WorkspaceCreateViewModel.State.Error -> {
            PButton("Create", onClick = onCreateClick)
        }
    }
}