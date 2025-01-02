package ai.thepredict.domain.usecases.validators

class ValidateNameUseCase : Validator<String> {
    private companion object {
        const val MIN_LENGTH = 3
    }

    override operator fun invoke(value: String): Boolean {
        return value.length >= MIN_LENGTH
    }
}