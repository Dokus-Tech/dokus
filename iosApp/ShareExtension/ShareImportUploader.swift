import Foundation
import Security

final class ShareImportUploader {
    private enum Constants {
        static let authService = "auth"
        static let accessTokenKey = "auth.access_token"
        static let refreshTokenKey = "auth.refresh_token"
        static let lastSelectedTenantKey = "auth.last_selected_tenant_id"
        static let defaultServerBaseUrl = "https://dokus.invoid.vision"
        static let appGroupIdentifier = "group.vision.invoid.dokus.share"
        static let serverBaseUrlKey = "share.server.base_url"
        static let uploadPrefix = "documents"
        static let tenantClaimKey = "tenant_id"
        static let tokenExpiryClaimKey = "exp"
        static let refreshLeadSeconds: TimeInterval = 60
    }

    private struct TenantInfo {
        let id: String
        let displayName: String
    }

    private struct TokenBundle {
        let accessToken: String
        let refreshToken: String
    }

    struct UploadSessionContext {
        let baseURL: URL
        let accessToken: String
        let workspaceId: String
        let workspaceName: String
    }

    private let keychain: SharedKeychainStore
    private let defaults: UserDefaults?
    private let baseSession: URLSession

    init(appGroupIdentifier: String? = nil, keychainAccessGroup: String? = nil) {
        keychain = SharedKeychainStore(
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
        baseSession = URLSession(configuration: configuration)
    }

    func resolveSessionContext() async throws -> UploadSessionContext {
        guard var accessToken = keychain.string(for: Constants.accessTokenKey) else {
            throw ShareImportFailure(
                type: .notAuthenticated,
                message: "You are logged out. Open Dokus, sign in, and try again.",
                retryable: false
            )
        }

        let refreshToken = keychain.string(for: Constants.refreshTokenKey)
        let lastSelectedTenantId = keychain.string(for: Constants.lastSelectedTenantKey)

        let baseURL = resolvedBaseURL()

        if needsRefresh(for: accessToken), let refreshToken {
            let preferredTenant = tenantIdFromClaims(token: accessToken) ?? lastSelectedTenantId
            let refreshed = try await refreshTokens(
                baseURL: baseURL,
                refreshToken: refreshToken,
                tenantId: preferredTenant
            )
            persist(tokens: refreshed)
            accessToken = refreshed.accessToken
        }

        var tenants: [TenantInfo]
        do {
            tenants = try await fetchTenants(baseURL: baseURL, accessToken: accessToken)
        } catch let failure as ShareImportFailure where failure.type == .notAuthenticated {
            guard let refreshToken else {
                throw failure
            }
            let preferredTenant = tenantIdFromClaims(token: accessToken) ?? lastSelectedTenantId
            let refreshed = try await refreshTokens(
                baseURL: baseURL,
                refreshToken: refreshToken,
                tenantId: preferredTenant
            )
            persist(tokens: refreshed)
            accessToken = refreshed.accessToken
            tenants = try await fetchTenants(baseURL: baseURL, accessToken: accessToken)
        }

        if tenants.isEmpty {
            throw ShareImportFailure(
                type: .workspaceSelectionFailed,
                message: "No workspace is available for this account.",
                retryable: true
            )
        }

        let claimsTenantId = tenantIdFromClaims(token: accessToken)
        let resolvedWorkspace: TenantInfo
        if tenants.count == 1 {
            resolvedWorkspace = tenants[0]
        } else {
            let candidateIds = [claimsTenantId, lastSelectedTenantId].compactMap { $0 }
            guard let match = candidateIds
                .first(where: { candidate in tenants.contains(where: { $0.id == candidate }) })
                .flatMap({ candidate in tenants.first(where: { $0.id == candidate }) }) else {
                throw ShareImportFailure(
                    type: .workspaceContextUnavailable,
                    message: "Unable to resolve workspace for this share. Switch workspace in app and try again.",
                    retryable: true
                )
            }
            resolvedWorkspace = match
        }

        if claimsTenantId != resolvedWorkspace.id {
            let switched = try await selectTenant(
                baseURL: baseURL,
                accessToken: accessToken,
                tenantId: resolvedWorkspace.id
            )
            persist(tokens: switched)
            accessToken = switched.accessToken
        }

        keychain.set(resolvedWorkspace.id, for: Constants.lastSelectedTenantKey)

        return UploadSessionContext(
            baseURL: baseURL,
            accessToken: accessToken,
            workspaceId: resolvedWorkspace.id,
            workspaceName: resolvedWorkspace.displayName
        )
    }

    func upload(
        file: SharedImportFile,
        context: UploadSessionContext,
        onProgress: @escaping (Double) -> Void
    ) async throws -> String? {
        let fileData: Data
        do {
            fileData = try Data(contentsOf: file.fileURL)
        } catch {
            throw ShareImportFailure(
                type: .payloadUnavailable,
                message: "Unable to read the selected PDF file.",
                retryable: true
            )
        }

        let multipart = MultipartBuilder.documentUploadBody(
            fileName: file.name,
            mimeType: file.mimeType,
            fileData: fileData,
            prefix: Constants.uploadPrefix
        )

        var request = URLRequest(url: context.baseURL.appending(path: "/api/v1/documents/upload"))
        request.httpMethod = "POST"
        request.setValue("Bearer \(context.accessToken)", forHTTPHeaderField: "Authorization")
        request.setValue(multipart.contentType, forHTTPHeaderField: "Content-Type")

        let response = try await uploadWithProgress(request: request, body: multipart.body, onProgress: onProgress)
        let data = response.data
        let statusCode = response.response.statusCode

        if statusCode == 401 {
            throw ShareImportFailure(
                type: .notAuthenticated,
                message: "Your session expired. Open Dokus and sign in again.",
                retryable: false
            )
        }

        guard (200...299).contains(statusCode) else {
            let message = parseServerMessage(from: data) ?? "Upload failed. Please try again."
            throw ShareImportFailure(
                type: .upload,
                message: message,
                retryable: true
            )
        }

        return parseDocumentId(from: data)
    }

    private func resolvedBaseURL() -> URL {
        let raw = defaults?
            .string(forKey: Constants.serverBaseUrlKey)?
            .trimmingCharacters(in: .whitespacesAndNewlines)
        let fallback = Constants.defaultServerBaseUrl
        let candidate = raw.flatMap { $0.isEmpty ? nil : $0 } ?? fallback
        return URL(string: candidate) ?? URL(string: fallback)!
    }

    private func fetchTenants(baseURL: URL, accessToken: String) async throws -> [TenantInfo] {
        var request = URLRequest(url: baseURL.appending(path: "/api/v1/tenants"))
        request.httpMethod = "GET"
        request.setValue("Bearer \(accessToken)", forHTTPHeaderField: "Authorization")

        let result: (Data, URLResponse)
        do {
            result = try await baseSession.data(for: request)
        } catch {
            throw ShareImportFailure(
                type: .network,
                message: "Network error while loading workspaces.",
                retryable: true
            )
        }

        guard let httpResponse = result.1 as? HTTPURLResponse else {
            throw ShareImportFailure(
                type: .unknown,
                message: "Invalid server response.",
                retryable: true
            )
        }

        if httpResponse.statusCode == 401 {
            throw ShareImportFailure(
                type: .notAuthenticated,
                message: "Your session expired. Open Dokus and sign in again.",
                retryable: false
            )
        }

        guard (200...299).contains(httpResponse.statusCode) else {
            throw ShareImportFailure(
                type: .workspaceSelectionFailed,
                message: parseServerMessage(from: result.0) ?? "Unable to load workspaces.",
                retryable: true
            )
        }

        return parseTenants(from: result.0)
    }

    private func refreshTokens(
        baseURL: URL,
        refreshToken: String,
        tenantId: String?
    ) async throws -> TokenBundle {
        var request = URLRequest(url: baseURL.appending(path: "/api/v1/identity/refresh"))
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
            result = try await baseSession.data(for: request)
        } catch {
            throw ShareImportFailure(
                type: .network,
                message: "Network error while refreshing session.",
                retryable: true
            )
        }

        guard let httpResponse = result.1 as? HTTPURLResponse else {
            throw ShareImportFailure(
                type: .unknown,
                message: "Invalid session response.",
                retryable: true
            )
        }

        guard (200...299).contains(httpResponse.statusCode) else {
            throw ShareImportFailure(
                type: .notAuthenticated,
                message: "Your session is no longer valid. Open Dokus and sign in.",
                retryable: false
            )
        }

        guard
            let object = (try? JSONSerialization.jsonObject(with: result.0)) as? [String: Any],
            let accessToken = object["accessToken"] as? String,
            let newRefreshToken = object["refreshToken"] as? String
        else {
            throw ShareImportFailure(
                type: .unknown,
                message: "Invalid refresh response.",
                retryable: true
            )
        }

        return TokenBundle(accessToken: accessToken, refreshToken: newRefreshToken)
    }

