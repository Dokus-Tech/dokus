import Foundation

struct DokusResolvedSession {
    let baseURL: URL
    let accessToken: String
}

actor DokusFileProviderSessionProvider {
    private let keychain: DokusSharedKeychainStore
    private let defaults: UserDefaults?
    private let session: URLSession

    init() {
        let appGroupIdentifier = Bundle.main.object(forInfoDictionaryKey: "DokusShareAppGroupIdentifier") as? String
        let keychainAccessGroup = Bundle.main.object(forInfoDictionaryKey: "DokusSharedKeychainAccessGroup") as? String

        keychain = DokusSharedKeychainStore(
            service: DokusFileProviderConstants.authService,
            accessGroup: keychainAccessGroup
        )
        defaults = UserDefaults(suiteName: appGroupIdentifier ?? DokusFileProviderConstants.appGroupIdentifier)

        let configuration = URLSessionConfiguration.ephemeral
        configuration.timeoutIntervalForRequest = 60
        configuration.timeoutIntervalForResource = 300
        if let appGroupIdentifier {
            configuration.sharedContainerIdentifier = appGroupIdentifier
        }
        session = URLSession(configuration: configuration)
    }

    func resolvedSession(workspaceId: String?) async throws -> DokusResolvedSession {
        guard var accessToken = keychain.string(for: DokusFileProviderConstants.accessTokenKey) else {
            DokusFileProviderLog.session.error("resolvedSession failed: missing access token")
            throw DokusFileProviderError.notAuthenticated
        }

        let refreshToken = keychain.string(for: DokusFileProviderConstants.refreshTokenKey)
        let baseURL = resolvedBaseURL()
        DokusFileProviderLog.session.debug(
            "resolvedSession start workspaceId=\(workspaceId ?? "nil", privacy: .public) baseURL=\(baseURL.absoluteString, privacy: .public)"
        )

        if needsRefresh(for: accessToken), let refreshToken {
            DokusFileProviderLog.session.debug("resolvedSession refreshing token")
            let refreshed = try await refreshTokens(
                baseURL: baseURL,
                refreshToken: refreshToken,
                tenantId: workspaceId ?? tenantIdFromClaims(token: accessToken)
            )
            persist(tokens: refreshed)
            accessToken = refreshed.accessToken
            DokusFileProviderLog.session.debug("resolvedSession token refresh succeeded")
        }

        guard let targetWorkspaceId = workspaceId else {
            DokusFileProviderLog.session.debug("resolvedSession returning base session without tenant switch")
            return DokusResolvedSession(baseURL: baseURL, accessToken: accessToken)
        }

        let claimedTenant = tenantIdFromClaims(token: accessToken)
        if claimedTenant == targetWorkspaceId {
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
        persist(tokens: switched)
        keychain.set(targetWorkspaceId, for: DokusFileProviderConstants.lastSelectedTenantKey)
        DokusFileProviderLog.session.debug("resolvedSession tenant switch succeeded tenantId=\(targetWorkspaceId, privacy: .public)")
        return DokusResolvedSession(baseURL: baseURL, accessToken: switched.accessToken)
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

        guard
            let object = (try? JSONSerialization.jsonObject(with: result.0)) as? [String: Any],
            let accessToken = object["accessToken"] as? String,
            let newRefreshToken = object["refreshToken"] as? String
        else {
            throw DokusFileProviderError.invalidServerResponse
        }

        return TokenBundle(accessToken: accessToken, refreshToken: newRefreshToken)
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

        guard
            let object = (try? JSONSerialization.jsonObject(with: result.0)) as? [String: Any],
            let newAccessToken = object["accessToken"] as? String,
            let newRefreshToken = object["refreshToken"] as? String
        else {
            throw DokusFileProviderError.invalidServerResponse
        }

        return TokenBundle(accessToken: newAccessToken, refreshToken: newRefreshToken)
    }

    private func persist(tokens: TokenBundle) {
        keychain.set(tokens.accessToken, for: DokusFileProviderConstants.accessTokenKey)
        keychain.set(tokens.refreshToken, for: DokusFileProviderConstants.refreshTokenKey)
    }

    private func tenantIdFromClaims(token: String) -> String? {
        guard let payload = decodeJwtPayload(token: token) else {
            return nil
        }

        if let tenantId = payload[DokusFileProviderConstants.tenantClaimKey] as? String, !tenantId.isEmpty {
            return tenantId
        }

        if
            let tenantsString = payload["tenants"] as? String,
            let tenantsData = tenantsString.data(using: .utf8),
            let tenantsObject = try? JSONSerialization.jsonObject(with: tenantsData) as? [[String: Any]],
            let first = tenantsObject.first,
            let legacyTenantId = first[DokusFileProviderConstants.tenantClaimKey] as? String,
            !legacyTenantId.isEmpty {
            return legacyTenantId
        }

        return nil
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
}

private struct TokenBundle {
    let accessToken: String
    let refreshToken: String
}
