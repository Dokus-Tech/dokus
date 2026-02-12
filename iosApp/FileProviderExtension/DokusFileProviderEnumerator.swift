import Foundation
import FileProvider

final class DokusFileProviderEnumerator: NSObject, NSFileProviderEnumerator {
    private let containerItemIdentifier: NSFileProviderItemIdentifier
    private let runtime: DokusFileProviderRuntime

    init(containerItemIdentifier: NSFileProviderItemIdentifier, runtime: DokusFileProviderRuntime) {
        self.containerItemIdentifier = containerItemIdentifier
        self.runtime = runtime
    }

    func invalidate() {
        // No-op. Runtime state is managed by the actor and refreshed lazily.
    }

    func enumerateItems(for observer: NSFileProviderEnumerationObserver, startingAt page: NSFileProviderPage) {
        Task {
            do {
                let children = try await runtime.children(
                    for: containerItemIdentifier,
                    forceRefresh: true
                )
                let offset = decodePage(page)
                let suggested = max(observer.suggestedPageSize, 1)
                let pageSize = min(max(suggested, 100), 500)

                guard offset < children.count else {
                    observer.finishEnumerating(upTo: nil)
                    return
                }

                let end = min(children.count, offset + pageSize)
                let batch = children[offset..<end].map(DokusFileProviderItem.init(projected:))
                observer.didEnumerate(batch)

                if end >= children.count {
                    observer.finishEnumerating(upTo: nil)
                } else {
                    observer.finishEnumerating(upTo: encodePage(end))
                }
            } catch let error as DokusFileProviderError {
                observer.finishEnumeratingWithError(error.nsError)
            } catch {
                observer.finishEnumeratingWithError(
                    NSError(
                        domain: NSFileProviderErrorDomain,
                        code: NSFileProviderError.cannotSynchronize.rawValue,
                        userInfo: [NSLocalizedDescriptionKey: error.localizedDescription]
                    )
                )
            }
        }
    }

    func enumerateChanges(for observer: NSFileProviderChangeObserver, from syncAnchor: NSFileProviderSyncAnchor) {
        Task {
            do {
                let changeSet = try await runtime.changes(from: syncAnchor)
                var updatedItems: [DokusFileProviderItem] = []
                updatedItems.reserveCapacity(changeSet.updatedIdentifiers.count)

                for identifier in changeSet.updatedIdentifiers {
                    if let projected = try? await runtime.item(for: identifier, forceRefresh: false) {
                        updatedItems.append(DokusFileProviderItem(projected: projected))
                    }
                }

                if !updatedItems.isEmpty {
                    observer.didUpdate(updatedItems)
                }
                if !changeSet.deletedIdentifiers.isEmpty {
                    observer.didDeleteItems(withIdentifiers: changeSet.deletedIdentifiers)
                }

                observer.finishEnumeratingChanges(
                    upTo: changeSet.anchor,
                    moreComing: false
                )
            } catch let error as DokusFileProviderError {
                observer.finishEnumeratingWithError(error.nsError)
            } catch {
                observer.finishEnumeratingWithError(
                    NSError(
                        domain: NSFileProviderErrorDomain,
                        code: NSFileProviderError.cannotSynchronize.rawValue,
                        userInfo: [NSLocalizedDescriptionKey: error.localizedDescription]
                    )
                )
            }
        }
    }

    func currentSyncAnchor(completionHandler: @escaping (NSFileProviderSyncAnchor?) -> Void) {
        Task {
            let anchor = try? await runtime.currentAnchor(forceRefresh: true)
            completionHandler(anchor ?? nil)
        }
    }

    private func decodePage(_ page: NSFileProviderPage) -> Int {
        if page == NSFileProviderInitialPageSortedByName || page == NSFileProviderInitialPageSortedByDate {
            return 0
        }
        guard page.count == MemoryLayout<Int32>.size else {
            return 0
        }
        return page.withUnsafeBytes { buffer in
            Int(Int32(bigEndian: buffer.load(as: Int32.self)))
        }
    }

    private func encodePage(_ value: Int) -> NSFileProviderPage {
        var bigEndian = Int32(value).bigEndian
        return Data(bytes: &bigEndian, count: MemoryLayout<Int32>.size)
    }
}
