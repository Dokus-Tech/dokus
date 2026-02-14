import Foundation
import Security

struct DokusWorkspaceDomain: Hashable {
    let id: String
    let name: String
}

enum DokusWorkspaceDiscoveryState {
    case signedOut
    case signedIn([DokusWorkspaceDomain])
}

protocol DokusWorkspaceDiscovering {
    func workspaceState() async throws -> DokusWorkspaceDiscoveryState
}

actor DokusWorkspaceDiscoveryClient: DokusWorkspaceDiscovering {
    private enum Constants {
        static let authService = "auth"
        static let accessTokenKey = "auth.access_token"
        static let refreshTokenKey = "auth.refresh_token"
        static let tenantClaimKey = "tenant_id"
        static let tokenExpiryClaimKey = "exp"
        static let defaultServerBaseURL = "https://dokus.invoid.vision"
        static let serverBaseURLKey = "share.server.base_url"
        static let appGroupIdentifier = "group.vision.invoid.dokus.share"
        static let refreshLeadSeconds: TimeInterval = 60
    }

    private struct TokenBundle {
        let accessToken: String
        let refreshToken: String
    }

    private let keychain: DokusWorkspaceKeychainStore
    private let defaults: UserDefaults?
    private let session: URLSession

    init() {
        let appGroupIdentifier = Bundle.main.object(forInfoDictionaryKey: "DokusShareAppGroupIdentifier") as? String
        let keychainAccessGroup = Bundle.main.object(forInfoDictionaryKey: "DokusSharedKeychainAccessGroup") as? String

        keychain = DokusWorkspaceKeychainStore(
            service: Constants.authService,
            accessGroup: keychainAccessGroup
        )
        defaults = UserDefaults(suiteName: appGroupIdentifier ?? Constants.appGroupIdentifier)

        let configuration = URLSessionConfiguration.ephemeral
        configuration.timeoutIntervalForRequest = 60
        configuration.timeoutIntervalForResource = 300
        if let appGroupIdentifier {
            configuration.sharedContainerIdentifier = appGroupIdentifier
        }
        session = URLSession(configuration: configuration)
    }

    func workspaceState() async throws -> DokusWorkspaceDiscoveryState {
        guard var accessToken = keychain.string(for: Constants.accessTokenKey) else {
            return .signedOut
        }

        let refreshToken = keychain.string(for: Constants.refreshTokenKey)
        let baseURL = resolvedBaseURL()

        if needsRefresh(for: accessToken) {
            guard let refreshToken else {
                return .signedOut
            }
            let refreshed = try await refreshTokens(
                baseURL: baseURL,
                refreshToken: refreshToken,
                tenantId: tenantIdFromClaims(token: accessToken)
            )
            persist(tokens: refreshed)
            accessToken = refreshed.accessToken
        }

        do {
            let workspaces = try await listWorkspaces(baseURL: baseURL, accessToken: accessToken)
            return .signedIn(workspaces)
        } catch DiscoveryError.notAuthenticated {
            guard let refreshToken else {
                return .signedOut
            }

            let refreshed = try await refreshTokens(
                baseURL: baseURL,
                refreshToken: refreshToken,
                tenantId: tenantIdFromClaims(token: accessToken)
            )
            persist(tokens: refreshed)
            let workspaces = try await listWorkspaces(baseURL: baseURL, accessToken: refreshed.accessToken)
            return .signedIn(workspaces)
        }
    }

    private func resolvedBaseURL() -> URL {
        let raw = defaults?
            .string(forKey: Constants.serverBaseURLKey)?
            .trimmingCharacters(in: .whitespacesAndNewlines)
        let candidate = (raw?.isEmpty == false ? raw : nil) ?? Constants.defaultServerBaseURL
        return URL(string: candidate) ?? URL(string: Constants.defaultServerBaseURL)!
    }

    private func listWorkspaces(baseURL: URL, accessToken: String) async throws -> [DokusWorkspaceDomain] {
        var request = URLRequest(url: baseURL.dokusAppendingPathQuery("/api/v1/tenants"))
        request.httpMethod = "GET"
        request.setValue("Bearer \(accessToken)", forHTTPHeaderField: "Authorization")

        let (data, response): (Data, URLResponse)
        do {
            (data, response) = try await session.data(for: request)
        } catch {
            throw DiscoveryError.network
        }

        guard let httpResponse = response as? HTTPURLResponse else {
            throw DiscoveryError.invalidResponse
        }

        if httpResponse.statusCode == 401 {
            throw DiscoveryError.notAuthenticated
        }

        guard (200...299).contains(httpResponse.statusCode) else {
            throw DiscoveryError.network
        }

        let rows: [[String: Any]]
        if let array = try? JSONSerialization.jsonObject(with: data) as? [[String: Any]] {
            rows = array
        } else if
            let object = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
            let dataRows = object["data"] as? [[String: Any]] {
            rows = dataRows
        } else {
            throw DiscoveryError.invalidResponse
        }

        return rows.compactMap { row in
            guard let id = decodeFlexibleString(row["id"]) else {
                return nil
            }
            let displayName = decodeFlexibleString(row["displayName"])
                ?? decodeFlexibleString(row["legalName"])
                ?? "Workspace"
            return DokusWorkspaceDomain(id: id, name: displayName)
        }
    }

    private func refreshTokens(
        baseURL: URL,
        refreshToken: String,
        tenantId: String?
    ) async throws -> TokenBundle {
        var request = URLRequest(url: baseURL.dokusAppendingPathQuery("/api/v1/identity/refresh"))
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")

        var payload: [String: Any] = [
            "refreshToken": refreshToken,
            "deviceType": "IOS"
        ]
        if let tenantId, !tenantId.isEmpty {
            payload["tenantId"] = tenantId
        }
        request.httpBody = try? JSONSerialization.data(withJSONObject: payload)

        let (data, response): (Data, URLResponse)
        do {
            (data, response) = try await session.data(for: request)
        } catch {
            throw DiscoveryError.network
        }

        guard let httpResponse = response as? HTTPURLResponse else {
            throw DiscoveryError.invalidResponse
        }
        guard (200...299).contains(httpResponse.statusCode) else {
            throw DiscoveryError.notAuthenticated
        }

        guard
            let object = (try? JSONSerialization.jsonObject(with: data)) as? [String: Any],
            let accessToken = object["accessToken"] as? String,
            let newRefreshToken = object["refreshToken"] as? String
        else {
            throw DiscoveryError.invalidResponse
        }

        return TokenBundle(accessToken: accessToken, refreshToken: newRefreshToken)
    }

    private func persist(tokens: TokenBundle) {
        keychain.set(tokens.accessToken, for: Constants.accessTokenKey)
        keychain.set(tokens.refreshToken, for: Constants.refreshTokenKey)
    }

    private func decodeFlexibleString(_ value: Any?) -> String? {
        if let string = value as? String, !string.isEmpty {
            return string
        }
        if let number = value as? NSNumber {
            return number.stringValue
        }
        if let dictionary = value as? [String: Any] {
            if let nested = dictionary["value"] as? String, !nested.isEmpty {
                return nested
            }
            if let nested = dictionary["id"] as? String, !nested.isEmpty {
                return nested
            }
        }
        return nil
    }

    private func tenantIdFromClaims(token: String) -> String? {
        guard let payload = decodeJwtPayload(token: token) else {
            return nil
        }

        if let tenantId = payload[Constants.tenantClaimKey] as? String, !tenantId.isEmpty {
            return tenantId
        }

        if
            let tenantsString = payload["tenants"] as? String,
            let tenantsData = tenantsString.data(using: .utf8),
            let tenantsObject = try? JSONSerialization.jsonObject(with: tenantsData) as? [[String: Any]],
            let first = tenantsObject.first,
            let tenantId = first[Constants.tenantClaimKey] as? String,
            !tenantId.isEmpty {
            return tenantId
        }

        return nil
    }

    private func needsRefresh(for token: String) -> Bool {
        guard let payload = decodeJwtPayload(token: token) else {
            return true
        }

        let now = Date().timeIntervalSince1970 + Constants.refreshLeadSeconds
        if let exp = payload[Constants.tokenExpiryClaimKey] as? NSNumber {
            return exp.doubleValue <= now
        }
        if let exp = payload[Constants.tokenExpiryClaimKey] as? Double {
            return exp <= now
        }
        if let exp = payload[Constants.tokenExpiryClaimKey] as? Int {
            return Double(exp) <= now
        }
        return true
    }

    private func decodeJwtPayload(token: String) -> [String: Any]? {
        let parts = token.split(separator: ".")
        guard parts.count == 3 else {
            return nil
        }

        var base64 = String(parts[1])
            .replacingOccurrences(of: "-", with: "+")
            .replacingOccurrences(of: "_", with: "/")
        let remainder = base64.count % 4
        if remainder > 0 {
            base64 += String(repeating: "=", count: 4 - remainder)
        }

        guard
            let data = Data(base64Encoded: base64),
            let object = try? JSONSerialization.jsonObject(with: data) as? [String: Any]
        else {
            return nil
        }

        return object
    }
}

private enum DiscoveryError: Error {
    case notAuthenticated
    case network
    case invalidResponse
}

private final class DokusWorkspaceKeychainStore {
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
        let attributes: [String: Any] = [kSecValueData as String: data]
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

private extension URL {
    func dokusAppendingPathQuery(_ path: String) -> URL {
        if #available(iOS 16.0, *) {
            return self.appending(path: path)
        }
        return self.appendingPathComponent(path)
    }
}
