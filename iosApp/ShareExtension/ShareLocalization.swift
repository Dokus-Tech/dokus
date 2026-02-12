import Foundation

enum ShareLocalizationKey: String, CaseIterable {
    case appLabel = "share.app_label"
    case headingPreparing = "share.heading.preparing"
    case headingResolving = "share.heading.resolving"
    case headingUploading = "share.heading.uploading"
    case headingSuccess = "share.heading.success"
    case headingError = "share.heading.error"
    case descriptionPreparing = "share.description.preparing"
    case descriptionResolving = "share.description.resolving"
    case keepOpen = "share.keep_open"
    case fileProgress = "share.file_progress"
    case progressLabel = "share.progress_label"
    case uploadedSingle = "share.uploaded_single"
    case uploadedMultiple = "share.uploaded_multiple"
    case savedToDocuments = "share.saved_to_documents"
    case closingAutomatically = "share.closing_automatically"
    case buttonRetry = "share.button.retry"
    case buttonOpenApp = "share.button.open_app"
    case buttonClose = "share.button.close"
    case workspaceSwitchHint = "share.workspace_switch_hint"
    case fallbackWorkspaceName = "share.workspace_fallback_name"
    case errorLoggedOut = "share.error.logged_out"
    case errorNoWorkspaceAvailable = "share.error.no_workspace_available"
    case errorWorkspaceContextUnavailable = "share.error.workspace_context_unavailable"
    case errorUnableReadPdf = "share.error.unable_read_pdf"
    case errorUploadFailed = "share.error.upload_failed"
    case errorNetworkLoadWorkspaces = "share.error.network_load_workspaces"
    case errorInvalidServerResponse = "share.error.invalid_server_response"
    case errorSessionExpired = "share.error.session_expired"
    case errorUnableLoadWorkspaces = "share.error.unable_load_workspaces"
    case errorNetworkRefreshSession = "share.error.network_refresh_session"
    case errorInvalidSessionResponse = "share.error.invalid_session_response"
    case errorSessionInvalid = "share.error.session_invalid"
    case errorInvalidRefreshResponse = "share.error.invalid_refresh_response"
    case errorNetworkSelectWorkspace = "share.error.network_select_workspace"
    case errorInvalidWorkspaceResponse = "share.error.invalid_workspace_response"
    case errorFailedSwitchWorkspace = "share.error.failed_switch_workspace"
    case errorInvalidWorkspaceSwitchResponse = "share.error.invalid_workspace_switch_response"
    case errorNoPdfFound = "share.error.no_pdf_found"
    case errorNoPdfRetry = "share.error.no_pdf_retry"
    case errorUnexpectedPrepareUpload = "share.error.unexpected_prepare_upload"
    case errorUploadSessionUnavailable = "share.error.upload_session_unavailable"
}

enum ShareLocalizationArgument: Equatable {
    case int(Int)
    case string(String)

    fileprivate var cVarArg: CVarArg {
        switch self {
        case .int(let value):
            return value
        case .string(let value):
            return value
        }
    }
}

struct ShareLocalizedMessage: Equatable {
    let key: ShareLocalizationKey
    var arguments: [ShareLocalizationArgument] = []
    var serverMessageOverride: String? = nil

    func resolve(bundle: Bundle = .main, locale: Locale = .current) -> String {
        if let serverMessageOverride {
            let trimmed = serverMessageOverride.trimmingCharacters(in: .whitespacesAndNewlines)
            if !trimmed.isEmpty {
                return trimmed
            }
        }

        return key.localized(arguments: arguments, bundle: bundle, locale: locale)
    }
}

enum ShareLocalization {
    static let supportedLocaleIdentifiers = [
        "Base",
        "en",
        "de",
        "es",
        "fr",
        "fr-BE",
        "it",
        "nl",
        "nl-BE",
        "ru"
    ]

    static func bundle(for localeIdentifier: String, from rootBundle: Bundle) -> Bundle? {
        guard let path = rootBundle.path(forResource: localeIdentifier, ofType: "lproj") else {
            return nil
        }

        return Bundle(path: path)
    }
}

extension ShareLocalizationKey {
    func localized(
        arguments: [ShareLocalizationArgument] = [],
        bundle: Bundle = .main,
        locale: Locale = .current
    ) -> String {
        let format = bundle.localizedString(forKey: rawValue, value: rawValue, table: nil)
        guard !arguments.isEmpty else {
            return format
        }

        let cVarArgs = arguments.map(\.cVarArg)
        return withVaList(cVarArgs) { pointer in
            NSString(format: format, locale: locale, arguments: pointer) as String
        }
    }
}
