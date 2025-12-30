package ai.dokus.app.auth.screen

import ai.dokus.app.auth.components.CurrentServerSection
import ai.dokus.app.auth.usecases.ConnectToServerUseCase
import ai.dokus.app.auth.usecases.LogoutUseCase
import ai.dokus.app.auth.viewmodel.ProfileSettingsAction
import ai.dokus.app.auth.viewmodel.ProfileSettingsContainer
import ai.dokus.app.auth.viewmodel.ProfileSettingsIntent
import ai.dokus.app.auth.viewmodel.ProfileSettingsState
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.common_empty_value
import tech.dokus.aura.resources.profile_cancel
import tech.dokus.aura.resources.profile_danger_zone
import tech.dokus.aura.resources.profile_deactivate_account
import tech.dokus.aura.resources.profile_deactivate_warning
import tech.dokus.aura.resources.profile_edit
import tech.dokus.aura.resources.profile_email
import tech.dokus.aura.resources.profile_first_name
import tech.dokus.aura.resources.profile_last_name
import tech.dokus.aura.resources.profile_load_failed
import tech.dokus.aura.resources.profile_logging_out
import tech.dokus.aura.resources.profile_logout
import tech.dokus.aura.resources.profile_logout_description
import tech.dokus.aura.resources.profile_personal_info
import tech.dokus.aura.resources.profile_reset_to_cloud_failed
import tech.dokus.aura.resources.profile_save
import tech.dokus.aura.resources.profile_save_success
import tech.dokus.aura.resources.profile_settings_title
import tech.dokus.foundation.aura.components.POutlinedButton
import tech.dokus.foundation.aura.components.PPrimaryButton
import tech.dokus.foundation.aura.components.DokusCard
import tech.dokus.foundation.aura.components.DokusCardPadding
import tech.dokus.foundation.aura.components.common.PTopAppBar
import tech.dokus.foundation.aura.components.fields.PTextFieldName
import tech.dokus.foundation.aura.constrains.withContentPaddingForScrollable
import tech.dokus.foundation.aura.extensions.localized
import tech.dokus.domain.Name
import tech.dokus.domain.config.ServerConfigManager
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.navigation.destinations.AuthDestination
import tech.dokus.navigation.local.LocalNavController
import tech.dokus.navigation.navigateTo
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import pro.respawn.flowmvi.api.IntentReceiver
import pro.respawn.flowmvi.compose.dsl.DefaultLifecycle
import pro.respawn.flowmvi.compose.dsl.subscribe
import tech.dokus.foundation.app.mvi.container

/**
 * Profile settings screen with top bar and navigation.
 * For mobile navigation flow.
 */
@Composable
fun ProfileSettingsScreen(
    container: ProfileSettingsContainer = container()
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingSuccess by remember { mutableStateOf(false) }
    var pendingError by remember { mutableStateOf<DokusException?>(null) }
    val navController = LocalNavController.current
    val saveSuccessMessage = if (pendingSuccess) {
        stringResource(Res.string.profile_save_success)
    } else {
        null
    }
    val errorMessage = pendingError?.localized

    LaunchedEffect(saveSuccessMessage) {
        if (saveSuccessMessage != null) {
            snackbarHostState.showSnackbar(saveSuccessMessage)
            pendingSuccess = false
        }
    }

    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            snackbarHostState.showSnackbar(errorMessage)
            pendingError = null
        }
    }

    val state by container.store.subscribe(DefaultLifecycle) { action ->
        when (action) {
            ProfileSettingsAction.ShowSaveSuccess -> {
                pendingSuccess = true
            }
            is ProfileSettingsAction.ShowSaveError -> {
                pendingError = action.error
            }
            ProfileSettingsAction.NavigateBack -> {
                navController.navigateUp()
            }
        }
    }

    Scaffold(
        topBar = {
            PTopAppBar(
                title = stringResource(Res.string.profile_settings_title)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { contentPadding ->
        with(container.store) {
            ProfileSettingsContent(
                state = state,
                modifier = Modifier.padding(contentPadding),
                snackbarHostState = snackbarHostState
            )
        }
    }
}

/**
 * Profile settings content without scaffold.
 * Can be embedded in split-pane layout for desktop or used in full-screen for mobile.
 */
@Composable
fun IntentReceiver<ProfileSettingsIntent>.ProfileSettingsContent(
    state: ProfileSettingsState,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    val resetToCloudFailedMessage = stringResource(Res.string.profile_reset_to_cloud_failed)

    val logger = remember { Logger.withTag("ProfileSettingsContent") }
    val logoutUseCase: LogoutUseCase = koinInject()
    val serverConfigManager: ServerConfigManager = koinInject()
    val connectToServerUseCase: ConnectToServerUseCase = koinInject()
    val navController = LocalNavController.current
    val scope = rememberCoroutineScope()

    val currentServer by serverConfigManager.currentServer.collectAsState()
    var isResettingToCloud by remember { mutableStateOf(false) }
    var isLoggingOut by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .withContentPaddingForScrollable(),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Profile content based on state
        when (state) {
            ProfileSettingsState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is ProfileSettingsState.Viewing -> {
                ProfileViewingSection(
                    state = state,
                    onEditClick = { intent(ProfileSettingsIntent.StartEditing) }
                )
                DangerZoneSection()
            }
            is ProfileSettingsState.Editing -> {
                ProfileEditingSection(
                    state = state,
                    onFirstNameChange = { intent(ProfileSettingsIntent.UpdateFirstName(it)) },
                    onLastNameChange = { intent(ProfileSettingsIntent.UpdateLastName(it)) },
                    onSave = { intent(ProfileSettingsIntent.SaveClicked) },
                    onCancel = { intent(ProfileSettingsIntent.CancelEditing) }
                )
                DangerZoneSection()
            }
            is ProfileSettingsState.Saving -> {
                ProfileSavingSection(state = state)
                DangerZoneSection()
            }
            is ProfileSettingsState.Error -> {
                ProfileErrorSection()
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
                            snackbarHostState.showSnackbar(resetToCloudFailedMessage)
                            isResettingToCloud = false
                        }
                    )
                }
            }
        )

        // Logout Section - ALWAYS visible regardless of profile state
        LogoutSection(
            isLoggingOut = isLoggingOut,
            onLogout = {
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
            }
        )

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ProfileViewingSection(
    state: ProfileSettingsState.Viewing,
    onEditClick: () -> Unit
) {
    DokusCard(
        modifier = Modifier.fillMaxWidth(),
        padding = DokusCardPadding.Default,
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(Res.string.profile_personal_info),
                    style = MaterialTheme.typography.titleMedium
                )
                TextButton(onClick = onEditClick) {
                    Text(stringResource(Res.string.profile_edit))
                }
            }

            Spacer(Modifier.height(16.dp))

            ProfileField(
                label = stringResource(Res.string.profile_email),
                value = state.user.email.value
            )

            Spacer(Modifier.height(12.dp))

            ProfileField(
                label = stringResource(Res.string.profile_first_name),
                value = state.user.firstName?.value
                    ?: stringResource(Res.string.common_empty_value)
            )

            Spacer(Modifier.height(12.dp))

            ProfileField(
                label = stringResource(Res.string.profile_last_name),
                value = state.user.lastName?.value
                    ?: stringResource(Res.string.common_empty_value)
            )
        }
    }
}

