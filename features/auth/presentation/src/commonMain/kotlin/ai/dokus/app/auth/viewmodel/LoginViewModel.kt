package ai.dokus.app.auth.viewmodel

import ai.dokus.app.auth.usecases.LoginUseCase
import ai.dokus.app.core.viewmodel.BaseViewModel
import ai.dokus.foundation.domain.Email
import ai.dokus.foundation.domain.Password
import ai.dokus.foundation.domain.exceptions.DokusException
import ai.dokus.foundation.domain.asbtractions.TokenManager
import ai.dokus.foundation.platform.Logger
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

internal class LoginViewModel : BaseViewModel<LoginViewModel.State>(State.Idle), KoinComponent {

    private val logger = Logger.forClass<LoginViewModel>()
    private val loginUseCase: LoginUseCase by inject()
    private val tokenManager: TokenManager by inject()

    private val mutableEffect = MutableSharedFlow<Effect>()
    val effect = mutableEffect.asSharedFlow()

    fun login(emailValue: Email, passwordValue: Password) = scope.launch {
        logger.d { "Login attempt started for email: ${emailValue.value.take(3)}***" }
        mutableState.value = State.Loading

        val result = loginUseCase(emailValue, passwordValue)

        result.fold(
            onSuccess = {
                logger.i { "Login successful, navigating to home" }
                mutableState.value = State.Idle
                val claims = tokenManager.getCurrentClaims()
                if (claims?.organization == null) {
                    mutableEffect.emit(Effect.NavigateToCompanySelect)
                } else {
                    mutableEffect.emit(Effect.NavigateToHome)
                }
            },
            onFailure = { error ->
                logger.e(error) { "Login failed" }
                val dokusException = when {
                    error.message?.contains("email") == true -> DokusException.Validation.InvalidEmail
                    error.message?.contains("password") == true -> DokusException.Validation.WeakPassword
                    error.message?.contains("Invalid credentials") == true -> DokusException.InvalidCredentials()
                    else -> DokusException.ConnectionError()
                }
                mutableState.value = State.Error(dokusException)
            }
        )
    }

    sealed interface State {
        data object Idle : State

        data object Loading : State

        data class Error(val exception: DokusException) : State
    }

    sealed interface Effect {
        data object NavigateToHome : Effect
        data object NavigateToCompanySelect : Effect
    }
}
