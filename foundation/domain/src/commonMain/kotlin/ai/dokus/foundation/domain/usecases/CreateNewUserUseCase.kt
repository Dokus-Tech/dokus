package ai.dokus.foundation.domain.usecases

import ai.dokus.foundation.domain.exceptions.PredictException
import ai.dokus.foundation.domain.usecases.validators.ValidateEmailUseCase
import ai.dokus.foundation.domain.usecases.validators.ValidateNameUseCase
import ai.dokus.foundation.domain.usecases.validators.ValidatePasswordUseCase
import ai.dokus.foundation.domain.usecases.validators.Validator

class CreateNewUserUseCase(
    private val emailValidator: Validator<String> = ValidateEmailUseCase(),
    private val passwordValidator: Validator<String> = ValidatePasswordUseCase(),
    private val nameValidator: Validator<String> = ValidateNameUseCase(),
) {

    operator fun invoke(
        firstName: String,
        lastName: String,
        email: String,
        password: String
    ): Result<Any> {
        if (!nameValidator(firstName)) return Result.failure(PredictException.InvalidFirstName)
        if (!nameValidator(lastName)) return Result.failure(PredictException.InvalidLastName)
        if (!emailValidator(email)) return Result.failure(PredictException.InvalidEmail)
        if (!passwordValidator(password)) return Result.failure(PredictException.WeakPassword)
        return Result.success(Any())
    }
}