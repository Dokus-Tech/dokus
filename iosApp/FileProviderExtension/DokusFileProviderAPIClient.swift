import Foundation
import UniformTypeIdentifiers

final class DokusFileProviderAPIClient {
    private let sessionProvider: DokusFileProviderSessionProvider
    private let session: URLSession

    init(sessionProvider: DokusFileProviderSessionProvider) {
        self.sessionProvider = sessionProvider

        let configuration = URLSessionConfiguration.ephemeral
        configuration.timeoutIntervalForRequest = 60
        configuration.timeoutIntervalForResource = 300
        self.session = URLSession(configuration: configuration)
    }

    func listWorkspaces() async throws -> [DokusWorkspace] {
        DokusFileProviderLog.api.debug("listWorkspaces started")
        let resolved = try await sessionProvider.resolvedSession(workspaceId: nil)
        var request = URLRequest(url: resolved.baseURL.appendingPathQuery("/api/v1/tenants"))
        request.httpMethod = "GET"
        request.setValue("Bearer \(resolved.accessToken)", forHTTPHeaderField: "Authorization")

        let data = try await dataForRequest(request)
        let json = try parseJsonObject(from: data)

        let rows: [[String: Any]]
        if let array = json as? [[String: Any]] {
            rows = array
        } else if
            let object = json as? [String: Any],
            let dataRows = object["data"] as? [[String: Any]] {
            rows = dataRows
        } else {
            rows = []
        }

        let workspaces: [DokusWorkspace] = rows.compactMap { row -> DokusWorkspace? in
            guard let id = decodeFlexibleString(row["id"]) else { return nil }
            let name = decodeFlexibleString(row["displayName"])
                ?? decodeFlexibleString(row["legalName"])
                ?? "Workspace"
            let role = DokusWorkspaceRole.from(raw: decodeFlexibleString(row["role"]))
            return DokusWorkspace(id: id, name: name, role: role)
        }
        DokusFileProviderLog.api.debug("listWorkspaces completed count=\(workspaces.count, privacy: .public)")
        return workspaces
    }

    func listAllDocuments(workspaceId: String) async throws -> [DokusDocumentRecord] {
        DokusFileProviderLog.api.debug("listAllDocuments started workspaceId=\(workspaceId, privacy: .public)")
        let resolved = try await sessionProvider.resolvedSession(workspaceId: workspaceId)

        var page = 0
        let requestedLimit = 200
        var effectiveLimit = requestedLimit
        var all: [DokusDocumentRecord] = []
        var totalExpected = Int.max

        while all.count < totalExpected {
            var components = URLComponents(url: resolved.baseURL.appendingPathQuery("/api/v1/documents"), resolvingAgainstBaseURL: false)
            components?.queryItems = [
                URLQueryItem(name: "page", value: "\(page)"),
                URLQueryItem(name: "limit", value: "\(requestedLimit)")
            ]

            guard let url = components?.url else {
                throw DokusFileProviderError.invalidServerResponse
            }

            var request = URLRequest(url: url)
            request.httpMethod = "GET"
            request.setValue("Bearer \(resolved.accessToken)", forHTTPHeaderField: "Authorization")

            let data = try await dataForRequest(request)
            guard let object = try parseJsonObject(from: data) as? [String: Any] else {
                throw DokusFileProviderError.invalidServerResponse
            }

            let items = object["items"] as? [[String: Any]] ?? []
            totalExpected = decodeFlexibleInt(object["total"]) ?? items.count
            effectiveLimit = max(decodeFlexibleInt(object["limit"]) ?? effectiveLimit, 1)
            DokusFileProviderLog.api.debug(
                "listAllDocuments page=\(page, privacy: .public) items=\(items.count, privacy: .public) effectiveLimit=\(effectiveLimit, privacy: .public) totalExpected=\(totalExpected, privacy: .public)"
            )
            if items.isEmpty {
                break
            }

            let pageRecords = items.compactMap { parseDocumentRecord(workspaceId: workspaceId, row: $0) }
            all.append(contentsOf: pageRecords)

            if items.count < effectiveLimit {
                break
            }
            page += 1
        }

        DokusFileProviderLog.api.debug("listAllDocuments completed workspaceId=\(workspaceId, privacy: .public) count=\(all.count, privacy: .public)")
        return all
    }

