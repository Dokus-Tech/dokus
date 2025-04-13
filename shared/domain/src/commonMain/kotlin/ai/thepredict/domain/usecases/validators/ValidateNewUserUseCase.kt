package ai.thepredict.domain.usecases.validators

import ai.thepredict.domain.exceptions.PredictException
import ai.thepredict.domain.model.NewUser

class ValidateNewUserUseCase(
    private val emailValidator: Validator<String> = ValidateEmailUseCase(),
    private val passwordValidator: Validator<String> = ValidatePasswordUseCase(),
    private val nameValidator: Validator<String> = ValidateNameUseCase(),
) : ValidatorThrowable<NewUser> {

    override operator fun invoke(value: NewUser) {
        if (!emailValidator(value.email)) throw PredictException.InvalidEmail
        if (!passwordValidator(value.password)) throw PredictException.WeakPassword
        if (!nameValidator(value.name)) throw PredictException.InvalidName
    }
}