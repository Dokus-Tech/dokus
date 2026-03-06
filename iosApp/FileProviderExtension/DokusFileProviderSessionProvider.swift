import Foundation

struct DokusResolvedSession {
    let baseURL: URL
    let accessToken: String
}

actor DokusFileProviderSessionProvider {
    private let keychain: DokusFileProviderStringStore
    private let defaults: UserDefaults?
    private let session: URLSession

    init(
        keychain: DokusFileProviderStringStore? = nil,
        defaults: UserDefaults? = nil,
        session: URLSession? = nil
    ) {
        let appGroupIdentifier = Bundle.main.object(forInfoDictionaryKey: "DokusShareAppGroupIdentifier") as? String
        let keychainAccessGroup = Bundle.main.object(forInfoDictionaryKey: "DokusSharedKeychainAccessGroup") as? String

        self.keychain = keychain ?? DokusSharedKeychainStore(
            service: DokusFileProviderConstants.authService,
            accessGroup: keychainAccessGroup
        )
        self.defaults = defaults ?? UserDefaults(
            suiteName: appGroupIdentifier ?? DokusFileProviderConstants.appGroupIdentifier
        )
        self.session = session ?? Self.makeSession(appGroupIdentifier: appGroupIdentifier)
    }

    func resolvedSession(workspaceId: String?) async throws -> DokusResolvedSession {
        guard var accessToken = keychain.string(for: DokusFileProviderConstants.accessTokenKey) else {
            DokusFileProviderLog.session.error("resolvedSession failed: missing access token")
            throw DokusFileProviderError.notAuthenticated
        }

        let refreshToken = keychain.string(for: DokusFileProviderConstants.refreshTokenKey)
        var selectedTenantId = keychain.string(for: DokusFileProviderConstants.lastSelectedTenantKey)
        let baseURL = resolvedBaseURL()
        DokusFileProviderLog.session.debug(
            "resolvedSession start workspaceId=\(workspaceId ?? "nil", privacy: .public) baseURL=\(baseURL.absoluteString, privacy: .public)"
        )

        if needsRefresh(for: accessToken), let refreshToken {
            DokusFileProviderLog.session.debug("resolvedSession refreshing token")
            let refreshTenantId = workspaceId ?? selectedTenantId
            let refreshed = try await refreshTokens(
                baseURL: baseURL,
                refreshToken: refreshToken,
                tenantId: refreshTenantId
            )
            persist(tokens: refreshed, fallbackSelectedTenantId: refreshTenantId)
            accessToken = refreshed.accessToken
            selectedTenantId = refreshed.selectedTenantId ?? refreshTenantId
            DokusFileProviderLog.session.debug("resolvedSession token refresh succeeded")
        }

        guard let targetWorkspaceId = workspaceId else {
            DokusFileProviderLog.session.debug("resolvedSession returning base session without tenant switch")
            return DokusResolvedSession(baseURL: baseURL, accessToken: accessToken)
        }

        if selectedTenantId == targetWorkspaceId {
            DokusFileProviderLog.session.debug("resolvedSession already on tenantId=\(targetWorkspaceId, privacy: .public)")
            return DokusResolvedSession(baseURL: baseURL, accessToken: accessToken)
        }

        guard refreshToken != nil else {
            DokusFileProviderLog.session.error(
                "resolvedSession tenant switch failed: no refresh token tenantId=\(targetWorkspaceId, privacy: .public)"
            )
            throw DokusFileProviderError.notAuthenticated
        }

        DokusFileProviderLog.session.debug("resolvedSession switching tenant to tenantId=\(targetWorkspaceId, privacy: .public)")
        let switched = try await selectTenant(
            baseURL: baseURL,
            accessToken: accessToken,
            tenantId: targetWorkspaceId
        )
        persist(tokens: switched, fallbackSelectedTenantId: targetWorkspaceId)
        DokusFileProviderLog.session.debug("resolvedSession tenant switch succeeded tenantId=\(targetWorkspaceId, privacy: .public)")
        return DokusResolvedSession(baseURL: baseURL, accessToken: switched.accessToken)
    }

    private static func makeSession(appGroupIdentifier: String?) -> URLSession {
        let configuration = URLSessionConfiguration.ephemeral
        configuration.timeoutIntervalForRequest = 60
        configuration.timeoutIntervalForResource = 300
        if let appGroupIdentifier {
            configuration.sharedContainerIdentifier = appGroupIdentifier
        }
        return URLSession(configuration: configuration)
    }

    private func resolvedBaseURL() -> URL {
        let raw = defaults?
            .string(forKey: DokusFileProviderConstants.serverBaseURLKey)?
            .trimmingCharacters(in: .whitespacesAndNewlines)
        let candidate = (raw?.isEmpty == false ? raw : nil) ?? DokusFileProviderConstants.defaultServerBaseURL
        return URL(string: candidate) ?? URL(string: DokusFileProviderConstants.defaultServerBaseURL)!
    }

    private func refreshTokens(
        baseURL: URL,
        refreshToken: String,
        tenantId: String?
    ) async throws -> TokenBundle {
        var request = URLRequest(url: baseURL.appendingPathQuery("/api/v1/identity/refresh"))
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

        let result: (Data, URLResponse)
        do {
            result = try await session.data(for: request)
        } catch {
            DokusFileProviderLog.session.error("refreshTokens network failure error=\(error.localizedDescription, privacy: .public)")
            throw DokusFileProviderError.network("Network unavailable while refreshing authentication")
        }

        guard let httpResponse = result.1 as? HTTPURLResponse else {
            DokusFileProviderLog.session.error("refreshTokens invalid HTTP response")
            throw DokusFileProviderError.invalidServerResponse
        }
        guard (200...299).contains(httpResponse.statusCode) else {
            DokusFileProviderLog.session.error("refreshTokens failed status=\(httpResponse.statusCode, privacy: .public)")
            throw DokusFileProviderError.notAuthenticated
        }

        return try parseTokenBundle(from: result.0)
    }

    private func selectTenant(
        baseURL: URL,
        accessToken: String,
        tenantId: String
    ) async throws -> TokenBundle {
        var request = URLRequest(url: baseURL.appendingPathQuery("/api/v1/account/active-tenant"))
        request.httpMethod = "PUT"
        request.setValue("Bearer \(accessToken)", forHTTPHeaderField: "Authorization")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = try? JSONSerialization.data(withJSONObject: ["tenantId": tenantId])

        let result: (Data, URLResponse)
        do {
            result = try await session.data(for: request)
        } catch {
            DokusFileProviderLog.session.error("selectTenant network failure tenantId=\(tenantId, privacy: .public) error=\(error.localizedDescription, privacy: .public)")
            throw DokusFileProviderError.network("Network unavailable while selecting workspace")
        }

        guard let httpResponse = result.1 as? HTTPURLResponse else {
            DokusFileProviderLog.session.error("selectTenant invalid HTTP response tenantId=\(tenantId, privacy: .public)")
            throw DokusFileProviderError.invalidServerResponse
        }
        guard (200...299).contains(httpResponse.statusCode) else {
            DokusFileProviderLog.session.error(
                "selectTenant failed tenantId=\(tenantId, privacy: .public) status=\(httpResponse.statusCode, privacy: .public)"
            )
            throw DokusFileProviderError.notAuthenticated
        }

        return try parseTokenBundle(from: result.0)
    }

    private func persist(tokens: TokenBundle, fallbackSelectedTenantId: String?) {
        keychain.set(tokens.accessToken, for: DokusFileProviderConstants.accessTokenKey)
        keychain.set(tokens.refreshToken, for: DokusFileProviderConstants.refreshTokenKey)
        let selectedTenantId = tokens.selectedTenantId ?? fallbackSelectedTenantId
        if let selectedTenantId, !selectedTenantId.isEmpty {
            keychain.set(selectedTenantId, for: DokusFileProviderConstants.lastSelectedTenantKey)
        } else {
            keychain.remove(DokusFileProviderConstants.lastSelectedTenantKey)
        }
    }

    private func needsRefresh(for token: String) -> Bool {
        guard let payload = decodeJwtPayload(token: token) else {
            return true
        }

        let now = Date().timeIntervalSince1970
        let threshold = now + DokusFileProviderConstants.refreshLeadSeconds
        if let exp = payload[DokusFileProviderConstants.tokenExpiryClaimKey] as? NSNumber {
            return exp.doubleValue <= threshold
        }
        if let exp = payload[DokusFileProviderConstants.tokenExpiryClaimKey] as? Double {
            return exp <= threshold
        }
        if let exp = payload[DokusFileProviderConstants.tokenExpiryClaimKey] as? Int {
            return Double(exp) <= threshold
        }
        return true
    }

    private func decodeJwtPayload(token: String) -> [String: Any]? {
        let parts = token.split(separator: ".")
        guard parts.count == 3 else { return nil }

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

    private func parseTokenBundle(from data: Data) throws -> TokenBundle {
        guard
            let object = (try? JSONSerialization.jsonObject(with: data)) as? [String: Any],
            let accessToken = object["accessToken"] as? String,
            let refreshToken = object["refreshToken"] as? String
        else {
            throw DokusFileProviderError.invalidServerResponse
        }

        return TokenBundle(
            accessToken: accessToken,
            refreshToken: refreshToken,
            selectedTenantId: decodeFlexibleString(object["selectedTenantId"])
        )
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
}

private struct TokenBundle {
    let accessToken: String
    let refreshToken: String
    let selectedTenantId: String?
}
