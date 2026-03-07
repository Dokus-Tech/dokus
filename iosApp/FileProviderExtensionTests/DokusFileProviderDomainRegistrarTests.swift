import XCTest
import FileProvider
import UniformTypeIdentifiers

final class DokusFileProviderDomainRegistrarTests: XCTestCase {
    private let managedPrefix = "vision.invoid.dokus.fileprovider"

    func testRegistersDomainPerWorkspace() async {
        let manager = FakeDomainManager(domains: [])
        let discovery = FakeWorkspaceDiscovery(state: .signedIn([
            DokusWorkspaceDomain(id: "ws-1", name: "Invoid BV"),
            DokusWorkspaceDomain(id: "ws-2", name: "TechFlow BVBA")
        ]))
        let registrar = DokusFileProviderDomainRegistrar(
            domainManager: manager,
            workspaceDiscovery: discovery
        )

        await registrar.synchronizeRegistrationNow()

        XCTAssertEqual(manager.domains.count, 2)
        XCTAssertEqual(Set(manager.domains.map(\.identifier.rawValue)), Set([
            "\(managedPrefix).ws.ws-1",
            "\(managedPrefix).ws.ws-2"
        ]))
        XCTAssertEqual(Set(manager.domains.map(\.displayName)), Set([
            "Dokus — Invoid BV",
            "Dokus — TechFlow BVBA"
        ]))
        XCTAssertEqual(manager.resolvedErrorsByDomainIdentifier["\(managedPrefix).ws.ws-1"]?.count, 4)
        XCTAssertEqual(manager.resolvedErrorsByDomainIdentifier["\(managedPrefix).ws.ws-2"]?.count, 4)
        XCTAssertEqual(Set(manager.signaledWorkingSetDomainIdentifiers), Set([
            "\(managedPrefix).ws.ws-1",
            "\(managedPrefix).ws.ws-2"
        ]))
    }

    func testRemovesRevokedDomains() async {
        let manager = FakeDomainManager(domains: [
            workspaceDomain(id: "ws-1", name: "Invoid BV"),
            workspaceDomain(id: "ws-2", name: "TechFlow BVBA")
        ])
        let discovery = FakeWorkspaceDiscovery(state: .signedIn([
            DokusWorkspaceDomain(id: "ws-1", name: "Invoid BV")
        ]))
        let registrar = DokusFileProviderDomainRegistrar(
            domainManager: manager,
            workspaceDiscovery: discovery
        )

        await registrar.synchronizeRegistrationNow()

        XCTAssertEqual(manager.domains.count, 1)
        XCTAssertEqual(manager.domains.first?.identifier.rawValue, "\(managedPrefix).ws.ws-1")
        XCTAssertTrue(manager.removedDomainIdentifiers.contains("\(managedPrefix).ws.ws-2"))
    }

    func testWorkspaceRenameUpdatesDomainDisplayName() async {
        let manager = FakeDomainManager(domains: [
            workspaceDomain(id: "ws-1", name: "Old Name")
        ])
        let discovery = FakeWorkspaceDiscovery(state: .signedIn([
            DokusWorkspaceDomain(id: "ws-1", name: "New Name")
        ]))
        let registrar = DokusFileProviderDomainRegistrar(
            domainManager: manager,
            workspaceDiscovery: discovery
        )

        await registrar.synchronizeRegistrationNow()

        XCTAssertEqual(manager.domains.count, 1)
        XCTAssertEqual(manager.domains.first?.displayName, "Dokus — New Name")
        XCTAssertTrue(manager.addedDomainIdentifiers.contains("\(managedPrefix).ws.ws-1"))
        XCTAssertFalse(manager.removedDomainIdentifiers.contains("\(managedPrefix).ws.ws-1"))
    }

    func testSignedOutRemovesAllManagedDomains() async {
        let manager = FakeDomainManager(domains: [
            NSFileProviderDomain(
                identifier: NSFileProviderDomainIdentifier("\(managedPrefix).ws.ws-1"),
                displayName: "Invoid BV"
            ),
            NSFileProviderDomain(
                identifier: NSFileProviderDomainIdentifier("com.example.other"),
                displayName: "Other"
            )
        ])
        let discovery = FakeWorkspaceDiscovery(state: .signedOut)
        let registrar = DokusFileProviderDomainRegistrar(
            domainManager: manager,
            workspaceDiscovery: discovery
        )

        await registrar.synchronizeRegistrationNow()

        XCTAssertEqual(manager.domains.count, 1)
        XCTAssertEqual(manager.domains.first?.identifier.rawValue, "com.example.other")
        XCTAssertTrue(manager.removedDomainIdentifiers.contains("\(managedPrefix).ws.ws-1"))
    }

    func testLegacyDomainRemovedDuringMigration() async {
        let manager = FakeDomainManager(domains: [
            NSFileProviderDomain(
                identifier: NSFileProviderDomainIdentifier(managedPrefix),
                displayName: "Dokus"
            )
        ])
        let discovery = FakeWorkspaceDiscovery(state: .signedIn([
            DokusWorkspaceDomain(id: "ws-1", name: "Invoid BV")
        ]))
        let registrar = DokusFileProviderDomainRegistrar(
            domainManager: manager,
            workspaceDiscovery: discovery
        )

        await registrar.synchronizeRegistrationNow()

        XCTAssertTrue(manager.removedDomainIdentifiers.contains(managedPrefix))
        XCTAssertTrue(manager.domains.contains(where: { $0.identifier.rawValue == "\(managedPrefix).ws.ws-1" }))
    }

