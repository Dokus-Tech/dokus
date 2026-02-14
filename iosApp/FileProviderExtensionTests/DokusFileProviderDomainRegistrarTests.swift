import XCTest
import FileProvider

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
        XCTAssertEqual(manager.resolvedErrorsByDomainIdentifier["\(managedPrefix).ws.ws-1"]?.count, 3)
        XCTAssertEqual(manager.resolvedErrorsByDomainIdentifier["\(managedPrefix).ws.ws-2"]?.count, 3)
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

    private func workspaceDomain(id: String, name: String) -> NSFileProviderDomain {
        NSFileProviderDomain(
            identifier: NSFileProviderDomainIdentifier("\(managedPrefix).ws.\(id)"),
            displayName: name
        )
    }
}

private final class FakeDomainManager: DokusFileProviderDomainManaging {
    private(set) var domains: [NSFileProviderDomain]
    private(set) var addedDomainIdentifiers: [String] = []
    private(set) var removedDomainIdentifiers: [String] = []
    private(set) var signaledDomainIdentifiers: [String] = []
    private(set) var resolvedErrorsByDomainIdentifier: [String: [Int]] = [:]

    init(domains: [NSFileProviderDomain]) {
        self.domains = domains
    }

    func currentDomains() async -> [NSFileProviderDomain] {
        domains
    }

    func add(domain: NSFileProviderDomain) async {
        addedDomainIdentifiers.append(domain.identifier.rawValue)
        domains.removeAll(where: { $0.identifier == domain.identifier })
        domains.append(domain)
    }

    func remove(domain: NSFileProviderDomain) async {
        removedDomainIdentifiers.append(domain.identifier.rawValue)
        domains.removeAll(where: { $0.identifier == domain.identifier })
    }

    func signalEnumerators(for domain: NSFileProviderDomain) async {
        signaledDomainIdentifiers.append(domain.identifier.rawValue)
    }

    func signalErrorResolved(for domain: NSFileProviderDomain, error: NSError) async {
        resolvedErrorsByDomainIdentifier[domain.identifier.rawValue, default: []].append(error.code)
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
