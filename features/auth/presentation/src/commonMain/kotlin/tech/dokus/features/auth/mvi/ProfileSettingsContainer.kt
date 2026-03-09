package tech.dokus.features.auth.mvi

import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.api.Store
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.dsl.updateStateImmediate
import pro.respawn.flowmvi.plugins.init
import pro.respawn.flowmvi.plugins.reduce
import tech.dokus.domain.Name
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.exceptions.asDokusException
import tech.dokus.features.auth.usecases.GetCurrentUserUseCase
import tech.dokus.features.auth.usecases.ResendVerificationEmailUseCase
import tech.dokus.features.auth.usecases.UploadUserAvatarUseCase
import tech.dokus.features.auth.usecases.UpdateProfileUseCase
import tech.dokus.features.auth.usecases.WatchCurrentUserUseCase
import tech.dokus.foundation.app.picker.inferImageContentType
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.app.state.isSuccess
import tech.dokus.foundation.platform.Logger

internal typealias ProfileSettingsCtx =
    PipelineContext<ProfileSettingsState, ProfileSettingsIntent, ProfileSettingsAction>

/**
 * Container for Profile Settings screen using FlowMVI.
 * Manages viewing and editing user profile.
 *
 * Use with Koin's `container<>` DSL for automatic ViewModel wrapping and lifecycle management.
 */
