package ai.dokus.foundation.domain.usecases

import ai.dokus.foundation.domain.Email
import ai.dokus.foundation.domain.Name
import ai.dokus.foundation.domain.Password
import ai.dokus.foundation.domain.exceptions.DokusException
import ai.dokus.foundation.domain.usecases.validators.ValidateEmailUseCase
import ai.dokus.foundation.domain.usecases.validators.ValidateNameUseCase
import ai.dokus.foundation.domain.usecases.validators.ValidatePasswordUseCase
import ai.dokus.foundation.domain.usecases.validators.Validator

class CreateNewUserUseCase(
    private val emailValidator: Validator<Email> = ValidateEmailUseCase,
    private val passwordValidator: Validator<Password> = ValidatePasswordUseCase,
    private val nameValidator: Validator<Name> = ValidateNameUseCase,
) {

    operator fun invoke(
        firstName: Name,
        lastName: Name,
        email: Email,
        password: Password
    ): Result<Any> {
        if (!nameValidator(firstName)) return Result.failure(DokusException.Validation.InvalidFirstName())
        if (!nameValidator(lastName)) return Result.failure(DokusException.Validation.InvalidLastName())
        if (!emailValidator(email)) return Result.failure(DokusException.Validation.InvalidEmail())
        if (!passwordValidator(password)) return Result.failure(DokusException.Validation.WeakPassword())
        return Result.success(Any())
    }
}