    func uploadDocument(
        workspaceId: String,
        from fileURL: URL,
        filename: String,
        mimeType: String
    ) async throws -> String {
        let resolved = try await sessionProvider.resolvedSession(workspaceId: workspaceId)
        let fileData = try Data(contentsOf: fileURL)

        let multipart = buildMultipartPayload(
            fileName: filename,
            mimeType: mimeType,
            fileData: fileData
        )

        var request = URLRequest(url: resolved.baseURL.appendingPathQuery("/api/v1/documents/upload"))
        request.httpMethod = "POST"
        request.setValue("Bearer \(resolved.accessToken)", forHTTPHeaderField: "Authorization")
        request.setValue(multipart.contentType, forHTTPHeaderField: "Content-Type")
        request.httpBody = multipart.body

        let data = try await dataForRequest(request)
        guard let object = try parseJsonObject(from: data) as? [String: Any] else {
            throw DokusFileProviderError.invalidServerResponse
        }

        if let id = decodeFlexibleString(object["id"]) {
            return id
        }
        if
            let document = object["document"] as? [String: Any],
            let id = decodeFlexibleString(document["id"]) {
            return id
        }
        throw DokusFileProviderError.invalidServerResponse
    }

    func deleteDocument(workspaceId: String, documentId: String) async throws {
        let resolved = try await sessionProvider.resolvedSession(workspaceId: workspaceId)
        let url = resolved.baseURL.appendingPathQuery("/api/v1/documents/\(documentId)")

        var request = URLRequest(url: url)
        request.httpMethod = "DELETE"
        request.setValue("Bearer \(resolved.accessToken)", forHTTPHeaderField: "Authorization")

        _ = try await dataForRequest(request, allowNoContent: true)
    }

    func downloadDocument(
        workspaceId: String,
        record: DokusDocumentRecord,
        temporaryDirectoryURL: URL?
    ) async throws -> URL {
        let resolved = try await sessionProvider.resolvedSession(workspaceId: workspaceId)
        DokusFileProviderLog.api.debug(
            "downloadDocument started workspaceId=\(workspaceId, privacy: .public) documentId=\(record.documentId, privacy: .public)"
        )

        do {
            let data = try await downloadDocumentViaApi(resolved: resolved, record: record)
            DokusFileProviderLog.api.debug(
                "downloadDocument via API content endpoint succeeded documentId=\(record.documentId, privacy: .public) bytes=\(data.count, privacy: .public)"
            )
            return try writeDownloadedData(
                data,
                record: record,
                temporaryDirectoryURL: temporaryDirectoryURL
            )
        } catch let error as DokusFileProviderError {
            guard shouldFallbackToDirectDownload(after: error) else {
                DokusFileProviderLog.api.error(
                    "downloadDocument via API content endpoint failed without fallback documentId=\(record.documentId, privacy: .public) error=\(error.localizedDescription, privacy: .public)"
                )
                throw error
            }
            DokusFileProviderLog.api.warning(
                "downloadDocument via API content endpoint unavailable; falling back documentId=\(record.documentId, privacy: .public) error=\(error.localizedDescription, privacy: .public)"
            )
        } catch {
            DokusFileProviderLog.api.warning(
                "downloadDocument via API content endpoint failed documentId=\(record.documentId, privacy: .public) error=\(String(describing: error), privacy: .public)"
            )
            throw DokusFileProviderError.network(error.localizedDescription)
        }

        let downloadURL = try await resolveDownloadURL(resolved: resolved, record: record)
        let candidates = directDownloadCandidates(from: downloadURL, apiBaseURL: resolved.baseURL)
        var firstRejectedHost: String?
        var lastError: DokusFileProviderError?

        for (index, candidate) in candidates.enumerated() {
            if shouldRejectDirectDownloadURL(candidate, apiBaseURL: resolved.baseURL) {
                let host = candidate.host ?? "unknown"
                if firstRejectedHost == nil {
                    firstRejectedHost = host
                }
                DokusFileProviderLog.api.warning(
                    "downloadDocument skipping direct candidate index=\(index, privacy: .public) host=\(host, privacy: .public) path=\(candidate.path, privacy: .public)"
                )
                continue
            }

            var request = URLRequest(url: candidate)
            request.httpMethod = "GET"
            let attachAuthorization = shouldAttachAuthorization(to: candidate, apiBaseURL: resolved.baseURL)
            if attachAuthorization {
                request.setValue("Bearer \(resolved.accessToken)", forHTTPHeaderField: "Authorization")
            }
            DokusFileProviderLog.api.debug(
                "downloadDocument direct candidate index=\(index, privacy: .public) host=\(candidate.host ?? "nil", privacy: .public) path=\(candidate.path, privacy: .public) attachAuthorization=\(attachAuthorization, privacy: .public)"
            )

            do {
                let data = try await dataForRequest(request)
                DokusFileProviderLog.api.debug(
                    "downloadDocument direct candidate succeeded index=\(index, privacy: .public) documentId=\(record.documentId, privacy: .public) bytes=\(data.count, privacy: .public)"
                )
                return try writeDownloadedData(
                    data,
                    record: record,
                    temporaryDirectoryURL: temporaryDirectoryURL
                )
            } catch let error as DokusFileProviderError {
                lastError = error
                DokusFileProviderLog.api.warning(
                    "downloadDocument direct candidate failed index=\(index, privacy: .public) error=\(error.localizedDescription, privacy: .public)"
                )
                if !shouldRetryDirectDownload(after: error) {
                    throw error
                }
            }
        }

        if let lastError {
            throw lastError
        }

        if let rejectedHost = firstRejectedHost {
            throw DokusFileProviderError.network(
                "Direct download host '\(rejectedHost)' is not reachable from this device"
            )
        }
        throw DokusFileProviderError.network("No reachable direct download candidate")
    }