@Composable
private fun ProfileEditingSection(
    state: ProfileSettingsState.Editing,
    onFirstNameChange: (Name) -> Unit,
    onLastNameChange: (Name) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    DokusCard(
        modifier = Modifier.fillMaxWidth(),
        padding = DokusCardPadding.Default,
    ) {
        Column {
            Text(
                text = stringResource(Res.string.profile_personal_info),
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(Modifier.height(16.dp))

            ProfileField(
                label = stringResource(Res.string.profile_email),
                value = state.user.email.value
            )

            Spacer(Modifier.height(12.dp))

            PTextFieldName(
                fieldName = stringResource(Res.string.profile_first_name),
                value = state.editFirstName,
                icon = null,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth(),
                onValueChange = onFirstNameChange
            )

            Spacer(Modifier.height(12.dp))

            PTextFieldName(
                fieldName = stringResource(Res.string.profile_last_name),
                value = state.editLastName,
                icon = null,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done
                ),
                onAction = { if (state.canSave) onSave() },
                modifier = Modifier.fillMaxWidth(),
                onValueChange = onLastNameChange
            )

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                POutlinedButton(
                    text = stringResource(Res.string.profile_cancel),
                    onClick = onCancel
                )
                Spacer(Modifier.width(8.dp))
                PPrimaryButton(
                    text = stringResource(Res.string.profile_save),
                    enabled = state.canSave,
                    onClick = onSave
                )
            }
        }
    }
}

@Composable
private fun ProfileSavingSection(
    state: ProfileSettingsState.Saving
) {
    DokusCard(
        modifier = Modifier.fillMaxWidth(),
        padding = DokusCardPadding.Default,
    ) {
        Column {
            Text(
                text = stringResource(Res.string.profile_personal_info),
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(Modifier.height(16.dp))

            ProfileField(
                label = stringResource(Res.string.profile_email),
                value = state.user.email.value
            )

            Spacer(Modifier.height(12.dp))

            ProfileField(
                label = stringResource(Res.string.profile_first_name),
                value = state.editFirstName.value
            )

            Spacer(Modifier.height(12.dp))

            ProfileField(
                label = stringResource(Res.string.profile_last_name),
                value = state.editLastName.value
            )

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Box(
                    modifier = Modifier.height(42.dp).width(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(24.dp).width(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileErrorSection() {
    DokusCard(
        modifier = Modifier.fillMaxWidth(),
        padding = DokusCardPadding.Default,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(Res.string.profile_load_failed),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun DangerZoneSection() {
    DokusCard(
        modifier = Modifier.fillMaxWidth(),
        padding = DokusCardPadding.Default,
    ) {
        Column {
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

@Composable
private fun LogoutSection(
    isLoggingOut: Boolean,
    onLogout: () -> Unit
) {
    DokusCard(
        modifier = Modifier.fillMaxWidth(),
        padding = DokusCardPadding.Default,
    ) {
        Column {
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
                text = if (isLoggingOut) {
                    stringResource(Res.string.profile_logging_out)
                } else {
                    stringResource(Res.string.profile_logout)
                },
                enabled = !isLoggingOut,
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth()
            )
        }
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
