package ai.dokus.app.core

import ai.dokus.foundation.domain.model.Country
import ai.dokus.foundation.domain.usecases.CreateNewUserUseCase
import ai.dokus.foundation.domain.usecases.validators.ValidateAddressUseCase
import ai.dokus.foundation.domain.usecases.validators.ValidateNewWorkspaceUseCase
import ai.dokus.foundation.domain.usecases.validators.ValidateNotShortUseCase
import ai.dokus.foundation.domain.usecases.validators.ValidatePostalCode
import ai.dokus.foundation.domain.usecases.validators.ValidateWorkspaceNameUseCase
import ai.dokus.foundation.domain.usecases.validators.ValidateWorkspaceTaxNumberUseCase
import org.koin.dsl.module

val coreDiModule = module {
    factory<CreateNewUserUseCase> {
        CreateNewUserUseCase()
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