import Foundation
import FileProvider

struct DokusChangeSet {
    let updatedIdentifiers: [NSFileProviderItemIdentifier]
    let deletedIdentifiers: [NSFileProviderItemIdentifier]
    let anchor: NSFileProviderSyncAnchor
}

final class DokusFileProviderSnapshotStore {
    private struct SnapshotEntry: Codable, Hashable {
        let identifier: String
        let parentIdentifier: String
        let filename: String
        let isFolder: Bool
        let modifiedAt: TimeInterval?
    }

    private struct SnapshotVersion: Codable {
        let generation: Int64
        let entries: [SnapshotEntry]
    }

    private struct SnapshotState: Codable {
        var versions: [SnapshotVersion]
        var currentGeneration: Int64
    }

    private let queue = DispatchQueue(label: "vision.invoid.dokus.fileprovider.snapshot")
    private let fileURL: URL

    init(domainIdentifier: String) {
        let appGroup = Bundle.main.object(forInfoDictionaryKey: "DokusShareAppGroupIdentifier") as? String
        let containerURL = FileManager.default.containerURL(
            forSecurityApplicationGroupIdentifier: appGroup ?? DokusFileProviderConstants.appGroupIdentifier
        ) ?? URL(fileURLWithPath: NSTemporaryDirectory(), isDirectory: true)

        let snapshotDirectory = containerURL.appendingPathComponent(
            DokusFileProviderConstants.snapshotDirectoryName,
            isDirectory: true
        )
        try? FileManager.default.createDirectory(at: snapshotDirectory, withIntermediateDirectories: true)

        let safeDomain = domainIdentifier.replacingOccurrences(of: ".", with: "_")
        self.fileURL = snapshotDirectory.appendingPathComponent("snapshot-\(safeDomain).json")
    }

    func update(with projection: DokusProjection) -> Int64 {
        queue.sync {
            var state = loadState()
            let entries = makeEntries(from: projection)

            if let current = state.versions.first(where: { $0.generation == state.currentGeneration }),
               Set(current.entries) == Set(entries) {
                return current.generation
            }

            let nextGeneration = (state.versions.map(\.generation).max() ?? 0) + 1
            state.currentGeneration = nextGeneration
            state.versions.append(SnapshotVersion(generation: nextGeneration, entries: entries))
            if state.versions.count > 5 {
                state.versions.removeFirst(state.versions.count - 5)
            }
            saveState(state)
            return nextGeneration
        }
    }

    func currentAnchor() -> NSFileProviderSyncAnchor? {
        queue.sync {
            let state = loadState()
            guard state.currentGeneration > 0 else {
                return nil
            }
            return makeAnchor(from: state.currentGeneration)
        }
    }

    func changes(from anchor: NSFileProviderSyncAnchor?, projection: DokusProjection) -> DokusChangeSet {
        queue.sync {
            var state = loadState()
            let currentEntries = makeEntries(from: projection)

            if let current = state.versions.first(where: { $0.generation == state.currentGeneration }),
               Set(current.entries) != Set(currentEntries) {
                let nextGeneration = (state.versions.map(\.generation).max() ?? 0) + 1
                state.currentGeneration = nextGeneration
                state.versions.append(SnapshotVersion(generation: nextGeneration, entries: currentEntries))
                if state.versions.count > 5 {
                    state.versions.removeFirst(state.versions.count - 5)
                }
                saveState(state)
            } else if state.currentGeneration == 0 {
                state.currentGeneration = 1
                state.versions = [SnapshotVersion(generation: 1, entries: currentEntries)]
                saveState(state)
            }

            let currentVersion = state.versions.first(where: { $0.generation == state.currentGeneration })
                ?? SnapshotVersion(generation: state.currentGeneration, entries: currentEntries)

            let fromGeneration = decodeAnchor(anchor)
            let fromVersion = state.versions.first(where: { $0.generation == fromGeneration })

            let fromById = Dictionary(
                uniqueKeysWithValues: (fromVersion?.entries ?? []).map { ($0.identifier, $0) }
            )
            let currentById = Dictionary(uniqueKeysWithValues: currentVersion.entries.map { ($0.identifier, $0) })

            let updated = currentById.compactMap { id, entry -> NSFileProviderItemIdentifier? in
                if let previous = fromById[id], previous == entry {
                    return nil
                }
                return NSFileProviderItemIdentifier(id)
            }

            let deleted = fromById.keys
                .filter { currentById[$0] == nil }
                .map { NSFileProviderItemIdentifier($0) }

            return DokusChangeSet(
                updatedIdentifiers: updated,
                deletedIdentifiers: deleted,
                anchor: makeAnchor(from: currentVersion.generation)
            )
        }
    }

    private func makeEntries(from projection: DokusProjection) -> [SnapshotEntry] {
        projection.itemsByIdentifier.values.map { item in
            SnapshotEntry(
                identifier: item.identifier.rawValue,
                parentIdentifier: item.parentIdentifier.rawValue,
                filename: item.filename,
                isFolder: item.isFolder,
                modifiedAt: item.contentModificationDate?.timeIntervalSince1970
            )
        }
    }

    private func decodeAnchor(_ anchor: NSFileProviderSyncAnchor?) -> Int64? {
        guard let anchor else {
            return nil
        }
        let data = anchor.rawValue as Data
        guard data.count == MemoryLayout<Int64>.size else {
            return nil
        }
        return data.withUnsafeBytes { rawBuffer in
            rawBuffer.load(as: Int64.self).bigEndian
        }
    }

    private func makeAnchor(from generation: Int64) -> NSFileProviderSyncAnchor {
        var bigEndian = generation.bigEndian
        let data = Data(bytes: &bigEndian, count: MemoryLayout<Int64>.size)
        return NSFileProviderSyncAnchor(rawValue: data)
    }

    private func loadState() -> SnapshotState {
        guard
            let data = try? Data(contentsOf: fileURL),
            let state = try? JSONDecoder().decode(SnapshotState.self, from: data)
        else {
            return SnapshotState(versions: [], currentGeneration: 0)
        }
        return state
    }

    private func saveState(_ state: SnapshotState) {
        guard let data = try? JSONEncoder().encode(state) else { return }
        try? data.write(to: fileURL, options: .atomic)
    }
}
