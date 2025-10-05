package ai.thepredict.domain.usecases.validators

class ValidateNotShortUseCase : Validator<String> {
    private companion object Companion {
        private const val MIN_LENGTH = 4
    }

    override fun invoke(value: String): Boolean {
        return value.length >= MIN_LENGTH
    }
}