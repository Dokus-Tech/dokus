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
import org.koin.dsl.module

val coreDiModule = module {
    factory<ValidateEmailUseCase> {
        ValidateEmailUseCase()
    }

    factory<ValidatePasswordUseCase> {
        ValidatePasswordUseCase()
    }

    factory<ValidateNameUseCase> {
        ValidateNameUseCase()
    }

    factory<CreateNewUserUseCase> {
        CreateNewUserUseCase(
            emailValidator = get(),
            passwordValidator = get(),
            nameValidator = get()
        )
    }

    factory<ValidateWorkspaceNameUseCase> {
        ValidateWorkspaceNameUseCase()
    }

    factory<ValidateWorkspaceTaxNumberUseCase> {
        ValidateWorkspaceTaxNumberUseCase()
    }

    factory<ValidateNotShortUseCase> {
        ValidateNotShortUseCase()
    }

    factory<ValidatePostalCode> {
        ValidatePostalCode(Country.BE)
    }

    factory<ValidateAddressUseCase> {
        ValidateAddressUseCase(
            country = Country.BE,
            streetNameValidator = get(),
            cityValidator = get(),
            postalCodeValidator = get(),
            countryValidator = get()
        )
    }

    factory<ValidateNewWorkspaceUseCase> {
        ValidateNewWorkspaceUseCase(
            nameValidator = get(),
            taxNumberValidator = get(),
            addressValidator = get()
        )
    }
}