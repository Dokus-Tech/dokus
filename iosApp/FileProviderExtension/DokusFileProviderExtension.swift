import Foundation
import FileProvider

final class DokusFileProviderExtension: NSObject, NSFileProviderReplicatedExtension, NSFileProviderThumbnailing {
    private let domain: NSFileProviderDomain
    private let runtime: DokusFileProviderRuntime
    private let manager: NSFileProviderManager?
    private let providerManager: DokusFileProviderManagerHandling?
    private let errorResolver = DokusFileProviderErrorResolver()

    required init(domain: NSFileProviderDomain) {
        self.domain = domain
        let manager = NSFileProviderManager(for: domain)
        self.manager = manager
        let providerManager = manager.map(DokusSystemFileProviderManager.init(manager:))
        self.providerManager = providerManager
        let sessionProvider = DokusFileProviderSessionProvider()
        let apiClient = DokusFileProviderAPIClient(
            sessionProvider: sessionProvider,
            taskRegistrar: providerManager
        )
        let workspaceId = DokusFileProviderConstants.workspaceId(
            fromDomainIdentifier: domain.identifier.rawValue
        )
        self.runtime = DokusFileProviderRuntime(
            apiClient: apiClient,
            domainIdentifier: domain.identifier.rawValue,
            workspaceId: workspaceId,
            domainDisplayName: domain.displayName
        )
        super.init()
        DokusFileProviderLog.extension.debug(
            "init domainId=\(domain.identifier.rawValue, privacy: .public) displayName=\(domain.displayName, privacy: .public) workspaceId=\(workspaceId ?? "nil", privacy: .public)"
        )
    }

    func invalidate() {
        DokusFileProviderLog.extension.debug("invalidate domainId=\(self.domain.identifier.rawValue, privacy: .public)")
    }

    func enumerator(
        for containerItemIdentifier: NSFileProviderItemIdentifier,
        request: NSFileProviderRequest
    ) throws -> NSFileProviderEnumerator {
        DokusFileProviderLog.extension.debug(
            "enumerator requested container=\(containerItemIdentifier.rawValue, privacy: .public)"
        )
        return DokusFileProviderEnumerator(
            containerItemIdentifier: containerItemIdentifier,
            runtime: runtime,
            onSuccessfulSync: { [weak self] in
                self?.resolveDomainErrorsIfNeeded(source: "enumerator")
            }
        )
    }

    func item(
        for identifier: NSFileProviderItemIdentifier,
        request: NSFileProviderRequest,
        completionHandler: @escaping (NSFileProviderItem?, Error?) -> Void
    ) -> Progress {
        let progress = Progress(totalUnitCount: 100)
        let task = Task {
            do {
                DokusFileProviderLog.extension.debug("item request identifier=\(identifier.rawValue, privacy: .public)")
                let item = try await runtime.fileProviderItem(for: identifier, forceRefresh: false)
                guard !progress.isCancelled else {
                    completionHandler(nil, NSError(domain: NSCocoaErrorDomain, code: NSUserCancelledError))
                    return
                }
                progress.completedUnitCount = 100
                completionHandler(item, nil)
            } catch let error as DokusFileProviderError {
                DokusFileProviderLog.extension.error(
                    "item request failed identifier=\(identifier.rawValue, privacy: .public) error=\(error.localizedDescription, privacy: .public)"
                )
                completionHandler(nil, error.nsError)
            } catch {
                DokusFileProviderLog.extension.error(
                    "item request failed identifier=\(identifier.rawValue, privacy: .public) error=\(String(describing: error), privacy: .public)"
                )
                completionHandler(nil, DokusUnexpectedFileProviderError.nsError(from: error))
            }
        }
        let existingCancellation = progress.cancellationHandler
        progress.cancellationHandler = {
            existingCancellation?()
            task.cancel()
        }
        return progress
    }

