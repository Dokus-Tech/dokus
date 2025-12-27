package ai.dokus.app.auth.screen

import ai.dokus.app.auth.components.CurrentServerSection
import ai.dokus.app.auth.usecases.ConnectToServerUseCase
import ai.dokus.app.auth.usecases.LogoutUseCase
import ai.dokus.app.auth.viewmodel.ProfileSettingsAction
import ai.dokus.app.auth.viewmodel.ProfileSettingsContainer
import ai.dokus.app.auth.viewmodel.ProfileSettingsIntent
import ai.dokus.app.auth.viewmodel.ProfileSettingsState
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
import ai.dokus.app.resources.generated.profile_settings_title
import ai.dokus.foundation.design.components.POutlinedButton
import ai.dokus.foundation.design.components.PPrimaryButton
import ai.dokus.foundation.design.components.common.PTopAppBar
import ai.dokus.foundation.design.components.fields.PTextFieldName
import ai.dokus.foundation.design.constrains.withContentPaddingForScrollable
import tech.dokus.domain.Name
import tech.dokus.domain.config.ServerConfigManager
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
import androidx.compose.foundation.text.KeyboardOptions
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
    val navController = LocalNavController.current

    val state by container.store.subscribe(DefaultLifecycle) { action ->
        when (action) {
            ProfileSettingsAction.ShowSaveSuccess -> {
                snackbarHostState.showSnackbar("Profile saved successfully")
            }
            is ProfileSettingsAction.ShowSaveError -> {
                snackbarHostState.showSnackbar(action.message)
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
        verticalArrangement = Arrangement.spacedBy(16.dp)
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
                            snackbarHostState.showSnackbar("Failed to reset to cloud")
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
                value = state.user.firstName?.value ?: "-"
            )

            Spacer(Modifier.height(12.dp))

            ProfileField(
                label = stringResource(Res.string.profile_last_name),
                value = state.user.lastName?.value ?: "-"
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
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
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
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
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

@Composable
private fun DangerZoneSection() {
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

@Composable
private fun LogoutSection(
    isLoggingOut: Boolean,
    onLogout: () -> Unit
) {
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
