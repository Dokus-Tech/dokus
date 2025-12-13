package ai.dokus.app.auth.viewmodel

import ai.dokus.app.auth.repository.AuthRepository
import ai.dokus.app.core.state.DokusState
import ai.dokus.app.core.state.emit
import ai.dokus.app.core.state.emitLoading
import ai.dokus.app.core.viewmodel.BaseViewModel
import ai.dokus.foundation.domain.Name
import ai.dokus.foundation.domain.model.User
import ai.dokus.foundation.platform.Logger
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

internal class ProfileSettingsViewModel : BaseViewModel<DokusState<User>>(DokusState.idle()), KoinComponent {

    private val logger = Logger.forClass<ProfileSettingsViewModel>()
    private val authRepository: AuthRepository by inject()

    private val _isEditing = MutableStateFlow(false)
    val isEditing: StateFlow<Boolean> = _isEditing.asStateFlow()

    private val _editFirstName = MutableStateFlow(Name(""))
    val editFirstName: StateFlow<Name> = _editFirstName.asStateFlow()

    private val _editLastName = MutableStateFlow(Name(""))
    val editLastName: StateFlow<Name> = _editLastName.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _effect = MutableSharedFlow<Effect>()
    val effect = _effect.asSharedFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        scope.launch {
            logger.d { "Loading user profile" }
            mutableState.emitLoading()

            authRepository.getCurrentUser().fold(
                onSuccess = { user ->
                    logger.i { "User profile loaded: ${user.email.value}" }
                    mutableState.emit(user)
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to load user profile" }
                    mutableState.emit(error) { loadProfile() }
                }
            )
        }
    }

    fun startEditing() {
        val currentState = state.value
        if (currentState is DokusState.Success) {
            val user = currentState.data
            _editFirstName.value = user.firstName ?: Name("")
            _editLastName.value = user.lastName ?: Name("")
            _isEditing.value = true
            logger.d { "Started editing profile" }
        }
    }

    fun cancelEditing() {
        _isEditing.value = false
        logger.d { "Cancelled editing profile" }
    }

    fun updateFirstName(name: Name) {
        _editFirstName.value = name
    }

    fun updateLastName(name: Name) {
        _editLastName.value = name
    }

    fun saveProfile() {
        scope.launch {
            logger.d { "Saving profile" }
            _isSaving.value = true

            val firstName = _editFirstName.value.takeIf { it.value.isNotBlank() }
            val lastName = _editLastName.value.takeIf { it.value.isNotBlank() }

            authRepository.updateProfile(firstName, lastName).fold(
                onSuccess = { updatedUser ->
                    logger.i { "Profile saved successfully" }
                    mutableState.emit(updatedUser)
                    _isEditing.value = false
                    _effect.emit(Effect.ShowSaveSuccess)
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to save profile" }
                    _effect.emit(Effect.ShowSaveError(error.message ?: "Failed to save profile"))
                }
            )

            _isSaving.value = false
        }
    }

    fun hasChanges(): Boolean {
        val currentState = state.value
        if (currentState !is DokusState.Success) return false

        val user = currentState.data
        val currentFirstName = user.firstName?.value ?: ""
        val currentLastName = user.lastName?.value ?: ""

        return _editFirstName.value.value != currentFirstName ||
                _editLastName.value.value != currentLastName
    }

    fun canSave(): Boolean {
        return _editFirstName.value.isValid && _editLastName.value.isValid && hasChanges()
    }

    sealed interface Effect {
        data object ShowSaveSuccess : Effect
        data class ShowSaveError(val message: String) : Effect
    }
}