    func testDuplicateWorkspaceNamesAreDisambiguated() async {
        let manager = FakeDomainManager(domains: [])
        let discovery = FakeWorkspaceDiscovery(state: .signedIn([
            DokusWorkspaceDomain(id: "ws-b", name: "Acme"),
            DokusWorkspaceDomain(id: "ws-a", name: "Acme")
        ]))
        let registrar = DokusFileProviderDomainRegistrar(
            domainManager: manager,
            workspaceDiscovery: discovery
        )

        await registrar.synchronizeRegistrationNow()

        let byId = Dictionary(uniqueKeysWithValues: manager.domains.map { ($0.identifier.rawValue, $0.displayName) })
        XCTAssertEqual(byId["\(managedPrefix).ws.ws-a"], "Dokus — Acme")
        XCTAssertEqual(byId["\(managedPrefix).ws.ws-b"], "Dokus — Acme (2)")
    }

    func testDisconnectedDomainsAreReconnectedBeforeRefresh() async {
        let manager = FakeDomainManager(domainStates: [
            managedDomain(id: "ws-1", name: "Invoid BV", isDisconnected: true)
        ])
        let discovery = FakeWorkspaceDiscovery(state: .signedIn([
            DokusWorkspaceDomain(id: "ws-1", name: "Invoid BV")
        ]))
        let registrar = DokusFileProviderDomainRegistrar(
            domainManager: manager,
            workspaceDiscovery: discovery
        )

        await registrar.synchronizeRegistrationNow()

        XCTAssertEqual(manager.reconnectedDomainIdentifiers, ["\(managedPrefix).ws.ws-1"])
        XCTAssertEqual(manager.signaledWorkingSetDomainIdentifiers, ["\(managedPrefix).ws.ws-1"])
        XCTAssertEqual(manager.resolvedErrorsByDomainIdentifier["\(managedPrefix).ws.ws-1"]?.count, 4)
    }

    func testUserDisabledDomainsAreNotAutoHealed() async {
        let manager = FakeDomainManager(domainStates: [
            managedDomain(id: "ws-1", name: "Invoid BV", isUserEnabled: false)
        ])
        let discovery = FakeWorkspaceDiscovery(state: .signedIn([
            DokusWorkspaceDomain(id: "ws-1", name: "Invoid BV")
        ]))
        let registrar = DokusFileProviderDomainRegistrar(
            domainManager: manager,
            workspaceDiscovery: discovery
        )

        await registrar.synchronizeRegistrationNow()

        XCTAssertTrue(manager.reconnectedDomainIdentifiers.isEmpty)
        XCTAssertTrue(manager.signaledWorkingSetDomainIdentifiers.isEmpty)
        XCTAssertNil(manager.resolvedErrorsByDomainIdentifier["\(managedPrefix).ws.ws-1"])
    }

    private func workspaceDomain(id: String, name: String) -> NSFileProviderDomain {
        NSFileProviderDomain(
            identifier: NSFileProviderDomainIdentifier("\(managedPrefix).ws.\(id)"),
            displayName: name
        )
    }

    private func managedDomain(
        id: String,
        name: String,
        isDisconnected: Bool = false,
        isUserEnabled: Bool = true
    ) -> DokusManagedFileProviderDomain {
        DokusManagedFileProviderDomain(
            domain: workspaceDomain(id: id, name: name),
            isDisconnected: isDisconnected,
            isUserEnabled: isUserEnabled
        )
    }
}

private final class FakeDomainManager: DokusFileProviderDomainManaging {
    private(set) var domainStates: [DokusManagedFileProviderDomain]
    private(set) var addedDomainIdentifiers: [String] = []
    private(set) var removedDomainIdentifiers: [String] = []
    private(set) var signaledWorkingSetDomainIdentifiers: [String] = []
    private(set) var reconnectedDomainIdentifiers: [String] = []
    private(set) var resolvedErrorsByDomainIdentifier: [String: [Int]] = [:]

    var domains: [NSFileProviderDomain] {
        domainStates.map(\.domain)
    }

    init(domains: [NSFileProviderDomain]) {
        self.domainStates = domains.map {
            DokusManagedFileProviderDomain(
                domain: $0,
                isDisconnected: false,
                isUserEnabled: true
            )
        }
    }

    init(domainStates: [DokusManagedFileProviderDomain]) {
        self.domainStates = domainStates
    }

    func currentDomains() async -> [DokusManagedFileProviderDomain] {
        domainStates
    }

    func add(domain: NSFileProviderDomain) async {
        addedDomainIdentifiers.append(domain.identifier.rawValue)
        let existingState = domainStates.first(where: { $0.domain.identifier == domain.identifier })
        domainStates.removeAll(where: { $0.domain.identifier == domain.identifier })
        domainStates.append(
            DokusManagedFileProviderDomain(
                domain: domain,
                isDisconnected: existingState?.isDisconnected ?? false,
                isUserEnabled: existingState?.isUserEnabled ?? true
            )
        )
    }

    func remove(domain: NSFileProviderDomain) async {
        removedDomainIdentifiers.append(domain.identifier.rawValue)
        domainStates.removeAll(where: { $0.domain.identifier == domain.identifier })
    }

    func signalWorkingSet(for domain: NSFileProviderDomain) async {
        signaledWorkingSetDomainIdentifiers.append(domain.identifier.rawValue)
    }

    func signalErrorResolved(for domain: NSFileProviderDomain, error: NSError) async {
        resolvedErrorsByDomainIdentifier[domain.identifier.rawValue, default: []].append(error.code)
    }

    func reconnect(domain: NSFileProviderDomain) async {
        reconnectedDomainIdentifiers.append(domain.identifier.rawValue)
        domainStates = domainStates.map { current in
            guard current.domain.identifier == domain.identifier else {
                return current
            }
            return DokusManagedFileProviderDomain(
                domain: current.domain,
                isDisconnected: false,
                isUserEnabled: current.isUserEnabled
            )
        }
    }
}

private struct FakeWorkspaceDiscovery: DokusWorkspaceDiscovering {
    let state: DokusWorkspaceDiscoveryState
    var error: Error? = nil