    func fetchContents(
        for itemIdentifier: NSFileProviderItemIdentifier,
        version requestedVersion: NSFileProviderItemVersion?,
        request: NSFileProviderRequest,
        completionHandler: @escaping (URL?, NSFileProviderItem?, Error?) -> Void
    ) -> Progress {
        let progress = Progress(totalUnitCount: 100)
        let task = Task {
            do {
                DokusFileProviderLog.extension.debug("fetchContents request identifier=\(itemIdentifier.rawValue, privacy: .public)")
                let item = try await runtime.fileProviderItem(for: itemIdentifier, forceRefresh: false)
                let temporaryDirectoryURL = fileProviderTemporaryDirectoryURL()
                let fileURL = try await runtime.fetchContents(
                    itemIdentifier: itemIdentifier,
                    temporaryDirectoryURL: temporaryDirectoryURL,
                    transfer: DokusFileProviderTransfer(
                        itemIdentifier: itemIdentifier,
                        progress: progress
                    )
                )
                guard !progress.isCancelled else {
                    completionHandler(nil, nil, NSError(domain: NSCocoaErrorDomain, code: NSUserCancelledError))
                    return
                }
                completionHandler(fileURL, item, nil)
                resolveDomainErrorsIfNeeded(source: "fetchContents")
            } catch is CancellationError {
                completionHandler(nil, nil, NSError(domain: NSCocoaErrorDomain, code: NSUserCancelledError))
            } catch let error as DokusFileProviderError {
                DokusFileProviderLog.extension.error(
                    "fetchContents failed identifier=\(itemIdentifier.rawValue, privacy: .public) error=\(error.localizedDescription, privacy: .public)"
                )
                completionHandler(nil, nil, error.nsError)
            } catch {
                DokusFileProviderLog.extension.error(
                    "fetchContents failed identifier=\(itemIdentifier.rawValue, privacy: .public) error=\(String(describing: error), privacy: .public)"
                )
                completionHandler(nil, nil, DokusUnexpectedFileProviderError.nsError(from: error))
            }
        }
        let existingCancellation = progress.cancellationHandler
        progress.cancellationHandler = {
            existingCancellation?()
            task.cancel()
        }
        return progress
    }

    func fetchThumbnails(
        for itemIdentifiers: [NSFileProviderItemIdentifier],
        requestedSize size: CGSize,
        perThumbnailCompletionHandler: @escaping (NSFileProviderItemIdentifier, Data?, Error?) -> Void,
        completionHandler: @escaping (Error?) -> Void
    ) -> Progress {
        let progress = Progress(totalUnitCount: Int64(itemIdentifiers.count))
        let requestedPixelSize = max(Int(max(size.width, size.height)), 0)
        DokusFileProviderLog.extension.debug(
            "fetchThumbnails request count=\(itemIdentifiers.count, privacy: .public) requestedPixelSize=\(requestedPixelSize, privacy: .public)"
        )

        let task = Task {
            for identifier in itemIdentifiers {
                if Task.isCancelled {
                    perThumbnailCompletionHandler(
                        identifier,
                        nil,
                        NSError(domain: NSCocoaErrorDomain, code: NSUserCancelledError)
                    )
                    continue
                }

                do {
                    let thumbnailData = try await runtime.fetchThumbnail(
                        itemIdentifier: identifier,
                        requestedPixelSize: requestedPixelSize
                    )
                    perThumbnailCompletionHandler(identifier, thumbnailData, nil)
                } catch let error as DokusFileProviderError {
                    DokusFileProviderLog.extension.error(
                        "fetchThumbnail failed identifier=\(identifier.rawValue, privacy: .public) error=\(error.localizedDescription, privacy: .public)"
                    )
                    perThumbnailCompletionHandler(identifier, nil, nil)
                } catch {
                    DokusFileProviderLog.extension.error(
                        "fetchThumbnail failed identifier=\(identifier.rawValue, privacy: .public) error=\(String(describing: error), privacy: .public)"
                    )
                    perThumbnailCompletionHandler(identifier, nil, nil)
                }

                progress.completedUnitCount += 1
            }

            completionHandler(nil)
        }

        progress.cancellationHandler = {
            task.cancel()
        }
        return progress
    }