    private func selectTenant(
        baseURL: URL,
        accessToken: String,
        tenantId: String
    ) async throws -> TokenBundle {
        var request = URLRequest(url: baseURL.appending(path: "/api/v1/account/active-tenant"))
        request.httpMethod = "PUT"
        request.setValue("Bearer \(accessToken)", forHTTPHeaderField: "Authorization")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = try? JSONSerialization.data(withJSONObject: ["tenantId": tenantId])

        let result: (Data, URLResponse)
        do {
            result = try await baseSession.data(for: request)
        } catch {
            throw ShareImportFailure(
                type: .network,
                message: "Network error while selecting workspace.",
                retryable: true
            )
        }

        guard let httpResponse = result.1 as? HTTPURLResponse else {
            throw ShareImportFailure(
                type: .workspaceSelectionFailed,
                message: "Invalid workspace response.",
                retryable: true
            )
        }

        guard (200...299).contains(httpResponse.statusCode) else {
            throw ShareImportFailure(
                type: .workspaceSelectionFailed,
                message: parseServerMessage(from: result.0) ?? "Failed to switch workspace.",
                retryable: true
            )
        }

        guard
            let object = (try? JSONSerialization.jsonObject(with: result.0)) as? [String: Any],
            let newAccessToken = object["accessToken"] as? String,
            let newRefreshToken = object["refreshToken"] as? String
        else {
            throw ShareImportFailure(
                type: .workspaceSelectionFailed,
                message: "Invalid workspace switch response.",
                retryable: true
            )
        }

        return TokenBundle(accessToken: newAccessToken, refreshToken: newRefreshToken)
    }

