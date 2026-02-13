import Foundation
import FileProvider
import OSLog

final class DokusFileProviderDomainRegistrar {
    private enum Constants {
        static let managedDomainIdentifierPrefix = "vision.invoid.dokus.fileprovider"
        static let legacyDomainIdentifier = managedDomainIdentifierPrefix
        static let workspaceDomainSeparator = ".ws."
        static let resolvableFileProviderErrorCodes: [NSFileProviderError.Code] = [
            .notAuthenticated,
            .serverUnreachable,
            .cannotSynchronize
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
            for domain in domains {
                await domainManager.signalEnumerators(for: domain)
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
            for domain in currentManaged {
                await domainManager.signalEnumerators(for: domain)
            }
            return
        }

        switch workspaceState {
        case .signedOut:
            Constants.log.debug("workspace state signedOut removing managed domains")
            for domain in currentManaged {
                await domainManager.remove(domain: domain)
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
                    await domainManager.remove(domain: current)
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

            for desired in desiredDomains {
                await signalResolvableErrorsAsResolved(for: desired)
                await domainManager.signalEnumerators(for: desired)
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

    private func managedDomains() async -> [NSFileProviderDomain] {
        let allDomains = await domainManager.currentDomains()
        return allDomains.filter {
            isManagedDomainIdentifier($0.identifier.rawValue)
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
            let baseName = workspace.name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
                ? "Workspace"
                : workspace.name
            let key = baseName.folding(options: [.caseInsensitive, .diacriticInsensitive], locale: .current)
            let count = (nameCounts[key] ?? 0) + 1
            nameCounts[key] = count

            let displayName = count == 1 ? baseName : "\(baseName) (\(count))"
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

protocol DokusFileProviderDomainManaging {
    func currentDomains() async -> [NSFileProviderDomain]
    func add(domain: NSFileProviderDomain) async
    func remove(domain: NSFileProviderDomain) async
    func signalEnumerators(for domain: NSFileProviderDomain) async
    func signalErrorResolved(for domain: NSFileProviderDomain, error: NSError) async
}

private final class DokusSystemDomainManager: DokusFileProviderDomainManaging {
    func currentDomains() async -> [NSFileProviderDomain] {
        await withCheckedContinuation { continuation in
            NSFileProviderManager.getDomainsWithCompletionHandler { domains, _ in
                continuation.resume(returning: domains)
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

    func signalEnumerators(for domain: NSFileProviderDomain) async {
        guard let manager = NSFileProviderManager(for: domain) else {
            return
        }

        await withCheckedContinuation { continuation in
            manager.signalEnumerator(for: .rootContainer) { _ in
                continuation.resume()
            }
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
}
