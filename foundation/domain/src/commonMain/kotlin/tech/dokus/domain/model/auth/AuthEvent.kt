package tech.dokus.domain.model.auth

/**
 * Authentication events that can occur in the application.
 */
sealed interface AuthEvent {
    /** User was forcibly logged out due to authentication failure */
    data object ForceLogout : AuthEvent

    /** User voluntarily logged out */
    data object UserLogout : AuthEvent

    /** User successfully logged in */
    data object LoginSuccess : AuthEvent
}
