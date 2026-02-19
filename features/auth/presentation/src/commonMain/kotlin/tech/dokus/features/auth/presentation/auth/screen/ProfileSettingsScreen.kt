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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import tech.dokus.features.auth.presentation.auth.components.AccountCard
import tech.dokus.features.auth.presentation.auth.components.DangerZoneCard
import tech.dokus.features.auth.presentation.auth.components.LogOutCard
import tech.dokus.features.auth.presentation.auth.components.ProfileEditingSection
import tech.dokus.features.auth.presentation.auth.components.ProfileErrorSection
import tech.dokus.features.auth.presentation.auth.components.ProfileHero
import tech.dokus.features.auth.presentation.auth.components.ProfileSavingSection
import tech.dokus.features.auth.presentation.auth.components.SecurityCard
import tech.dokus.features.auth.presentation.auth.components.ServerCard
import tech.dokus.features.auth.presentation.auth.components.VersionFooter
import tech.dokus.foundation.aura.components.common.DokusLoader
import tech.dokus.foundation.aura.components.common.PTopAppBar
import tech.dokus.foundation.aura.local.LocalScreenSize

private val MaxContentWidth = 440.dp
private val ContentPaddingH = 16.dp
private val SectionSpacing = 14.dp

/**
 * Profile settings screen with top bar and navigation.
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
 * Profile settings content — iOS Settings-style grouped cards, centered.
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
    @Suppress("UNUSED_PARAMETER") contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = MaxContentWidth)
                .padding(horizontal = ContentPaddingH)
                .padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(SectionSpacing),
        ) {
            when (state) {
                ProfileSettingsState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        DokusLoader()
                    }
                }

                is ProfileSettingsState.Viewing -> {
                    ProfileHero(user = state.user)
                    AccountCard(
                        user = state.user,
                        isResendingVerification = state.isResendingVerification,
                        onResendVerification = onResendVerification,
                        onEditClick = { onIntent(ProfileSettingsIntent.StartEditing) },
                    )
                    SecurityCard(
                        onChangePassword = onChangePassword,
                        onMySessions = onMySessions,
                    )
                    ServerCard(
                        currentServer = currentServer,
                        onChangeServer = onChangeServer,
                        onResetToCloud = onResetToCloud,
                    )
                    DangerZoneCard()
                    LogOutCard(isLoggingOut = isLoggingOut, onLogout = onLogout)
                    VersionFooter()
                }

                is ProfileSettingsState.Editing -> {
                    ProfileEditingSection(
                        state = state,
                        onFirstNameChange = { onIntent(ProfileSettingsIntent.UpdateFirstName(it)) },
                        onLastNameChange = { onIntent(ProfileSettingsIntent.UpdateLastName(it)) },
                        onSave = { onIntent(ProfileSettingsIntent.SaveClicked) },
                        onCancel = { onIntent(ProfileSettingsIntent.CancelEditing) }
                    )
                }

                is ProfileSettingsState.Saving -> {
                    ProfileSavingSection(state = state)
                }

                is ProfileSettingsState.Error -> {
                    ProfileErrorSection()
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}
