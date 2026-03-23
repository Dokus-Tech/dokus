package tech.dokus.features.auth.presentation.auth.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDateTime
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.profile_settings_title
import tech.dokus.domain.Email
import tech.dokus.domain.Name
import tech.dokus.domain.asbtractions.RetryHandler
import tech.dokus.domain.config.ServerConfig
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.UserId
import tech.dokus.domain.model.User
import tech.dokus.features.auth.mvi.AvatarState
import tech.dokus.features.auth.mvi.MySessionsState
import tech.dokus.features.auth.mvi.ProfileSettingsIntent
import tech.dokus.features.auth.mvi.ProfileSettingsState
import tech.dokus.features.auth.presentation.auth.components.AccountCard
import tech.dokus.features.auth.presentation.auth.components.DangerZoneCard
import tech.dokus.features.auth.presentation.auth.components.LogOutCard
import tech.dokus.features.auth.presentation.auth.components.ProfileEditingSection
import tech.dokus.features.auth.presentation.auth.components.ProfileHero
import tech.dokus.features.auth.presentation.auth.components.ProfileSavingSection
import tech.dokus.features.auth.presentation.auth.components.SecurityCard
import tech.dokus.features.auth.presentation.auth.components.ServerCard
import tech.dokus.features.auth.presentation.auth.components.VersionFooter
import tech.dokus.foundation.app.picker.rememberImagePicker
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.app.state.isSuccess
import tech.dokus.foundation.aura.components.common.DokusErrorBanner
import tech.dokus.foundation.aura.components.common.ErrorOverlay
import tech.dokus.foundation.aura.components.common.PTopAppBar
import tech.dokus.foundation.aura.local.LocalScreenSize
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private val MaxContentWidth = 440.dp
private val ContentPaddingH = 16.dp
private val SectionSpacing = 14.dp
private val DetailPaneWidth = 520.dp

/**
 * Profile settings screen with top bar and navigation.
 */
