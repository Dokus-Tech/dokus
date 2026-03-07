import Foundation
import FileProvider
import OSLog

final class DokusFileProviderDomainRegistrar {
    private enum Constants {
        static let managedDomainIdentifierPrefix = "vision.invoid.dokus.fileprovider"
        static let legacyDomainIdentifier = managedDomainIdentifierPrefix
        static let workspaceDomainSeparator = ".ws."
        static let providerDisplayPrefix = "Dokus — "
        static let resolvableFileProviderErrorCodes: [NSFileProviderError.Code] = [
            .notAuthenticated,
            .serverUnreachable,
            .cannotSynchronize,
            .excludedFromSync
        ]
        static let log = Logger(subsystem: "vision.invoid.dokus.fileprovider", category: "registrar")
    }

    static let shared = DokusFileProviderDomainRegistrar()

    private let domainManager: DokusFileProviderDomainManaging
    private let workspaceDiscovery: DokusWorkspaceDiscovering

    convenience init() {
        self.init(
            domainManager: DokusSystemDomainManager(),
            workspaceDiscovery: DokusWorkspaceDiscoveryClient()
        )
    }

    init(
        domainManager: DokusFileProviderDomainManaging,
        workspaceDiscovery: DokusWorkspaceDiscovering
    ) {
        self.domainManager = domainManager
        self.workspaceDiscovery = workspaceDiscovery
    }

    func synchronizeRegistration() {
        Task {
            await synchronizeRegistrationNow()
        }
    }

    func signalRefresh() {
        Task {
            let domains = await managedDomains()
            for managed in domains {
                await healAndSignalIfNeeded(managed)
            }
        }
    }

    func synchronizeRegistrationNow() async {
        let currentManaged = await managedDomains()
        Constants.log.debug("synchronizeRegistrationNow currentManagedCount=\(currentManaged.count, privacy: .public)")

        let workspaceState: DokusWorkspaceDiscoveryState
        do {
            workspaceState = try await workspaceDiscovery.workspaceState()
        } catch {
            Constants.log.error("workspace discovery failed error=\(String(describing: error), privacy: .public)")
            for managed in currentManaged {
                guard managed.isUserEnabled else {
                    Constants.log.debug(
                        "skipping refresh for disabled domain id=\(managed.domain.identifier.rawValue, privacy: .public)"
                    )
                    continue
                }
                await domainManager.signalWorkingSet(for: managed.domain)
            }
            return
        }

        switch workspaceState {
        case .signedOut:
            Constants.log.debug("workspace state signedOut removing managed domains")
            for domain in currentManaged {
                await domainManager.remove(domain: domain.domain)
            }
        case .signedIn(let workspaces):
            Constants.log.debug("workspace state signedIn workspaces=\(workspaces.count, privacy: .public)")
            let desiredDomains = makeDesiredDomains(from: workspaces)
            let desiredByIdentifier = Dictionary(
                uniqueKeysWithValues: desiredDomains.map { ($0.identifier.rawValue, $0) }
            )

            for current in currentManaged {
                guard desiredByIdentifier[current.identifier.rawValue] != nil else {
                    Constants.log.debug(
                        "removing stale domain id=\(current.identifier.rawValue, privacy: .public) displayName=\(current.displayName, privacy: .public)"
                    )
                    await domainManager.remove(domain: current.domain)
                    continue
                }
            }

            for desired in desiredDomains {
                // add(domain:) updates display name for existing identifiers.
                Constants.log.debug(
                    "adding/updating domain id=\(desired.identifier.rawValue, privacy: .public) displayName=\(desired.displayName, privacy: .public)"
                )
                await domainManager.add(domain: desired)
            }

            let updatedManaged = await managedDomains()
            let managedByIdentifier = Dictionary(
                uniqueKeysWithValues: updatedManaged.map { ($0.domain.identifier.rawValue, $0) }
            )
            for desired in desiredDomains {
                guard let managed = managedByIdentifier[desired.identifier.rawValue] else {
                    continue
                }
                await healAndSignalIfNeeded(managed)
            }
        }
    }

    private func signalResolvableErrorsAsResolved(for domain: NSFileProviderDomain) async {
        for code in Constants.resolvableFileProviderErrorCodes {
            let error = NSError(
                domain: NSFileProviderErrorDomain,
                code: code.rawValue
            )
            Constants.log.debug(
                "signalErrorResolved domainId=\(domain.identifier.rawValue, privacy: .public) code=\(code.rawValue, privacy: .public)"
            )
            await domainManager.signalErrorResolved(for: domain, error: error)
        }
    }

    private func healAndSignalIfNeeded(_ managed: DokusManagedFileProviderDomain) async {
        let domain = managed.domain
        if !managed.isUserEnabled {
            Constants.log.debug(
                "leaving disabled domain untouched id=\(domain.identifier.rawValue, privacy: .public)"
            )
            return
        }

        if managed.isDisconnected {
            Constants.log.warning(
                "reconnecting disconnected domain id=\(domain.identifier.rawValue, privacy: .public)"
            )
            await domainManager.reconnect(domain: domain)
        }

        await signalResolvableErrorsAsResolved(for: domain)
        await domainManager.signalWorkingSet(for: domain)
    }