    func workspaceState() async throws -> DokusWorkspaceDiscoveryState {
        if let error {
            throw error
        }
        return state
    }
}

final class DokusFileProviderSessionProviderTests: XCTestCase {
    override func tearDown() {
        FileProviderTestURLProtocol.reset()
        super.tearDown()
    }

    func testResolvedSessionRefreshUsesStoredSelectedTenantWhenJwtHasNoTenantClaims() async throws {
        let store = InMemoryStringStore(values: [
            DokusFileProviderConstants.accessTokenKey: makeJwt(expiration: Date().timeIntervalSince1970 - 60),
            DokusFileProviderConstants.refreshTokenKey: "refresh-token",
            DokusFileProviderConstants.lastSelectedTenantKey: "ws-stored"
        ])
        let defaults = makeIsolatedUserDefaults()
        let session = makeInterceptedSession()
        let expectedAccessToken = makeJwt(expiration: Date().timeIntervalSince1970 + 3600)

        FileProviderTestURLProtocol.requestHandler = { request in
            XCTAssertEqual(request.url?.path, "/api/v1/identity/refresh")
            XCTAssertNil(request.value(forHTTPHeaderField: DokusFileProviderConstants.tenantHeaderName))
            let body = try request.jsonBody()
            XCTAssertEqual(body["tenantId"] as? String, "ws-stored")

            return try httpJSONResponse(
                for: request,
                body: [
                    "accessToken": expectedAccessToken,
                    "refreshToken": "refresh-token-2",
                    "selectedTenantId": "ws-stored"
                ]
            )
        }

        let provider = DokusFileProviderSessionProvider(
            keychain: store,
            defaults: defaults,
            session: session
        )

        let resolved = try await provider.resolvedSession(workspaceId: nil)

        XCTAssertEqual(resolved.accessToken, expectedAccessToken)
        XCTAssertEqual(store.string(for: DokusFileProviderConstants.refreshTokenKey), "refresh-token-2")
        XCTAssertEqual(store.string(for: DokusFileProviderConstants.lastSelectedTenantKey), "ws-stored")
    }

    func testResolvedSessionSwitchesWorkspaceUsingStoredSelectionInsteadOfJwtClaims() async throws {
        let store = InMemoryStringStore(values: [
            DokusFileProviderConstants.accessTokenKey: makeJwt(expiration: Date().timeIntervalSince1970 + 3600),
            DokusFileProviderConstants.refreshTokenKey: "refresh-token",
            DokusFileProviderConstants.lastSelectedTenantKey: "ws-old"
        ])
        let defaults = makeIsolatedUserDefaults()
        let session = makeInterceptedSession()
        let expectedAccessToken = makeJwt(expiration: Date().timeIntervalSince1970 + 7200)

        FileProviderTestURLProtocol.requestHandler = { request in
            XCTAssertEqual(request.url?.path, "/api/v1/account/active-tenant")
            XCTAssertEqual(request.httpMethod, "PUT")
            XCTAssertEqual(request.value(forHTTPHeaderField: "Authorization"), "Bearer \(store.string(for: DokusFileProviderConstants.accessTokenKey)!)")
            let body = try request.jsonBody()
            XCTAssertEqual(body["tenantId"] as? String, "ws-target")

            return try httpJSONResponse(
                for: request,
                body: [
                    "accessToken": expectedAccessToken,
                    "refreshToken": "refresh-token-2",
                    "selectedTenantId": "ws-target"
                ]
            )
        }

        let provider = DokusFileProviderSessionProvider(
            keychain: store,
            defaults: defaults,
            session: session
        )

        let resolved = try await provider.resolvedSession(workspaceId: "ws-target")

        XCTAssertEqual(resolved.accessToken, expectedAccessToken)
        XCTAssertEqual(store.string(for: DokusFileProviderConstants.lastSelectedTenantKey), "ws-target")
    }
}

final class DokusWorkspaceDiscoveryClientTests: XCTestCase {
    override func tearDown() {
        FileProviderTestURLProtocol.reset()
        super.tearDown()
    }

    func testWorkspaceDiscoveryRefreshUsesStoredTenantWithoutJwtClaim() async throws {
        let store = InMemoryStringStore(values: [
            "auth.access_token": makeJwt(expiration: Date().timeIntervalSince1970 - 60),
            "auth.refresh_token": "refresh-token",
            "auth.last_selected_tenant_id": "ws-stored"
        ])
        let defaults = makeIsolatedUserDefaults()
        let session = makeInterceptedSession()
        let refreshedAccessToken = makeJwt(expiration: Date().timeIntervalSince1970 + 3600)

        FileProviderTestURLProtocol.requestHandler = { request in
            switch request.url?.path {
            case "/api/v1/identity/refresh":
                let body = try request.jsonBody()
                XCTAssertEqual(body["tenantId"] as? String, "ws-stored")
                return try httpJSONResponse(
                    for: request,
                    body: [
                        "accessToken": refreshedAccessToken,
                        "refreshToken": "refresh-token-2",
                        "selectedTenantId": "ws-stored"
                    ]
                )
            case "/api/v1/tenants":
                XCTAssertNil(request.value(forHTTPHeaderField: DokusFileProviderConstants.tenantHeaderName))
                return try httpJSONResponse(
                    for: request,
                    body: [
                        ["id": "ws-stored", "displayName": "Invoid BV"]
                    ]
                )
            default:
                return try httpDataResponse(for: request, statusCode: 404)
            }
        }

        let client = DokusWorkspaceDiscoveryClient(
            keychain: store,
            defaults: defaults,
            session: session
        )

        let state = try await client.workspaceState()

        switch state {
        case .signedOut:
            XCTFail("Expected signed-in workspace state")
        case .signedIn(let workspaces):
            XCTAssertEqual(workspaces, [DokusWorkspaceDomain(id: "ws-stored", name: "Invoid BV")])
        }
        XCTAssertEqual(store.string(for: "auth.last_selected_tenant_id"), "ws-stored")
    }
}

