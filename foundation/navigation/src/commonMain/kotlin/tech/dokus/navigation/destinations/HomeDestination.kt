package tech.dokus.navigation.destinations

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

sealed interface HomeDestination : NavigationDestination {

    @Serializable
    @SerialName("today")
    data object Today : HomeDestination

    @Serializable
    @SerialName("tomorrow")
    data object Tomorrow : HomeDestination

    @Serializable
    @SerialName("documents")
    data object Documents : HomeDestination

    @Serializable
    @SerialName("cashflow")
    data object Cashflow : HomeDestination

    @Serializable
    @SerialName("contacts")
    data object Contacts : HomeDestination

    @Serializable
    @SerialName("team")
    data object Team : HomeDestination

    @Serializable
    @SerialName("home/workspace_details")
    data object WorkspaceDetails : HomeDestination

    @Serializable
    @SerialName("accountant")
    data object Accountant : HomeDestination

    @Serializable
    @SerialName("ai-chat")
    data object AiChat : HomeDestination

    @Serializable
    @SerialName("settings")
    data object Settings : HomeDestination

    @Serializable
    @SerialName("more")
    data object More : HomeDestination

    @Serializable
    @SerialName("home/under_development")
    data object UnderDevelopment : HomeDestination

}

/** Route string matching the @SerialName value for backstack matching. */
val NavigationDestination.route: String get() = when (this) {
    is HomeDestination -> when (this) {
        HomeDestination.Today -> "today"
        HomeDestination.Tomorrow -> "tomorrow"
        HomeDestination.Documents -> "documents"
        HomeDestination.Cashflow -> "cashflow"
        HomeDestination.Contacts -> "contacts"
        HomeDestination.Team -> "team"
        HomeDestination.WorkspaceDetails -> "home/workspace_details"
        HomeDestination.Accountant -> "accountant"
        HomeDestination.AiChat -> "ai-chat"
        HomeDestination.Settings -> "settings"
        HomeDestination.More -> "more"
        HomeDestination.UnderDevelopment -> "home/under_development"
    }
    is SettingsDestination -> when (this) {
        SettingsDestination.WorkspaceSettings -> "settings/workspace"
        SettingsDestination.TeamSettings -> "settings/workspace/team"
        SettingsDestination.AppearanceSettings -> "settings/appearance"
        SettingsDestination.NotificationPreferences -> "settings/notifications"
        SettingsDestination.PeppolRegistration -> "settings/peppol"
    }
    is AuthDestination -> when (this) {
        AuthDestination.Login -> "login"
        AuthDestination.Register -> "register"
        AuthDestination.ForgotPassword -> "forgot-password"
        AuthDestination.PasswordChangeRequested -> "password-change-requested"
        is AuthDestination.ResetPassword -> "reset-password"
        is AuthDestination.VerifyEmail -> "verify-email"
        AuthDestination.ChangePassword -> "change-password"
        AuthDestination.WorkspaceSelect -> "workspace/select"
        AuthDestination.WorkspaceCreate -> "workspace/create"
        AuthDestination.ProfileSettings -> "profile_settings"
        is AuthDestination.Profile -> "profile"
        AuthDestination.SelectProfile -> "select_profile"
        AuthDestination.PendingConfirmAccount -> "pending_confirm_account"
        AuthDestination.PendingConfirmAccountInstructions -> "pending_confirm_account/instructions"
        AuthDestination.MySessions -> "sessions"
        AuthDestination.QrLoginDisplay -> "auth/qr/display"
        AuthDestination.QrLoginDisplayInstructions -> "auth/qr/display/instructions"
        is AuthDestination.QrLoginDecision -> "auth/qr/decision"
        is AuthDestination.ServerConnection -> "server/connect"
    }
    is CashFlowDestination -> when (this) {
        CashFlowDestination.AddDocument -> "cashflow/add_document"
        CashFlowDestination.CreateInvoice -> "cashflow/create_invoice"
        is CashFlowDestination.DocumentReview -> "cashflow/document_review"
        is CashFlowDestination.DocumentChat -> "cashflow/document_chat"
        is CashFlowDestination.CashflowLedger -> "cashflow/ledger"
    }
    is ContactsDestination -> when (this) {
        is ContactsDestination.CreateContact -> "contacts/create"
        is ContactsDestination.EditContact -> "contacts/edit"
        is ContactsDestination.ContactDetails -> "contacts/details"
    }
    is CoreDestination -> when (this) {
        CoreDestination.Splash -> "splash"
        CoreDestination.Home -> "home"
        CoreDestination.UpdateRequired -> "update_required"
    }
    is AppDestination -> when (this) {
        AppDestination.Notifications -> "notifications"
        AppDestination.UnderDevelopment -> "app/under_development"
        AppDestination.Empty -> "empty"
        AppDestination.ShareImport -> "app/share_import"
    }
}
