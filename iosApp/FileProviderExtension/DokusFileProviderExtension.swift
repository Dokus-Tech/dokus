import Foundation
import FileProvider

final class DokusFileProviderExtension: NSObject, NSFileProviderReplicatedExtension, NSFileProviderThumbnailing {
    private let domain: NSFileProviderDomain
    private let runtime: DokusFileProviderRuntime
    private let manager: NSFileProviderManager?

    required init(domain: NSFileProviderDomain) {
        self.domain = domain
        let sessionProvider = DokusFileProviderSessionProvider()
        let apiClient = DokusFileProviderAPIClient(sessionProvider: sessionProvider)
        let workspaceId = DokusFileProviderConstants.workspaceId(
            fromDomainIdentifier: domain.identifier.rawValue
        )
        self.runtime = DokusFileProviderRuntime(
            apiClient: apiClient,
            domainIdentifier: domain.identifier.rawValue,
            workspaceId: workspaceId,
            domainDisplayName: domain.displayName
        )
        self.manager = NSFileProviderManager(for: domain)
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
            runtime: runtime
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
                let projected = try await runtime.item(for: identifier, forceRefresh: false)
                guard !progress.isCancelled else {
                    completionHandler(nil, NSError(domain: NSCocoaErrorDomain, code: NSUserCancelledError))
                    return
                }
                progress.completedUnitCount = 100
                completionHandler(DokusFileProviderItem(projected: projected), nil)
            } catch let error as DokusFileProviderError {
                DokusFileProviderLog.extension.error(
                    "item request failed identifier=\(identifier.rawValue, privacy: .public) error=\(error.localizedDescription, privacy: .public)"
                )
                completionHandler(nil, error.nsError)
            } catch {
                DokusFileProviderLog.extension.error(
                    "item request failed identifier=\(identifier.rawValue, privacy: .public) error=\(String(describing: error), privacy: .public)"
                )
                completionHandler(
                    nil,
                    NSError(
                        domain: NSFileProviderErrorDomain,
                        code: NSFileProviderError.cannotSynchronize.rawValue,
                        userInfo: [NSLocalizedDescriptionKey: error.localizedDescription]
                    )
                )
            }
        }
        progress.cancellationHandler = {
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
                let projected = try await runtime.item(for: itemIdentifier, forceRefresh: false)
                let temporaryDirectoryURL = fileProviderTemporaryDirectoryURL()
                let fileURL = try await runtime.fetchContents(
                    itemIdentifier: itemIdentifier,
                    temporaryDirectoryURL: temporaryDirectoryURL
                )
                guard !progress.isCancelled else {
                    completionHandler(nil, nil, NSError(domain: NSCocoaErrorDomain, code: NSUserCancelledError))
                    return
                }
                progress.completedUnitCount = 100
                completionHandler(fileURL, DokusFileProviderItem(projected: projected), nil)
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
                completionHandler(
                    nil,
                    nil,
                    NSError(
                        domain: NSFileProviderErrorDomain,
                        code: NSFileProviderError.cannotSynchronize.rawValue,
                        userInfo: [NSLocalizedDescriptionKey: error.localizedDescription]
                    )
                )
            }
        }
        progress.cancellationHandler = {
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
                    mimeType: mimeType
                )
                progress.completedUnitCount = 100
                completionHandler(DokusFileProviderItem(projected: created), [], false, nil)
            } catch let error as DokusFileProviderError {
                DokusFileProviderLog.extension.error(
                    "createItem failed parent=\(itemTemplate.parentItemIdentifier.rawValue, privacy: .public) filename=\(itemTemplate.filename, privacy: .public) error=\(error.localizedDescription, privacy: .public)"
                )
                completionHandler(nil, [], false, error.nsError)
            } catch {
                DokusFileProviderLog.extension.error(
                    "createItem failed parent=\(itemTemplate.parentItemIdentifier.rawValue, privacy: .public) filename=\(itemTemplate.filename, privacy: .public) error=\(String(describing: error), privacy: .public)"
                )
                completionHandler(
                    nil,
                    [],
                    false,
                    NSError(
                        domain: NSFileProviderErrorDomain,
                        code: NSFileProviderError.cannotSynchronize.rawValue,
                        userInfo: [NSLocalizedDescriptionKey: error.localizedDescription]
                    )
                )
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
        let disallowedFields: NSFileProviderItemFields = [.contents, .filename, .parentItemIdentifier]
        if !changedFields.intersection(disallowedFields).isEmpty {
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
                try await runtime.deleteDocument(itemIdentifier: identifier)
                progress.completedUnitCount = 100
                completionHandler(nil)
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
                completionHandler(
                    NSError(
                        domain: NSFileProviderErrorDomain,
                        code: NSFileProviderError.cannotSynchronize.rawValue,
                        userInfo: [NSLocalizedDescriptionKey: error.localizedDescription]
                    )
                )
            }
        }
        progress.cancellationHandler = {
            task.cancel()
        }
        return progress
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
}
