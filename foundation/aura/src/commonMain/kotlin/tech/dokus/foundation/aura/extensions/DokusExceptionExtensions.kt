package tech.dokus.foundation.aura.extensions

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.auth_company_lookup_failed
import tech.dokus.aura.resources.auth_host_no_spaces
import tech.dokus.aura.resources.auth_host_required
import tech.dokus.aura.resources.auth_port_invalid_number
import tech.dokus.aura.resources.auth_port_out_of_range
import tech.dokus.aura.resources.auth_port_required
import tech.dokus.aura.resources.cashflow_confirm_missing_fields
import tech.dokus.aura.resources.cashflow_contact_bind_failed
import tech.dokus.aura.resources.cashflow_contact_clear_failed
import tech.dokus.aura.resources.cashflow_contact_save_failed
import tech.dokus.aura.resources.cashflow_preview_load_failed
import tech.dokus.aura.resources.chat_error_invalid_document_reference
import tech.dokus.aura.resources.chat_error_load_conversation
import tech.dokus.aura.resources.chat_error_no_document_selected
import tech.dokus.aura.resources.chat_error_send_message
import tech.dokus.aura.resources.common_unknown
import tech.dokus.aura.resources.contacts_company_name_required
import tech.dokus.aura.resources.contacts_create_failed
import tech.dokus.aura.resources.contacts_delete_failed
import tech.dokus.aura.resources.contacts_email_or_phone_required
import tech.dokus.aura.resources.contacts_full_name_required
import tech.dokus.aura.resources.contacts_lookup_search_failed
import tech.dokus.aura.resources.contacts_merge_failed
import tech.dokus.aura.resources.contacts_name_required
import tech.dokus.aura.resources.contacts_note_add_failed
import tech.dokus.aura.resources.contacts_note_delete_failed
import tech.dokus.aura.resources.contacts_note_empty_error
import tech.dokus.aura.resources.contacts_note_update_failed
import tech.dokus.aura.resources.contacts_peppol_id_required
import tech.dokus.aura.resources.contacts_peppol_update_failed
import tech.dokus.aura.resources.exception_account_inactive
import tech.dokus.aura.resources.exception_account_locked
import tech.dokus.aura.resources.exception_api_key_required
import tech.dokus.aura.resources.exception_api_secret_required
import tech.dokus.aura.resources.exception_connection_error
import tech.dokus.aura.resources.exception_email_already_verified
import tech.dokus.aura.resources.exception_email_not_verified
import tech.dokus.aura.resources.exception_email_verification_token_expired
import tech.dokus.aura.resources.exception_email_verification_token_invalid
import tech.dokus.aura.resources.exception_invalid_api_credentials
import tech.dokus.aura.resources.exception_invalid_bic
import tech.dokus.aura.resources.exception_invalid_city
import tech.dokus.aura.resources.exception_invalid_country
import tech.dokus.aura.resources.exception_invalid_credentials
import tech.dokus.aura.resources.exception_invalid_email
import tech.dokus.aura.resources.exception_invalid_first_name
import tech.dokus.aura.resources.exception_invalid_iban
import tech.dokus.aura.resources.exception_invalid_invoice_number
import tech.dokus.aura.resources.exception_invalid_last_name
import tech.dokus.aura.resources.exception_invalid_money
import tech.dokus.aura.resources.exception_invalid_peppol_id
import tech.dokus.aura.resources.exception_invalid_percentage
import tech.dokus.aura.resources.exception_invalid_postal_code
import tech.dokus.aura.resources.exception_invalid_quantity
import tech.dokus.aura.resources.exception_invalid_street_name
import tech.dokus.aura.resources.exception_invalid_tax_number
import tech.dokus.aura.resources.exception_invalid_vat_number
import tech.dokus.aura.resources.exception_invalid_vat_rate
import tech.dokus.aura.resources.exception_invalid_workspace_name
import tech.dokus.aura.resources.exception_invoice_client_required
import tech.dokus.aura.resources.exception_invoice_due_date_before_issue
import tech.dokus.aura.resources.exception_invoice_items_required
import tech.dokus.aura.resources.exception_missing_company_address
import tech.dokus.aura.resources.exception_missing_vat_number
import tech.dokus.aura.resources.exception_not_authenticated
import tech.dokus.aura.resources.exception_not_authorized
import tech.dokus.aura.resources.exception_not_found
import tech.dokus.aura.resources.exception_not_implemented
import tech.dokus.aura.resources.exception_password_do_not_match
import tech.dokus.aura.resources.exception_password_reset_token_expired
import tech.dokus.aura.resources.exception_password_reset_token_invalid
import tech.dokus.aura.resources.exception_refresh_token_expired
import tech.dokus.aura.resources.exception_refresh_token_revoked
import tech.dokus.aura.resources.exception_session_expired
import tech.dokus.aura.resources.exception_session_invalid
import tech.dokus.aura.resources.exception_tenant_creation_failed
import tech.dokus.aura.resources.exception_token_expired
import tech.dokus.aura.resources.exception_token_invalid
import tech.dokus.aura.resources.exception_too_many_login_attempts
import tech.dokus.aura.resources.exception_too_many_sessions
import tech.dokus.aura.resources.exception_unknown
import tech.dokus.aura.resources.exception_user_already_exists
import tech.dokus.aura.resources.exception_user_not_found
import tech.dokus.aura.resources.exception_validation_error
import tech.dokus.aura.resources.exception_weak_password
import tech.dokus.aura.resources.profile_save_error
import tech.dokus.aura.resources.settings_save_failed
import tech.dokus.aura.resources.team_email_required
import tech.dokus.aura.resources.team_invite_cancel_failed
import tech.dokus.aura.resources.team_invite_failed
import tech.dokus.aura.resources.team_member_removed_failed
import tech.dokus.aura.resources.team_ownership_transferred_failed
import tech.dokus.aura.resources.team_role_update_failed
import tech.dokus.aura.resources.upload_failed_message
import tech.dokus.aura.resources.workspace_avatar_delete_failed
import tech.dokus.aura.resources.workspace_avatar_upload_failed
import tech.dokus.aura.resources.workspace_context_unavailable
import tech.dokus.aura.resources.workspace_create_failed
import tech.dokus.aura.resources.workspace_select_failed
import tech.dokus.domain.exceptions.DokusException

