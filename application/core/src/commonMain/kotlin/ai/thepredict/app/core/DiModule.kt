package ai.thepredict.app.core

import ai.thepredict.domain.model.Country
import ai.thepredict.domain.usecases.CreateNewUserUseCase
import ai.thepredict.domain.usecases.validators.ValidateAddressUseCase
import ai.thepredict.domain.usecases.validators.ValidateEmailUseCase
import ai.thepredict.domain.usecases.validators.ValidateNameUseCase
import ai.thepredict.domain.usecases.validators.ValidateNewWorkspaceUseCase
import ai.thepredict.domain.usecases.validators.ValidateNotShortUseCase
import ai.thepredict.domain.usecases.validators.ValidatePasswordUseCase
import ai.thepredict.domain.usecases.validators.ValidatePostalCode
import ai.thepredict.domain.usecases.validators.ValidateWorkspaceNameUseCase
import ai.thepredict.domain.usecases.validators.ValidateWorkspaceTaxNumberUseCase
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

    bindProvider<ValidateNotShortUseCase> {
        ValidateNotShortUseCase()
    }

    bindProvider<ValidatePostalCode> {
        ValidatePostalCode(Country.BE)
    }

    bindProvider<ValidateAddressUseCase> {
        ValidateAddressUseCase(
            country = Country.BE,
            streetNameValidator = instance<ValidateNotShortUseCase>(),
            cityValidator = instance<ValidateNotShortUseCase>(),
            postalCodeValidator = instance<ValidatePostalCode>(),
            countryValidator = instance<ValidateNotShortUseCase>()
        )
    }

    bindProvider<ValidateNewWorkspaceUseCase> {
        ValidateNewWorkspaceUseCase(
            nameValidator = instance<ValidateWorkspaceNameUseCase>(),
            taxNumberValidator = instance<ValidateWorkspaceTaxNumberUseCase>(),
            addressValidator = instance<ValidateAddressUseCase>()
        )
    }
}