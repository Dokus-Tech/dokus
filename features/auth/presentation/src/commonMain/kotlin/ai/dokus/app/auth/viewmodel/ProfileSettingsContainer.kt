package ai.dokus.app.auth.viewmodel

import ai.dokus.app.auth.repository.AuthRepository
import ai.dokus.app.resources.generated.Res
import ai.dokus.app.resources.generated.profile_save_error
import ai.dokus.foundation.platform.Logger
import org.jetbrains.compose.resources.getString
import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.api.Store
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.dsl.withState
import pro.respawn.flowmvi.plugins.init
import pro.respawn.flowmvi.plugins.reduce
import tech.dokus.domain.Name
import tech.dokus.domain.exceptions.asDokusException

internal typealias ProfileSettingsCtx = PipelineContext<ProfileSettingsState, ProfileSettingsIntent, ProfileSettingsAction>

/**
 * Container for Profile Settings screen using FlowMVI.
 * Manages viewing and editing user profile.
 *
 * Use with Koin's `container<>` DSL for automatic ViewModel wrapping and lifecycle management.
 */
class ProfileSettingsContainer(
    private val authRepository: AuthRepository,
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
                    is ProfileSettingsIntent.BackClicked -> handleBack()
                }
            }
        }

    private suspend fun ProfileSettingsCtx.loadProfile() {
        logger.d { "Loading user profile" }

        authRepository.getCurrentUser().fold(
            onSuccess = { user ->
                logger.i { "User profile loaded: ${user.email.value}" }
                updateState { ProfileSettingsState.Viewing(user) }
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
            updateState { ProfileSettingsState.Viewing(user) }
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

            authRepository.updateProfile(firstName, lastName).fold(
                onSuccess = { updatedUser ->
                    logger.i { "Profile saved successfully" }
                    updateState { ProfileSettingsState.Viewing(updatedUser) }
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
                    action(ProfileSettingsAction.ShowSaveError(getString(Res.string.profile_save_error)))
                }
            )
        }
    }

    private suspend fun ProfileSettingsCtx.handleBack() {
        action(ProfileSettingsAction.NavigateBack)
    }
}
