package ai.dokus.foundation.domain.usecases.validators

class ValidateWorkspaceNameUseCase : Validator<String> {
    private companion object {
        private const val MIN_LENGTH = 4
    }

    override fun invoke(value: String): Boolean {
        return value.length >= MIN_LENGTH
    }
}