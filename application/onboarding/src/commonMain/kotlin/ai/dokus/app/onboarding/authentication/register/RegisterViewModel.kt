package ai.dokus.app.onboarding.authentication.register

import ai.dokus.app.core.viewmodel.BaseViewModel
import ai.dokus.app.repository.extensions.authCredentials
import ai.dokus.app.repository.extensions.user
import ai.dokus.foundation.apispec.AuthApi
import ai.dokus.foundation.apispec.UserApi
import ai.dokus.foundation.domain.Email
import ai.dokus.foundation.domain.Name
import ai.dokus.foundation.domain.Password
import ai.dokus.foundation.domain.exceptions.DokusException
import ai.dokus.foundation.domain.exceptions.asDokusException
import ai.dokus.foundation.domain.model.AuthCredentials
import ai.dokus.foundation.domain.model.CreateUserRequest
import ai.dokus.foundation.domain.model.JwtTokenDataSchema
import ai.dokus.foundation.domain.model.LoginRequest
import ai.dokus.foundation.domain.model.User
import ai.dokus.foundation.domain.usecases.CreateNewUserUseCase
import ai.dokus.foundation.platform.persistence
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

internal class RegisterViewModel : BaseViewModel<RegisterViewModel.State>(State.Loading), KoinComponent {

    private val createNewUserUseCase: CreateNewUserUseCase by inject()

    private val authApi: AuthApi by inject()
    private val userApi: UserApi by inject()

    private val mutableEffect = MutableSharedFlow<Effect>()
    val effect = mutableEffect.asSharedFlow()

    fun createUser(newEmail: Email, newPassword: Password, firstName: Name, lastName: Name) {
        scope.launch {
            createNewUserUseCase(
                firstName = firstName,
                lastName = lastName,
                email = newEmail,
                password = newPassword
            ).getOrElse {
                mutableState.value = State.Error(it.asDokusException)
                return@launch
            }

            val request = CreateUserRequest(
                firstName = firstName,
                lastName = lastName,
                email = newEmail,
                password = newPassword
            )
            val createdUser = userApi.createUser(request).getOrElse {
                mutableState.value = State.Error(it.asDokusException)
                return@getOrElse
            }

            val loginRequest = LoginRequest(email = newEmail, password = newPassword)
            val jwtRaw = authApi.login(loginRequest).getOrElse {
                mutableState.value = State.Error(it.asDokusException)
                return@launch
            }

            val jwtSchema = JwtTokenDataSchema.from(jwtRaw).getOrElse {
                mutableState.value = State.Error(it.asDokusException)
                return@launch
            }

            persistence.authCredentials = AuthCredentials.from(jwtSchema, jwtRaw)
            persistence.user = User.from(jwtSchema)

            mutableEffect.emit(Effect.NavigateToRegistrationConfirmation)
        }
    }

    sealed interface State {
        data object Loading : State

        data class Error(val exception: DokusException) : State
    }

    sealed interface Effect {
        data object NavigateToRegistrationConfirmation : Effect
    }
}