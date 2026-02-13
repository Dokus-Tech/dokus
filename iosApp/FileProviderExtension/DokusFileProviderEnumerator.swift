import Foundation
import FileProvider

final class DokusFileProviderEnumerator: NSObject, NSFileProviderEnumerator {
    private let containerItemIdentifier: NSFileProviderItemIdentifier
    private let runtime: DokusFileProviderRuntime
    private let onSuccessfulSync: (() -> Void)?

    init(
        containerItemIdentifier: NSFileProviderItemIdentifier,
        runtime: DokusFileProviderRuntime,
        onSuccessfulSync: (() -> Void)? = nil
    ) {
        self.containerItemIdentifier = containerItemIdentifier
        self.runtime = runtime
        self.onSuccessfulSync = onSuccessfulSync
    }

    func invalidate() {
        DokusFileProviderLog.extension.debug(
            "enumerator invalidate container=\(self.containerItemIdentifier.rawValue, privacy: .public)"
        )
    }

    func enumerateItems(for observer: NSFileProviderEnumerationObserver, startingAt page: NSFileProviderPage) {
        Task {
            do {
                let children = try await runtime.children(
                    for: containerItemIdentifier,
                    forceRefresh: false
                )
                let offset = decodePage(page)
                let suggested = max(observer.suggestedPageSize ?? 100, 1)
                let pageSize = min(max(suggested, 100), 500)
                DokusFileProviderLog.extension.debug(
                    "enumerateItems container=\(self.containerItemIdentifier.rawValue, privacy: .public) offset=\(offset, privacy: .public) pageSize=\(pageSize, privacy: .public) childCount=\(children.count, privacy: .public)"
                )

                guard offset < children.count else {
                    observer.finishEnumerating(upTo: nil)
                    onSuccessfulSync?()
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
                onSuccessfulSync?()
            } catch let error as DokusFileProviderError {
                DokusFileProviderLog.extension.error(
                    "enumerateItems failed container=\(self.containerItemIdentifier.rawValue, privacy: .public) error=\(error.localizedDescription, privacy: .public)"
                )
                observer.finishEnumeratingWithError(error.nsError)
            } catch {
                DokusFileProviderLog.extension.error(
                    "enumerateItems failed container=\(self.containerItemIdentifier.rawValue, privacy: .public) error=\(String(describing: error), privacy: .public)"
                )
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
                DokusFileProviderLog.extension.debug(
                    "enumerateChanges container=\(self.containerItemIdentifier.rawValue, privacy: .public) updated=\(changeSet.updatedIdentifiers.count, privacy: .public) deleted=\(changeSet.deletedIdentifiers.count, privacy: .public)"
                )
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
                onSuccessfulSync?()
            } catch let error as DokusFileProviderError {
                DokusFileProviderLog.extension.error(
                    "enumerateChanges failed container=\(self.containerItemIdentifier.rawValue, privacy: .public) error=\(error.localizedDescription, privacy: .public)"
                )
                observer.finishEnumeratingWithError(error.nsError)
            } catch {
                DokusFileProviderLog.extension.error(
                    "enumerateChanges failed container=\(self.containerItemIdentifier.rawValue, privacy: .public) error=\(String(describing: error), privacy: .public)"
                )
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
            let anchor = try? await runtime.currentAnchor(forceRefresh: false)
            DokusFileProviderLog.extension.debug(
                "currentSyncAnchor container=\(self.containerItemIdentifier.rawValue, privacy: .public) hasAnchor=\((anchor != nil), privacy: .public)"
            )
            completionHandler(anchor ?? nil)
        }
    }

    private func decodePage(_ page: NSFileProviderPage) -> Int {
        let initialByName = NSFileProviderPage(rawValue: NSFileProviderPage.initialPageSortedByName as Data)
        let initialByDate = NSFileProviderPage(rawValue: NSFileProviderPage.initialPageSortedByDate as Data)
        if page == initialByName || page == initialByDate {
            return 0
        }
        let data = page.rawValue as Data
        guard data.count == MemoryLayout<Int32>.size else {
            return 0
        }
        return data.withUnsafeBytes { buffer in
            Int(Int32(bigEndian: buffer.load(as: Int32.self)))
        }
    }

    private func encodePage(_ value: Int) -> NSFileProviderPage {
        var bigEndian = Int32(value).bigEndian
        let data = Data(bytes: &bigEndian, count: MemoryLayout<Int32>.size)
        return NSFileProviderPage(rawValue: data)
    }
}
