package ai.dokus.foundation.navigation.destinations

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

sealed interface AuthDestination : NavigationDestination {

    @Serializable
    @SerialName("login")
    data object Login : AuthDestination

    @Serializable
    @SerialName("register")
    data object Register : AuthDestination

    @Serializable
    @SerialName("forgot-password")
    data object ForgotPassword : AuthDestination

    @Serializable
    @SerialName("password-change-requested")
    data object PasswordChangeRequested : AuthDestination

    @Serializable
    @SerialName("profile_settings")
    data object ProfileSettings : AuthDestination

    @Serializable
    @SerialName("profile")
    data class Profile(val userId: String) : AuthDestination

    @Serializable
    @SerialName("select_profile")
    data object SelectProfile : AuthDestination

    @Serializable
    @SerialName("pending_confirm_account")
    data object PendingConfirmAccount : AuthDestination

    @Serializable
    @SerialName("pending_confirm_account/instructions")
    data object PendingConfirmAccountInstructions : AuthDestination

    @Serializable
    @SerialName("sessions")
    data object MySessions : AuthDestination

    @Serializable
    @SerialName("auth/qr/display")
    data object QrLoginDisplay : AuthDestination

    @Serializable
    @SerialName("auth/qr/display/instructions")
    data object QrLoginDisplayInstructions : AuthDestination

    @Serializable
    @SerialName("auth/qr/decision")
    data class QrLoginDecision(val sessionId: String, val token: String) : AuthDestination
}