    private func managedDomains() async -> [DokusManagedFileProviderDomain] {
        let allDomains = await domainManager.currentDomains()
        return allDomains.filter {
            isManagedDomainIdentifier($0.domain.identifier.rawValue)
        }
    }

    private func isManagedDomainIdentifier(_ identifier: String) -> Bool {
        if identifier == Constants.legacyDomainIdentifier {
            return true
        }
        let prefix = "\(Constants.managedDomainIdentifierPrefix)\(Constants.workspaceDomainSeparator)"
        return identifier.hasPrefix(prefix)
    }

    private func makeDesiredDomains(from workspaces: [DokusWorkspaceDomain]) -> [NSFileProviderDomain] {
        guard !workspaces.isEmpty else {
            return []
        }

        let sorted = workspaces.sorted { lhs, rhs in
            let compare = lhs.name.localizedCaseInsensitiveCompare(rhs.name)
            if compare == .orderedSame {
                return lhs.id < rhs.id
            }
            return compare == .orderedAscending
        }

        var nameCounts: [String: Int] = [:]
        var domains: [NSFileProviderDomain] = []
        domains.reserveCapacity(sorted.count)

        for workspace in sorted {
            let workspaceName = workspace.name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
                ? "Workspace"
                : workspace.name
            let key = workspaceName.folding(options: [.caseInsensitive, .diacriticInsensitive], locale: .current)
            let count = (nameCounts[key] ?? 0) + 1
            nameCounts[key] = count

            let uniqueWorkspaceName = count == 1 ? workspaceName : "\(workspaceName) (\(count))"
            let displayName = "\(Constants.providerDisplayPrefix)\(uniqueWorkspaceName)"
            let identifier = NSFileProviderDomainIdentifier(domainIdentifier(forWorkspaceId: workspace.id))
            let domain = NSFileProviderDomain(identifier: identifier, displayName: displayName)
            domains.append(domain)
        }

        return domains
    }

    private func domainIdentifier(forWorkspaceId workspaceId: String) -> String {
        "\(Constants.managedDomainIdentifierPrefix)\(Constants.workspaceDomainSeparator)\(workspaceId)"
    }
}

struct DokusManagedFileProviderDomain {
    let domain: NSFileProviderDomain
    let isDisconnected: Bool
    let isUserEnabled: Bool

    var identifier: NSFileProviderDomainIdentifier {
        domain.identifier
    }

    var displayName: String {
        domain.displayName
    }
}

protocol DokusFileProviderDomainManaging {
    func currentDomains() async -> [DokusManagedFileProviderDomain]
    func add(domain: NSFileProviderDomain) async
    func remove(domain: NSFileProviderDomain) async
    func signalWorkingSet(for domain: NSFileProviderDomain) async
    func signalErrorResolved(for domain: NSFileProviderDomain, error: NSError) async
    func reconnect(domain: NSFileProviderDomain) async
}

private final class DokusSystemDomainManager: DokusFileProviderDomainManaging {
    func currentDomains() async -> [DokusManagedFileProviderDomain] {
        await withCheckedContinuation { continuation in
            NSFileProviderManager.getDomainsWithCompletionHandler { domains, _ in
                continuation.resume(
                    returning: domains.map {
                        DokusManagedFileProviderDomain(
                            domain: $0,
                            // iOS does not expose disconnected-state inspection on NSFileProviderDomain.
                            isDisconnected: false,
                            isUserEnabled: $0.userEnabled
                        )
                    }
                )
            }
        }
    }

    func add(domain: NSFileProviderDomain) async {
        await withCheckedContinuation { continuation in
            NSFileProviderManager.add(domain) { _ in
                continuation.resume()
            }
        }
    }

    func remove(domain: NSFileProviderDomain) async {
        await withCheckedContinuation { continuation in
            NSFileProviderManager.remove(domain) { _ in
                continuation.resume()
            }
        }
    }

    func signalWorkingSet(for domain: NSFileProviderDomain) async {
        guard let manager = NSFileProviderManager(for: domain) else {
            return
        }

        await withCheckedContinuation { continuation in
            manager.signalEnumerator(for: .workingSet) { _ in
                continuation.resume()
            }
        }
    }

    func signalErrorResolved(for domain: NSFileProviderDomain, error: NSError) async {
        guard let manager = NSFileProviderManager(for: domain) else {
            return
        }

        await withCheckedContinuation { continuation in
            manager.signalErrorResolved(error) { _ in
                continuation.resume()
            }
        }
    }

    func reconnect(domain: NSFileProviderDomain) async {
        // iOS does not expose reconnect on NSFileProviderManager.
        return
    }
}
