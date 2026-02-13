import Foundation
import FileProvider

final class DokusFileProviderDomainRegistrar {
    private enum Constants {
        static let managedDomainIdentifierPrefix = "vision.invoid.dokus.fileprovider"
        static let legacyDomainIdentifier = managedDomainIdentifierPrefix
        static let workspaceDomainSeparator = ".ws."
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

        let workspaceState: DokusWorkspaceDiscoveryState
        do {
            workspaceState = try await workspaceDiscovery.workspaceState()
        } catch {
            for domain in currentManaged {
                await domainManager.signalEnumerators(for: domain)
            }
            return
        }

        switch workspaceState {
        case .signedOut:
            for domain in currentManaged {
                await domainManager.remove(domain: domain)
            }
        case .signedIn(let workspaces):
            let desiredDomains = makeDesiredDomains(from: workspaces)
            let desiredByIdentifier = Dictionary(
                uniqueKeysWithValues: desiredDomains.map { ($0.identifier.rawValue, $0) }
            )
            let currentByIdentifier = Dictionary(
                uniqueKeysWithValues: currentManaged.map { ($0.identifier.rawValue, $0) }
            )

            for current in currentManaged {
                guard let desired = desiredByIdentifier[current.identifier.rawValue] else {
                    await domainManager.remove(domain: current)
                    continue
                }

                if current.displayName != desired.displayName {
                    await domainManager.remove(domain: current)
                }
            }

            for desired in desiredDomains {
                guard let current = currentByIdentifier[desired.identifier.rawValue] else {
                    await domainManager.add(domain: desired)
                    continue
                }

                if current.displayName != desired.displayName {
                    await domainManager.add(domain: desired)
                }
            }

            for desired in desiredDomains {
                await domainManager.signalEnumerators(for: desired)
            }
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
}
