package ai.dokus.app.auth.screen

import ai.dokus.app.auth.components.CurrentServerSection
import ai.dokus.app.auth.usecases.ConnectToServerUseCase
import ai.dokus.app.auth.viewmodel.ProfileSettingsViewModel
import ai.dokus.app.core.state.DokusState
import ai.dokus.app.core.state.isLoading
import ai.dokus.app.core.state.isSuccess
import ai.dokus.app.resources.generated.Res
import ai.dokus.app.resources.generated.profile_cancel
import ai.dokus.app.resources.generated.profile_danger_zone
import ai.dokus.app.resources.generated.profile_deactivate_account
import ai.dokus.app.resources.generated.profile_deactivate_warning
import ai.dokus.app.resources.generated.profile_edit
import ai.dokus.app.resources.generated.profile_email
import ai.dokus.app.resources.generated.profile_first_name
import ai.dokus.app.resources.generated.profile_last_name
import ai.dokus.app.resources.generated.profile_logout
import ai.dokus.app.resources.generated.profile_logout_description
import ai.dokus.app.resources.generated.profile_personal_info
import ai.dokus.app.resources.generated.profile_save
import ai.dokus.app.resources.generated.profile_save_error
import ai.dokus.app.resources.generated.profile_save_success
import ai.dokus.app.resources.generated.profile_settings_title
import ai.dokus.app.auth.usecases.LogoutUseCase
import ai.dokus.foundation.design.components.PPrimaryButton
import ai.dokus.foundation.design.components.POutlinedButton
import ai.dokus.foundation.design.components.common.PTopAppBar
import ai.dokus.foundation.design.components.fields.PTextFieldName
import ai.dokus.foundation.design.constrains.withContentPaddingForScrollable
import ai.dokus.foundation.domain.Name
import ai.dokus.foundation.domain.config.ServerConfigManager
import ai.dokus.foundation.domain.model.User
import ai.dokus.foundation.navigation.destinations.AuthDestination
import ai.dokus.foundation.navigation.local.LocalNavController
import ai.dokus.foundation.navigation.navigateTo
import ai.dokus.foundation.platform.Logger
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/**
 * Profile settings screen with top bar and navigation.
 * For mobile navigation flow.
 */
