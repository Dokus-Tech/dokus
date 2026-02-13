import XCTest
import FileProvider

final class DokusFileProviderOperationPolicyTests: XCTestCase {
    private let builder = DokusProjectionBuilder()

    func testOperationCapabilitiesMatchInboxOnlyWritePolicy() {
        let workspace = DokusWorkspace(id: "ws-cap", name: "Invoid BV", role: .owner)
        let records = [
            DokusDocumentRecord(
                workspaceId: workspace.id,
                documentId: "inbox",
                originalFilename: "scan.pdf",
                contentType: "application/pdf",
                sizeBytes: 1,
                uploadedAt: Date(),
                updatedAt: Date(),
                downloadURL: nil,
                latestIngestionStatus: .queued,
                draftStatus: nil,
                draftType: nil,
                draftDirection: .unknown,
                issueDate: nil,
                amountMinor: nil,
                counterpartyName: nil,
                documentNumber: nil
            ),
            DokusDocumentRecord(
                workspaceId: workspace.id,
                documentId: "typed",
                originalFilename: "invoice.pdf",
                contentType: "application/pdf",
                sizeBytes: 1,
                uploadedAt: Date(),
                updatedAt: Date(),
                downloadURL: nil,
                latestIngestionStatus: .succeeded,
                draftStatus: .confirmed,
                draftType: .invoice,
                draftDirection: .inbound,
                issueDate: Date(),
                amountMinor: 1000,
                counterpartyName: "Client",
                documentNumber: "INV-1"
            )
        ]

        let projection = builder.build(
            workspace: workspace,
            records: records,
            rootDisplayName: DokusFileProviderConstants.domainDisplayName
        )
        let children = projection.children(of: .rootContainer)

        let inboxFolder = children.first(where: { $0.filename == DokusLifecycleFolder.inbox.rawValue })
        XCTAssertNotNil(inboxFolder)
        XCTAssertTrue(inboxFolder!.capabilities.contains(.allowsAddingSubItems))

        let invoicesInFolder = children.first(where: { $0.filename == DokusTypedFolder.invoicesIn.rawValue })
        XCTAssertNotNil(invoicesInFolder)
        XCTAssertFalse(invoicesInFolder!.capabilities.contains(.allowsAddingSubItems))

        let inboxItem = projection.itemsByIdentifier.values.first(where: { $0.documentId == "inbox" })
        XCTAssertNotNil(inboxItem)
        XCTAssertTrue(inboxItem!.capabilities.contains(.allowsDeleting))

        let typedItem = projection.itemsByIdentifier.values.first(where: { $0.documentId == "typed" })
        XCTAssertNotNil(typedItem)
        XCTAssertFalse(typedItem!.capabilities.contains(.allowsDeleting))
        XCTAssertFalse(typedItem!.capabilities.contains(.allowsWriting))
    }

    func testModifyPolicyAllowsMetadataOnlyChanges() {
        XCTAssertFalse(DokusFileProviderExtension.isDisallowedModify(.lastUsedDate))
        XCTAssertFalse(DokusFileProviderExtension.isDisallowedModify(.favoriteRank))

        XCTAssertTrue(DokusFileProviderExtension.isDisallowedModify(.contents))
        XCTAssertTrue(DokusFileProviderExtension.isDisallowedModify(.filename))
        XCTAssertTrue(DokusFileProviderExtension.isDisallowedModify(.parentItemIdentifier))

        XCTAssertTrue(
            DokusFileProviderExtension.isDisallowedModify([.lastUsedDate, .filename])
        )
    }
}
