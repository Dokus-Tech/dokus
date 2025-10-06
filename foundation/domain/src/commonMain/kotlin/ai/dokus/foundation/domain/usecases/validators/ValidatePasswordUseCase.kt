package ai.dokus.foundation.domain.usecases.validators

class ValidatePasswordUseCase : Validator<String> {
    private companion object {
        const val MIN_LENGTH = 8
    }

    override operator fun invoke(value: String): Boolean {
        return value.length >= MIN_LENGTH
    }
}