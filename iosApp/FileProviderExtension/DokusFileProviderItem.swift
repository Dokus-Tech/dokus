import Foundation
import FileProvider
import UniformTypeIdentifiers
import CryptoKit

final class DokusFileProviderItem: NSObject, NSFileProviderItem {
    private let projected: DokusProjectedItem

    init(projected: DokusProjectedItem) {
        self.projected = projected
    }

    var itemIdentifier: NSFileProviderItemIdentifier {
        projected.identifier
    }

    var parentItemIdentifier: NSFileProviderItemIdentifier {
        projected.parentIdentifier
    }

    var filename: String {
        projected.filename
    }

    var contentType: UTType {
        projected.contentType
    }

    var capabilities: NSFileProviderItemCapabilities {
        projected.capabilities
    }

    var documentSize: NSNumber? {
        guard let size = projected.documentSize else { return nil }
        return NSNumber(value: size)
    }

    var childItemCount: NSNumber? {
        guard let count = projected.childItemCount else { return nil }
        return NSNumber(value: count)
    }

    var creationDate: Date? {
        projected.creationDate
    }

    var contentModificationDate: Date? {
        projected.contentModificationDate
    }

    var itemVersion: NSFileProviderItemVersion {
        let contentToken: Data
        if projected.isFolder {
            contentToken = Data("folder".utf8)
        } else {
            let modified = Int64((projected.contentModificationDate?.timeIntervalSince1970 ?? 0) * 1000)
            let size = projected.documentSize ?? 0
            contentToken = Data("\(projected.documentId ?? projected.identifier.rawValue):\(modified):\(size)".utf8)
        }

        let metadataToken = Data("\(projected.parentIdentifier.rawValue):\(projected.filename)".utf8)
        return NSFileProviderItemVersion(
            contentVersion: contentToken.sha256(),
            metadataVersion: metadataToken.sha256()
        )
    }

    var isDownloaded: Bool {
        false
    }

    var isMostRecentVersionDownloaded: Bool {
        false
    }
}

private extension Data {
    func sha256() -> Data {
        let digest = SHA256.hash(data: self)
        return Data(digest)
    }
}
