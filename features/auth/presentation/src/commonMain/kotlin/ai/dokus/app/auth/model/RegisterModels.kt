package ai.dokus.app.auth.model

import tech.dokus.foundation.app.extensions.rememberIsValid
import tech.dokus.domain.Email
import tech.dokus.domain.Name
import tech.dokus.domain.Password
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue

/**
 * Holds form field values for the user registration flow.
 *
 * This data class represents the state of the multi-step registration form,
 * containing validated value objects for each field. It provides computed
 * properties for validation status and UI display (initials) that integrate
 * with Compose's reactive state system.
 *
 * The form fields are split across two pages ([RegisterPage]):
 * - [RegisterPage.Profile]: First and last name
 * - [RegisterPage.Credentials]: Email and password
 *
 * @property firstName The user's first name wrapped in a [Name] value object.
 *                     Defaults to empty. Must pass [Name.isValid] validation.
 * @property lastName The user's last name wrapped in a [Name] value object.
 *                    Defaults to empty. Must pass [Name.isValid] validation.
 * @property email The user's email address wrapped in an [Email] value object.
 *                 Defaults to empty. Must pass [Email.isValid] validation.
 * @property password The user's password wrapped in a [Password] value object.
 *                    Defaults to empty. Must pass [Password.isValid] validation.
 *
 * @see RegisterPage for the multi-step registration page structure
 */
@Stable
internal data class RegisterFormFields(
    val firstName: Name = Name(""),
    val lastName: Name = Name(""),
    val email: Email = Email(""),
    val password: Password = Password(""),
) {
    /**
     * The user's initials derived from the first characters of first and last name.
     *
     * Used for avatar display during registration. Returns empty string if both
     * names are empty, or partial initials if only one name is provided.
     */
    val initials: String = "${firstName.value.firstOrNull() ?: ""}${lastName.value.firstOrNull() ?: ""}"

    /**
     * Whether both first and last name pass validation.
     *
     * This is a Compose-aware property that uses [derivedStateOf] for efficient
     * recomposition. Should be used to enable/disable the "Next" button on the
     * [RegisterPage.Profile] step.
     *
     * @return `true` if both [firstName] and [lastName] are valid, `false` otherwise.
     */
    val namesAreValid: Boolean
        @Composable get() {
            val firstNameIsValid = firstName.rememberIsValid()
            val lastNameIsValid = lastName.rememberIsValid()
            val isValid by derivedStateOf { firstNameIsValid && lastNameIsValid }
            return isValid
        }

    /**
     * Whether both email and password pass validation.
     *
     * This is a Compose-aware property that uses [derivedStateOf] for efficient
     * recomposition. Should be used to enable/disable the "Register" button on the
     * [RegisterPage.Credentials] step.
     *
     * @return `true` if both [email] and [password] are valid, `false` otherwise.
     */
    val credentialsAreValid: Boolean
        @Composable get() {
            val emailIsValid = email.rememberIsValid()
            val passwordIsValid = password.rememberIsValid()
            val isValid by derivedStateOf { emailIsValid && passwordIsValid }
            return isValid
        }
}

/**
 * Represents the pages in the multi-step registration flow.
 *
 * The registration process is split into two sequential pages to improve
 * user experience by grouping related fields together:
 *
 * 1. [Profile] (index 0): Collects first and last name
 * 2. [Credentials] (index 1): Collects email and password
 *
 * This enum is used by the registration UI to manage navigation between
 * steps and determine which fields to display and validate.
 *
 * @see RegisterFormFields for the form data associated with each page
 */
internal enum class RegisterPage {
    /**
     * First registration page collecting the user's profile information.
     *
     * This page contains fields for [RegisterFormFields.firstName] and
     * [RegisterFormFields.lastName]. Users must complete this page before
     * proceeding to the [Credentials] page.
     */
    Profile,

    /**
     * Second registration page collecting the user's authentication credentials.
     *
     * This page contains fields for [RegisterFormFields.email] and
     * [RegisterFormFields.password]. Completing this page submits the
     * registration request.
     */
    Credentials;

    companion object {
        /**
         * Converts a page index to the corresponding [RegisterPage].
         *
         * Used for integrating with ViewPager or similar index-based navigation
         * components that report page changes as integer indices.
         *
         * @param index The zero-based page index (0 for [Profile], 1 for [Credentials]).
         * @return The [RegisterPage] corresponding to the given index.
         * @throws IllegalArgumentException if the index is not 0 or 1.
         */
        fun fromIndex(index: Int): RegisterPage {
            return when (index) {
                0 -> Profile
                1 -> Credentials
                else -> throw IllegalArgumentException("Invalid index $index")
            }
        }
    }
}