    func fetchThumbnail(
        workspaceId: String,
        record: DokusDocumentRecord,
        requestedPixelSize: Int
    ) async throws -> Data? {
        let resolved = try await sessionProvider.resolvedSession(workspaceId: workspaceId)

        guard record.contentType.lowercased().contains("pdf") else {
            return nil
        }

        let dpi = normalizedDpi(fromRequestedPixelSize: requestedPixelSize)
        var components = URLComponents(
            url: resolved.baseURL.appendingPathQuery("/api/v1/documents/\(record.documentId)/pages/1.png"),
            resolvingAgainstBaseURL: false
        )
        components?.queryItems = [URLQueryItem(name: "dpi", value: "\(dpi)")]
        guard let url = components?.url else {
            throw DokusFileProviderError.invalidServerResponse
        }

        var request = URLRequest(url: url)
        request.httpMethod = "GET"
        request.setValue("Bearer \(resolved.accessToken)", forHTTPHeaderField: "Authorization")
        DokusFileProviderLog.api.debug(
            "fetchThumbnail request workspaceId=\(workspaceId, privacy: .public) documentId=\(record.documentId, privacy: .public) dpi=\(dpi, privacy: .public)"
        )
        return try await dataForRequest(request)
    }

    private func downloadDocumentViaApi(
        resolved: DokusResolvedSession,
        record: DokusDocumentRecord
    ) async throws -> Data {
        var request = URLRequest(
            url: resolved.baseURL.appendingPathQuery("/api/v1/documents/\(record.documentId)/content")
        )
        request.httpMethod = "GET"
        request.setValue("Bearer \(resolved.accessToken)", forHTTPHeaderField: "Authorization")
        return try await dataForRequest(request)
    }

    private func writeDownloadedData(
        _ data: Data,
        record: DokusDocumentRecord,
        temporaryDirectoryURL: URL?
    ) throws -> URL {
        let suggestedExtension = URL(fileURLWithPath: record.originalFilename).pathExtension
        let fileExtension = suggestedExtension.isEmpty
            ? UTType.fromMimeType(record.contentType).preferredFilenameExtension ?? "bin"
            : suggestedExtension

        let directory = temporaryDirectoryURL ?? URL(fileURLWithPath: NSTemporaryDirectory(), isDirectory: true)
        try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)

