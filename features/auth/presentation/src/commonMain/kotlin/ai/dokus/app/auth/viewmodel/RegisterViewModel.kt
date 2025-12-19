package ai.dokus.app.auth.viewmodel

import ai.dokus.app.auth.usecases.RegisterAndLoginUseCase
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.app.state.emit
import tech.dokus.foundation.app.state.emitIdle
import tech.dokus.foundation.app.state.emitLoading
import tech.dokus.foundation.app.viewmodel.BaseViewModel
import ai.dokus.foundation.domain.Email
import ai.dokus.foundation.domain.Name
import ai.dokus.foundation.domain.Password
import ai.dokus.foundation.domain.asbtractions.TokenManager
import ai.dokus.foundation.platform.Logger
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

internal class RegisterViewModel : BaseViewModel<DokusState<Unit>>(DokusState.idle()), KoinComponent {

    private val logger = Logger.forClass<RegisterViewModel>()
    private val registerAndLoginUseCase: RegisterAndLoginUseCase by inject()
    private val tokenManager: TokenManager by inject()
    private val mutableEffect = MutableSharedFlow<Effect>()
    val effect = mutableEffect.asSharedFlow()

    fun register(
        email: Email,
        password: Password,
        firstName: Name,
        lastName: Name
    ) {
        scope.launch {
            logger.d { "Registration attempt started for email: ${email.value.take(3)}***" }
            mutableState.emitLoading()

            val result = registerAndLoginUseCase(email, password, firstName, lastName)

            result.fold(
                onSuccess = {
                    logger.i { "Registration successful, navigating to home" }
                    mutableState.emitIdle()
                    val claims = tokenManager.getCurrentClaims()
                    if (claims?.tenant == null) {
                        mutableEffect.emit(Effect.NavigateToWorkspaceSelect)
                    } else {
                        mutableEffect.emit(Effect.NavigateToHome)
                    }
                },
                onFailure = { error ->
                    logger.e(error) { "Registration failed" }
                    mutableState.emit(error) { register(email, password, firstName, lastName) }
                }
            )
        }
    }

    sealed interface Effect {
        data object NavigateToHome : Effect
        data object NavigateToWorkspaceSelect : Effect
    }
}