    func createItem(
        basedOn itemTemplate: NSFileProviderItem,
        fields: NSFileProviderItemFields,
        contents url: URL?,
        options: NSFileProviderCreateItemOptions = [],
        request: NSFileProviderRequest,
        completionHandler: @escaping (NSFileProviderItem?, NSFileProviderItemFields, Bool, Error?) -> Void
    ) -> Progress {
        let progress = Progress(totalUnitCount: 100)
        let task = Task {
            do {
                DokusFileProviderLog.extension.debug(
                    "createItem request parent=\(itemTemplate.parentItemIdentifier.rawValue, privacy: .public) filename=\(itemTemplate.filename, privacy: .public)"
                )
                guard let contentURL = url else {
                    throw DokusFileProviderError.unsupportedOperation("Only file uploads to Inbox are supported")
                }
                let templateType = itemTemplate.contentType ?? .data
                if templateType.conforms(to: .folder) {
                    throw DokusFileProviderError.unsupportedOperation("Creating folders is not allowed")
                }

                let mimeType = templateType.preferredMIMEType ?? "application/octet-stream"
                let created = try await runtime.uploadDocument(
                    to: itemTemplate.parentItemIdentifier,
                    filename: itemTemplate.filename,
                    fileURL: contentURL,
                    mimeType: mimeType,
                    transfer: DokusFileProviderTransfer(
                        itemIdentifier: itemTemplate.itemIdentifier,
                        progress: progress
                    )
                )
                completionHandler(await runtime.fileProviderItem(from: created), [], false, nil)
                resolveDomainErrorsIfNeeded(source: "createItem")
                signalWorkingSetIfNeeded(
                    source: "createItem",
                    currentParentIdentifier: created.parentIdentifier
                )
            } catch let error as DokusFileProviderError {
                DokusFileProviderLog.extension.error(
                    "createItem failed parent=\(itemTemplate.parentItemIdentifier.rawValue, privacy: .public) filename=\(itemTemplate.filename, privacy: .public) error=\(error.localizedDescription, privacy: .public)"
                )
                completionHandler(nil, [], false, error.nsError)
            } catch {
                DokusFileProviderLog.extension.error(
                    "createItem failed parent=\(itemTemplate.parentItemIdentifier.rawValue, privacy: .public) filename=\(itemTemplate.filename, privacy: .public) error=\(String(describing: error), privacy: .public)"
                )
                completionHandler(nil, [], false, DokusUnexpectedFileProviderError.nsError(from: error))
            }
        }
        progress.cancellationHandler = {
            task.cancel()
        }
        return progress
    }

    func modifyItem(
        _ item: NSFileProviderItem,
        baseVersion version: NSFileProviderItemVersion,
        changedFields: NSFileProviderItemFields,
        contents newContents: URL?,
        options: NSFileProviderModifyItemOptions = [],
        request: NSFileProviderRequest,
        completionHandler: @escaping (NSFileProviderItem?, NSFileProviderItemFields, Bool, Error?) -> Void
    ) -> Progress {
        let progress = Progress(totalUnitCount: 100)
        if DokusModifyPolicy.isDisallowedModify(changedFields) {
            let error = DokusFileProviderError.unsupportedOperation(
                "Rename, move and edit operations are not supported in Dokus Files"
            ).nsError
            completionHandler(nil, [], false, error)
            DokusFileProviderLog.extension.debug(
                "modifyItem denied identifier=\(item.itemIdentifier.rawValue, privacy: .public) changedFields=\(changedFields.rawValue, privacy: .public)"
            )
            progress.completedUnitCount = 100
            return progress
        }

        DokusFileProviderLog.extension.debug(
            "modifyItem accepted as metadata no-op identifier=\(item.itemIdentifier.rawValue, privacy: .public) changedFields=\(changedFields.rawValue, privacy: .public)"
        )
        completionHandler(item, [], false, nil)
        progress.completedUnitCount = 100
        return progress
    }

