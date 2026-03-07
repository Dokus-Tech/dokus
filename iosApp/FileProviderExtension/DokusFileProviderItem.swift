import Foundation
import FileProvider
import UniformTypeIdentifiers
import CryptoKit

struct DokusFileProviderItemState {
    let isUploaded: Bool
    let isUploading: Bool
    let uploadingError: NSError?
    let isDownloaded: Bool
    let isDownloading: Bool
    let downloadingError: NSError?
    let isMostRecentVersionDownloaded: Bool

    static let placeholder = DokusFileProviderItemState(
        isUploaded: true,
        isUploading: false,
        uploadingError: nil,
        isDownloaded: false,
        isDownloading: false,
        downloadingError: nil,
        isMostRecentVersionDownloaded: false
    )

    static let materializedContainer = DokusFileProviderItemState(
        isUploaded: true,
        isUploading: false,
        uploadingError: nil,
        isDownloaded: true,
        isDownloading: false,
        downloadingError: nil,
        isMostRecentVersionDownloaded: true
    )
}

struct DokusPendingFileProviderItemState: Hashable {
    let itemIdentifier: NSFileProviderItemIdentifier
    let isUploaded: Bool
    let isUploading: Bool
    let uploadingErrorCode: Int?
    let uploadingErrorDomain: String?
    let uploadingErrorDescription: String?
    let isDownloaded: Bool
    let isDownloading: Bool
    let downloadingErrorCode: Int?
    let downloadingErrorDomain: String?
    let downloadingErrorDescription: String?
    let isMostRecentVersionDownloaded: Bool

    init(item: any NSFileProviderItem) {
        let uploadingError = (item.uploadingError ?? nil).map { $0 as NSError }
        let downloadingError = (item.downloadingError ?? nil).map { $0 as NSError }
        self.itemIdentifier = item.itemIdentifier
        self.isUploaded = item.isUploaded ?? true
        self.isUploading = item.isUploading ?? false
        self.uploadingErrorCode = uploadingError?.code
        self.uploadingErrorDomain = uploadingError?.domain
        self.uploadingErrorDescription = uploadingError?.localizedDescription
        self.isDownloaded = item.isDownloaded ?? false
        self.isDownloading = item.isDownloading ?? false
        self.downloadingErrorCode = downloadingError?.code
        self.downloadingErrorDomain = downloadingError?.domain
        self.downloadingErrorDescription = downloadingError?.localizedDescription
        self.isMostRecentVersionDownloaded = item.isMostRecentVersionDownloaded ?? false
    }

    private func makeError(
        domain: String?,
        code: Int?,
        description: String?
    ) -> NSError? {
        guard let domain, let code else {
            return nil
        }
        var userInfo: [String: Any] = [:]
        if let description {
            userInfo[NSLocalizedDescriptionKey] = description
        }
        return NSError(domain: domain, code: code, userInfo: userInfo)
    }

    var itemState: DokusFileProviderItemState {
        DokusFileProviderItemState(
            isUploaded: isUploaded,
            isUploading: isUploading,
            uploadingError: makeError(
                domain: uploadingErrorDomain,
                code: uploadingErrorCode,
                description: uploadingErrorDescription
            ),
            isDownloaded: isDownloaded,
            isDownloading: isDownloading,
            downloadingError: makeError(
                domain: downloadingErrorDomain,
                code: downloadingErrorCode,
                description: downloadingErrorDescription
            ),
            isMostRecentVersionDownloaded: isMostRecentVersionDownloaded
        )
    }
}

final class DokusFileProviderItem: NSObject, NSFileProviderItem {
    private let projected: DokusProjectedItem
    private let state: DokusFileProviderItemState

    init(projected: DokusProjectedItem, state: DokusFileProviderItemState = .placeholder) {
        self.projected = projected
        self.state = state
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

    var contentPolicy: NSFileProviderContentPolicy {
        projected.contentPolicy
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
        state.isDownloaded
    }

    var isUploaded: Bool {
        state.isUploaded
    }

    var isUploading: Bool {
        state.isUploading
    }

    var uploadingError: Error? {
        state.uploadingError
    }

    var isDownloading: Bool {
        state.isDownloading
    }

    var downloadingError: Error? {
        state.downloadingError
    }

    var isMostRecentVersionDownloaded: Bool {
        state.isMostRecentVersionDownloaded
    }
}

private extension Data {
    func sha256() -> Data {
        let digest = SHA256.hash(data: self)
        return Data(digest)
    }
}
