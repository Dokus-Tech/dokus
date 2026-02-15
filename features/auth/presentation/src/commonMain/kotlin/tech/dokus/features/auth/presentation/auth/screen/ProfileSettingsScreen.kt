package tech.dokus.features.auth.presentation.auth.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.profile_settings_title
import tech.dokus.domain.config.ServerConfig
import tech.dokus.features.auth.mvi.ProfileSettingsIntent
import tech.dokus.features.auth.mvi.ProfileSettingsState
import tech.dokus.features.auth.presentation.auth.components.CurrentServerSection
import tech.dokus.features.auth.presentation.auth.components.DangerZoneSection
import tech.dokus.features.auth.presentation.auth.components.LogoutSection
import tech.dokus.features.auth.presentation.auth.components.ProfileEditingSection
import tech.dokus.features.auth.presentation.auth.components.ProfileErrorSection
import tech.dokus.features.auth.presentation.auth.components.ProfileSavingSection
import tech.dokus.features.auth.presentation.auth.components.ProfileViewingSection
import tech.dokus.foundation.aura.components.common.PTopAppBar
import tech.dokus.foundation.aura.constrains.withContentPaddingForScrollable
import tech.dokus.foundation.aura.local.LocalScreenSize

/**
 * Profile settings screen with top bar and navigation.
 * For mobile navigation flow.
 */
@Composable
fun ProfileSettingsScreen(
    state: ProfileSettingsState,
    currentServer: ServerConfig,
    isLoggingOut: Boolean,
    snackbarHostState: SnackbarHostState,
    onIntent: (ProfileSettingsIntent) -> Unit,
    onResendVerification: () -> Unit,
    onChangePassword: () -> Unit,
    onMySessions: () -> Unit,
    onChangeServer: () -> Unit,
    onResetToCloud: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLargeScreen = LocalScreenSize.current.isLarge

    Scaffold(
        topBar = {
            if (!isLargeScreen) PTopAppBar(Res.string.profile_settings_title)
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { contentPadding ->
        ProfileSettingsContent(
            state = state,
            currentServer = currentServer,
            isLoggingOut = isLoggingOut,
            onIntent = onIntent,
            onResendVerification = onResendVerification,
            onChangePassword = onChangePassword,
            onMySessions = onMySessions,
            onChangeServer = onChangeServer,
            onResetToCloud = onResetToCloud,
            onLogout = onLogout,
            modifier = modifier.padding(contentPadding)
        )
    }
}

/**
 * Profile settings content without scaffold.
 * Can be embedded in split-pane layout for desktop or used in full-screen for mobile.
 */
@Composable
fun ProfileSettingsContent(
    state: ProfileSettingsState,
    currentServer: ServerConfig,
    isLoggingOut: Boolean,
    onIntent: (ProfileSettingsIntent) -> Unit,
    onResendVerification: () -> Unit,
    onChangePassword: () -> Unit,
    onMySessions: () -> Unit,
    onChangeServer: () -> Unit,
    onResetToCloud: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
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
                    onEditClick = { onIntent(ProfileSettingsIntent.StartEditing) },
                    onResendVerification = onResendVerification,
                    onChangePasswordClick = onChangePassword,
                    onMySessionsClick = onMySessions
                )
                DangerZoneSection()
            }

            is ProfileSettingsState.Editing -> {
                ProfileEditingSection(
                    state = state,
                    onFirstNameChange = { onIntent(ProfileSettingsIntent.UpdateFirstName(it)) },
                    onLastNameChange = { onIntent(ProfileSettingsIntent.UpdateLastName(it)) },
                    onSave = { onIntent(ProfileSettingsIntent.SaveClicked) },
                    onCancel = { onIntent(ProfileSettingsIntent.CancelEditing) }
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
            onChangeServer = onChangeServer,
            onResetToCloud = onResetToCloud
        )

        // Logout Section - ALWAYS visible regardless of profile state
        LogoutSection(
            isLoggingOut = isLoggingOut,
            onLogout = onLogout
        )

        Spacer(Modifier.height(16.dp))
    }
}