    func deleteItem(
        identifier: NSFileProviderItemIdentifier,
        baseVersion version: NSFileProviderItemVersion,
        options: NSFileProviderDeleteItemOptions = [],
        request: NSFileProviderRequest,
        completionHandler: @escaping (Error?) -> Void
    ) -> Progress {
        let progress = Progress(totalUnitCount: 100)
        let task = Task {
            do {
                DokusFileProviderLog.extension.debug("deleteItem request identifier=\(identifier.rawValue, privacy: .public)")
                let previousParentIdentifier = try await runtime.deleteDocument(itemIdentifier: identifier)
                progress.completedUnitCount = 100
                completionHandler(nil)
                resolveDomainErrorsIfNeeded(source: "deleteItem")
                signalWorkingSetIfNeeded(
                    source: "deleteItem",
                    previousParentIdentifier: previousParentIdentifier
                )
            } catch is CancellationError {
                completionHandler(NSError(domain: NSCocoaErrorDomain, code: NSUserCancelledError))
            } catch let error as DokusFileProviderError {
                DokusFileProviderLog.extension.error(
                    "deleteItem failed identifier=\(identifier.rawValue, privacy: .public) error=\(error.localizedDescription, privacy: .public)"
                )
                completionHandler(error.nsError)
            } catch {
                DokusFileProviderLog.extension.error(
                    "deleteItem failed identifier=\(identifier.rawValue, privacy: .public) error=\(String(describing: error), privacy: .public)"
                )
                completionHandler(DokusUnexpectedFileProviderError.nsError(from: error))
            }
        }
        progress.cancellationHandler = {
            task.cancel()
        }
        return progress
    }

    func materializedItemsDidChange(completionHandler: @escaping () -> Void) {
        let domainIdentifier = domain.identifier.rawValue
        guard let providerManager else {
            DokusFileProviderLog.runtime.debug(
                "materializedItemsDidChange skipped domainId=\(domainIdentifier, privacy: .public) reason=noManager"
            )
            completionHandler()
            return
        }

        Task {
            let materializedIdentifiers = await providerManager.materializedItemIdentifiers()
            await runtime.replaceMaterializedItems(with: materializedIdentifiers)
            DokusFileProviderLog.runtime.debug(
                "materializedItemsDidChange completed domainId=\(domainIdentifier, privacy: .public) count=\(materializedIdentifiers.count, privacy: .public)"
            )
            completionHandler()
        }
    }

    func pendingItemsDidChange(completionHandler: @escaping () -> Void) {
        let domainIdentifier = domain.identifier.rawValue
        guard let providerManager else {
            DokusFileProviderLog.runtime.debug(
                "pendingItemsDidChange skipped domainId=\(domainIdentifier, privacy: .public) reason=noManager"
            )
            completionHandler()
            return
        }

        Task {
            let pendingItems = await providerManager.pendingItemStates()
            await runtime.replacePendingItems(with: pendingItems)
            DokusFileProviderLog.runtime.debug(
                "pendingItemsDidChange completed domainId=\(domainIdentifier, privacy: .public) count=\(pendingItems.count, privacy: .public)"
            )
            if pendingItems.isEmpty {
                resolveDomainErrorsIfNeeded(source: "pendingItemsDidChange")
            }
            completionHandler()
        }
    }

    private func fileProviderTemporaryDirectoryURL() -> URL? {
        guard let manager else {
            return nil
        }
        do {
            let url = try manager.temporaryDirectoryURL()
            DokusFileProviderLog.extension.debug(
                "temporaryDirectoryURL path=\(url.path, privacy: .public)"
            )
            return url
        } catch {
            DokusFileProviderLog.extension.error(
                "temporaryDirectoryURL failed error=\(String(describing: error), privacy: .public)"
            )
            return nil
        }
    }

    private func resolveDomainErrorsIfNeeded(source: String) {
        let domainIdentifier = self.domain.identifier.rawValue
        guard let providerManager else {
            DokusFileProviderLog.domainHealth.debug(
                "resolveDomainErrors skipped domainId=\(domainIdentifier, privacy: .public) source=\(source, privacy: .public) reason=noManager"
            )
            return
        }
        Task {
            let pendingItems = await providerManager.pendingItemStates()
            await runtime.replacePendingItems(with: pendingItems)
            if !pendingItems.isEmpty {
                DokusFileProviderLog.domainHealth.debug(
                    "resolveDomainErrors continuing with pending items domainId=\(domainIdentifier, privacy: .public) source=\(source, privacy: .public) pendingCount=\(pendingItems.count, privacy: .public)"
                )
            }
            await errorResolver.resolveDomainErrorsIfNeeded(
                manager: providerManager,
                domainIdentifier: domainIdentifier,
                source: source
            )
        }
    }

