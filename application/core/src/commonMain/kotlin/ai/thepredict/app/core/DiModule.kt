package ai.thepredict.app.core

import BackgroundAnimationViewModel
import ai.thepredict.domain.usecases.CreateNewUserUseCase
import ai.thepredict.domain.usecases.CreateNewWorkspaceUseCase
import ai.thepredict.domain.usecases.validators.ValidateEmailUseCase
import ai.thepredict.domain.usecases.validators.ValidateNameUseCase
import ai.thepredict.domain.usecases.validators.ValidatePasswordUseCase
import ai.thepredict.domain.usecases.validators.ValidateWorkspaceNameUseCase
import ai.thepredict.domain.usecases.validators.ValidateWorkspaceTaxNumberUseCase
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.bindProvider
import org.kodein.di.instance
import org.kodein.di.singleton

val coreDiModule by DI.Module("core") {
    bind<BackgroundAnimationViewModel>() with singleton { BackgroundAnimationViewModel() }

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
            emailValidator = instance<ValidateEmailUseCase>(),
            passwordValidator = instance<ValidatePasswordUseCase>(),
            nameValidator = instance<ValidateNameUseCase>()
        )
    }

    bindProvider<ValidateWorkspaceNameUseCase> {
        ValidateWorkspaceNameUseCase()
    }

    bindProvider<ValidateWorkspaceTaxNumberUseCase> {
        ValidateWorkspaceTaxNumberUseCase()
    }

    bindProvider<CreateNewWorkspaceUseCase> {
        CreateNewWorkspaceUseCase(
            validateWorkspaceNameUseCase = instance<ValidateWorkspaceNameUseCase>(),
            validateWorkspaceTaxNumberUseCase = instance<ValidateWorkspaceTaxNumberUseCase>()
        )
    }
}