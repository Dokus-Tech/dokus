package ai.dokus.app.auth.model

import ai.dokus.foundation.domain.Email
import ai.dokus.foundation.domain.Name
import ai.dokus.foundation.domain.Password
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue

@Stable
internal data class RegisterFormFields(
    val firstName: Name = Name(""),
    val lastName: Name = Name(""),
    val email: Email = Email(""),
    val password: Password = Password(""),
) {
    val initials: String = "${firstName.value.firstOrNull() ?: ""}${lastName.value.firstOrNull() ?: ""}"

    val namesAreValid: Boolean
        @Composable get() {
            val firstNameIsValid = firstName.value.isNotBlank() && firstName.value.length >= 2
            val lastNameIsValid = lastName.value.isNotBlank() && lastName.value.length >= 2
            val isValid by derivedStateOf { firstNameIsValid && lastNameIsValid }
            return isValid
        }

    val credentialsAreValid: Boolean
        @Composable get() {
            val emailIsValid = email.value.isNotBlank() && email.value.contains("@")
            val passwordIsValid = password.value.isNotBlank() && password.value.length >= 8
            val isValid by derivedStateOf { emailIsValid && passwordIsValid }
            return isValid
        }
}

internal enum class RegisterPage {
    Profile,
    Credentials;

    companion object {
        fun fromIndex(index: Int): RegisterPage {
            return when (index) {
                0 -> Profile
                1 -> Credentials
                else -> throw IllegalArgumentException("Invalid index $index")
            }
        }
    }
}
