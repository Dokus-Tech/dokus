package ai.thepredict.domain.usecases

import ai.thepredict.data.NewUser
import ai.thepredict.domain.exceptions.PredictException
import ai.thepredict.domain.usecases.validators.Validator
import ai.thepredict.domain.usecases.validators.ValidateEmailUseCase
import ai.thepredict.domain.usecases.validators.ValidateNameUseCase
import ai.thepredict.domain.usecases.validators.ValidatePasswordUseCase

class CreateNewUserUseCase(
    private val emailValidator: Validator<String> = ValidateEmailUseCase(),
    private val passwordValidator: Validator<String> = ValidatePasswordUseCase(),
    private val nameValidator: Validator<String> = ValidateNameUseCase(),
) {

    operator fun invoke(name: String, email: String, password: String): Result<NewUser> {
        if (!emailValidator(email)) return Result.failure(PredictException.InvalidEmail)
        if (!passwordValidator(password)) return Result.failure(PredictException.WeakPassword)
        if (!nameValidator(name)) return Result.failure(PredictException.InvalidName)
        return Result.success(NewUser(name, email, password))
    }
}