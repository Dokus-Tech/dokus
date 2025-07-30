package ai.thepredict.app.onboarding.workspaces.create

import ai.thepredict.app.core.constrains.isLargeScreen
import ai.thepredict.app.navigation.CoreNavigation
import ai.thepredict.domain.exceptions.PredictException
import ai.thepredict.ui.PPrimaryButton
import ai.thepredict.ui.fields.PTextFieldTaxNumber
import ai.thepredict.ui.fields.PTextFieldTaxNumberDefaults
import ai.thepredict.ui.fields.PTextFieldWorkspaceName
import ai.thepredict.ui.fields.PTextFieldWorkspaceNameDefaults
import ai.thepredict.ui.text.AppNameText
import ai.thepredict.ui.text.SectionTitle
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
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
                if (isLargeScreen) {
                    WorkspaceCreateScreenMobileContent(
                        focusManager = focusManager,
                        workspaceName = workspaceName,
                        onWorkspaceNameChange = { workspaceName = it },
                        vatNumber = taxNumber,
                        onVatNumberChange = { taxNumber = it },
                        fieldsError = fieldsError,
                        onCreateClick = {
                            viewModel.create(
                                workspaceName,
                                legalName,
                                taxNumber
                            )
                        },
                        onBackClick = {
                            navigator.pop()
                        }
                    )
                } else {
                    WorkspaceCreateScreenMobileContent(
                        focusManager = focusManager,
                        workspaceName = workspaceName,
                        onWorkspaceNameChange = { workspaceName = it },
                        vatNumber = taxNumber,
                        onVatNumberChange = { taxNumber = it },
                        fieldsError = fieldsError,
                        onCreateClick = {
                            viewModel.create(
                                workspaceName,
                                legalName,
                                taxNumber
                            )
                        },
                        onBackClick = {
                            navigator.pop()
                        },
                        modifier = Modifier.padding(horizontal = 16.dp).fillMaxSize(),
                    )
                }
            }
        }
    }
}

@Composable
internal fun WorkspaceCreateScreenMobileContent(
    focusManager: FocusManager,
    workspaceName: String,
    onWorkspaceNameChange: (String) -> Unit,
    vatNumber: String,
    onVatNumberChange: (String) -> Unit,
    fieldsError: PredictException?,
    onCreateClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Center,
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        AppNameText()

        WorkspaceCreateForm(
            focusManager = focusManager,
            workspaceName = workspaceName,
            onWorkspaceNameChange = onWorkspaceNameChange,
            vatNumber = vatNumber,
            onVatNumberChange = onVatNumberChange,
            fieldsError = fieldsError,
            onCreateClick = onCreateClick,
            onBackClick = onBackClick,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
internal fun WorkspaceCreateForm(
    focusManager: FocusManager,
    workspaceName: String,
    onWorkspaceNameChange: (String) -> Unit,
    vatNumber: String,
    onVatNumberChange: (String) -> Unit,
    fieldsError: PredictException?,
    onCreateClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Center,
    ) {
        SectionTitle("Create workspace", onBackPress = onBackClick)

        Spacer(modifier = Modifier.height(24.dp))

        // Form fields
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PTextFieldWorkspaceName(
                fieldName = "Company name",
                error = fieldsError.takeIf { it is PredictException.InvalidWorkspaceName },
                value = workspaceName,
                keyboardOptions = PTextFieldWorkspaceNameDefaults.keyboardOptions.copy(imeAction = ImeAction.Next),
                onAction = { focusManager.moveFocus(FocusDirection.Next) },
                modifier = Modifier.fillMaxWidth(),
                onValueChange = onWorkspaceNameChange
            )

            PTextFieldTaxNumber(
                fieldName = "VAT number",
                error = fieldsError.takeIf { it is PredictException.InvalidTaxNumber },
                value = vatNumber,
                singleLine = true,
                keyboardOptions = PTextFieldTaxNumberDefaults.keyboardOptions.copy(imeAction = ImeAction.Next),
                onAction = { focusManager.moveFocus(FocusDirection.Next) },
                modifier = Modifier.fillMaxWidth(),
                onValueChange = onVatNumberChange
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        PPrimaryButton(
            text = "Create",
            modifier = Modifier.fillMaxWidth(),
            onClick = onCreateClick
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}