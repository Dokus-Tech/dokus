import Foundation

enum ShareImportErrorType {
    case notAuthenticated
    case workspaceContextUnavailable
    case workspaceSelectionFailed
    case payloadUnavailable
    case network
    case upload
    case unknown

    var canOpenApp: Bool {
        switch self {
        case .notAuthenticated, .workspaceContextUnavailable, .workspaceSelectionFailed:
            return true
        case .payloadUnavailable, .network, .upload, .unknown:
            return false
        }
    }
}

enum ShareImportState {
    case loadingPayload
    case resolvingSession
    case uploading(UploadProgress)
    case success(uploadedCount: Int)
    case error(type: ShareImportErrorType, message: String, retryable: Bool)
}

struct UploadProgress {
    let currentIndex: Int
    let totalCount: Int
    let fileName: String
    let fileProgress: Double
    let overallProgress: Double
}

struct SharedImportFile {
    let name: String
    let mimeType: String
    let fileURL: URL
}

enum SharedFileUploadStatus {
    case pending
    case uploaded(documentId: String?)
    case failed(ShareImportFailure)
}

struct ShareImportFailure: Error {
    let type: ShareImportErrorType
    let message: String
    let retryable: Bool
}

extension Comparable {
    func clamped(to range: ClosedRange<Self>) -> Self {
        min(max(self, range.lowerBound), range.upperBound)
    }
}