        let destination = directory
            .appendingPathComponent(UUID().uuidString)
            .appendingPathExtension(fileExtension)
        try data.write(to: destination, options: .atomic)
        DokusFileProviderLog.api.debug(
            "writeDownloadedData destination=\(destination.path, privacy: .public) bytes=\(data.count, privacy: .public)"
        )
        return destination
    }

    private func resolveDownloadURL(
        resolved: DokusResolvedSession,
        record: DokusDocumentRecord
    ) async throws -> URL {
        if let downloadURL = record.downloadURL {
            DokusFileProviderLog.api.debug(
                "resolveDownloadURL from list documentId=\(record.documentId, privacy: .public) host=\(downloadURL.host ?? "nil", privacy: .public) path=\(downloadURL.path, privacy: .public)"
            )
            return downloadURL
        }

        var request = URLRequest(url: resolved.baseURL.appendingPathQuery("/api/v1/documents/\(record.documentId)"))
        request.httpMethod = "GET"
        request.setValue("Bearer \(resolved.accessToken)", forHTTPHeaderField: "Authorization")

        let data = try await dataForRequest(request)
        guard
            let object = try parseJsonObject(from: data) as? [String: Any],
            let document = object["document"] as? [String: Any],
            let rawDownloadURL = decodeFlexibleString(document["downloadUrl"]),
            let url = URL(string: rawDownloadURL)
        else {
            throw DokusFileProviderError.invalidServerResponse
        }

        DokusFileProviderLog.api.debug(
            "resolveDownloadURL from detail documentId=\(record.documentId, privacy: .public) host=\(url.host ?? "nil", privacy: .public) path=\(url.path, privacy: .public)"
        )
        return url
    }

    private func shouldFallbackToDirectDownload(after error: DokusFileProviderError) -> Bool {
        switch error {
        case .network(let message):
            return message.contains("404") || message.contains("405")
        case .invalidServerResponse:
            return true
        case .notAuthenticated, .unsupportedOperation, .noSuchItem:
            return false
        }
    }

    private func shouldRetryDirectDownload(after error: DokusFileProviderError) -> Bool {
        switch error {
        case .network:
            return true
        case .invalidServerResponse:
            return true
        case .notAuthenticated, .unsupportedOperation, .noSuchItem:
            return false
        }
    }

    private func directDownloadCandidates(from downloadURL: URL, apiBaseURL: URL) -> [URL] {
        var candidates: [URL] = [downloadURL]
        guard
            let downloadHost = downloadURL.host,
            let apiHost = apiBaseURL.host,
            isLikelyLocalOnlyHost(downloadHost)
        else {
            return candidates
        }

        guard var components = URLComponents(url: downloadURL, resolvingAgainstBaseURL: false) else {
            return candidates
        }

        let preferredScheme = apiBaseURL.scheme ?? components.scheme
        let preferredPort = apiBaseURL.port
        components.scheme = preferredScheme
        components.host = apiHost
        components.port = preferredPort
        if let rewritten = components.url {
            candidates.append(rewritten)
        }

        if preferredPort != 9000 {
            components.port = 9000
            if let minioPortURL = components.url {
                candidates.append(minioPortURL)
            }
        }

        var unique: [URL] = []
        var seen = Set<String>()
        for candidate in candidates {
            let key = candidate.absoluteString
            if seen.insert(key).inserted {
                unique.append(candidate)
            }
        }
        return unique
    }

    private func shouldRejectDirectDownloadURL(_ downloadURL: URL, apiBaseURL: URL) -> Bool {
        guard
            let host = downloadURL.host,
            let apiHost = apiBaseURL.host
        else {
            return true
        }
        if host.caseInsensitiveCompare(apiHost) == .orderedSame {
            return false
        }
        if isLikelyLocalOnlyHost(host) {
            return true
        }
        return isLikelyPrivateIPAddress(host)
    }

    private func isLikelyLocalOnlyHost(_ host: String) -> Bool {
        let normalized = host.lowercased()
        if normalized == "localhost" || normalized == "127.0.0.1" || normalized == "::1" {
            return true
        }
        if normalized.hasSuffix(".local") {
            return true
        }
        if !normalized.contains(".") {
            return true
        }
        return isLikelyPrivateIPAddress(normalized)
    }

    private func isLikelyPrivateIPAddress(_ host: String) -> Bool {
        let parts = host.split(separator: ".").compactMap { Int($0) }
        guard parts.count == 4, parts.allSatisfy({ 0...255 ~= $0 }) else {
            return false
        }

        if parts[0] == 10 {
            return true
        }
        if parts[0] == 172, 16...31 ~= parts[1] {
            return true
        }
        if parts[0] == 192, parts[1] == 168 {
            return true
        }
        if parts[0] == 169, parts[1] == 254 {
            return true
        }
        return false
    }

    private func shouldAttachAuthorization(to downloadURL: URL, apiBaseURL: URL) -> Bool {
        guard
            let downloadHost = downloadURL.host?.lowercased(),
            let apiHost = apiBaseURL.host?.lowercased(),
            let downloadScheme = downloadURL.scheme?.lowercased(),
            let apiScheme = apiBaseURL.scheme?.lowercased(),
            downloadHost == apiHost,
            downloadScheme == apiScheme
        else {
            return false
        }

        let downloadPort = normalizedPort(for: downloadURL)
        let apiPort = normalizedPort(for: apiBaseURL)
        guard downloadPort == apiPort else {
            return false
        }

        return !isPresignedDownloadURL(downloadURL)
    }

    private func isPresignedDownloadURL(_ url: URL) -> Bool {
        guard let components = URLComponents(url: url, resolvingAgainstBaseURL: false) else {
            return false
        }

        let signedQueryKeys: Set<String> = [
            "x-amz-algorithm",
            "x-amz-credential",
            "x-amz-date",
            "x-amz-expires",
            "x-amz-signature",
            "x-amz-signedheaders",
            "signature",
            "expires",
            "awsaccesskeyid"
        ]

        for item in components.queryItems ?? [] {
            if signedQueryKeys.contains(item.name.lowercased()) {
                return true
            }
        }
        return false
    }

    private func normalizedPort(for url: URL) -> Int {
        if let explicitPort = url.port {
            return explicitPort
        }
        switch url.scheme?.lowercased() {
        case "https":
            return 443
        case "http":
            return 80
        default:
            return -1
        }
    }

    private func dataForRequest(_ request: URLRequest, allowNoContent: Bool = false) async throws -> Data {
        let method = request.httpMethod ?? "GET"
        let requestURL = request.url
        let requestHost = requestURL?.host ?? "nil"
        let requestPath = requestURL?.path ?? ""
        DokusFileProviderLog.api.debug(
            "HTTP request method=\(method, privacy: .public) host=\(requestHost, privacy: .public) path=\(requestPath, privacy: .public)"
        )

        let response: (Data, URLResponse)
        do {
            response = try await session.data(for: request)
        } catch {
            if let urlError = error as? URLError, urlError.code == .cancelled {
                throw CancellationError()
            }
            if let urlError = error as? URLError {
                DokusFileProviderLog.api.error(
                    "HTTP network failure method=\(method, privacy: .public) host=\(requestHost, privacy: .public) path=\(requestPath, privacy: .public) code=\(urlError.code.rawValue, privacy: .public) message=\(urlError.localizedDescription, privacy: .public)"
                )
            } else {
                DokusFileProviderLog.api.error(
                    "HTTP network failure method=\(method, privacy: .public) host=\(requestHost, privacy: .public) path=\(requestPath, privacy: .public) error=\(String(describing: error), privacy: .public)"
                )
            }
            let nsError = error as NSError
            if nsError.domain == NSCocoaErrorDomain && nsError.code == NSUserCancelledError {
                throw CancellationError()
            }
            throw DokusFileProviderError.network(error.localizedDescription)
        }

        guard let httpResponse = response.1 as? HTTPURLResponse else {
            DokusFileProviderLog.api.error(
                "HTTP invalid response method=\(method, privacy: .public) host=\(requestHost, privacy: .public) path=\(requestPath, privacy: .public)"
            )
            throw DokusFileProviderError.invalidServerResponse
        }
        DokusFileProviderLog.api.debug(
            "HTTP response method=\(method, privacy: .public) host=\(requestHost, privacy: .public) path=\(requestPath, privacy: .public) status=\(httpResponse.statusCode, privacy: .public) bytes=\(response.0.count, privacy: .public)"
        )

        if httpResponse.statusCode == 401 {
            throw DokusFileProviderError.notAuthenticated
        }

        if allowNoContent, httpResponse.statusCode == 204 {
            return Data()
        }

        guard (200...299).contains(httpResponse.statusCode) else {
            let message = parseServerMessage(from: response.0) ?? "Server error \(httpResponse.statusCode)"
            throw DokusFileProviderError.network(message)
        }

        return response.0
    }

    private func normalizedDpi(fromRequestedPixelSize requestedPixelSize: Int) -> Int {
        if requestedPixelSize <= 0 {
            return 150
        }
        if requestedPixelSize <= 96 {
            return 96
        }
        if requestedPixelSize <= 192 {
            return 150
        }
        if requestedPixelSize <= 320 {
            return 200
        }
        return 300
    }

    private func parseJsonObject(from data: Data) throws -> Any {
        guard let object = try? JSONSerialization.jsonObject(with: data) else {
            throw DokusFileProviderError.invalidServerResponse
        }
        return object
    }

    private func parseDocumentRecord(workspaceId: String, row: [String: Any]) -> DokusDocumentRecord? {
        guard let document = row["document"] as? [String: Any] else {
            return nil
        }
        guard
            let documentId = decodeFlexibleString(document["id"]),
            let filename = decodeFlexibleString(document["filename"])
        else {
            return nil
        }

        let contentType = decodeFlexibleString(document["contentType"]) ?? "application/octet-stream"
        let sizeBytes = Int64(decodeFlexibleInt(document["sizeBytes"]) ?? 0)
        let uploadedAt = parseDateTime(decodeFlexibleString(document["uploadedAt"]))
        let downloadURL = decodeFlexibleString(document["downloadUrl"]).flatMap(URL.init(string:))

        let latestIngestion = row["latestIngestion"] as? [String: Any]
        let ingestionStatus = DokusIngestionStatus(rawValue: decodeFlexibleString(latestIngestion?["status"]) ?? "")

        let draft = row["draft"] as? [String: Any]
        let draftStatus = DokusDraftStatus(rawValue: decodeFlexibleString(draft?["documentStatus"]) ?? "")
        let draftType = DokusDocumentType(rawValue: decodeFlexibleString(draft?["documentType"]) ?? "")
        let draftDirection = DokusDocumentDirection(rawValue: decodeFlexibleString(draft?["direction"]) ?? "") ?? .unknown
        let extractedData = draft?["extractedData"] as? [String: Any]

        let issueDate = parseIssueDate(from: extractedData, fallbackUploadedAt: uploadedAt)
        let amountMinor = parseAmountMinor(from: extractedData)
        let counterparty = parseCounterparty(from: extractedData, direction: draftDirection)
        let number = parseDocumentNumber(from: extractedData)
        let updatedAt = parseDateTime(decodeFlexibleString(draft?["updatedAt"]))
            ?? parseDateTime(decodeFlexibleString(latestIngestion?["finishedAt"]))
            ?? uploadedAt

        return DokusDocumentRecord(
            workspaceId: workspaceId,
            documentId: documentId,
            originalFilename: filename,
            contentType: contentType,
            sizeBytes: sizeBytes,
            uploadedAt: uploadedAt,
            updatedAt: updatedAt,
            downloadURL: downloadURL,
            latestIngestionStatus: ingestionStatus,
            draftStatus: draftStatus,
            draftType: draftType,
            draftDirection: draftDirection,
            issueDate: issueDate,
            amountMinor: amountMinor,
            counterpartyName: counterparty,
            documentNumber: number
        )
    }

    private func parseIssueDate(from extractedData: [String: Any]?, fallbackUploadedAt: Date?) -> Date? {
        guard let extractedData else { return fallbackUploadedAt }

        let keys = ["issueDate", "date"]
        for key in keys {
            if let value = decodeFlexibleString(extractedData[key]), let date = parseDate(value) {
                return date
            }
        }
        return fallbackUploadedAt
    }

    private func parseAmountMinor(from extractedData: [String: Any]?) -> Int64? {
        guard let extractedData else { return nil }
        for key in ["totalAmount", "amount", "subtotalAmount"] {
            if let amount = decodeFlexibleInt64(extractedData[key]) {
                return amount
            }
        }
        return nil
    }

    private func parseCounterparty(from extractedData: [String: Any]?, direction: DokusDocumentDirection) -> String? {
        guard let extractedData else { return nil }
        if let direct = decodeFlexibleString(extractedData["counterpartyName"]), !direct.isEmpty {
            return direct
        }
        if let merchant = decodeFlexibleString(extractedData["merchantName"]), !merchant.isEmpty {
            return merchant
        }

        let sellerName = ((extractedData["seller"] as? [String: Any]).flatMap { decodeFlexibleString($0["name"]) }) ?? ""
        let buyerName = ((extractedData["buyer"] as? [String: Any]).flatMap { decodeFlexibleString($0["name"]) }) ?? ""

        switch direction {
        case .inbound:
            return sellerName.isEmpty ? nil : sellerName
        case .outbound:
            return buyerName.isEmpty ? nil : buyerName
        case .unknown:
            if !sellerName.isEmpty { return sellerName }
            if !buyerName.isEmpty { return buyerName }
            return nil
        }
    }

    private func parseDocumentNumber(from extractedData: [String: Any]?) -> String? {
        guard let extractedData else { return nil }
        for key in ["invoiceNumber", "creditNoteNumber", "receiptNumber", "quoteNumber", "proFormaNumber"] {
            if let number = decodeFlexibleString(extractedData[key]), !number.isEmpty {
                return number
            }
        }
        return nil
    }

    private func parseDateTime(_ value: String?) -> Date? {
        guard let value else { return nil }
        if let withFraction = DateFormatter.dokusISODateTime.date(from: value) {
            return withFraction
        }
        if let withoutFraction = DateFormatter.dokusISODateTimeNoFraction.date(from: value) {
            return withoutFraction
        }
        return parseLocalDateTimeWithoutTimeZone(value)
    }

    private func parseLocalDateTimeWithoutTimeZone(_ value: String) -> Date? {
        let normalized = value.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !normalized.isEmpty else { return nil }

        let parts = normalized.split(separator: ".", maxSplits: 1, omittingEmptySubsequences: false)
        let basePart = String(parts[0])
        guard let baseDate = DateFormatter.dokusLocalDateTime.date(from: basePart) else {
            return nil
        }

        guard parts.count == 2 else {
            return baseDate
        }

        let fractionDigits = parts[1].prefix { $0.isNumber }
        guard !fractionDigits.isEmpty else {
            return baseDate
        }

        let digits = String(fractionDigits.prefix(9))
        let padded = digits.padding(toLength: 9, withPad: "0", startingAt: 0)
        guard let nanoseconds = Int(padded) else {
            return baseDate
        }
        return baseDate.addingTimeInterval(Double(nanoseconds) / 1_000_000_000)
    }

    private func parseDate(_ value: String?) -> Date? {
        guard let value else { return nil }
        if let date = DateFormatter.dokusLocalDate.date(from: value) {
            return date
        }
        return parseDateTime(value)
    }

    private func parseServerMessage(from data: Data) -> String? {
        guard let object = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else {
            return nil
        }
        if let message = object["message"] as? String, !message.isEmpty {
            return message
        }
        if let error = object["error"] as? [String: Any], let message = error["message"] as? String, !message.isEmpty {
            return message
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

    private func decodeFlexibleInt(_ value: Any?) -> Int? {
        if let int = value as? Int {
            return int
        }
        if let number = value as? NSNumber {
            return number.intValue
        }
        if let string = value as? String {
            return Int(string)
        }
        return nil
    }

    private func decodeFlexibleInt64(_ value: Any?) -> Int64? {
        if let int64 = value as? Int64 {
            return int64
        }
        if let int = value as? Int {
            return Int64(int)
        }
        if let number = value as? NSNumber {
            return number.int64Value
        }
        if let string = value as? String {
            return Int64(string)
        }
        if let dictionary = value as? [String: Any], let minor = dictionary["minor"] {
            return decodeFlexibleInt64(minor)
        }
        return nil
    }

    private func buildMultipartPayload(fileName: String, mimeType: String, fileData: Data) -> (contentType: String, body: Data) {
        let boundary = "Boundary-\(UUID().uuidString)"
        var body = Data()
        let lineBreak = "\r\n"

        body.append("--\(boundary)\(lineBreak)".data(using: .utf8)!)
        body.append("Content-Disposition: form-data; name=\"file\"; filename=\"\(fileName)\"\(lineBreak)".data(using: .utf8)!)
        body.append("Content-Type: \(mimeType)\(lineBreak)\(lineBreak)".data(using: .utf8)!)
        body.append(fileData)
        body.append(lineBreak.data(using: .utf8)!)

        body.append("--\(boundary)--\(lineBreak)".data(using: .utf8)!)
        return ("multipart/form-data; boundary=\(boundary)", body)
    }
}