class ProfileSettingsContainer(
    private val getCurrentUser: GetCurrentUserUseCase,
    private val updateProfile: UpdateProfileUseCase,
    private val uploadUserAvatar: UploadUserAvatarUseCase,
    private val watchCurrentUserUseCase: WatchCurrentUserUseCase,
    private val resendVerificationEmailUseCase: ResendVerificationEmailUseCase,
) : Container<ProfileSettingsState, ProfileSettingsIntent, ProfileSettingsAction> {

    private val logger = Logger.forClass<ProfileSettingsContainer>()

    override val store: Store<ProfileSettingsState, ProfileSettingsIntent, ProfileSettingsAction> =
        store(ProfileSettingsState.initial) {
            configure {
                name = "ProfileSettingsStore"
            }

            init {
                intent(ProfileSettingsIntent.LoadProfile)
            }

            reduce { intent ->
                when (intent) {
                    is ProfileSettingsIntent.LoadProfile -> loadProfile()
                    is ProfileSettingsIntent.StartEditing -> handleStartEditing()
                    is ProfileSettingsIntent.CancelEditing -> handleCancelEditing()
                    is ProfileSettingsIntent.UpdateFirstName -> handleUpdateFirstName(intent.value)
                    is ProfileSettingsIntent.UpdateLastName -> handleUpdateLastName(intent.value)
                    is ProfileSettingsIntent.SaveClicked -> handleSave()
                    is ProfileSettingsIntent.UploadAvatar -> handleUploadAvatar(intent.imageBytes, intent.filename)
                    is ProfileSettingsIntent.ResetAvatarState -> handleResetAvatarState()
                    is ProfileSettingsIntent.ResendVerificationClicked -> handleResendVerification()
                    is ProfileSettingsIntent.ChangePasswordClicked -> handleChangePassword()
                    is ProfileSettingsIntent.MySessionsClicked -> handleMySessions()
                    is ProfileSettingsIntent.BackClicked -> handleBack()
                }
            }
        }

    private suspend fun ProfileSettingsCtx.loadProfile() {
        logger.d { "Loading user profile" }
        updateState { copy(user = user.asLoading) }

        getCurrentUser().fold(
            onSuccess = { user ->
                logger.i { "User profile loaded: ${user.email.value}" }
                updateState { copy(user = DokusState.success(user)) }
            },
            onFailure = { error ->
                logger.e(error) { "Failed to load user profile" }
                updateState {
                    copy(
                        user = DokusState.error(
                            exception = error.asDokusException,
                            retryHandler = { intent(ProfileSettingsIntent.LoadProfile) }
                        )
                    )
                }
            }
        )
    }

    private suspend fun ProfileSettingsCtx.handleStartEditing() {
        withState {
            if (!user.isSuccess()) return@withState
            val currentUser = user.data
            logger.d { "Started editing profile" }
            updateState {
                copy(
                    isEditing = true,
                    editFirstName = currentUser.firstName ?: Name(""),
                    editLastName = currentUser.lastName ?: Name(""),
                )
            }
        }
    }

    private suspend fun ProfileSettingsCtx.handleCancelEditing() {
        logger.d { "Cancelled editing profile" }
        updateState {
            copy(
                isEditing = false,
                editFirstName = Name.Empty,
                editLastName = Name.Empty,
            )
        }
    }

    private suspend fun ProfileSettingsCtx.handleUpdateFirstName(value: Name) {
        updateState {
            if (isEditing) copy(editFirstName = value) else this
        }
    }

    private suspend fun ProfileSettingsCtx.handleUpdateLastName(value: Name) {
        updateState {
            if (isEditing) copy(editLastName = value) else this
        }
    }

    private suspend fun ProfileSettingsCtx.handleSave() {
        withState {
            if (!isEditing || !user.isSuccess()) return@withState
            logger.d { "Saving profile" }

            val currentEditFirstName = editFirstName
            val currentEditLastName = editLastName

            updateState { copy(isSaving = true) }

            val firstName = currentEditFirstName.takeIf { it.value.isNotBlank() }
            val lastName = currentEditLastName.takeIf { it.value.isNotBlank() }

            updateProfile(firstName, lastName).fold(
                onSuccess = { updatedUser ->
                    logger.i { "Profile saved successfully" }
                    watchCurrentUserUseCase.refresh()
                    updateState {
                        copy(
                            user = DokusState.success(updatedUser),
                            isEditing = false,
                            isSaving = false,
                            editFirstName = Name.Empty,
                            editLastName = Name.Empty,
                        )
                    }
                    action(ProfileSettingsAction.ShowSaveSuccess)
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to save profile" }
                    updateState { copy(isSaving = false) }
                    val exception = error.asDokusException
                    val displayException = if (exception is DokusException.Unknown) {
                        DokusException.ProfileSaveFailed
                    } else {
                        exception
                    }
                    action(ProfileSettingsAction.ShowSaveError(displayException))
                }
            )
        }
    }

    private suspend fun ProfileSettingsCtx.handleResendVerification() {
        withState {
            if (!user.isSuccess()) return@withState
            val currentUser = user.data
            if (currentUser.emailVerified) return@withState

            updateState { copy(isResendingVerification = true) }
            resendVerificationEmailUseCase().fold(
                onSuccess = {
                    updateState { copy(isResendingVerification = false) }
                    action(ProfileSettingsAction.ShowVerificationEmailSent)
                },
                onFailure = { error ->
                    updateState { copy(isResendingVerification = false) }
                    action(ProfileSettingsAction.ShowVerificationEmailError(error.asDokusException))
                }
            )
        }
    }

    private suspend fun ProfileSettingsCtx.handleUploadAvatar(imageBytes: ByteArray, filename: String) {
        withState {
            if (!user.isSuccess()) return@withState
            val currentUser = user.data
            updateState { copy(avatarState = AvatarState.Uploading(0f)) }

            uploadUserAvatar(
                userId = currentUser.id,
                imageBytes = imageBytes,
                filename = filename,
                contentType = inferImageContentType(filename),
                onProgress = { progress ->
                    updateStateImmediate {
                        copy(avatarState = AvatarState.Uploading(progress))
                    }
                }
            ).fold(
                onSuccess = { avatar ->
                    watchCurrentUserUseCase.refresh()
                    updateState {
                        copy(
                            user = DokusState.success(currentUser.copy(avatar = avatar)),
                            avatarState = AvatarState.Success,
                        )
                    }
                },
                onFailure = { error ->
                    val exception = error.asDokusException
                    val displayException = if (exception is DokusException.Unknown) {
                        DokusException.ProfileAvatarUploadFailed
                    } else {
                        exception
                    }
                    updateState {
                        copy(avatarState = AvatarState.Error(displayException))
                    }
                    action(ProfileSettingsAction.ShowAvatarError(displayException))
                }
            )
        }
    }

    private suspend fun ProfileSettingsCtx.handleResetAvatarState() {
        updateState { copy(avatarState = AvatarState.Idle) }
    }

    private suspend fun ProfileSettingsCtx.handleChangePassword() {
        action(ProfileSettingsAction.NavigateToChangePassword)
    }

    private suspend fun ProfileSettingsCtx.handleMySessions() {
        action(ProfileSettingsAction.NavigateToMySessions)
    }

    private suspend fun ProfileSettingsCtx.handleBack() {
        action(ProfileSettingsAction.NavigateBack)
    }
}