/**
 * Extension property to get a localized error message for a DokusException.
 *
 * This property uses Compose Multiplatform's string resources to provide
 * translations for all exception types. The appropriate translation is selected
 * based on the user's locale settings.
 *
 * Supported locales:
 * - English (default)
 * - French (France and Belgium)
 * - Dutch (Netherlands and Belgium)
 * - German
 *
 * Usage:
 * ```kotlin
 * @Composable
 * fun ErrorMessage(exception: DokusException) {
 *     Text(text = exception.localized)
 * }
 * ```
 */
val DokusException.localized: String
    @Composable get() = when (this) {
        // 400 Validation Errors
        is DokusException.Validation.InvalidEmail -> stringResource(Res.string.exception_invalid_email)
        is DokusException.Validation.EmailRequired -> stringResource(Res.string.team_email_required)
        is DokusException.Validation.WeakPassword -> stringResource(Res.string.exception_weak_password)
        is DokusException.Validation.PasswordDoNotMatch -> stringResource(Res.string.exception_password_do_not_match)
        is DokusException.Validation.InvalidFirstName -> stringResource(Res.string.exception_invalid_first_name)
        is DokusException.Validation.InvalidLastName -> stringResource(Res.string.exception_invalid_last_name)
        is DokusException.Validation.InvalidTaxNumber -> stringResource(Res.string.exception_invalid_tax_number)
        is DokusException.Validation.InvalidWorkspaceName -> stringResource(Res.string.exception_invalid_workspace_name)
        is DokusException.Validation.ServerHostRequired -> stringResource(Res.string.auth_host_required)
        is DokusException.Validation.ServerHostNoSpaces -> stringResource(Res.string.auth_host_no_spaces)
        is DokusException.Validation.ServerPortRequired -> stringResource(Res.string.auth_port_required)
        is DokusException.Validation.ServerPortInvalidNumber -> stringResource(Res.string.auth_port_invalid_number)
        is DokusException.Validation.ServerPortOutOfRange -> stringResource(Res.string.auth_port_out_of_range)
        is DokusException.Validation.ContactNameRequired -> stringResource(Res.string.contacts_name_required)
        is DokusException.Validation.CompanyNameRequired -> stringResource(Res.string.contacts_company_name_required)
        is DokusException.Validation.FullNameRequired -> stringResource(Res.string.contacts_full_name_required)
        is DokusException.Validation.ContactEmailOrPhoneRequired -> stringResource(
            Res.string.contacts_email_or_phone_required
        )
        is DokusException.Validation.InvalidVatNumber -> stringResource(Res.string.exception_invalid_vat_number)
        is DokusException.Validation.InvalidIban -> stringResource(Res.string.exception_invalid_iban)
        is DokusException.Validation.InvalidBic -> stringResource(Res.string.exception_invalid_bic)
        is DokusException.Validation.InvalidPeppolId -> stringResource(Res.string.exception_invalid_peppol_id)
        is DokusException.Validation.PeppolIdRequired -> stringResource(Res.string.contacts_peppol_id_required)
        is DokusException.Validation.InvalidInvoiceNumber -> stringResource(Res.string.exception_invalid_invoice_number)
        is DokusException.Validation.InvalidMoney -> stringResource(Res.string.exception_invalid_money)
        is DokusException.Validation.InvalidVatRate -> stringResource(Res.string.exception_invalid_vat_rate)
        is DokusException.Validation.InvalidPercentage -> stringResource(Res.string.exception_invalid_percentage)
        is DokusException.Validation.InvalidQuantity -> stringResource(Res.string.exception_invalid_quantity)
        is DokusException.Validation.InvoiceClientRequired -> stringResource(
            Res.string.exception_invoice_client_required
        )
        is DokusException.Validation.InvoiceItemsRequired -> stringResource(Res.string.exception_invoice_items_required)
        is DokusException.Validation.InvoiceDueDateBeforeIssue -> stringResource(
            Res.string.exception_invoice_due_date_before_issue
        )
        is DokusException.Validation.DocumentMissingFields -> stringResource(Res.string.cashflow_confirm_missing_fields)
        is DokusException.Validation.NoteContentRequired -> stringResource(Res.string.contacts_note_empty_error)
        is DokusException.Validation.InvalidStreetName -> stringResource(Res.string.exception_invalid_street_name)
        is DokusException.Validation.InvalidCity -> stringResource(Res.string.exception_invalid_city)
        is DokusException.Validation.InvalidPostalCode -> stringResource(Res.string.exception_invalid_postal_code)
        is DokusException.Validation.InvalidCountry -> stringResource(Res.string.exception_invalid_country)
        is DokusException.Validation.ApiKeyRequired -> stringResource(Res.string.exception_api_key_required)
        is DokusException.Validation.ApiSecretRequired -> stringResource(Res.string.exception_api_secret_required)
        is DokusException.Validation.InvalidApiCredentials -> stringResource(
            Res.string.exception_invalid_api_credentials
        )
        is DokusException.Validation.MissingVatNumber -> stringResource(Res.string.exception_missing_vat_number)
        is DokusException.Validation.MissingCompanyAddress -> stringResource(
            Res.string.exception_missing_company_address
        )
        is DokusException.Validation -> stringResource(Res.string.exception_validation_error)
        is DokusException.BadRequest -> message

        // 401 Authentication Errors
        is DokusException.NotAuthenticated -> stringResource(Res.string.exception_not_authenticated)
        is DokusException.InvalidCredentials -> stringResource(Res.string.exception_invalid_credentials)
        is DokusException.TokenExpired -> stringResource(Res.string.exception_token_expired)
        is DokusException.TokenInvalid -> stringResource(Res.string.exception_token_invalid)
        is DokusException.RefreshTokenExpired -> stringResource(Res.string.exception_refresh_token_expired)
        is DokusException.RefreshTokenRevoked -> stringResource(Res.string.exception_refresh_token_revoked)
        is DokusException.SessionExpired -> stringResource(Res.string.exception_session_expired)
        is DokusException.SessionInvalid -> stringResource(Res.string.exception_session_invalid)
        is DokusException.PasswordResetTokenExpired -> stringResource(Res.string.exception_password_reset_token_expired)
        is DokusException.PasswordResetTokenInvalid -> stringResource(Res.string.exception_password_reset_token_invalid)
        is DokusException.EmailVerificationTokenExpired -> stringResource(
            Res.string.exception_email_verification_token_expired
        )
        is DokusException.EmailVerificationTokenInvalid -> stringResource(
            Res.string.exception_email_verification_token_invalid
        )

        // 403 Authorization Errors
        is DokusException.NotAuthorized -> stringResource(Res.string.exception_not_authorized)
        is DokusException.AccountInactive -> stringResource(Res.string.exception_account_inactive)
        is DokusException.AccountLocked -> stringResource(Res.string.exception_account_locked)
        is DokusException.EmailNotVerified -> stringResource(Res.string.exception_email_not_verified)
        is DokusException.EmailAlreadyVerified -> stringResource(Res.string.exception_email_already_verified)
        is DokusException.TooManySessions -> stringResource(Res.string.exception_too_many_sessions)

        // 409 Conflict Errors
        is DokusException.UserAlreadyExists -> stringResource(Res.string.exception_user_already_exists)

        // 429 Rate Limiting Errors
        is DokusException.TooManyLoginAttempts -> stringResource(Res.string.exception_too_many_login_attempts)

        // 500 Server Errors
        is DokusException.ContactCreateFailed -> stringResource(Res.string.contacts_create_failed)
        is DokusException.ContactLookupFailed -> stringResource(Res.string.contacts_lookup_search_failed)
        is DokusException.ContactDeleteFailed -> stringResource(Res.string.contacts_delete_failed)
        is DokusException.ContactMergeFailed -> stringResource(Res.string.contacts_merge_failed)
        is DokusException.ContactPeppolUpdateFailed -> stringResource(Res.string.contacts_peppol_update_failed)
        is DokusException.ContactNoteAddFailed -> stringResource(Res.string.contacts_note_add_failed)
        is DokusException.ContactNoteUpdateFailed -> stringResource(Res.string.contacts_note_update_failed)
        is DokusException.ContactNoteDeleteFailed -> stringResource(Res.string.contacts_note_delete_failed)
        is DokusException.TeamInviteFailed -> stringResource(Res.string.team_invite_failed)
        is DokusException.TeamInviteCancelFailed -> stringResource(Res.string.team_invite_cancel_failed)
        is DokusException.TeamRoleUpdateFailed -> stringResource(Res.string.team_role_update_failed)
        is DokusException.TeamMemberRemoveFailed -> stringResource(Res.string.team_member_removed_failed)
        is DokusException.TeamOwnershipTransferFailed -> stringResource(Res.string.team_ownership_transferred_failed)
        is DokusException.WorkspaceSettingsSaveFailed -> stringResource(Res.string.settings_save_failed)
        is DokusException.WorkspaceAvatarUploadFailed -> stringResource(Res.string.workspace_avatar_upload_failed)
        is DokusException.WorkspaceAvatarDeleteFailed -> stringResource(Res.string.workspace_avatar_delete_failed)
        is DokusException.WorkspaceCreateFailed -> stringResource(Res.string.workspace_create_failed)
        is DokusException.CompanyLookupFailed -> stringResource(Res.string.auth_company_lookup_failed)
        is DokusException.WorkspaceSelectFailed -> stringResource(Res.string.workspace_select_failed)
        is DokusException.WorkspaceContextUnavailable -> stringResource(Res.string.workspace_context_unavailable)
        is DokusException.ProfileSaveFailed -> stringResource(Res.string.profile_save_error)
        is DokusException.ChatLoadConversationFailed -> {
            val reasonText = reason?.takeIf { it.isNotBlank() }
                ?: stringResource(Res.string.common_unknown)
            stringResource(Res.string.chat_error_load_conversation, reasonText)
        }
        is DokusException.ChatSendMessageFailed -> {
            val reasonText = reason?.takeIf { it.isNotBlank() }
                ?: stringResource(Res.string.common_unknown)
            stringResource(Res.string.chat_error_send_message, reasonText)
        }
        is DokusException.ChatNoDocumentSelected ->
            stringResource(Res.string.chat_error_no_document_selected)
        is DokusException.ChatInvalidDocumentReference ->
            stringResource(Res.string.chat_error_invalid_document_reference)
        is DokusException.DocumentContactClearFailed ->
            stringResource(Res.string.cashflow_contact_clear_failed)
        is DokusException.DocumentContactSaveFailed ->
            stringResource(Res.string.cashflow_contact_save_failed)
        is DokusException.DocumentContactBindFailed ->
            stringResource(Res.string.cashflow_contact_bind_failed)
        is DokusException.DocumentPreviewLoadFailed ->
            stringResource(Res.string.cashflow_preview_load_failed)
        is DokusException.DocumentUploadFailed ->
            stringResource(Res.string.upload_failed_message)
        is DokusException.InternalError -> errorMessage
        is DokusException.TenantCreationFailed -> stringResource(Res.string.exception_tenant_creation_failed)
        is DokusException.Unknown -> message ?: stringResource(Res.string.exception_unknown)

        // 404 Not Found
        is DokusException.NotFound -> stringResource(Res.string.exception_not_found)
        is DokusException.UserNotFound -> stringResource(Res.string.exception_user_not_found)

        // 501 Not Implemented
        is DokusException.NotImplemented -> message ?: stringResource(Res.string.exception_not_implemented)

        // 503 Service Unavailable
        is DokusException.ConnectionError -> stringResource(Res.string.exception_connection_error)
    }
