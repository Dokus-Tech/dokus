import Foundation
import FileProvider
import UniformTypeIdentifiers

actor DokusFileProviderRuntime {
    private let apiClient: DokusFileProviderAPIClient
    private let snapshotStore: DokusFileProviderSnapshotStore
    private let projectionBuilder = DokusProjectionBuilder()
    private let workspaceId: String?
    private let domainDisplayName: String

    private var cachedProjection: DokusProjection?
    private var cachedRecordsByKey: [String: DokusDocumentRecord] = [:]
    private var lastRefreshAt: Date?

    init(
        apiClient: DokusFileProviderAPIClient,
        domainIdentifier: String,
        workspaceId: String?,
        domainDisplayName: String
    ) {
        self.apiClient = apiClient
        self.snapshotStore = DokusFileProviderSnapshotStore(domainIdentifier: domainIdentifier)
        self.workspaceId = workspaceId
        self.domainDisplayName = domainDisplayName
    }

    func projection(forceRefresh: Bool) async throws -> DokusProjection {
        if
            !forceRefresh,
            let cachedProjection,
            let lastRefreshAt,
            Date().timeIntervalSince(lastRefreshAt) < 15 {
            DokusFileProviderLog.runtime.debug("projection returning cached generation=\(cachedProjection.generation, privacy: .public)")
            return cachedProjection
        }

        do {
            return try await refreshProjection()
        } catch is CancellationError {
            throw CancellationError()
        } catch {
            guard !forceRefresh, let cachedProjection else {
                throw error
            }
            // Keep previously synced view available during transient network failures.
            lastRefreshAt = Date()
            DokusFileProviderLog.runtime.warning(
                "projection refresh failed, returning cached generation=\(cachedProjection.generation, privacy: .public) error=\(String(describing: error), privacy: .public)"
            )
            return cachedProjection
        }
    }

    func item(for identifier: NSFileProviderItemIdentifier, forceRefresh: Bool = false) async throws -> DokusProjectedItem {
        let projection = try await self.projection(forceRefresh: forceRefresh)
        guard let item = projection.item(identifier) else {
            throw DokusFileProviderError.noSuchItem
        }
        return item
    }

    func children(for identifier: NSFileProviderItemIdentifier, forceRefresh: Bool) async throws -> [DokusProjectedItem] {
        let projection = try await self.projection(forceRefresh: forceRefresh)
        if identifier == .workingSet {
            return projection.itemsByIdentifier.values
                .filter { !$0.isFolder && $0.identifier != .rootContainer }
                .sorted(by: childComparator)
        }
        return projection.children(of: identifier).sorted(by: childComparator)
    }

    func currentAnchor(forceRefresh: Bool) async throws -> NSFileProviderSyncAnchor? {
        _ = try await projection(forceRefresh: forceRefresh)
        return snapshotStore.currentAnchor()
    }

    func changes(from anchor: NSFileProviderSyncAnchor?) async throws -> DokusChangeSet {
        do {
            let refreshedProjection = try await self.projection(forceRefresh: false)
            let changeSet = snapshotStore.changes(from: anchor, projection: refreshedProjection)
            DokusFileProviderLog.runtime.debug(
                "changes computed updated=\(changeSet.updatedIdentifiers.count, privacy: .public) deleted=\(changeSet.deletedIdentifiers.count, privacy: .public)"
            )
            return changeSet
        } catch {
            if let cachedProjection {
                let changeSet = snapshotStore.changes(from: anchor, projection: cachedProjection)
                DokusFileProviderLog.runtime.warning(
                    "changes fallback to cached projection updated=\(changeSet.updatedIdentifiers.count, privacy: .public) deleted=\(changeSet.deletedIdentifiers.count, privacy: .public) error=\(String(describing: error), privacy: .public)"
                )
                return changeSet
            }

            if let fallback = snapshotStore.fallbackChanges(from: anchor) {
                DokusFileProviderLog.runtime.warning(
                    "changes fallback to persisted snapshot updated=\(fallback.updatedIdentifiers.count, privacy: .public) deleted=\(fallback.deletedIdentifiers.count, privacy: .public) error=\(String(describing: error), privacy: .public)"
                )
                return fallback
            }
            throw error
        }
    }

    func fetchContents(
        itemIdentifier: NSFileProviderItemIdentifier,
        temporaryDirectoryURL: URL?
    ) async throws -> URL {
        let (workspaceId, record) = try await documentRecord(for: itemIdentifier, forceRefresh: false)
        DokusFileProviderLog.runtime.debug(
            "fetchContents workspaceId=\(workspaceId, privacy: .public) documentId=\(record.documentId, privacy: .public)"
        )
        return try await apiClient.downloadDocument(
            workspaceId: workspaceId,
            record: record,
            temporaryDirectoryURL: temporaryDirectoryURL
        )
    }

    func fetchThumbnail(
        itemIdentifier: NSFileProviderItemIdentifier,
        requestedPixelSize: Int
    ) async throws -> Data? {
        let (workspaceId, record) = try await documentRecord(for: itemIdentifier, forceRefresh: false)
        DokusFileProviderLog.runtime.debug(
            "fetchThumbnail workspaceId=\(workspaceId, privacy: .public) documentId=\(record.documentId, privacy: .public) requestedPixelSize=\(requestedPixelSize, privacy: .public)"
        )
        return try await apiClient.fetchThumbnail(
            workspaceId: workspaceId,
            record: record,
            requestedPixelSize: requestedPixelSize
        )
    }

    func uploadDocument(
        to parentIdentifier: NSFileProviderItemIdentifier,
        filename: String,
        fileURL: URL,
        mimeType: String
    ) async throws -> DokusProjectedItem {
        guard let parentKind = DokusItemIdentifierCodec.decode(parentIdentifier) else {
            throw DokusFileProviderError.unsupportedOperation("Uploads are only allowed in Inbox")
        }
        guard case .lifecycleFolder(let workspaceId, let folder) = parentKind, folder == .inbox else {
            throw DokusFileProviderError.unsupportedOperation("Uploads are only allowed in Inbox")
        }

        let documentId = try await apiClient.uploadDocument(
            workspaceId: workspaceId,
            from: fileURL,
            filename: filename,
            mimeType: mimeType
        )
        DokusFileProviderLog.runtime.debug(
            "uploadDocument completed workspaceId=\(workspaceId, privacy: .public) documentId=\(documentId, privacy: .public) filename=\(filename, privacy: .public)"
        )

        let createdIdentifier = DokusItemIdentifierCodec.encode(
            kind: .document(workspaceId: workspaceId, documentId: documentId)
        )
        do {
            let refreshed = try await projection(forceRefresh: true)
            if let created = refreshed.item(createdIdentifier) {
                return created
            }
        } catch is CancellationError {
            DokusFileProviderLog.runtime.warning(
                "uploadDocument refresh cancelled workspaceId=\(workspaceId, privacy: .public) documentId=\(documentId, privacy: .public)"
            )
        } catch {
            DokusFileProviderLog.runtime.warning(
                "uploadDocument refresh failed workspaceId=\(workspaceId, privacy: .public) documentId=\(documentId, privacy: .public) error=\(String(describing: error), privacy: .public)"
            )
        }

        if let created = cachedProjection?.item(createdIdentifier) {
            return created
        }

        return DokusProjectedItem(
            identifier: createdIdentifier,
            parentIdentifier: parentIdentifier,
            filename: filename,
            contentType: UTType.fromMimeType(mimeType),
            isFolder: false,
            capabilities: [.allowsReading, .allowsDeleting],
            documentSize: (try? Data(contentsOf: fileURL).count).flatMap(Int64.init),
            creationDate: Date(),
            contentModificationDate: Date(),
            childItemCount: nil,
            workspaceId: workspaceId,
            documentId: documentId,
            placement: .inbox
        )
    }

    func deleteDocument(itemIdentifier: NSFileProviderItemIdentifier) async throws {
        let item = try await self.item(for: itemIdentifier, forceRefresh: false)
        guard
            let workspaceId = item.workspaceId,
            let documentId = item.documentId
        else {
            throw DokusFileProviderError.noSuchItem
        }
        guard item.placement == .inbox else {
            throw DokusFileProviderError.unsupportedOperation("Delete is allowed only in Inbox")
        }

        try await apiClient.deleteDocument(workspaceId: workspaceId, documentId: documentId)
        do {
            _ = try await projection(forceRefresh: true)
        } catch is CancellationError {
            DokusFileProviderLog.runtime.warning(
                "deleteDocument refresh cancelled workspaceId=\(workspaceId, privacy: .public) documentId=\(documentId, privacy: .public)"
            )
        } catch {
            DokusFileProviderLog.runtime.warning(
                "deleteDocument refresh failed workspaceId=\(workspaceId, privacy: .public) documentId=\(documentId, privacy: .public) error=\(String(describing: error), privacy: .public)"
            )
        }
        DokusFileProviderLog.runtime.debug(
            "deleteDocument completed workspaceId=\(workspaceId, privacy: .public) documentId=\(documentId, privacy: .public)"
        )
    }

    private func recordKey(workspaceId: String, documentId: String) -> String {
        "\(workspaceId)#\(documentId)"
    }

    private func documentRecord(
        for itemIdentifier: NSFileProviderItemIdentifier,
        forceRefresh: Bool
    ) async throws -> (workspaceId: String, record: DokusDocumentRecord) {
        let item = try await self.item(for: itemIdentifier, forceRefresh: forceRefresh)
        guard
            let workspaceId = item.workspaceId,
            let documentId = item.documentId
        else {
            throw DokusFileProviderError.noSuchItem
        }

        let key = recordKey(workspaceId: workspaceId, documentId: documentId)
        if let record = cachedRecordsByKey[key] {
            return (workspaceId, record)
        }

        guard !forceRefresh else {
            throw DokusFileProviderError.noSuchItem
        }

        _ = try await projection(forceRefresh: true)
        if let refreshedRecord = cachedRecordsByKey[key] {
            return (workspaceId, refreshedRecord)
        }
        throw DokusFileProviderError.noSuchItem
    }

    private func childComparator(_ lhs: DokusProjectedItem, _ rhs: DokusProjectedItem) -> Bool {
        if lhs.isFolder != rhs.isFolder {
            return lhs.isFolder && !rhs.isFolder
        }
        return lhs.filename.localizedCaseInsensitiveCompare(rhs.filename) == .orderedAscending
    }

    private func refreshProjection() async throws -> DokusProjection {
        DokusFileProviderLog.runtime.debug(
            "projection refreshing workspaceId=\(self.workspaceId ?? "nil", privacy: .public)"
        )

        guard let workspaceId else {
            var projection = projectionBuilder.buildEmpty(rootDisplayName: domainDisplayName)
            let generation = snapshotStore.update(with: projection)
            projection.generation = generation
            cachedProjection = projection
            cachedRecordsByKey = [:]
            lastRefreshAt = Date()
            return projection
        }

        let workspaces = try await apiClient.listWorkspaces()
        guard let workspace = workspaces.first(where: { $0.id == workspaceId }) else {
            var projection = projectionBuilder.buildEmpty(rootDisplayName: domainDisplayName)
            let generation = snapshotStore.update(with: projection)
            projection.generation = generation
            cachedProjection = projection
            cachedRecordsByKey = [:]
            lastRefreshAt = Date()
            return projection
        }

        let records = try await apiClient.listAllDocuments(workspaceId: workspaceId)
        var recordsByKey: [String: DokusDocumentRecord] = [:]
        for record in records {
            recordsByKey[recordKey(workspaceId: workspaceId, documentId: record.documentId)] = record
        }

        var projection = projectionBuilder.build(
            workspace: workspace,
            records: records,
            rootDisplayName: domainDisplayName
        )

        let generation = snapshotStore.update(with: projection)
        projection.generation = generation
        cachedProjection = projection
        cachedRecordsByKey = recordsByKey
        lastRefreshAt = Date()
        DokusFileProviderLog.runtime.debug(
            "projection refreshed generation=\(generation, privacy: .public) records=\(records.count, privacy: .public)"
        )
        return projection
    }
}
