package ai.thepredict.app.core

import ai.thepredict.domain.usecases.CreateNewUserUseCase
import ai.thepredict.domain.usecases.validators.ValidateEmailUseCase
import ai.thepredict.domain.usecases.validators.ValidateNameUseCase
import ai.thepredict.domain.usecases.validators.ValidatePasswordUseCase
import org.kodein.di.DI
import org.kodein.di.bindProvider
import org.kodein.di.instance

val coreDiModule by DI.Module("core") {
    bindProvider<ValidateEmailUseCase> {
        ValidateEmailUseCase()
    }

    bindProvider<ValidatePasswordUseCase> {
        ValidatePasswordUseCase()
    }

    bindProvider<ValidateNameUseCase> {
        ValidateNameUseCase()
    }

    bindProvider<CreateNewUserUseCase> {
        CreateNewUserUseCase(
            emailValidator = instance(),
            passwordValidator = instance(),
            nameValidator = instance()
        )
    }
}