final class DokusFileProviderAPIClientTests: XCTestCase {
    private let workspaceId = "ws-123"

    override func tearDown() {
        FileProviderTestURLProtocol.reset()
        super.tearDown()
    }

    func testListWorkspacesDoesNotSendTenantHeader() async throws {
        let apiClient = makeAPIClient(selectedTenantId: workspaceId)

        FileProviderTestURLProtocol.requestHandler = { request in
            XCTAssertEqual(request.url?.path, "/api/v1/tenants")
            XCTAssertNil(request.value(forHTTPHeaderField: DokusFileProviderConstants.tenantHeaderName))
            return try httpJSONResponse(
                for: request,
                body: [
                    ["id": "ws-1", "displayName": "Invoid BV", "role": "OWNER"]
                ]
            )
        }

        let workspaces = try await apiClient.listWorkspaces()

        XCTAssertEqual(workspaces, [DokusWorkspace(id: "ws-1", name: "Invoid BV", role: .owner)])
    }

    func testListAllDocumentsSendsTenantHeaderAndParsesCurrentDocumentRecordShape() async throws {
        let apiClient = makeAPIClient(selectedTenantId: workspaceId)

        FileProviderTestURLProtocol.requestHandler = { request in
            XCTAssertEqual(request.url?.path, "/api/v1/documents")
            XCTAssertEqual(request.value(forHTTPHeaderField: DokusFileProviderConstants.tenantHeaderName), self.workspaceId)
            XCTAssertEqual(request.httpMethod, "GET")
            return try httpJSONResponse(
                for: request,
                body: [
                    "items": [
                        [
                            "document": [
                                "id": "doc-1",
                                "filename": "invoice.pdf",
                                "contentType": "application/pdf",
                                "sizeBytes": 1234,
                                "uploadedAt": "2026-03-01T10:00:00",
                                "downloadUrl": "https://files.example.com/doc-1.pdf"
                            ],
                            "draft": [
                                "documentStatus": "CONFIRMED",
                                "documentType": "INVOICE",
                                "direction": "INBOUND",
                                "updatedAt": "2026-03-02T11:00:00",
                                "extractedData": [
                                    "issueDate": "2026-02-28",
                                    "totalAmount": 12345,
                                    "seller": ["name": "Acme BV"],
                                    "invoiceNumber": "INV-2026-001"
                                ]
                            ],
                            "latestIngestion": [
                                "status": "SUCCEEDED",
                                "finishedAt": "2026-03-02T11:00:00"
                            ]
                        ]
                    ],
                    "total": 1,
                    "limit": 200,
                    "offset": 0
                ]
            )
        }

        let records = try await apiClient.listAllDocuments(workspaceId: workspaceId)

        XCTAssertEqual(records.count, 1)
        XCTAssertEqual(records[0].documentId, "doc-1")
        XCTAssertEqual(records[0].workspaceId, workspaceId)
        XCTAssertEqual(records[0].latestIngestionStatus, .succeeded)
        XCTAssertEqual(records[0].draftStatus, .confirmed)
        XCTAssertEqual(records[0].draftType, .invoice)
        XCTAssertEqual(records[0].draftDirection, .inbound)
        XCTAssertEqual(records[0].amountMinor, 12345)
        XCTAssertEqual(records[0].counterpartyName, "Acme BV")
        XCTAssertEqual(records[0].documentNumber, "INV-2026-001")
        XCTAssertNotNil(records[0].issueDate)
        XCTAssertEqual(records[0].downloadURL?.absoluteString, "https://files.example.com/doc-1.pdf")
    }

    func testDownloadDocumentContentRequestSendsTenantHeader() async throws {
        let apiClient = makeAPIClient(selectedTenantId: workspaceId)
        let record = makeRecord(documentId: "doc-content", downloadURL: nil)
        let expectedBytes = Data("%PDF-content".utf8)

        FileProviderTestURLProtocol.requestHandler = { request in
            XCTAssertEqual(request.url?.path, "/api/v1/documents/doc-content/content")
            XCTAssertEqual(request.value(forHTTPHeaderField: DokusFileProviderConstants.tenantHeaderName), self.workspaceId)
            return try httpDataResponse(
                for: request,
                data: expectedBytes,
                headers: ["Content-Type": "application/pdf"]
            )
        }

        let downloadDirectory = try makeTemporaryDirectory()
        let fileURL = try await apiClient.downloadDocument(
            workspaceId: workspaceId,
            record: record,
            temporaryDirectoryURL: downloadDirectory
        )

        XCTAssertEqual(try Data(contentsOf: fileURL), expectedBytes)
    }

    func testDownloadDocumentDetailFallbackSendsTenantHeader() async throws {
        let apiClient = makeAPIClient(selectedTenantId: workspaceId)
        let record = makeRecord(documentId: "doc-detail", downloadURL: nil)
        let expectedBytes = Data("%PDF-detail".utf8)

        FileProviderTestURLProtocol.requestHandler = { request in
            switch (request.url?.host, request.url?.path) {
            case ("api.example.com", "/api/v1/documents/doc-detail/content"):
                XCTAssertEqual(request.value(forHTTPHeaderField: DokusFileProviderConstants.tenantHeaderName), self.workspaceId)
                return try httpDataResponse(for: request, statusCode: 404)
            case ("api.example.com", "/api/v1/documents/doc-detail"):
                XCTAssertEqual(request.value(forHTTPHeaderField: DokusFileProviderConstants.tenantHeaderName), self.workspaceId)
                return try httpJSONResponse(
                    for: request,
                    body: [
                        "document": [
                            "downloadUrl": "https://files.example.com/doc-detail.pdf"
                        ]
                    ]
                )
            case ("files.example.com", "/doc-detail.pdf"):
                return try httpDataResponse(
                    for: request,
                    data: expectedBytes,
                    headers: ["Content-Type": "application/pdf"]
                )
            default:
                return try httpDataResponse(for: request, statusCode: 404)
            }
        }

        let downloadDirectory = try makeTemporaryDirectory()
        let fileURL = try await apiClient.downloadDocument(
            workspaceId: workspaceId,
            record: record,
            temporaryDirectoryURL: downloadDirectory
        )

        XCTAssertEqual(try Data(contentsOf: fileURL), expectedBytes)
    }