    private func signalWorkingSetIfNeeded(
        source: String,
        currentParentIdentifier: NSFileProviderItemIdentifier? = nil,
        previousParentIdentifier: NSFileProviderItemIdentifier? = nil
    ) {
        guard let providerManager else {
            return
        }

        Task {
            let shouldSignal = await runtime.shouldSignalWorkingSet(
                currentParentIdentifier: currentParentIdentifier,
                previousParentIdentifier: previousParentIdentifier
            )
            guard shouldSignal else {
                DokusFileProviderLog.runtime.debug(
                    "working set signal skipped domainId=\(self.domain.identifier.rawValue, privacy: .public) source=\(source, privacy: .public)"
                )
                return
            }
            DokusFileProviderLog.runtime.debug(
                "working set signal requested domainId=\(self.domain.identifier.rawValue, privacy: .public) source=\(source, privacy: .public)"
            )
            await providerManager.signalWorkingSet()
        }
    }
}

private actor DokusFileProviderErrorResolver {
    private let minInterval: TimeInterval = 30
    private var lastResolvedAt: Date?
    private let resolvableErrorCodes: [NSFileProviderError.Code] = [
        .serverUnreachable,
        .cannotSynchronize,
        .notAuthenticated,
        .excludedFromSync
    ]

    func resolveDomainErrorsIfNeeded(
        manager: DokusFileProviderManagerHandling,
        domainIdentifier: String,
        source: String
    ) async {
        let now = Date()
        if let lastResolvedAt {
            let elapsed = now.timeIntervalSince(lastResolvedAt)
            if elapsed < minInterval {
                let retryIn = Int((minInterval - elapsed).rounded(.up))
                DokusFileProviderLog.domainHealth.debug(
                    "resolveDomainErrors throttled domainId=\(domainIdentifier, privacy: .public) source=\(source, privacy: .public) retryInSec=\(retryIn, privacy: .public)"
                )
                return
            }
        }

        DokusFileProviderLog.domainHealth.debug(
            "resolveDomainErrors started domainId=\(domainIdentifier, privacy: .public) source=\(source, privacy: .public)"
        )

        lastResolvedAt = now

        for code in resolvableErrorCodes {
            let error = NSError(domain: NSFileProviderErrorDomain, code: code.rawValue)
            let resolveError = await manager.signalErrorResolved(error)
            if let resolveError {
                DokusFileProviderLog.domainHealth.warning(
                    "signalErrorResolved failed domainId=\(domainIdentifier, privacy: .public) code=\(code.rawValue, privacy: .public) error=\(resolveError.localizedDescription, privacy: .public)"
                )
            } else {
                DokusFileProviderLog.domainHealth.debug(
                    "signalErrorResolved succeeded domainId=\(domainIdentifier, privacy: .public) code=\(code.rawValue, privacy: .public)"
                )
            }
        }

        DokusFileProviderLog.domainHealth.debug(
            "resolveDomainErrors completed domainId=\(domainIdentifier, privacy: .public) source=\(source, privacy: .public)"
        )
    }
}

protocol DokusFileProviderManagerHandling: DokusFileProviderTaskRegistering {
    func signalErrorResolved(_ error: NSError) async -> NSError?
    func signalWorkingSet() async
    func materializedItemIdentifiers() async -> Set<NSFileProviderItemIdentifier>
    func pendingItemStates() async -> [DokusPendingFileProviderItemState]
}

private final class DokusSystemFileProviderManager: DokusFileProviderManagerHandling {
    private let manager: NSFileProviderManager

    init(manager: NSFileProviderManager) {
        self.manager = manager
    }

    func register(task: URLSessionTask, for itemIdentifier: NSFileProviderItemIdentifier) {
        manager.register(task, forItemWithIdentifier: itemIdentifier) { error in
            if let error {
                DokusFileProviderLog.runtime.warning(
                    "registerURLSessionTask failed identifier=\(itemIdentifier.rawValue, privacy: .public) error=\(error.localizedDescription, privacy: .public)"
                )
            }
        }
    }

    func signalErrorResolved(_ error: NSError) async -> NSError? {
        await withCheckedContinuation { continuation in
            manager.signalErrorResolved(error) { resolveError in
                continuation.resume(returning: resolveError as NSError?)
            }
        }
    }

