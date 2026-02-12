import XCTest
import FileProvider
@testable import DokusFileProviderExtension

final class DokusFileProviderProjectionTests: XCTestCase {
    private let builder = DokusProjectionBuilder()

    func testLifecyclePrecedenceAndDirectionalRouting() {
        let workspace = DokusWorkspace(id: "ws-1", name: "Invoid BV", role: .owner)
        let docs = [
            makeRecord(id: "d1", ingestion: .queued, draftStatus: .confirmed, draftType: .invoice, direction: .inbound),
            makeRecord(id: "d2", ingestion: .failed, draftStatus: .confirmed, draftType: .invoice, direction: .inbound),
            makeRecord(id: "d3", ingestion: .succeeded, draftStatus: .needsReview, draftType: .invoice, direction: .inbound),
            makeRecord(id: "d4", ingestion: .succeeded, draftStatus: .confirmed, draftType: .invoice, direction: .inbound),
            makeRecord(id: "d5", ingestion: .succeeded, draftStatus: .confirmed, draftType: .invoice, direction: .outbound),
            makeRecord(id: "d6", ingestion: .succeeded, draftStatus: .confirmed, draftType: .creditNote, direction: .outbound),
            makeRecord(id: "d7", ingestion: .succeeded, draftStatus: .confirmed, draftType: .receipt, direction: .inbound),
            makeRecord(id: "d8", ingestion: .succeeded, draftStatus: .confirmed, draftType: .unknown, direction: .unknown),
        ]

        let projection = builder.build(
            workspaces: [workspace],
            documentsByWorkspace: [workspace.id: docs]
        )

        XCTAssertEqual(parentFolderName(ofDocument: "d1", in: projection), DokusLifecycleFolder.inbox.rawValue)
        XCTAssertEqual(parentFolderName(ofDocument: "d2", in: projection), DokusLifecycleFolder.needsReview.rawValue)
        XCTAssertEqual(parentFolderName(ofDocument: "d3", in: projection), DokusLifecycleFolder.needsReview.rawValue)
        XCTAssertEqual(topTypedFolderName(ofDocument: "d4", in: projection), DokusTypedFolder.invoicesIn.rawValue)
        XCTAssertEqual(topTypedFolderName(ofDocument: "d5", in: projection), DokusTypedFolder.invoicesOut.rawValue)
        XCTAssertEqual(topTypedFolderName(ofDocument: "d6", in: projection), DokusTypedFolder.creditNotesOut.rawValue)
        XCTAssertEqual(topTypedFolderName(ofDocument: "d7", in: projection), DokusTypedFolder.receiptsIn.rawValue)
        XCTAssertEqual(parentFolderName(ofDocument: "d8", in: projection), DokusLifecycleFolder.needsReview.rawValue)

        let allDocumentIds = projection.itemsByIdentifier.values.compactMap { $0.documentId }
        let grouped = Dictionary(grouping: allDocumentIds, by: { $0 })
        XCTAssertTrue(grouped.values.allSatisfy { $0.count == 1 }, "Each document must appear only once")
    }

    func testRoleVisibilityHidesLifecycleForAccountant() {
        let workspace = DokusWorkspace(id: "ws-2", name: "Bakkerij Peeters", role: .accountant)
        let docs = [
            makeRecord(id: "queued", ingestion: .queued, draftStatus: nil, draftType: nil, direction: .unknown),
            makeRecord(id: "confirmed", ingestion: .succeeded, draftStatus: .confirmed, draftType: .invoice, direction: .inbound),
        ]

        let projection = builder.build(
            workspaces: [workspace],
            documentsByWorkspace: [workspace.id: docs]
        )

        let workspaceIdentifier = DokusItemIdentifierCodec.encode(kind: .workspace(workspaceId: workspace.id))
        let workspaceChildren = projection.children(of: workspaceIdentifier).map(\.filename)
        XCTAssertFalse(workspaceChildren.contains(DokusLifecycleFolder.inbox.rawValue))
        XCTAssertFalse(workspaceChildren.contains(DokusLifecycleFolder.needsReview.rawValue))
        XCTAssertTrue(workspaceChildren.contains(DokusTypedFolder.invoicesIn.rawValue))

        XCTAssertNil(itemForDocument("queued", in: projection), "Non-confirmed docs must not be shown for accountant/viewer")
        XCTAssertNotNil(itemForDocument("confirmed", in: projection))
    }