    func testUploadDeleteAndThumbnailRequestsSendTenantHeader() async throws {
        let apiClient = makeAPIClient(selectedTenantId: workspaceId)
        let sourceFileURL = FileManager.default.temporaryDirectory
            .appendingPathComponent(UUID().uuidString)
            .appendingPathExtension("pdf")
        try Data("%PDF-upload".utf8).write(to: sourceFileURL)
        defer { try? FileManager.default.removeItem(at: sourceFileURL) }

        FileProviderTestURLProtocol.requestHandler = { request in
            switch (request.httpMethod, request.url?.path) {
            case ("POST", "/api/v1/documents/upload"):
                XCTAssertEqual(request.value(forHTTPHeaderField: DokusFileProviderConstants.tenantHeaderName), self.workspaceId)
                return try httpJSONResponse(
                    for: request,
                    statusCode: 201,
                    body: ["id": "doc-uploaded"]
                )
            case ("DELETE", "/api/v1/documents/doc-uploaded"):
                XCTAssertEqual(request.value(forHTTPHeaderField: DokusFileProviderConstants.tenantHeaderName), self.workspaceId)
                return try httpDataResponse(for: request, statusCode: 204)
            case ("GET", "/api/v1/documents/doc-uploaded/pages/1.png"):
                XCTAssertEqual(request.value(forHTTPHeaderField: DokusFileProviderConstants.tenantHeaderName), self.workspaceId)
                let query = URLComponents(url: try XCTUnwrap(request.url), resolvingAgainstBaseURL: false)
                XCTAssertEqual(query?.queryItems?.first(where: { $0.name == "dpi" })?.value, "150")
                return try httpDataResponse(
                    for: request,
                    data: Data([0x89, 0x50, 0x4E, 0x47]),
                    headers: ["Content-Type": "image/png"]
                )
            default:
                return try httpDataResponse(for: request, statusCode: 404)
            }
        }

        let documentId = try await apiClient.uploadDocument(
            workspaceId: workspaceId,
            from: sourceFileURL,
            filename: "scan.pdf",
            mimeType: "application/pdf"
        )
        XCTAssertEqual(documentId, "doc-uploaded")

        try await apiClient.deleteDocument(workspaceId: workspaceId, documentId: documentId)

        let thumbnail = try await apiClient.fetchThumbnail(
            workspaceId: workspaceId,
            record: makeRecord(documentId: documentId, downloadURL: nil),
            requestedPixelSize: 128
        )
        XCTAssertEqual(thumbnail, Data([0x89, 0x50, 0x4E, 0x47]))
    }

    func testUploadAndDownloadTransfersRegisterTasksAndCompleteProgress() async throws {
        let registrar = FakeTaskRegistrar()
        let apiClient = makeAPIClient(selectedTenantId: workspaceId, taskRegistrar: registrar)
        let downloadProgress = Progress(totalUnitCount: 100)
        let uploadProgress = Progress(totalUnitCount: 100)
        let sourceFileURL = FileManager.default.temporaryDirectory
            .appendingPathComponent(UUID().uuidString)
            .appendingPathExtension("pdf")
        try Data("%PDF-upload".utf8).write(to: sourceFileURL)
        defer { try? FileManager.default.removeItem(at: sourceFileURL) }

        FileProviderTestURLProtocol.requestHandler = { request in
            switch (request.httpMethod, request.url?.path) {
            case ("GET", "/api/v1/documents/doc-download/content"):
                return try httpDataResponse(
                    for: request,
                    data: Data("%PDF-download".utf8),
                    headers: ["Content-Type": "application/pdf"]
                )
            case ("POST", "/api/v1/documents/upload"):
                return try httpJSONResponse(
                    for: request,
                    statusCode: 201,
                    body: ["id": "doc-uploaded"]
                )
            default:
                return try httpDataResponse(for: request, statusCode: 404)
            }
        }

        let downloadDirectory = try makeTemporaryDirectory()
        _ = try await apiClient.downloadDocument(
            workspaceId: workspaceId,
            record: makeRecord(documentId: "doc-download", downloadURL: nil),
            temporaryDirectoryURL: downloadDirectory,
            transfer: DokusFileProviderTransfer(
                itemIdentifier: NSFileProviderItemIdentifier("download-item"),
                progress: downloadProgress
            )
        )

        let uploadedDocumentId = try await apiClient.uploadDocument(
            workspaceId: workspaceId,
            from: sourceFileURL,
            filename: "scan.pdf",
            mimeType: "application/pdf",
            transfer: DokusFileProviderTransfer(
                itemIdentifier: NSFileProviderItemIdentifier("pending-upload"),
                progress: uploadProgress
            )
        )

        XCTAssertEqual(uploadedDocumentId, "doc-uploaded")
        XCTAssertEqual(Set(registrar.registeredIdentifiers), Set(["download-item", "pending-upload"]))
        XCTAssertTrue(registrar.registeredTaskStates.allSatisfy { $0 == URLSessionTask.State.suspended.rawValue })
        XCTAssertEqual(downloadProgress.completedUnitCount, 100)
        XCTAssertEqual(uploadProgress.completedUnitCount, 100)
    }

