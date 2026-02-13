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
                var updatedItems: [DokusFileProviderItem] = []
                updatedItems.reserveCapacity(changeSet.updatedIdentifiers.count)
                var deletedIdentifiers: [NSFileProviderItemIdentifier] = []
                var deletedIdentifierSet: Set<NSFileProviderItemIdentifier> = []

                func appendDeleted(_ identifier: NSFileProviderItemIdentifier) {
                    if deletedIdentifierSet.insert(identifier).inserted {
                        deletedIdentifiers.append(identifier)
                    }
                }

                for identifier in changeSet.updatedIdentifiers {
                    let wasInScope = wasInScopeBeforeChange(identifier, changeSet: changeSet)
                    if let projected = try? await runtime.item(for: identifier, forceRefresh: false) {
                        if isInScopeNow(projected) {
                            updatedItems.append(DokusFileProviderItem(projected: projected))
                        } else if wasInScope {
                            appendDeleted(identifier)
                        }
                    } else if wasInScope {
                        appendDeleted(identifier)
                    }
                }

                for identifier in changeSet.deletedIdentifiers where wasInScopeBeforeChange(identifier, changeSet: changeSet) {
                    appendDeleted(identifier)
                }

                DokusFileProviderLog.extension.debug(
                    "enumerateChanges container=\(self.containerItemIdentifier.rawValue, privacy: .public) updated=\(updatedItems.count, privacy: .public) deleted=\(deletedIdentifiers.count, privacy: .public)"
                )

                if !updatedItems.isEmpty {
                    observer.didUpdate(updatedItems)
                }
                if !deletedIdentifiers.isEmpty {
                    observer.didDeleteItems(withIdentifiers: deletedIdentifiers)
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

    private func isInScopeNow(_ item: DokusProjectedItem) -> Bool {
        if containerItemIdentifier == .workingSet {
            return !item.isFolder && item.identifier != .rootContainer
        }
        if containerItemIdentifier == .rootContainer {
            return item.identifier != .rootContainer && item.identifier != .workingSet
        }
        if item.isFolder {
            return isFolderIdentifier(item.identifier, descendantOf: containerItemIdentifier)
        }
        return isFolderIdentifier(item.parentIdentifier, descendantOf: containerItemIdentifier)
    }

    private func wasInScopeBeforeChange(
        _ identifier: NSFileProviderItemIdentifier,
        changeSet: DokusChangeSet
    ) -> Bool {
        let previousWasFolder = changeSet.previousWasFolder(identifier)
        if containerItemIdentifier == .workingSet {
            return previousWasFolder == false
        }
        if containerItemIdentifier == .rootContainer {
            return identifier != .rootContainer && identifier != .workingSet
        }

        if previousWasFolder == true {
            return isFolderIdentifier(identifier, descendantOf: containerItemIdentifier)
        }

        guard let previousParent = changeSet.previousParentIdentifier(for: identifier) else {
            return false
        }
        return isFolderIdentifier(previousParent, descendantOf: containerItemIdentifier)
    }

    private func isFolderIdentifier(
        _ candidate: NSFileProviderItemIdentifier,
        descendantOf container: NSFileProviderItemIdentifier
    ) -> Bool {
        if container == .rootContainer {
            return candidate != .rootContainer && candidate != .workingSet
        }
        if candidate == container {
            return true
        }

        guard
            let candidateKind = DokusItemIdentifierCodec.decode(candidate),
            let containerKind = DokusItemIdentifierCodec.decode(container)
        else {
            return false
        }

        return isIdentifierKind(candidateKind, descendantOf: containerKind)
    }

    private func isIdentifierKind(
        _ candidate: DokusIdentifierKind,
        descendantOf container: DokusIdentifierKind
    ) -> Bool {
        switch (container, candidate) {
        case (.root, _):
            return true
        case (.workspace(let containerWorkspaceId), .workspace(let workspaceId)):
            return workspaceId == containerWorkspaceId
        case (.workspace(let containerWorkspaceId), .lifecycleFolder(let workspaceId, _)):
            return workspaceId == containerWorkspaceId
        case (.workspace(let containerWorkspaceId), .typedFolder(let workspaceId, _)):
            return workspaceId == containerWorkspaceId
        case (.workspace(let containerWorkspaceId), .yearFolder(let workspaceId, _, _)):
            return workspaceId == containerWorkspaceId
        case (.workspace(let containerWorkspaceId), .monthFolder(let workspaceId, _, _, _)):
            return workspaceId == containerWorkspaceId
        case (.workspace(let containerWorkspaceId), .document(let workspaceId, _)):
            return workspaceId == containerWorkspaceId
        case (.lifecycleFolder(let containerWorkspaceId, let containerFolder), .lifecycleFolder(let workspaceId, let folder)):
            return workspaceId == containerWorkspaceId && folder == containerFolder
        case (.typedFolder(let containerWorkspaceId, let containerFolder), .typedFolder(let workspaceId, let folder)):
            return workspaceId == containerWorkspaceId && folder == containerFolder
        case (.typedFolder(let containerWorkspaceId, let containerFolder), .yearFolder(let workspaceId, let folder, _)):
            return workspaceId == containerWorkspaceId && folder == containerFolder
        case (.typedFolder(let containerWorkspaceId, let containerFolder), .monthFolder(let workspaceId, let folder, _, _)):
            return workspaceId == containerWorkspaceId && folder == containerFolder
        case (
            .yearFolder(let containerWorkspaceId, let containerFolder, let containerYear),
            .yearFolder(let workspaceId, let folder, let year)
        ):
            return workspaceId == containerWorkspaceId && folder == containerFolder && year == containerYear
        case (
            .yearFolder(let containerWorkspaceId, let containerFolder, let containerYear),
            .monthFolder(let workspaceId, let folder, let year, _)
        ):
            return workspaceId == containerWorkspaceId && folder == containerFolder && year == containerYear
        case (
            .monthFolder(let containerWorkspaceId, let containerFolder, let containerYear, let containerMonth),
            .monthFolder(let workspaceId, let folder, let year, let month)
        ):
            return workspaceId == containerWorkspaceId &&
                folder == containerFolder &&
                year == containerYear &&
                month == containerMonth
        default:
            return false
        }
    }
}