    func testSnapshotDiffTracksUpdatesAndDeletes() {
        let store = DokusFileProviderSnapshotStore(domainIdentifier: "tests.\(UUID().uuidString)")
        let workspace = DokusWorkspace(id: "ws-3", name: "TechFlow BVBA", role: .owner)

        var projection = builder.build(
            workspaces: [workspace],
            documentsByWorkspace: [workspace.id: [makeRecord(id: "d1", ingestion: .queued, draftStatus: nil, draftType: nil, direction: .unknown)]]
        )
        projection.generation = store.update(with: projection)
        let firstAnchor = store.currentAnchor()
        XCTAssertNotNil(firstAnchor)

        let firstDiff = store.changes(from: nil, projection: projection)
        XCTAssertFalse(firstDiff.updatedIdentifiers.isEmpty)

        var projection2 = builder.build(
            workspaces: [workspace],
            documentsByWorkspace: [workspace.id: [makeRecord(id: "d1", ingestion: .succeeded, draftStatus: .confirmed, draftType: .invoice, direction: .outbound)]]
        )
        projection2.generation = store.update(with: projection2)
        let secondDiff = store.changes(from: firstAnchor, projection: projection2)
        XCTAssertTrue(secondDiff.updatedIdentifiers.contains(DokusItemIdentifierCodec.encode(kind: .document(workspaceId: workspace.id, documentId: "d1"))))

        let projection3 = builder.build(
            workspaces: [workspace],
            documentsByWorkspace: [workspace.id: []]
        )
        let thirdDiff = store.changes(from: secondDiff.anchor, projection: projection3)
        XCTAssertTrue(thirdDiff.deletedIdentifiers.contains(DokusItemIdentifierCodec.encode(kind: .document(workspaceId: workspace.id, documentId: "d1"))))
    }

    private func makeRecord(
        id: String,
        ingestion: DokusIngestionStatus?,
        draftStatus: DokusDraftStatus?,
        draftType: DokusDocumentType?,
        direction: DokusDocumentDirection
    ) -> DokusDocumentRecord {
        DokusDocumentRecord(
            workspaceId: "ws",
            documentId: id,
            originalFilename: "\(id).pdf",
            contentType: "application/pdf",
            sizeBytes: 128,
            uploadedAt: Date(timeIntervalSince1970: 1_700_000_000),
            updatedAt: Date(timeIntervalSince1970: 1_700_000_100),
            downloadURL: URL(string: "https://example.com/\(id).pdf"),
            latestIngestionStatus: ingestion,
            draftStatus: draftStatus,
            draftType: draftType,
            draftDirection: direction,
            issueDate: Date(timeIntervalSince1970: 1_700_000_000),
            amountMinor: 12_345,
            counterpartyName: "Counterparty",
            documentNumber: "2026-001"
        )
    }

    private func itemForDocument(_ documentId: String, in projection: DokusProjection) -> DokusProjectedItem? {
        projection.itemsByIdentifier.values.first(where: { $0.documentId == documentId })
    }

    private func parentFolderName(ofDocument documentId: String, in projection: DokusProjection) -> String? {
        guard
            let document = itemForDocument(documentId, in: projection),
            let parent = projection.item(document.parentIdentifier)
        else {
            return nil
        }
        return parent.filename
    }

    private func topTypedFolderName(ofDocument documentId: String, in projection: DokusProjection) -> String? {
        guard let document = itemForDocument(documentId, in: projection) else {
            return nil
        }

        var cursor: DokusProjectedItem? = projection.item(document.parentIdentifier)
        while let current = cursor {
            if DokusTypedFolder(rawValue: current.filename) != nil {
                return current.filename
            }
            if current.identifier == .rootContainer {
                return nil
            }
            cursor = projection.item(current.parentIdentifier)
        }
        return nil
    }
}
