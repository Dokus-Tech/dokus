import Foundation
import FileProvider
import UniformTypeIdentifiers
import Security
import OSLog

enum DokusFileProviderConstants {
    static let managedDomainIdentifierPrefix = "vision.invoid.dokus.fileprovider"
    static let legacyDomainIdentifier = managedDomainIdentifierPrefix
    static let workspaceDomainSeparator = ".ws."
    static let domainDisplayName = "Dokus"

    static let authService = "auth"
    static let accessTokenKey = "auth.access_token"
    static let refreshTokenKey = "auth.refresh_token"
    static let lastSelectedTenantKey = "auth.last_selected_tenant_id"
    static let tenantClaimKey = "tenant_id"
    static let tokenExpiryClaimKey = "exp"
    static let refreshLeadSeconds: TimeInterval = 60

    static let appGroupIdentifier = "group.vision.invoid.dokus.share"
    static let keychainAccessGroup = "$(AppIdentifierPrefix)vision.invoid.dokus.shared"
    static let serverBaseURLKey = "share.server.base_url"
    static let defaultServerBaseURL = "https://dokus.invoid.vision"

    static let snapshotDirectoryName = "FileProviderSnapshots"
    static let workspaceFolderPrefix = "ws"
    static let folderSeparator = ":"
    static let monthSeparator = " — "

    static func workspaceDomainIdentifier(workspaceId: String) -> String {
        "\(managedDomainIdentifierPrefix)\(workspaceDomainSeparator)\(workspaceId)"
    }

    static func workspaceId(fromDomainIdentifier domainIdentifier: String) -> String? {
        let prefix = "\(managedDomainIdentifierPrefix)\(workspaceDomainSeparator)"
        guard domainIdentifier.hasPrefix(prefix) else {
            return nil
        }
        let start = domainIdentifier.index(domainIdentifier.startIndex, offsetBy: prefix.count)
        let workspaceId = String(domainIdentifier[start...])
        return workspaceId.isEmpty ? nil : workspaceId
    }
}

enum DokusTypedFolder: String, CaseIterable {
    case invoicesIn = "Invoices[IN]"
    case invoicesOut = "Invoices[OUT]"
    case creditNotesIn = "CreditNotes[IN]"
    case creditNotesOut = "CreditNotes[OUT]"
    case receiptsIn = "Receipts[IN]"
    case receiptsOut = "Receipts[OUT]"
    case quotes = "Quotes"
    case proForma = "ProForma"
    case exports = "Exports"
}

enum DokusLifecycleFolder: String, CaseIterable {
    case inbox = "Inbox"
    case needsReview = "Needs Review"
}

enum DokusWorkspaceRole: String {
    case owner = "OWNER"
    case admin = "ADMIN"
    case accountant = "ACCOUNTANT"
    case editor = "EDITOR"
    case viewer = "VIEWER"

    var canSeeLifecycleFolders: Bool {
        switch self {
        case .owner, .admin, .editor:
            return true
        case .accountant, .viewer:
            return false
        }
    }

    static func from(raw: String?) -> DokusWorkspaceRole? {
        guard let raw else { return nil }
        return DokusWorkspaceRole(rawValue: raw.uppercased())
    }
}

struct DokusWorkspace: Hashable {
    let id: String
    let name: String
    let role: DokusWorkspaceRole?
}

enum DokusIngestionStatus: String {
    case queued = "QUEUED"
    case processing = "PROCESSING"
    case succeeded = "SUCCEEDED"
    case failed = "FAILED"
}

enum DokusDraftStatus: String {
    case needsReview = "NEEDS_REVIEW"
    case confirmed = "CONFIRMED"
    case rejected = "REJECTED"
}

enum DokusDocumentType: String {
    case invoice = "INVOICE"
    case creditNote = "CREDIT_NOTE"
    case receipt = "RECEIPT"
    case quote = "QUOTE"
    case proForma = "PRO_FORMA"
    case unknown = "UNKNOWN"
}

