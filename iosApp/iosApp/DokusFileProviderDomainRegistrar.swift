import Foundation
import FileProvider
import Security

final class DokusFileProviderDomainRegistrar {
    private enum Constants {
        static let domainIdentifier = "vision.invoid.dokus.fileprovider"
        static let domainDisplayName = "Dokus"
        static let authService = "auth"
        static let accessTokenKey = "auth.access_token"
    }

    static let shared = DokusFileProviderDomainRegistrar()

    private let keychain: KeychainStore
    private let domain = NSFileProviderDomain(
        identifier: NSFileProviderDomainIdentifier(Constants.domainIdentifier),
        displayName: Constants.domainDisplayName
    )

    private init() {
        let keychainAccessGroup = Bundle.main.object(forInfoDictionaryKey: "DokusSharedKeychainAccessGroup") as? String
        keychain = KeychainStore(
            service: Constants.authService,
            accessGroup: keychainAccessGroup
        )
    }

    func synchronizeRegistration() {
        Task { [domain] in
            let domains = await currentDomains()
            let isRegistered = domains.contains(where: { $0.identifier == domain.identifier })

            if hasActiveSession(), !isRegistered {
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

    private func hasActiveSession() -> Bool {
        guard let accessToken = keychain.string(for: Constants.accessTokenKey) else {
            return false
        }
        return !accessToken.isEmpty
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

private final class KeychainStore {
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
