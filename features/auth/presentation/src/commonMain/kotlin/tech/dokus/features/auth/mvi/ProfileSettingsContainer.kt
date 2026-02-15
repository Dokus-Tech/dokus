package tech.dokus.features.auth.mvi

import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.api.Store
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.dsl.withState
import pro.respawn.flowmvi.plugins.init
import pro.respawn.flowmvi.plugins.reduce
import tech.dokus.domain.Name
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.exceptions.asDokusException
import tech.dokus.features.auth.usecases.GetCurrentUserUseCase
import tech.dokus.features.auth.usecases.ResendVerificationEmailUseCase
import tech.dokus.features.auth.usecases.UpdateProfileUseCase
import tech.dokus.features.auth.usecases.WatchCurrentUserUseCase
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
    private val watchCurrentUserUseCase: WatchCurrentUserUseCase,
    private val resendVerificationEmailUseCase: ResendVerificationEmailUseCase,
) : Container<ProfileSettingsState, ProfileSettingsIntent, ProfileSettingsAction> {

    private val logger = Logger.forClass<ProfileSettingsContainer>()

    override val store: Store<ProfileSettingsState, ProfileSettingsIntent, ProfileSettingsAction> =
        store(ProfileSettingsState.Loading) {
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
                    is ProfileSettingsIntent.ResendVerificationClicked -> handleResendVerification()
                    is ProfileSettingsIntent.ChangePasswordClicked -> handleChangePassword()
                    is ProfileSettingsIntent.MySessionsClicked -> handleMySessions()
                    is ProfileSettingsIntent.BackClicked -> handleBack()
                }
            }
        }

    private suspend fun ProfileSettingsCtx.loadProfile() {
        logger.d { "Loading user profile" }

        getCurrentUser().fold(
            onSuccess = { user ->
                logger.i { "User profile loaded: ${user.email.value}" }
                updateState { ProfileSettingsState.Viewing(user = user) }
            },
            onFailure = { error ->
                logger.e(error) { "Failed to load user profile" }
                updateState {
                    ProfileSettingsState.Error(
                        exception = error.asDokusException,
                        retryHandler = { intent(ProfileSettingsIntent.LoadProfile) }
                    )
                }
            }
        )
    }

    private suspend fun ProfileSettingsCtx.handleStartEditing() {
        withState<ProfileSettingsState.Viewing, _> {
            logger.d { "Started editing profile" }
            updateState {
                ProfileSettingsState.Editing(
                    user = user,
                    editFirstName = user.firstName ?: Name(""),
                    editLastName = user.lastName ?: Name("")
                )
            }
        }
    }

    private suspend fun ProfileSettingsCtx.handleCancelEditing() {
        withState<ProfileSettingsState.Editing, _> {
            logger.d { "Cancelled editing profile" }
            updateState { ProfileSettingsState.Viewing(user = user) }
        }
    }

    private suspend fun ProfileSettingsCtx.handleUpdateFirstName(value: Name) {
        updateState {
            when (this) {
                is ProfileSettingsState.Editing -> copy(editFirstName = value)
                else -> this
            }
        }
    }

    private suspend fun ProfileSettingsCtx.handleUpdateLastName(value: Name) {
        updateState {
            when (this) {
                is ProfileSettingsState.Editing -> copy(editLastName = value)
                else -> this
            }
        }
    }

    private suspend fun ProfileSettingsCtx.handleSave() {
        withState<ProfileSettingsState.Editing, _> {
            logger.d { "Saving profile" }

            val currentUser = user
            val currentEditFirstName = editFirstName
            val currentEditLastName = editLastName

            // Transition to saving state
            updateState {
                ProfileSettingsState.Saving(
                    user = currentUser,
                    editFirstName = currentEditFirstName,
                    editLastName = currentEditLastName
                )
            }

            val firstName = currentEditFirstName.takeIf { it.value.isNotBlank() }
            val lastName = currentEditLastName.takeIf { it.value.isNotBlank() }

            updateProfile(firstName, lastName).fold(
                onSuccess = { updatedUser ->
                    logger.i { "Profile saved successfully" }
                    watchCurrentUserUseCase.refresh()
                    updateState { ProfileSettingsState.Viewing(user = updatedUser) }
                    action(ProfileSettingsAction.ShowSaveSuccess)
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to save profile" }
                    // Return to editing state on error
                    updateState {
                        ProfileSettingsState.Editing(
                            user = currentUser,
                            editFirstName = currentEditFirstName,
                            editLastName = currentEditLastName
                        )
                    }
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
        withState<ProfileSettingsState.Viewing, _> {
            if (user.emailVerified) return@withState

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
