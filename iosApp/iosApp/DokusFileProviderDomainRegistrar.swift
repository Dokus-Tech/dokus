import Foundation
import FileProvider

final class DokusFileProviderDomainRegistrar {
    private enum Constants {
        static let domainIdentifier = "vision.invoid.dokus.fileprovider"
        static let domainDisplayName = "Dokus"
    }

    static let shared = DokusFileProviderDomainRegistrar()

    private let domain = NSFileProviderDomain(
        identifier: NSFileProviderDomainIdentifier(Constants.domainIdentifier),
        displayName: Constants.domainDisplayName
    )

    private init() {}

    func synchronizeRegistration() {
        Task { [domain] in
            let domains = await currentDomains()
            let isRegistered = domains.contains(where: { $0.identifier == domain.identifier })

            if !isRegistered {
                await addDomain(domain)
            }

            await signalEnumerators(for: domain)
        }
    }

    func signalRefresh() {
        Task { [domain] in
            await signalEnumerators(for: domain)
        }
    }

    private func currentDomains() async -> [NSFileProviderDomain] {
        await withCheckedContinuation { continuation in
            NSFileProviderManager.getDomainsWithCompletionHandler { domains, _ in
                continuation.resume(returning: domains)
            }
        }
    }

    private func addDomain(_ domain: NSFileProviderDomain) async {
        await withCheckedContinuation { continuation in
            NSFileProviderManager.add(domain) { _ in
                continuation.resume()
            }
        }
    }

    private func signalEnumerators(for domain: NSFileProviderDomain) async {
        guard let manager = NSFileProviderManager(for: domain) else { return }

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
