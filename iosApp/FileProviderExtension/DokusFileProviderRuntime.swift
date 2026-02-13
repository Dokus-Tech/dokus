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
            return cachedProjection
        }

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
        return projection
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
        let projection = try await self.projection(forceRefresh: true)
        return snapshotStore.changes(from: anchor, projection: projection)
    }

    func fetchContents(itemIdentifier: NSFileProviderItemIdentifier) async throws -> URL {
        let item = try await self.item(for: itemIdentifier)
        guard
            let workspaceId = item.workspaceId,
            let documentId = item.documentId
        else {
            throw DokusFileProviderError.noSuchItem
        }

        let key = recordKey(workspaceId: workspaceId, documentId: documentId)
        guard let record = cachedRecordsByKey[key] else {
            throw DokusFileProviderError.noSuchItem
        }

        return try await apiClient.downloadDocument(workspaceId: workspaceId, record: record)
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

        let refreshed = try await projection(forceRefresh: true)
        let createdIdentifier = DokusItemIdentifierCodec.encode(
            kind: .document(workspaceId: workspaceId, documentId: documentId)
        )
        if let created = refreshed.item(createdIdentifier) {
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
        _ = try await projection(forceRefresh: true)
    }

    private func recordKey(workspaceId: String, documentId: String) -> String {
        "\(workspaceId)#\(documentId)"
    }

    private func childComparator(_ lhs: DokusProjectedItem, _ rhs: DokusProjectedItem) -> Bool {
        if lhs.isFolder != rhs.isFolder {
            return lhs.isFolder && !rhs.isFolder
        }
        return lhs.filename.localizedCaseInsensitiveCompare(rhs.filename) == .orderedAscending
    }
}