@Composable
fun ProfileSettingsScreen() {
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            PTopAppBar(
                title = stringResource(Res.string.profile_settings_title)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { contentPadding ->
        ProfileSettingsContent(
            modifier = Modifier.padding(contentPadding),
            snackbarHostState = snackbarHostState
        )
    }
}

/**
 * Profile settings content without scaffold.
 * Can be embedded in split-pane layout for desktop or used in full-screen for mobile.
 */
@Composable
fun ProfileSettingsContent(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    val logger = remember { Logger.withTag("ProfileSettingsContent") }
    val viewModel: ProfileSettingsViewModel = koinViewModel()
    val logoutUseCase: LogoutUseCase = koinInject()
    val serverConfigManager: ServerConfigManager = koinInject()
    val connectToServerUseCase: ConnectToServerUseCase = koinInject()
    val navController = LocalNavController.current
    val scope = rememberCoroutineScope()

    val currentServer by serverConfigManager.currentServer.collectAsState()
    var isResettingToCloud by remember { mutableStateOf(false) }

    val userState by viewModel.state.collectAsState()
    val isEditing by viewModel.isEditing.collectAsState()
    val editFirstName by viewModel.editFirstName.collectAsState()
    val editLastName by viewModel.editLastName.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()

    var isLoggingOut by remember { mutableStateOf(false) }

    // String resources for effects
    val saveSuccessMessage = stringResource(Res.string.profile_save_success)
    val saveErrorMessage = stringResource(Res.string.profile_save_error)

    // Handle effects
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ProfileSettingsViewModel.Effect.ShowSaveSuccess -> {
                    snackbarHostState.showSnackbar(saveSuccessMessage)
                }
                is ProfileSettingsViewModel.Effect.ShowSaveError -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .withContentPaddingForScrollable(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Profile content based on state
        when {
            userState.isLoading() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            userState.isSuccess() -> {
                val user = (userState as DokusState.Success).data
                // Personal Information Section
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(Res.string.profile_personal_info),
                                style = MaterialTheme.typography.titleMedium
                            )
                            if (!isEditing) {
                                TextButton(onClick = { viewModel.startEditing() }) {
                                    Text(stringResource(Res.string.profile_edit))
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        // Email (always read-only)
                        ProfileField(
                            label = stringResource(Res.string.profile_email),
                            value = user.email.value
                        )

                        Spacer(Modifier.height(12.dp))

                        if (isEditing) {
                            // Edit mode - show text fields
                            PTextFieldName(
                                fieldName = stringResource(Res.string.profile_first_name),
                                value = editFirstName,
                                icon = null,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Text,
                                    capitalization = KeyboardCapitalization.Words,
                                    imeAction = ImeAction.Next
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                onValueChange = { viewModel.updateFirstName(it) }
                            )

                            Spacer(Modifier.height(12.dp))

                            PTextFieldName(
                                fieldName = stringResource(Res.string.profile_last_name),
                                value = editLastName,
                                icon = null,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Text,
                                    capitalization = KeyboardCapitalization.Words,
                                    imeAction = ImeAction.Done
                                ),
                                onAction = { if (viewModel.canSave()) viewModel.saveProfile() },
                                modifier = Modifier.fillMaxWidth(),
                                onValueChange = { viewModel.updateLastName(it) }
                            )

                            Spacer(Modifier.height(16.dp))

                            // Save/Cancel buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                POutlinedButton(
                                    text = stringResource(Res.string.profile_cancel),
                                    enabled = !isSaving,
                                    onClick = { viewModel.cancelEditing() }
                                )
                                Spacer(Modifier.width(8.dp))
                                if (isSaving) {
                                    Box(
                                        modifier = Modifier.height(42.dp).width(80.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.height(24.dp).width(24.dp)
                                        )
                                    }
                                } else {
                                    PPrimaryButton(
                                        text = stringResource(Res.string.profile_save),
                                        enabled = viewModel.canSave(),
                                        onClick = { viewModel.saveProfile() }
                                    )
                                }
                            }
                        } else {
                            // View mode - show read-only fields
                            ProfileField(
                                label = stringResource(Res.string.profile_first_name),
                                value = user.firstName?.value ?: "-"
                            )

                            Spacer(Modifier.height(12.dp))

                            ProfileField(
                                label = stringResource(Res.string.profile_last_name),
                                value = user.lastName?.value ?: "-"
                            )
                        }
                    }
                }

                // Danger Zone (only show when profile loaded)
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(Res.string.profile_danger_zone),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )

                        Spacer(Modifier.height(12.dp))

                        Text(
                            text = stringResource(Res.string.profile_deactivate_warning),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(Modifier.height(12.dp))

                        POutlinedButton(
                            text = stringResource(Res.string.profile_deactivate_account),
                            onClick = { /* TODO: Implement deactivation dialog */ },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            else -> {
                // Error state - show error message
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Failed to load profile",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        // Server Connection Section - ALWAYS visible
        CurrentServerSection(
            currentServer = currentServer,
            onChangeServer = {
                navController.navigateTo(AuthDestination.ServerConnection())
            },
            onResetToCloud = {
                scope.launch {
                    isResettingToCloud = true
                    connectToServerUseCase.resetToCloud().fold(
                        onSuccess = {
                            navController.navigateTo(AuthDestination.Login) {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                        onFailure = { error ->
                            logger.e(error) { "Failed to reset to cloud" }
                            snackbarHostState.showSnackbar("Failed to reset to cloud")
                            isResettingToCloud = false
                        }
                    )
                }
            }
        )

        // Logout Section - ALWAYS visible regardless of profile state
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(Res.string.profile_logout),
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = stringResource(Res.string.profile_logout_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(12.dp))

                POutlinedButton(
                    text = if (isLoggingOut) "Logging out..." else stringResource(Res.string.profile_logout),
                    enabled = !isLoggingOut,
                    onClick = {
                        scope.launch {
                            isLoggingOut = true
                            logger.d { "Logging out user" }
                            logoutUseCase().fold(
                                onSuccess = {
                                    logger.i { "Logout successful" }
                                    navController.navigateTo(AuthDestination.Login) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                },
                                onFailure = { error ->
                                    logger.e(error) { "Logout failed" }
                                    isLoggingOut = false
                                }
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ProfileField(
    label: String,
    value: String
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
