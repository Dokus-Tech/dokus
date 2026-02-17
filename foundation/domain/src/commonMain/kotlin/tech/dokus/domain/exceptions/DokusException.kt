package tech.dokus.domain.exceptions

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
sealed class DokusException(
    val httpStatusCode: Int,
    val errorCode: String,
    val recoverable: Boolean = false,
    val errorId: String = generateErrorId(),
) : Throwable() {

    @Serializable
    @SerialName("DokusException.BadRequest")
    data class BadRequest(
        override val message: String = "Bad request",
    ) : DokusException(
        httpStatusCode = HTTP_STATUS,
        errorCode = ERROR_CODE,
        recoverable = false,
    ) {
        companion object {
            const val HTTP_STATUS = 400
            const val ERROR_CODE = "BAD_REQUEST"
        }
    }

    // 400 Bad Request - Validation Errors
    @Serializable
    @SerialName("DokusException.Validation")
    sealed class Validation(
        override val message: String? = "Validation error",
    ) : DokusException(
        httpStatusCode = HTTP_STATUS,
        errorCode = ERROR_CODE,
        recoverable = false,
    ) {
        companion object {
            const val HTTP_STATUS = 400
            const val ERROR_CODE = "VALIDATION_ERROR"
        }

        @Serializable
        @SerialName("DokusException.Validation.InvalidEmail")
        data object InvalidEmail : Validation(
            message = "Invalid email address",
        )

        @Serializable
        @SerialName("DokusException.Validation.EmailRequired")
        data object EmailRequired : Validation(
            message = "Email is required",
        )

        @Serializable
        @SerialName("DokusException.Validation.InvalidPhoneNumber")
        data object InvalidPhoneNumber : Validation(
            message = "Invalid phone number",
        )

        @Serializable
        @SerialName("DokusException.Validation.WeakPassword")
        data object WeakPassword : Validation(
            message = "Password does not meet security requirements",
        )

        @Serializable
        @SerialName("DokusException.Validation.PasswordDoNotMatch")
        data object PasswordDoNotMatch : Validation(
            message = "Passwords do not match",
        )

        @Serializable
        @SerialName("DokusException.Validation.InvalidFirstName")
        data object InvalidFirstName : Validation(
            message = "Invalid first name",
        )

        @Serializable
        @SerialName("DokusException.Validation.InvalidLastName")
        data object InvalidLastName : Validation(
            message = "Invalid last name",
        )

        @Serializable
        @SerialName("DokusException.Validation.InvalidTaxNumber")
        data object InvalidTaxNumber : Validation(
            message = "Invalid tax number",
        )

        @Serializable
        @SerialName("DokusException.Validation.InvalidWorkspaceName")
        data object InvalidWorkspaceName : Validation(
            message = "Invalid workspace name",
        )

        @Serializable
        @SerialName("DokusException.Validation.InvalidLegalName")
        data object InvalidLegalName : Validation(
            message = "Invalid legal name",
        )

        @Serializable
        @SerialName("DokusException.Validation.InvalidDisplayName")
        data object InvalidDisplayName : Validation(
            message = "Invalid display name",
        )

        // Server Connection Validation Errors
        @Serializable
        @SerialName("DokusException.Validation.ServerHostRequired")
        data object ServerHostRequired : Validation(
            message = "Host is required",
        )

        @Serializable
        @SerialName("DokusException.Validation.ServerHostNoSpaces")
        data object ServerHostNoSpaces : Validation(
            message = "Host must not contain spaces",
        )

        @Serializable
        @SerialName("DokusException.Validation.ServerPortRequired")
        data object ServerPortRequired : Validation(
            message = "Port is required",
        )

        @Serializable
        @SerialName("DokusException.Validation.ServerPortInvalidNumber")
        data object ServerPortInvalidNumber : Validation(
            message = "Port must be a number",
        )

        @Serializable
        @SerialName("DokusException.Validation.ServerPortOutOfRange")
        data object ServerPortOutOfRange : Validation(
            message = "Port must be between 1 and 65535",
        )

        // Contact Validation Errors
        @Serializable
        @SerialName("DokusException.Validation.ContactNameRequired")
        data object ContactNameRequired : Validation(
            message = "Name is required",
        )

        @Serializable
        @SerialName("DokusException.Validation.CompanyNameRequired")
        data object CompanyNameRequired : Validation(
            message = "Company name is required",
        )

        @Serializable
        @SerialName("DokusException.Validation.FullNameRequired")
        data object FullNameRequired : Validation(
            message = "Full name is required",
        )

        @Serializable
        @SerialName("DokusException.Validation.ContactEmailOrPhoneRequired")
        data object ContactEmailOrPhoneRequired : Validation(
            message = "Email or phone number is required",
        )

        @Serializable
        @SerialName("DokusException.Validation.InvalidVatNumber")
        data object InvalidVatNumber : Validation(
            message = "Invalid VAT number",
        )

        @Serializable
        @SerialName("DokusException.Validation.InvalidIban")
        data object InvalidIban : Validation(
            message = "Invalid IBAN",
        )

        @Serializable
        @SerialName("DokusException.Validation.InvalidStructuredCommunication")
        data object InvalidStructuredCommunication : Validation(
            message = "Invalid structured communication",
        )

        @Serializable
        @SerialName("DokusException.Validation.InvalidBic")
        data object InvalidBic : Validation(
            message = "Invalid BIC/SWIFT code",
        )

        @Serializable
        @SerialName("DokusException.Validation.InvalidPeppolId")
        data object InvalidPeppolId : Validation(
            message = "Invalid Peppol ID",
        )

        @Serializable
        @SerialName("DokusException.Validation.PeppolIdRequired")
        data object PeppolIdRequired : Validation(
            message = "Peppol ID is required",
        )

        @Serializable
        @SerialName("DokusException.Validation.InvalidInvoiceNumber")
        data object InvalidInvoiceNumber : Validation(
            message = "Invalid invoice number",
        )

        @Serializable
        @SerialName("DokusException.Validation.InvalidMoney")
        data object InvalidMoney : Validation(
            message = "Invalid monetary amount",
        )

        @Serializable
        @SerialName("DokusException.Validation.InvalidVatRate")
        data object InvalidVatRate : Validation(
            message = "Invalid VAT rate",
        )

        @Serializable
        @SerialName("DokusException.Validation.InvalidPercentage")
        data object InvalidPercentage : Validation(
            message = "Invalid percentage value",
        )

        @Serializable
        @SerialName("DokusException.Validation.InvalidQuantity")
        data object InvalidQuantity : Validation(
            message = "Invalid quantity",
        )

        @Serializable
        @SerialName("DokusException.Validation.InvoiceClientRequired")
        data object InvoiceClientRequired : Validation(
            message = "Client is required",
        )

        @Serializable
        @SerialName("DokusException.Validation.InvoiceItemsRequired")
        data object InvoiceItemsRequired : Validation(
            message = "At least one line item is required",
        )

        @Serializable
        @SerialName("DokusException.Validation.InvoiceDueDateBeforeIssue")
        data object InvoiceDueDateBeforeIssue : Validation(
            message = "Due date cannot be before issue date",
        )

        // Document Validation Errors
        @Serializable
        @SerialName("DokusException.Validation.DocumentMissingFields")
        data object DocumentMissingFields : Validation(
            message = "Required fields are missing",
        )

        @Serializable
        @SerialName("DokusException.Validation.NoteContentRequired")
        data object NoteContentRequired : Validation(
            message = "Note content is required",
        )

        // Address Validation Errors
        @Serializable
        @SerialName("DokusException.Validation.InvalidStreetName")
        data object InvalidStreetName : Validation(
            message = "Invalid street name",
        )

        @Serializable
        @SerialName("DokusException.Validation.InvalidCity")
        data object InvalidCity : Validation(
            message = "Invalid city",
        )

        @Serializable
        @SerialName("DokusException.Validation.InvalidPostalCode")
        data object InvalidPostalCode : Validation(
            message = "Invalid postal code",
        )

        @Serializable
        @SerialName("DokusException.Validation.InvalidCountry")
        data object InvalidCountry : Validation(
            message = "Invalid country",
        )

        @Serializable
        @SerialName("DokusException.Validation.Generic")
        data class Generic(
            val errorMessage: String,
        ) : Validation(message = errorMessage)

        // Peppol/API Credential Validation Errors
        @Serializable
        @SerialName("DokusException.Validation.ApiKeyRequired")
        data object ApiKeyRequired : Validation(
            message = "API Key is required",
        )

        @Serializable
        @SerialName("DokusException.Validation.ApiSecretRequired")
        data object ApiSecretRequired : Validation(
            message = "API Secret is required",
        )

        @Serializable
        @SerialName("DokusException.Validation.InvalidApiCredentials")
        data object InvalidApiCredentials : Validation(
            message = "Invalid API credentials",
        )

        @Serializable
        @SerialName("DokusException.Validation.MissingVatNumber")
        data object MissingVatNumber : Validation(
            message = "VAT number is required",
        )

        @Serializable
        @SerialName("DokusException.Validation.MissingCompanyAddress")
        data object MissingCompanyAddress : Validation(
            message = "Company address is required",
        )
    }

    // 401 Unauthorized - Authentication Errors
    @Serializable
    @SerialName("DokusException.NotAuthenticated")
    data class NotAuthenticated(
        override val message: String? = "Not authenticated",
    ) : DokusException(
        httpStatusCode = HTTP_STATUS,
        errorCode = ERROR_CODE,
    ) {
        companion object {
            const val HTTP_STATUS = 401
            const val ERROR_CODE = "NOT_AUTHENTICATED"
        }
    }

    @Serializable
    @SerialName("DokusException.InvalidCredentials")
    data class InvalidCredentials(
        override val message: String? = "Invalid email or password",
    ) : DokusException(
        httpStatusCode = HTTP_STATUS,
        errorCode = ERROR_CODE,
    ) {
        companion object {
            const val HTTP_STATUS = 401
            const val ERROR_CODE = "INVALID_CREDENTIALS"
        }
    }

    @Serializable
    @SerialName("DokusException.TokenExpired")
    data class TokenExpired(
        override val message: String? = "Authentication token has expired",
    ) : DokusException(
        httpStatusCode = HTTP_STATUS,
        errorCode = ERROR_CODE,
    ) {
        companion object {
            const val HTTP_STATUS = 401
            const val ERROR_CODE = "TOKEN_EXPIRED"
        }
    }

    @Serializable
    @SerialName("DokusException.TokenInvalid")
    data class TokenInvalid(
        override val message: String? = "Invalid authentication token",
    ) : DokusException(
        httpStatusCode = HTTP_STATUS,
        errorCode = ERROR_CODE,
    ) {
        companion object {
            const val HTTP_STATUS = 401
            const val ERROR_CODE = "TOKEN_INVALID"
        }
    }

    @Serializable
    @SerialName("DokusException.RefreshTokenExpired")
    data class RefreshTokenExpired(
        override val message: String? = "Refresh token has expired. Please log in again.",
    ) : DokusException(
        httpStatusCode = HTTP_STATUS,
        errorCode = ERROR_CODE,
    ) {
        companion object {
            const val HTTP_STATUS = 401
            const val ERROR_CODE = "REFRESH_TOKEN_EXPIRED"
        }
    }

    @Serializable
    @SerialName("DokusException.RefreshTokenRevoked")
    data class RefreshTokenRevoked(
        override val message: String? = "Refresh token has been revoked. Please log in again.",
    ) : DokusException(
        httpStatusCode = HTTP_STATUS,
        errorCode = ERROR_CODE,
    ) {
        companion object {
            const val HTTP_STATUS = 401
            const val ERROR_CODE = "REFRESH_TOKEN_REVOKED"
        }
    }

    @Serializable
    @SerialName("DokusException.SessionExpired")
    data class SessionExpired(
        override val message: String? = "Your session has expired. Please log in again.",
    ) : DokusException(
        httpStatusCode = HTTP_STATUS,
        errorCode = ERROR_CODE,
        recoverable = true,
    ) {
        companion object {
            const val HTTP_STATUS = 401
            const val ERROR_CODE = "SESSION_EXPIRED"
        }
    }

    @Serializable
    @SerialName("DokusException.SessionInvalid")
    data class SessionInvalid(
        override val message: String? = "Invalid session. Please log in again.",
    ) : DokusException(
        httpStatusCode = HTTP_STATUS,
        errorCode = ERROR_CODE,
    ) {
        companion object {
            const val HTTP_STATUS = 401
            const val ERROR_CODE = "SESSION_INVALID"
        }
    }

    @Serializable
    @SerialName("DokusException.PasswordResetTokenExpired")
    data class PasswordResetTokenExpired(
        override val message: String? = "Password reset token has expired. Please request a new one.",
    ) : DokusException(
        httpStatusCode = HTTP_STATUS,
        errorCode = ERROR_CODE,
    ) {
        companion object {
            const val HTTP_STATUS = 401
            const val ERROR_CODE = "PASSWORD_RESET_TOKEN_EXPIRED"
        }
    }

    @Serializable
    @SerialName("DokusException.PasswordResetTokenInvalid")
    data class PasswordResetTokenInvalid(
        override val message: String? = "Invalid password reset token. Please request a new one.",
    ) : DokusException(
        httpStatusCode = HTTP_STATUS,
        errorCode = ERROR_CODE,
    ) {
        companion object {
            const val HTTP_STATUS = 401
            const val ERROR_CODE = "PASSWORD_RESET_TOKEN_INVALID"
        }
    }

    @Serializable
    @SerialName("DokusException.EmailVerificationTokenExpired")
    data class EmailVerificationTokenExpired(
        override val message: String? = "Email verification token has expired. Please request a new one.",
    ) : DokusException(
        httpStatusCode = HTTP_STATUS,
        errorCode = ERROR_CODE,
    ) {
        companion object {
            const val HTTP_STATUS = 401
            const val ERROR_CODE = "EMAIL_VERIFICATION_TOKEN_EXPIRED"
        }
    }

    @Serializable
    @SerialName("DokusException.EmailVerificationTokenInvalid")
    data class EmailVerificationTokenInvalid(
        override val message: String? = "Invalid email verification token. Please request a new one.",
    ) : DokusException(
        httpStatusCode = HTTP_STATUS,
        errorCode = ERROR_CODE,
    ) {
        companion object {
            const val HTTP_STATUS = 401
            const val ERROR_CODE = "EMAIL_VERIFICATION_TOKEN_INVALID"
        }
    }

    // 403 Forbidden - Authorization Errors
    @Serializable
    @SerialName("DokusException.NotAuthorized")
    data class NotAuthorized(
        override val message: String? = "You do not have permission to access this resource",
    ) : DokusException(
        httpStatusCode = HTTP_STATUS,
        errorCode = ERROR_CODE,
    ) {
        companion object {
            const val HTTP_STATUS = 403
            const val ERROR_CODE = "NOT_AUTHORIZED"
        }
    }

    @Serializable
    @SerialName("DokusException.AccountInactive")
    data class AccountInactive(
        override val message: String? = "Your account is inactive. Please contact support.",
    ) : DokusException(
        httpStatusCode = HTTP_STATUS,
        errorCode = ERROR_CODE,
    ) {
        companion object {
            const val HTTP_STATUS = 403
            const val ERROR_CODE = "ACCOUNT_INACTIVE"
        }
    }

    @Serializable
    @SerialName("DokusException.AccountLocked")
    data class AccountLocked(
        override val message: String? = "Your account has been locked. Please contact support to unlock it.",
    ) : DokusException(
        httpStatusCode = HTTP_STATUS,
        errorCode = ERROR_CODE,
    ) {
        companion object {
            const val HTTP_STATUS = 403
            const val ERROR_CODE = "ACCOUNT_LOCKED"
        }
    }

    @Serializable
    @SerialName("DokusException.EmailNotVerified")
    data class EmailNotVerified(
        override val message: String? = "Please verify your email address to continue",
    ) : DokusException(
        httpStatusCode = HTTP_STATUS,
        errorCode = ERROR_CODE,
    ) {
        companion object {
            const val HTTP_STATUS = 403
            const val ERROR_CODE = "EMAIL_NOT_VERIFIED"
        }
    }

    @Serializable
    @SerialName("DokusException.EmailAlreadyVerified")
    data class EmailAlreadyVerified(
        override val message: String? = "Email address has already been verified",
    ) : DokusException(
        httpStatusCode = HTTP_STATUS,
        errorCode = ERROR_CODE,
    ) {
        companion object {
            const val HTTP_STATUS = 403
            const val ERROR_CODE = "EMAIL_ALREADY_VERIFIED"
        }
    }

    @Serializable
    @SerialName("DokusException.TooManySessions")
    data class TooManySessions(
        override val message: String? =
            "Maximum number of concurrent sessions reached. Please log out from another device.",
        val maxSessions: Int = 5,
    ) : DokusException(
        httpStatusCode = HTTP_STATUS,
        errorCode = ERROR_CODE,
        recoverable = true,
    ) {
        companion object {
            const val HTTP_STATUS = 403
            const val ERROR_CODE = "TOO_MANY_SESSIONS"
        }
    }

    // 409 Conflict - Duplicate Resources
    @Serializable
    @SerialName("DokusException.UserAlreadyExists")
    data class UserAlreadyExists(
        override val message: String? = "A user with this email already exists",
    ) : DokusException(
        httpStatusCode = HTTP_STATUS,
        errorCode = ERROR_CODE,
    ) {
        companion object {
            const val HTTP_STATUS = 409
            const val ERROR_CODE = "USER_ALREADY_EXISTS"
        }
    }

    // 404 Not Found
    @Serializable
    @SerialName("DokusException.UserNotFound")
    data class UserNotFound(
        override val message: String? = "User not found",
    ) : DokusException(
        httpStatusCode = 404,
        errorCode = "USER_NOT_FOUND",
    )

    @Serializable
    @SerialName("DokusException.NotFound")
    data class NotFound(
        override val message: String? = "Resource was not found",
    ) : DokusException(
        httpStatusCode = 404,
        errorCode = "RESOURCE_NOT_FOUND",
    )

    // 429 Too Many Requests
    @Serializable
    @SerialName("DokusException.TooManyLoginAttempts")
    data class TooManyLoginAttempts(
        override val message: String? = "Too many login attempts. Please try again later.",
        val retryAfterSeconds: Int = 60,
    ) : DokusException(
        httpStatusCode = HTTP_STATUS,
        errorCode = ERROR_CODE,
        recoverable = true,
    ) {
        companion object {
            const val HTTP_STATUS = 429
            const val ERROR_CODE = "TOO_MANY_LOGIN_ATTEMPTS"
        }
    }

    // 500 Internal Server Error
    @Serializable
    @SerialName("DokusException.InternalError")
    data class InternalError(
        val errorMessage: String,
    ) : DokusException(
        httpStatusCode = HTTP_STATUS,
        errorCode = ERROR_CODE,
        recoverable = true,
    ) {
        companion object {
            const val HTTP_STATUS = 500
            const val ERROR_CODE = "INTERNAL_ERROR"
        }
    }

    @Serializable
    @SerialName("DokusException.TenantCreationFailed")
    data class TenantCreationFailed(
        override val message: String? = "Failed to create tenant. Please try again.",
    ) : DokusException(
        httpStatusCode = HTTP_STATUS,
        errorCode = ERROR_CODE,
        recoverable = true,
    ) {
        companion object {
            const val HTTP_STATUS = 500
            const val ERROR_CODE = "TENANT_CREATION_FAILED"
        }
    }

    // 500 Internal Server Error - Contact Operations
    @Serializable
    @SerialName("DokusException.ContactCreateFailed")
    data object ContactCreateFailed : DokusException(
        httpStatusCode = 500,
        errorCode = "CONTACT_CREATE_FAILED",
        recoverable = true,
    ) {
        override val message: String? = "Failed to create contact"
    }

    @Serializable
    @SerialName("DokusException.ContactLookupFailed")
    data object ContactLookupFailed : DokusException(
        httpStatusCode = 500,
        errorCode = "CONTACT_LOOKUP_FAILED",
        recoverable = true,
    ) {
        override val message: String? = "Failed to search contacts"
    }

    @Serializable
    @SerialName("DokusException.ContactDeleteFailed")
    data object ContactDeleteFailed : DokusException(
        httpStatusCode = 500,
        errorCode = "CONTACT_DELETE_FAILED",
        recoverable = true,
    ) {
        override val message: String? = "Failed to delete contact"
    }

    @Serializable
    @SerialName("DokusException.ContactMergeFailed")
    data object ContactMergeFailed : DokusException(
        httpStatusCode = 500,
        errorCode = "CONTACT_MERGE_FAILED",
        recoverable = true,
    ) {
        override val message: String? = "Failed to merge contacts"
    }

    @Serializable
    @SerialName("DokusException.ContactPeppolUpdateFailed")
    data object ContactPeppolUpdateFailed : DokusException(
        httpStatusCode = 500,
        errorCode = "CONTACT_PEPPOL_UPDATE_FAILED",
        recoverable = true,
    ) {
        override val message: String? = "Failed to update Peppol settings"
    }

    @Serializable
    @SerialName("DokusException.ContactNoteAddFailed")
    data object ContactNoteAddFailed : DokusException(
        httpStatusCode = 500,
        errorCode = "CONTACT_NOTE_ADD_FAILED",
        recoverable = true,
    ) {
        override val message: String? = "Failed to add note"
    }

    @Serializable
    @SerialName("DokusException.ContactNoteUpdateFailed")
    data object ContactNoteUpdateFailed : DokusException(
        httpStatusCode = 500,
        errorCode = "CONTACT_NOTE_UPDATE_FAILED",
        recoverable = true,
    ) {
        override val message: String? = "Failed to update note"
    }

    @Serializable
    @SerialName("DokusException.ContactNoteDeleteFailed")
    data object ContactNoteDeleteFailed : DokusException(
        httpStatusCode = 500,
        errorCode = "CONTACT_NOTE_DELETE_FAILED",
        recoverable = true,
    ) {
        override val message: String? = "Failed to delete note"
    }

    // 500 Internal Server Error - Team Operations
    @Serializable
    @SerialName("DokusException.TeamInviteFailed")
    data object TeamInviteFailed : DokusException(
        httpStatusCode = 500,
        errorCode = "TEAM_INVITE_FAILED",
        recoverable = true,
    ) {
        override val message: String? = "Failed to send invitation"
    }

    @Serializable
    @SerialName("DokusException.TeamInviteCancelFailed")
    data object TeamInviteCancelFailed : DokusException(
        httpStatusCode = 500,
        errorCode = "TEAM_INVITE_CANCEL_FAILED",
        recoverable = true,
    ) {
        override val message: String? = "Failed to cancel invitation"
    }

    @Serializable
    @SerialName("DokusException.TeamRoleUpdateFailed")
    data object TeamRoleUpdateFailed : DokusException(
        httpStatusCode = 500,
        errorCode = "TEAM_ROLE_UPDATE_FAILED",
        recoverable = true,
    ) {
        override val message: String? = "Failed to update role"
    }

    @Serializable
    @SerialName("DokusException.TeamMemberRemoveFailed")
    data object TeamMemberRemoveFailed : DokusException(
        httpStatusCode = 500,
        errorCode = "TEAM_MEMBER_REMOVE_FAILED",
        recoverable = true,
    ) {
        override val message: String? = "Failed to remove member"
    }

    @Serializable
    @SerialName("DokusException.TeamOwnershipTransferFailed")
    data object TeamOwnershipTransferFailed : DokusException(
        httpStatusCode = 500,
        errorCode = "TEAM_OWNERSHIP_TRANSFER_FAILED",
        recoverable = true,
    ) {
        override val message: String? = "Failed to transfer ownership"
    }

    // 500 Internal Server Error - Workspace Operations
    @Serializable
    @SerialName("DokusException.WorkspaceSettingsSaveFailed")
    data object WorkspaceSettingsSaveFailed : DokusException(
        httpStatusCode = 500,
        errorCode = "WORKSPACE_SETTINGS_SAVE_FAILED",
        recoverable = true,
    ) {
        override val message: String? = "Failed to save settings"
    }

    @Serializable
    @SerialName("DokusException.WorkspaceAvatarUploadFailed")
    data object WorkspaceAvatarUploadFailed : DokusException(
        httpStatusCode = 500,
        errorCode = "WORKSPACE_AVATAR_UPLOAD_FAILED",
        recoverable = true,
    ) {
        override val message: String? = "Failed to upload avatar"
    }

    @Serializable
    @SerialName("DokusException.WorkspaceAvatarDeleteFailed")
    data object WorkspaceAvatarDeleteFailed : DokusException(
        httpStatusCode = 500,
        errorCode = "WORKSPACE_AVATAR_DELETE_FAILED",
        recoverable = true,
    ) {
        override val message: String? = "Failed to delete avatar"
    }

    @Serializable
    @SerialName("DokusException.WorkspaceCreateFailed")
    data object WorkspaceCreateFailed : DokusException(
        httpStatusCode = 500,
        errorCode = "WORKSPACE_CREATE_FAILED",
        recoverable = true,
    ) {
        override val message: String? = "Failed to create workspace"
    }

    @Serializable
    @SerialName("DokusException.CompanyLookupFailed")
    data object CompanyLookupFailed : DokusException(
        httpStatusCode = 500,
        errorCode = "COMPANY_LOOKUP_FAILED",
        recoverable = true,
    ) {
        override val message: String? = "Company lookup failed"
    }

    @Serializable
    @SerialName("DokusException.WorkspaceSelectFailed")
    data object WorkspaceSelectFailed : DokusException(
        httpStatusCode = 500,
        errorCode = "WORKSPACE_SELECT_FAILED",
        recoverable = true,
    ) {
        override val message: String? = "Failed to select workspace"
    }

    @Serializable
    @SerialName("DokusException.WorkspaceContextUnavailable")
    data object WorkspaceContextUnavailable : DokusException(
        httpStatusCode = 400,
        errorCode = "WORKSPACE_CONTEXT_UNAVAILABLE",
        recoverable = true,
    ) {
        override val message: String? = "Workspace context unavailable"
    }

    @Serializable
    @SerialName("DokusException.ProfileSaveFailed")
    data object ProfileSaveFailed : DokusException(
        httpStatusCode = 500,
        errorCode = "PROFILE_SAVE_FAILED",
        recoverable = true,
    ) {
        override val message: String? = "Failed to save profile"
    }

    // 500 Internal Server Error - Chat Operations
    @Serializable
    @SerialName("DokusException.ChatLoadConversationFailed")
    data class ChatLoadConversationFailed(
        val reason: String? = null,
        override val message: String? = reason,
    ) : DokusException(
        httpStatusCode = 500,
        errorCode = "CHAT_LOAD_CONVERSATION_FAILED",
        recoverable = true,
    )

    @Serializable
    @SerialName("DokusException.ChatSendMessageFailed")
    data class ChatSendMessageFailed(
        val reason: String? = null,
        override val message: String? = reason,
    ) : DokusException(
        httpStatusCode = 500,
        errorCode = "CHAT_SEND_MESSAGE_FAILED",
        recoverable = true,
    )

    @Serializable
    @SerialName("DokusException.ChatNoDocumentSelected")
    data object ChatNoDocumentSelected : DokusException(
        httpStatusCode = 400,
        errorCode = "CHAT_NO_DOCUMENT_SELECTED",
        recoverable = false,
    ) {
        override val message: String? = "No document selected"
    }

    @Serializable
    @SerialName("DokusException.ChatInvalidDocumentReference")
    data object ChatInvalidDocumentReference : DokusException(
        httpStatusCode = 400,
        errorCode = "CHAT_INVALID_DOCUMENT_REFERENCE",
        recoverable = false,
    ) {
        override val message: String? = "Invalid document reference"
    }

    // 500 Internal Server Error - Document Review Operations
    @Serializable
    @SerialName("DokusException.DocumentContactClearFailed")
    data object DocumentContactClearFailed : DokusException(
        httpStatusCode = 500,
        errorCode = "DOCUMENT_CONTACT_CLEAR_FAILED",
        recoverable = true,
    ) {
        override val message: String? = "Failed to clear contact"
    }

    @Serializable
    @SerialName("DokusException.DocumentContactSaveFailed")
    data object DocumentContactSaveFailed : DokusException(
        httpStatusCode = 500,
        errorCode = "DOCUMENT_CONTACT_SAVE_FAILED",
        recoverable = true,
    ) {
        override val message: String? = "Failed to save contact"
    }

    @Serializable
    @SerialName("DokusException.DocumentContactBindFailed")
    data object DocumentContactBindFailed : DokusException(
        httpStatusCode = 500,
        errorCode = "DOCUMENT_CONTACT_BIND_FAILED",
        recoverable = true,
    ) {
        override val message: String? = "Failed to bind contact"
    }

    @Serializable
    @SerialName("DokusException.DocumentPreviewLoadFailed")
    data object DocumentPreviewLoadFailed : DokusException(
        httpStatusCode = 500,
        errorCode = "DOCUMENT_PREVIEW_LOAD_FAILED",
        recoverable = true,
    ) {
        override val message: String? = "Failed to load preview"
    }

    @Serializable
    @SerialName("DokusException.DocumentUploadFailed")
    data object DocumentUploadFailed : DokusException(
        httpStatusCode = 500,
        errorCode = "DOCUMENT_UPLOAD_FAILED",
        recoverable = true,
    ) {
        override val message: String? = "Upload failed"
    }

    @Serializable
    @SerialName("DokusException.Unknown")
    data class Unknown(
        @Contextual val throwable: Throwable?,
        override val message: String? = throwable?.message,
    ) : DokusException(
        httpStatusCode = HTTP_STATUS,
        errorCode = ERROR_CODE,
        recoverable = false,
    ) {
        companion object {
            const val HTTP_STATUS = 500
            const val ERROR_CODE = "UNKNOWN_ERROR"
        }
    }

    // 501 Not Implemented
    @Serializable
    @SerialName("DokusException.NotImplemented")
    data class NotImplemented(
        override val message: String? = "This feature is not yet implemented.",
    ) : DokusException(
        httpStatusCode = HTTP_STATUS,
        errorCode = ERROR_CODE,
        recoverable = false,
    ) {
        companion object {
            const val HTTP_STATUS = 501
            const val ERROR_CODE = "NOT_IMPLEMENTED"
        }
    }

    // 503 Service Unavailable
    @Serializable
    @SerialName("DokusException.ConnectionError")
    data class ConnectionError(
        override val message: String? = "Connection error. Please try again later.",
    ) : DokusException(
        httpStatusCode = HTTP_STATUS,
        errorCode = ERROR_CODE,
        recoverable = true,
    ) {
        companion object {
            const val HTTP_STATUS = 503
            const val ERROR_CODE = "CONNECTION_ERROR"
        }
    }

    companion object {
fun generateErrorId(): String =
            "ERR-${Uuid.random()}"
    }
}
