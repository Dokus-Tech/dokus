package ai.dokus.app.auth.viewmodel

import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.app.state.emit
import tech.dokus.foundation.app.state.emitLoading
import tech.dokus.foundation.app.viewmodel.BaseViewModel
import ai.dokus.foundation.domain.Email
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent

internal class ForgotPasswordViewModel :
    BaseViewModel<DokusState<Unit>>(DokusState.idle()), KoinComponent {

    fun submit(emailValue: Email) {
        scope.launch {
            mutableState.emitLoading()

            runCatching { emailValue.validOrThrows }.getOrElse {
                mutableState.emit(it) { submit(emailValue) }
                return@launch
            }
//
//        persistence.user = api.authenticate(emailValue, passwordValue).getOrElse {
//            mutableState.value = State.Error(it.asPredictException)
//            return@launch
//        }

            mutableState.emit(Unit)
        }
    }
}