    private func makeAPIClient(
        selectedTenantId: String?,
        taskRegistrar: DokusFileProviderTaskRegistering? = nil
    ) -> DokusFileProviderAPIClient {
        let store = InMemoryStringStore(values: [
            DokusFileProviderConstants.accessTokenKey: makeJwt(expiration: Date().timeIntervalSince1970 + 3600),
            DokusFileProviderConstants.refreshTokenKey: "refresh-token"
        ])
        if let selectedTenantId {
            store.set(selectedTenantId, for: DokusFileProviderConstants.lastSelectedTenantKey)
        }
        let defaults = makeIsolatedUserDefaults()
        let sessionProvider = DokusFileProviderSessionProvider(
            keychain: store,
            defaults: defaults,
            session: makeInterceptedSession()
        )
        return DokusFileProviderAPIClient(
            sessionProvider: sessionProvider,
            session: makeInterceptedSession(),
            taskRegistrar: taskRegistrar
        )
    }

    private func makeRecord(documentId: String, downloadURL: URL?) -> DokusDocumentRecord {
        DokusDocumentRecord(
            workspaceId: workspaceId,
            documentId: documentId,
            originalFilename: "\(documentId).pdf",
            contentType: "application/pdf",
            sizeBytes: 512,
            uploadedAt: Date(timeIntervalSince1970: 1_700_000_000),
            updatedAt: Date(timeIntervalSince1970: 1_700_000_100),
            downloadURL: downloadURL,
            latestIngestionStatus: .succeeded,
            draftStatus: .confirmed,
            draftType: .invoice,
            draftDirection: .inbound,
            issueDate: nil,
            amountMinor: nil,
            counterpartyName: nil,
            documentNumber: nil
        )
    }
}

final class DokusFileProviderTransferBindingTests: XCTestCase {
    func testTransferBindingCancelsUnderlyingTaskAndTracksProgress() {
        let progress = Progress(totalUnitCount: 100)
        let task = FakeURLSessionTask()
        let binding = DokusFileProviderTransferBinding(progress: progress, task: task)

        task.exposedProgress.totalUnitCount = 100
        task.exposedProgress.completedUnitCount = 45

        XCTAssertEqual(progress.completedUnitCount, 45)

        progress.cancel()

        let timeout = Date().addingTimeInterval(1)
        while !task.cancelled, Date() < timeout {
            RunLoop.current.run(mode: .default, before: Date().addingTimeInterval(0.01))
        }

        XCTAssertTrue(task.cancelled)
        binding.complete()
        XCTAssertEqual(progress.completedUnitCount, 100)
        binding.invalidate()
    }
}

final class DokusFileProviderUnexpectedErrorMappingTests: XCTestCase {
    func testUrlErrorsMapToReachabilityInsteadOfCannotSynchronize() {
        let error = DokusUnexpectedFileProviderError.nsError(
            from: URLError(.notConnectedToInternet)
        )

        XCTAssertEqual(error.domain, NSFileProviderErrorDomain)
        XCTAssertEqual(error.code, NSFileProviderError.serverUnreachable.rawValue)
    }

    func testUnknownErrorsMapToInvalidServerResponse() {
        let error = DokusUnexpectedFileProviderError.nsError(
            from: NSError(domain: "tests.unknown", code: 1, userInfo: [NSLocalizedDescriptionKey: "broken payload"])
        )

        XCTAssertEqual(error.domain, NSFileProviderErrorDomain)
        XCTAssertEqual(error.code, NSFileProviderError.cannotSynchronize.rawValue)
    }
}

final class DokusFileProviderMaterializedContainerTests: XCTestCase {
    func testWorkingSetSignalPolicyMatchesMaterializedParents() async {
        let runtime = DokusFileProviderRuntime(
            apiClient: makeRuntimeAPIClient(),
            domainIdentifier: "tests.materialized.\(UUID().uuidString)",
            workspaceId: nil,
            domainDisplayName: "Dokus"
        )
        let materializedParent = DokusItemIdentifierCodec.encode(
            kind: .typedFolder(workspaceId: "ws-1", folder: .invoicesIn)
        )
        let otherParent = DokusItemIdentifierCodec.encode(
            kind: .typedFolder(workspaceId: "ws-1", folder: .receiptsIn)
        )

        await runtime.replaceMaterializedItems(with: Set([materializedParent]))

        let shouldSignal = await runtime.shouldSignalWorkingSet(currentParentIdentifier: materializedParent)
        let shouldNotSignal = await runtime.shouldSignalWorkingSet(currentParentIdentifier: otherParent)

        XCTAssertTrue(shouldSignal)
        XCTAssertFalse(shouldNotSignal)
    }

    func testMaterializedItemsMarkDocumentsAsCurrent() async {
        let runtime = DokusFileProviderRuntime(
            apiClient: makeRuntimeAPIClient(),
            domainIdentifier: "tests.materialized.\(UUID().uuidString)",
            workspaceId: nil,
            domainDisplayName: "Dokus"
        )
        let document = makeProjectedDocument(
            workspaceId: "ws-1",
            documentId: "doc-1",
            parentIdentifier: DokusItemIdentifierCodec.encode(
                kind: .lifecycleFolder(workspaceId: "ws-1", folder: .inbox)
            )
        )

        await runtime.replaceMaterializedItems(with: Set([document.identifier]))

        let item = await runtime.fileProviderItem(from: document)

        XCTAssertTrue(item.isUploaded)
        XCTAssertTrue(item.isDownloaded)
        XCTAssertTrue(item.isMostRecentVersionDownloaded)
    }