    private func persist(tokens: TokenBundle) {
        keychain.set(tokens.accessToken, for: Constants.accessTokenKey)
        keychain.set(tokens.refreshToken, for: Constants.refreshTokenKey)
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
            let legacyTenantId = first[Constants.tenantClaimKey] as? String,
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
        if let expDouble = payload[Constants.tokenExpiryClaimKey] as? Double {
            return expDouble <= now + Constants.refreshLeadSeconds
        }
        if let expInt = payload[Constants.tokenExpiryClaimKey] as? Int {
            return TimeInterval(expInt) <= now + Constants.refreshLeadSeconds
        }
        if let expNumber = payload[Constants.tokenExpiryClaimKey] as? NSNumber {
            return expNumber.doubleValue <= now + Constants.refreshLeadSeconds
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

    private func parseTenants(from data: Data) -> [TenantInfo] {
        guard let object = try? JSONSerialization.jsonObject(with: data) else {
            return []
        }

        let array: [[String: Any]]
        if let rootArray = object as? [[String: Any]] {
            array = rootArray
        } else if
            let rootObject = object as? [String: Any],
            let dataArray = rootObject["data"] as? [[String: Any]] {
            array = dataArray
        } else {
            return []
        }

        return array.compactMap { entry in
            guard let id = decodeFlexibleString(entry["id"]) else { return nil }
            let displayName = decodeFlexibleString(entry["displayName"]) ?? decodeFlexibleString(entry["legalName"]) ?? "Workspace"
            return TenantInfo(id: id, displayName: displayName)
        }
    }

    private func parseDocumentId(from data: Data) -> String? {
        guard let object = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else {
            return nil
        }

        if let direct = decodeFlexibleString(object["id"]) {
            return direct
        }

        if
            let document = object["document"] as? [String: Any],
            let nested = decodeFlexibleString(document["id"]) {
            return nested
        }

        return nil
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

    private func parseServerMessage(from data: Data) -> String? {
        guard let object = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else {
            return nil
        }

        if let message = object["message"] as? String, !message.isEmpty {
            return message
        }

        if
            let error = object["error"] as? [String: Any],
            let message = error["message"] as? String,
            !message.isEmpty {
            return message
        }

        return nil
    }

    private func uploadWithProgress(
        request: URLRequest,
        body: Data,
        onProgress: @escaping (Double) -> Void
    ) async throws -> (data: Data, response: HTTPURLResponse) {
        let configuration = URLSessionConfiguration.ephemeral
        configuration.timeoutIntervalForRequest = 60
        configuration.timeoutIntervalForResource = 300
        let delegate = UploadProgressDelegate(onProgress: onProgress)
        let session = URLSession(configuration: configuration, delegate: delegate, delegateQueue: nil)

        return try await withCheckedThrowingContinuation { continuation in
            delegate.onComplete = { result in
                session.finishTasksAndInvalidate()
                continuation.resume(with: result)
            }

            let task = session.uploadTask(with: request, from: body)
            task.resume()
        }
    }
}

private final class UploadProgressDelegate: NSObject, URLSessionTaskDelegate, URLSessionDataDelegate {
    var onProgress: (Double) -> Void
    var onComplete: ((Result<(data: Data, response: HTTPURLResponse), Error>) -> Void)?

    private var responseData = Data()
    private var response: HTTPURLResponse?

    init(onProgress: @escaping (Double) -> Void) {
        self.onProgress = onProgress
    }

    func urlSession(
        _ session: URLSession,
        task: URLSessionTask,
        didSendBodyData bytesSent: Int64,
        totalBytesSent: Int64,
        totalBytesExpectedToSend: Int64
    ) {
        guard totalBytesExpectedToSend > 0 else { return }
        let progress = Double(totalBytesSent) / Double(totalBytesExpectedToSend)
        onProgress(progress.clamped(to: 0...1))
    }

    func urlSession(_ session: URLSession, dataTask: URLSessionDataTask, didReceive data: Data) {
        responseData.append(data)
    }

    func urlSession(
        _ session: URLSession,
        dataTask: URLSessionDataTask,
        didReceive response: URLResponse,
        completionHandler: @escaping (URLSession.ResponseDisposition) -> Void
    ) {
        self.response = response as? HTTPURLResponse
        completionHandler(.allow)
    }

    func urlSession(_ session: URLSession, task: URLSessionTask, didCompleteWithError error: Error?) {
        if let error {
            onComplete?(.failure(error))
            return
        }

        guard let response else {
            onComplete?(.failure(URLError(.badServerResponse)))
            return
        }

        onComplete?(.success((data: responseData, response: response)))
    }
}

private final class SharedKeychainStore {
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