    func signalWorkingSet() async {
        await withCheckedContinuation { continuation in
            manager.signalEnumerator(for: .workingSet) { error in
                if let error {
                    DokusFileProviderLog.runtime.warning(
                        "signal working set failed error=\(error.localizedDescription, privacy: .public)"
                    )
                }
                continuation.resume()
            }
        }
    }

    func materializedItemIdentifiers() async -> Set<NSFileProviderItemIdentifier> {
        let enumerator = manager.enumeratorForMaterializedItems()
        var page = NSFileProviderPage(rawValue: Data())
        var identifiers = Set<NSFileProviderItemIdentifier>()

        while true {
            let result = await enumeratePage(
                enumerator: enumerator,
                startingAt: page
            )
            identifiers.formUnion(result.identifiers)
            guard let nextPage = result.nextPage else {
                break
            }
            page = nextPage
        }

        return identifiers
    }

    func pendingItemStates() async -> [DokusPendingFileProviderItemState] {
        let enumerator = manager.enumeratorForPendingItems()
        var page = NSFileProviderPage(rawValue: Data())
        var items: [DokusPendingFileProviderItemState] = []

        while true {
            let result = await enumeratePendingPage(
                enumerator: enumerator,
                startingAt: page
            )
            items.append(contentsOf: result.items)
            guard let nextPage = result.nextPage else {
                break
            }
            page = nextPage
        }

        return items
    }

    private func enumeratePage(
        enumerator: NSFileProviderEnumerator,
        startingAt page: NSFileProviderPage
    ) async -> (identifiers: Set<NSFileProviderItemIdentifier>, nextPage: NSFileProviderPage?) {
        await withCheckedContinuation { continuation in
            let observer = DokusMaterializedEnumerationObserver { identifiers, nextPage in
                continuation.resume(returning: (identifiers, nextPage))
            }
            enumerator.enumerateItems(for: observer, startingAt: page)
        }
    }

    private func enumeratePendingPage(
        enumerator: NSFileProviderEnumerator,
        startingAt page: NSFileProviderPage
    ) async -> (items: [DokusPendingFileProviderItemState], nextPage: NSFileProviderPage?) {
        await withCheckedContinuation { continuation in
            let observer = DokusPendingEnumerationObserver { items, nextPage in
                continuation.resume(returning: (items, nextPage))
            }
            enumerator.enumerateItems(for: observer, startingAt: page)
        }
    }
}

private final class DokusMaterializedEnumerationObserver: NSObject, NSFileProviderEnumerationObserver {
    private var identifiers = Set<NSFileProviderItemIdentifier>()
    private let onFinish: (Set<NSFileProviderItemIdentifier>, NSFileProviderPage?) -> Void

    init(onFinish: @escaping (Set<NSFileProviderItemIdentifier>, NSFileProviderPage?) -> Void) {
        self.onFinish = onFinish
    }

    func didEnumerate(_ updatedItems: [any NSFileProviderItem]) {
        identifiers.formUnion(updatedItems.map(\.itemIdentifier))
    }

    func finishEnumerating(upTo nextPage: NSFileProviderPage?) {
        onFinish(identifiers, nextPage)
    }

    func finishEnumeratingWithError(_ error: Error) {
        DokusFileProviderLog.runtime.warning(
            "materialized enumeration failed error=\(String(describing: error), privacy: .public)"
        )
        onFinish([], nil)
    }
}

private final class DokusPendingEnumerationObserver: NSObject, NSFileProviderEnumerationObserver {
    private var items: [DokusPendingFileProviderItemState] = []
    private let onFinish: ([DokusPendingFileProviderItemState], NSFileProviderPage?) -> Void

    init(onFinish: @escaping ([DokusPendingFileProviderItemState], NSFileProviderPage?) -> Void) {
        self.onFinish = onFinish
    }

    func didEnumerate(_ updatedItems: [any NSFileProviderItem]) {
        items.append(contentsOf: updatedItems.map(DokusPendingFileProviderItemState.init(item:)))
    }

    func finishEnumerating(upTo nextPage: NSFileProviderPage?) {
        onFinish(items, nextPage)
    }

    func finishEnumeratingWithError(_ error: Error) {
        DokusFileProviderLog.runtime.warning(
            "pending enumeration failed error=\(String(describing: error), privacy: .public)"
        )
        onFinish([], nil)
    }
}