    func testPendingItemsOverrideReportedSyncState() async {
        let runtime = DokusFileProviderRuntime(
            apiClient: makeRuntimeAPIClient(),
            domainIdentifier: "tests.pending.\(UUID().uuidString)",
            workspaceId: nil,
            domainDisplayName: "Dokus"
        )
        let document = makeProjectedDocument(
            workspaceId: "ws-1",
            documentId: "doc-2",
            parentIdentifier: DokusItemIdentifierCodec.encode(
                kind: .typedFolder(workspaceId: "ws-1", folder: .invoicesIn)
            )
        )
        let pendingSnapshot = DokusPendingFileProviderItemState(
            item: DokusFileProviderItem(
                projected: document,
                state: DokusFileProviderItemState(
                    isUploaded: false,
                    isUploading: false,
                    uploadingError: nil,
                    isDownloaded: false,
                    isDownloading: true,
                    downloadingError: NSError(
                        domain: NSFileProviderErrorDomain,
                        code: NSFileProviderError.serverUnreachable.rawValue,
                        userInfo: [NSLocalizedDescriptionKey: "Network unavailable"]
                    ),
                    isMostRecentVersionDownloaded: false
                )
            )
        )

        await runtime.replacePendingItems(with: [pendingSnapshot])

        let item = await runtime.fileProviderItem(from: document)
        let downloadingError = item.downloadingError as NSError?

        XCTAssertFalse(item.isUploaded)
        XCTAssertTrue(item.isDownloading)
        XCTAssertEqual(downloadingError?.domain, NSFileProviderErrorDomain)
        XCTAssertEqual(downloadingError?.code, NSFileProviderError.serverUnreachable.rawValue)
        XCTAssertFalse(item.isMostRecentVersionDownloaded)
    }

    func testWorkingSetIncludesFoldersAndDocuments() async throws {
        let runtime = DokusFileProviderRuntime(
            apiClient: makeWorkingSetRuntimeAPIClient(),
            domainIdentifier: "tests.working-set.\(UUID().uuidString)",
            workspaceId: "ws-1",
            domainDisplayName: "Dokus"
        )

        let workingSetItems = try await runtime.children(for: .workingSet, forceRefresh: true)

        XCTAssertTrue(workingSetItems.contains(where: \.isFolder))
        XCTAssertTrue(workingSetItems.contains(where: { !$0.isFolder }))
    }

    private func makeRuntimeAPIClient() -> DokusFileProviderAPIClient {
        let store = InMemoryStringStore(values: [
            DokusFileProviderConstants.accessTokenKey: makeJwt(expiration: Date().timeIntervalSince1970 + 3600),
            DokusFileProviderConstants.refreshTokenKey: "refresh-token"
        ])
        let defaults = makeIsolatedUserDefaults()
        let sessionProvider = DokusFileProviderSessionProvider(
            keychain: store,
            defaults: defaults,
            session: makeInterceptedSession()
        )
        return DokusFileProviderAPIClient(
            sessionProvider: sessionProvider,
            session: makeInterceptedSession()
        )
    }

    private func makeWorkingSetRuntimeAPIClient() -> DokusFileProviderAPIClient {
        FileProviderTestURLProtocol.requestHandler = { request in
            switch request.url?.path {
            case "/api/v1/tenants":
                return try httpJSONResponse(
                    for: request,
                    body: [
                        ["id": "ws-1", "displayName": "Invoid BV", "role": "OWNER"]
                    ]
                )
            case "/api/v1/documents":
                return try httpJSONResponse(
                    for: request,
                    body: [
                        "items": [
                            [
                                "document": [
                                    "id": "doc-1",
                                    "filename": "invoice.pdf",
                                    "contentType": "application/pdf",
                                    "sizeBytes": 1234,
                                    "uploadedAt": "2026-03-01T10:00:00",
                                    "downloadUrl": "https://files.example.com/doc-1.pdf"
                                ],
                                "draft": [
                                    "documentStatus": "CONFIRMED",
                                    "documentType": "INVOICE",
                                    "direction": "INBOUND",
                                    "updatedAt": "2026-03-02T11:00:00",
                                    "extractedData": [
                                        "seller": ["name": "Acme BV"],
                                        "invoiceNumber": "INV-2026-001"
                                    ]
                                ],
                                "latestIngestion": [
                                    "status": "SUCCEEDED",
                                    "finishedAt": "2026-03-02T11:00:00"
                                ]
                            ]
                        ],
                        "total": 1,
                        "limit": 200,
                        "offset": 0
                    ]
                )
            default:
                return try httpDataResponse(for: request, statusCode: 404)
            }
        }

        let store = InMemoryStringStore(values: [
            DokusFileProviderConstants.accessTokenKey: makeJwt(expiration: Date().timeIntervalSince1970 + 3600),
            DokusFileProviderConstants.refreshTokenKey: "refresh-token",
            DokusFileProviderConstants.lastSelectedTenantKey: "ws-1"
        ])
        let defaults = makeIsolatedUserDefaults()
        let sessionProvider = DokusFileProviderSessionProvider(
            keychain: store,
            defaults: defaults,
            session: makeInterceptedSession()
        )
        return DokusFileProviderAPIClient(
            sessionProvider: sessionProvider,
            session: makeInterceptedSession()
        )
    }
}

private final class InMemoryStringStore: DokusFileProviderStringStore, DokusWorkspaceStringStore {
    private var values: [String: String]

    init(values: [String: String] = [:]) {
        self.values = values
    }

    func string(for key: String) -> String? {
        values[key]
    }

    func set(_ value: String, for key: String) {
        values[key] = value
    }

    func remove(_ key: String) {
        values.removeValue(forKey: key)
    }
}

private final class FakeTaskRegistrar: DokusFileProviderTaskRegistering {
    private(set) var registeredIdentifiers: [String] = []
    private(set) var registeredTaskStates: [Int] = []

    func register(task: URLSessionTask, for itemIdentifier: NSFileProviderItemIdentifier) {
        registeredIdentifiers.append(itemIdentifier.rawValue)
        registeredTaskStates.append(task.state.rawValue)
    }
}

private final class FakeURLSessionTask: URLSessionTask, @unchecked Sendable {
    let exposedProgress = Progress(totalUnitCount: 100)
    private(set) var cancelled = false