@Composable
fun ProfileSettingsScreen(
    state: ProfileSettingsState,
    currentServer: ServerConfig,
    isLoggingOut: Boolean,
    resetToCloudError: DokusException? = null,
    onDismissResetToCloudError: () -> Unit = {},
    onIntent: (ProfileSettingsIntent) -> Unit,
    onResendVerification: () -> Unit,
    onChangePassword: () -> Unit,
    onMySessions: () -> Unit,
    onChangeServer: () -> Unit,
    onResetToCloud: () -> Unit,
    onLogout: () -> Unit,
    detailPaneContent: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isLargeScreen = LocalScreenSize.current.isLarge
    val errorToShow = state.actionError ?: resetToCloudError

    Scaffold(
        topBar = {
            if (!isLargeScreen) PTopAppBar(Res.string.profile_settings_title)
        },
    ) { contentPadding ->
        val contentModifier = modifier.padding(contentPadding)
        if (isLargeScreen && detailPaneContent != null) {
            Row(
                modifier = contentModifier.fillMaxSize()
            ) {
                ProfileSettingsContent(
                    state = state,
                    currentServer = currentServer,
                    isLoggingOut = isLoggingOut,
                    actionError = errorToShow,
                    onDismissError = {
                        onIntent(ProfileSettingsIntent.DismissActionError)
                        onDismissResetToCloudError()
                    },
                    onIntent = onIntent,
                    onResendVerification = onResendVerification,
                    onChangePassword = onChangePassword,
                    onMySessions = onMySessions,
                    onChangeServer = onChangeServer,
                    onResetToCloud = onResetToCloud,
                    onLogout = onLogout,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentHorizontalAlignment = Alignment.End,
                )
                VerticalDivider(modifier = Modifier.padding(vertical = 16.dp))
                Box(
                    modifier = Modifier
                        .width(DetailPaneWidth)
                        .fillMaxHeight()
                        .padding(start = 24.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
                    contentAlignment = Alignment.TopStart
                ) {
                    Box(modifier = Modifier.fillMaxHeight()) {
                        detailPaneContent()
                    }
                }
            }
        } else {
            ProfileSettingsContent(
                state = state,
                currentServer = currentServer,
                isLoggingOut = isLoggingOut,
                actionError = errorToShow,
                onDismissError = {
                    onIntent(ProfileSettingsIntent.DismissActionError)
                    onDismissResetToCloudError()
                },
                onIntent = onIntent,
                onResendVerification = onResendVerification,
                onChangePassword = onChangePassword,
                onMySessions = onMySessions,
                onChangeServer = onChangeServer,
                onResetToCloud = onResetToCloud,
                onLogout = onLogout,
                modifier = contentModifier
            )
        }
    }
}

/**
 * Profile settings content -- iOS Settings-style grouped cards, centered.
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
    actionError: DokusException? = null,
    onDismissError: () -> Unit = {},
    contentHorizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
) {
    val avatarPicker = rememberImagePicker { pickedImage ->
        onIntent(ProfileSettingsIntent.UploadAvatar(pickedImage.bytes, pickedImage.name))
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = contentHorizontalAlignment,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = MaxContentWidth)
                .padding(horizontal = ContentPaddingH)
                .padding(top = 16.dp)
                .align(Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(SectionSpacing),
        ) {
            actionError?.let { error ->
                DokusErrorBanner(
                    exception = error,
                    retryHandler = null,
                    onDismiss = onDismissError,
                )
            }

            // User data section: loading / error / editing / saving / viewing
            ErrorOverlay(
                exception = if (state.user is DokusState.Error) state.user.exception else null,
                retryHandler = if (state.user is DokusState.Error) state.user.retryHandler else null,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(SectionSpacing)) {
                    when {
                        state.isSaving && state.user.isSuccess() -> {
                            ProfileSavingSection(
                                email = state.user.data.email.value,
                                editFirstName = state.editFirstName,
                                editLastName = state.editLastName,
                            )
                        }

                        state.isEditing && state.user.isSuccess() -> {
                            ProfileEditingSection(
                                email = state.user.data.email.value,
                                editFirstName = state.editFirstName,
                                editLastName = state.editLastName,
                                canSave = state.canSave,
                                onFirstNameChange = {
                                    onIntent(
                                        ProfileSettingsIntent.UpdateFirstName(
                                            it
                                        )
                                    )
                                },
                                onLastNameChange = {
                                    onIntent(
                                        ProfileSettingsIntent.UpdateLastName(
                                            it
                                        )
                                    )
                                },
                                onSave = { onIntent(ProfileSettingsIntent.SaveClicked) },
                                onCancel = { onIntent(ProfileSettingsIntent.CancelEditing) },
                            )
                        }

                        state.user.isSuccess() -> {
                            val user = state.user.data
                            ProfileHero(
                                user = user,
                                avatarState = state.avatarState,
                                onUploadAvatar = { avatarPicker.launch() },
                                onResetAvatarState = { onIntent(ProfileSettingsIntent.ResetAvatarState) },
                            )
                            AccountCard(
                                user = user,
                                isResendingVerification = state.isResendingVerification,
                                onResendVerification = onResendVerification,
                                onEditClick = { onIntent(ProfileSettingsIntent.StartEditing) },
                            )
                        }

                        else -> {
                            val now = LocalDateTime(2026, 1, 1, 0, 0)
                            val emptyUser = User(
                                id = UserId.generate(),
                                email = Email(""),
                                createdAt = now,
                                updatedAt = now,
                            )
                            ProfileHero(
                                user = emptyUser,
                                avatarState = AvatarState.Idle,
                                onUploadAvatar = {},
                                onResetAvatarState = {},
                            )
                            AccountCard(
                                user = emptyUser,
                                isResendingVerification = false,
                                onResendVerification = {},
                                onEditClick = {},
                            )
                        }
                    }
                }
            }

            // Independent sections -- always visible regardless of state
            if (!state.isEditing && !state.isSaving) {
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

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Preview
@Composable
private fun ProfileSettingsLoadingPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        ProfileSettingsContent(
            state = ProfileSettingsState.initial,
            currentServer = ServerConfig.Cloud,
            isLoggingOut = false,
            onIntent = {},
            onResendVerification = {},
            onChangePassword = {},
            onMySessions = {},
            onChangeServer = {},
            onResetToCloud = {},
            onLogout = {},
        )
    }
}

@Preview
@Composable
private fun ProfileSettingsErrorPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        ProfileSettingsContent(
            state = ProfileSettingsState(
                user = DokusState.error(
                    exception = DokusException.ConnectionError(),
                    retryHandler = RetryHandler { },
                ),
            ),
            currentServer = ServerConfig.Cloud,
            isLoggingOut = false,
            onIntent = {},
            onResendVerification = {},
            onChangePassword = {},
            onMySessions = {},
            onChangeServer = {},
            onResetToCloud = {},
            onLogout = {},
        )
    }
}

@OptIn(ExperimentalUuidApi::class)
@Preview(name = "Profile Settings Desktop Idle", widthDp = 1366, heightDp = 900)
@Composable
private fun ProfileSettingsDesktopIdlePreview(
    @PreviewParameter(
        PreviewParametersProvider::class
    ) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        ProfileSettingsScreen(
            state = ProfileSettingsState(
                user = DokusState.success(
                    User(
                        id = UserId(Uuid.parse("00000000-0000-0000-0000-000000000001")),
                        email = Email("john@dokus.tech"),
                        firstName = Name("John"),
                        lastName = Name("Doe"),
                        emailVerified = true,
                        createdAt = LocalDateTime(2025, 1, 1, 0, 0),
                        updatedAt = LocalDateTime(2025, 1, 1, 0, 0),
                    ),
                ),
            ),
            currentServer = ServerConfig.Cloud,
            isLoggingOut = false,
            onIntent = {},
            onResendVerification = {},
            onChangePassword = {},
            onMySessions = {},
            onChangeServer = {},
            onResetToCloud = {},
            onLogout = {},
            detailPaneContent = {
                ProfileDetailPaneHost(
                    selection = ProfileDetailSelection.None,
                    sessionsState = MySessionsState.initial,
                    onSessionsIntent = {},
                )
            }
        )
    }
}

@OptIn(ExperimentalUuidApi::class)
@Preview(name = "Profile Settings Desktop Split", widthDp = 1366, heightDp = 900)
@Composable
private fun ProfileSettingsDesktopSplitPreview(
    @PreviewParameter(
        PreviewParametersProvider::class
    ) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        ProfileSettingsScreen(
            state = ProfileSettingsState(
                user = DokusState.success(
                    User(
                        id = UserId(Uuid.parse("00000000-0000-0000-0000-000000000001")),
                        email = Email("john@dokus.tech"),
                        firstName = Name("John"),
                        lastName = Name("Doe"),
                        emailVerified = true,
                        createdAt = LocalDateTime(2025, 1, 1, 0, 0),
                        updatedAt = LocalDateTime(2025, 1, 1, 0, 0),
                    ),
                ),
            ),
            currentServer = ServerConfig.Cloud,
            isLoggingOut = false,
            onIntent = {},
            onResendVerification = {},
            onChangePassword = {},
            onMySessions = {},
            onChangeServer = {},
            onResetToCloud = {},
            onLogout = {},
            detailPaneContent = {
                ProfileDetailPaneHost(
                    selection = ProfileDetailSelection.Sessions,
                    sessionsState = MySessionsState(
                        sessions = DokusState.success(previewSessions())
                    ),
                    onSessionsIntent = {},
                    nowEpochSeconds = SessionsPreviewNowEpochSeconds,
                )
            }
        )
    }
}

@OptIn(ExperimentalUuidApi::class)
@Preview
@Composable
private fun ProfileSettingsContentPreview(
    @PreviewParameter(
        PreviewParametersProvider::class
    ) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        ProfileSettingsContent(
            state = ProfileSettingsState(
                user = DokusState.success(
                    User(
                        id = UserId(Uuid.parse("00000000-0000-0000-0000-000000000001")),
                        email = Email("john@dokus.tech"),
                        firstName = Name("John"),
                        lastName = Name("Doe"),
                        emailVerified = true,
                        createdAt = LocalDateTime(2025, 1, 1, 0, 0),
                        updatedAt = LocalDateTime(2025, 1, 1, 0, 0),
                    ),
                ),
            ),
            currentServer = ServerConfig.Cloud,
            isLoggingOut = false,
            onIntent = {},
            onResendVerification = {},
            onChangePassword = {},
            onMySessions = {},
            onChangeServer = {},
            onResetToCloud = {},
            onLogout = {},
        )
    }
}