enum DokusDocumentDirection: String {
    case inbound = "INBOUND"
    case outbound = "OUTBOUND"
    case unknown = "UNKNOWN"
}

struct DokusDocumentRecord: Hashable {
    let workspaceId: String
    let documentId: String
    let originalFilename: String
    let contentType: String
    let sizeBytes: Int64
    let uploadedAt: Date?
    let updatedAt: Date?
    let downloadURL: URL?
    let latestIngestionStatus: DokusIngestionStatus?
    let draftStatus: DokusDraftStatus?
    let draftType: DokusDocumentType?
    let draftDirection: DokusDocumentDirection
    let issueDate: Date?
    let amountMinor: Int64?
    let counterpartyName: String?
    let documentNumber: String?
}

final class DokusSharedKeychainStore {
    private let service: String
    private let accessGroup: String?

    init(service: String, accessGroup: String?) {
        self.service = service
        self.accessGroup = accessGroup
    }

    func string(for key: String) -> String? {
        var query = baseQuery(for: key)
        query[kSecReturnData as String] = true
        query[kSecMatchLimit as String] = kSecMatchLimitOne

        var result: CFTypeRef?
        let status = SecItemCopyMatching(query as CFDictionary, &result)
        guard status == errSecSuccess else {
            return nil
        }

        guard
            let data = result as? Data,
            let value = String(data: data, encoding: .utf8)
        else {
            return nil
        }

        return value
    }

    func set(_ value: String, for key: String) {
        guard let data = value.data(using: .utf8) else { return }

        let query = baseQuery(for: key)
        let attributes: [String: Any] = [
            kSecValueData as String: data
        ]

        let updateStatus = SecItemUpdate(query as CFDictionary, attributes as CFDictionary)
        if updateStatus == errSecSuccess {
            return
        }

        if updateStatus == errSecItemNotFound {
            var addQuery = query
            addQuery[kSecValueData as String] = data
            SecItemAdd(addQuery as CFDictionary, nil)
        }
    }

    private func baseQuery(for key: String) -> [String: Any] {
        var query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: key
        ]

        if let accessGroup, !accessGroup.isEmpty {
            query[kSecAttrAccessGroup as String] = accessGroup
        }

        return query
    }
}

extension DateFormatter {
    static let dokusISODateTime: ISO8601DateFormatter = {
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        return formatter
    }()

    static let dokusISODateTimeNoFraction: ISO8601DateFormatter = {
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime]
        return formatter
    }()

    static let dokusLocalDate: DateFormatter = {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.timeZone = TimeZone(secondsFromGMT: 0)
        return formatter
    }()

    static let dokusLocalDateTime: DateFormatter = {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd'T'HH:mm:ss"
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.timeZone = TimeZone(secondsFromGMT: 0)
        return formatter
    }()
}

extension URL {
    func appendingPathQuery(_ path: String) -> URL {
        if #available(iOS 16.0, *) {
            return self.appending(path: path)
        }
        return self.appendingPathComponent(path)
    }
}

extension UTType {
    static func fromMimeType(_ mimeType: String?) -> UTType {
        guard
            let mimeType,
            let parsed = UTType(mimeType: mimeType)
        else {
            return .data
        }
        return parsed
    }
}

enum DokusFileProviderLog {
    private static let fallbackSubsystem = "vision.invoid.dokus.fileprovider"
    static let subsystem = Bundle.main.bundleIdentifier ?? fallbackSubsystem

    static let `extension` = Logger(subsystem: subsystem, category: "extension")
    static let domainHealth = Logger(subsystem: subsystem, category: "domain-health")
    static let runtime = Logger(subsystem: subsystem, category: "runtime")
    static let api = Logger(subsystem: subsystem, category: "api")
    static let session = Logger(subsystem: subsystem, category: "session")
}