    override var progress: Progress {
        exposedProgress
    }

    override func cancel() {
        cancelled = true
    }
}

private final class FileProviderTestURLProtocol: URLProtocol {
    private static let lock = NSLock()
    static var requestHandler: ((URLRequest) throws -> (HTTPURLResponse, Data))?

    override class func canInit(with request: URLRequest) -> Bool {
        true
    }

    override class func canonicalRequest(for request: URLRequest) -> URLRequest {
        request
    }

    override func startLoading() {
        do {
            let handler = Self.currentHandler()
            let (response, data) = try handler(request)
            client?.urlProtocol(self, didReceive: response, cacheStoragePolicy: .notAllowed)
            if !data.isEmpty {
                client?.urlProtocol(self, didLoad: data)
            }
            client?.urlProtocolDidFinishLoading(self)
        } catch {
            client?.urlProtocol(self, didFailWithError: error)
        }
    }

    override func stopLoading() {}

    static func reset() {
        lock.lock()
        defer { lock.unlock() }
        requestHandler = nil
    }

    private static func currentHandler() -> (URLRequest) throws -> (HTTPURLResponse, Data) {
        lock.lock()
        defer { lock.unlock() }
        return requestHandler ?? { request in
            throw NSError(
                domain: NSURLErrorDomain,
                code: NSURLErrorBadServerResponse,
                userInfo: [NSLocalizedDescriptionKey: "Missing request handler for \(request.url?.absoluteString ?? "unknown")"]
            )
        }
    }
}

private func makeInterceptedSession() -> URLSession {
    let configuration = URLSessionConfiguration.ephemeral
    configuration.protocolClasses = [FileProviderTestURLProtocol.self]
    configuration.timeoutIntervalForRequest = 5
    configuration.timeoutIntervalForResource = 5
    return URLSession(configuration: configuration)
}

private func makeIsolatedUserDefaults() -> UserDefaults {
    let suiteName = "tests.fileprovider.\(UUID().uuidString)"
    let defaults = UserDefaults(suiteName: suiteName)!
    defaults.removePersistentDomain(forName: suiteName)
    defaults.set("https://api.example.com", forKey: "share.server.base_url")
    return defaults
}

private func makeTemporaryDirectory() throws -> URL {
    let directory = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString, isDirectory: true)
    try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
    return directory
}

private func httpJSONResponse(
    for request: URLRequest,
    statusCode: Int = 200,
    body: Any
) throws -> (HTTPURLResponse, Data) {
    try httpDataResponse(
        for: request,
        statusCode: statusCode,
        data: JSONSerialization.data(withJSONObject: body),
        headers: ["Content-Type": "application/json"]
    )
}

private func httpDataResponse(
    for request: URLRequest,
    statusCode: Int = 200,
    data: Data = Data(),
    headers: [String: String] = [:]
) throws -> (HTTPURLResponse, Data) {
    let url = try XCTUnwrap(request.url)
    let response = try XCTUnwrap(
        HTTPURLResponse(
            url: url,
            statusCode: statusCode,
            httpVersion: nil,
            headerFields: headers
        )
    )
    return (response, data)
}

private func makeJwt(expiration: TimeInterval) -> String {
    let header = ["alg": "none", "typ": "JWT"]
    let payload: [String: Any] = [
        "sub": "user-id",
        "email": "user@test.dokus",
        "exp": Int(expiration)
    ]
    return [
        base64URLEncodedJSON(header),
        base64URLEncodedJSON(payload),
        "signature"
    ].joined(separator: ".")
}

private func base64URLEncodedJSON(_ object: Any) -> String {
    let data = try! JSONSerialization.data(withJSONObject: object)
    return data.base64EncodedString()
        .replacingOccurrences(of: "+", with: "-")
        .replacingOccurrences(of: "/", with: "_")
        .replacingOccurrences(of: "=", with: "")
}

private func makeProjectedDocument(
    workspaceId: String,
    documentId: String,
    parentIdentifier: NSFileProviderItemIdentifier
) -> DokusProjectedItem {
    DokusProjectedItem(
        identifier: DokusItemIdentifierCodec.encode(
            kind: .document(workspaceId: workspaceId, documentId: documentId)
        ),
        parentIdentifier: parentIdentifier,
        filename: "\(documentId).pdf",
        contentType: .pdf,
        isFolder: false,
        capabilities: [.allowsReading],
        contentPolicy: .inherited,
        documentSize: 1024,
        creationDate: Date(timeIntervalSince1970: 1_700_000_000),
        contentModificationDate: Date(timeIntervalSince1970: 1_700_000_100),
        childItemCount: nil,
        workspaceId: workspaceId,
        documentId: documentId,
        placement: .typed(.invoicesIn)
    )
}

private extension URLRequest {
    func jsonBody() throws -> [String: Any] {
        let data = try requestBodyData()
        return try XCTUnwrap(JSONSerialization.jsonObject(with: data) as? [String: Any])
    }

    func requestBodyData() throws -> Data {
        if let httpBody {
            return httpBody
        }

        let stream = try XCTUnwrap(httpBodyStream)
        stream.open()
        defer { stream.close() }

        var data = Data()
        let bufferSize = 1024
        let buffer = UnsafeMutablePointer<UInt8>.allocate(capacity: bufferSize)
        defer { buffer.deallocate() }

        while stream.hasBytesAvailable {
            let read = stream.read(buffer, maxLength: bufferSize)
            if read < 0 {
                throw stream.streamError ?? NSError(
                    domain: NSURLErrorDomain,
                    code: NSURLErrorUnknown,
                    userInfo: [NSLocalizedDescriptionKey: "Failed reading request body stream"]
                )
            }
            if read == 0 {
                break
            }
            data.append(buffer, count: read)
        }

        return data
    }
}
