package ai.dokus.app.auth.viewmodel

import ai.dokus.app.auth.usecases.LoginUseCase
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.app.state.emit
import tech.dokus.foundation.app.state.emitLoading
import tech.dokus.foundation.app.viewmodel.BaseViewModel
import ai.dokus.foundation.domain.Email
import ai.dokus.foundation.domain.Password
import ai.dokus.foundation.domain.asbtractions.TokenManager
import ai.dokus.foundation.platform.Logger
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

internal class LoginViewModel : BaseViewModel<DokusState<Unit>>(DokusState.idle()), KoinComponent {

    private val logger = Logger.forClass<LoginViewModel>()
    private val loginUseCase: LoginUseCase by inject()
    private val tokenManager: TokenManager by inject()

    private val mutableEffect = MutableSharedFlow<Effect>()
    val effect = mutableEffect.asSharedFlow()

    fun login(emailValue: Email, passwordValue: Password) {
        scope.launch {
            logger.d { "Login attempt started for email: ${emailValue.value.take(3)}***" }
            mutableState.emitLoading()

            val result = loginUseCase(emailValue, passwordValue)

            result.fold(
                onSuccess = {
                    logger.i { "Login successful, navigating to home" }
                    mutableState.emit(Unit)
                    val claims = tokenManager.getCurrentClaims()
                    if (claims?.tenant == null) {
                        mutableEffect.emit(Effect.NavigateToWorkspaceSelect)
                    } else {
                        mutableEffect.emit(Effect.NavigateToHome)
                    }
                },
                onFailure = { error ->
                    logger.e(error) { "Login failed" }
                    mutableState.emit(error) { login(emailValue, passwordValue) }
                }
            )
        }
    }

    sealed interface Effect {
        data object NavigateToHome : Effect
        data object NavigateToWorkspaceSelect : Effect
    }
}
