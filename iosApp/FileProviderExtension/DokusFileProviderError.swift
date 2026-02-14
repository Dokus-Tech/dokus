import Foundation
import FileProvider

enum DokusFileProviderError: LocalizedError {
    case notAuthenticated
    case network(String)
    case invalidServerResponse
    case unsupportedOperation(String)
    case noSuchItem

    var errorDescription: String? {
        switch self {
        case .notAuthenticated:
            return "Authentication is required. Open Dokus and sign in."
        case .network(let message):
            return message
        case .invalidServerResponse:
            return "Invalid server response"
        case .unsupportedOperation(let message):
            return message
        case .noSuchItem:
            return "Item not found"
        }
    }

    var nsError: NSError {
        switch self {
        case .notAuthenticated:
            return NSError(
                domain: NSFileProviderErrorDomain,
                code: NSFileProviderError.notAuthenticated.rawValue,
                userInfo: [NSLocalizedDescriptionKey: errorDescription ?? "Not authenticated"]
            )
        case .network(let message):
            return NSError(
                domain: NSFileProviderErrorDomain,
                code: NSFileProviderError.serverUnreachable.rawValue,
                userInfo: [NSLocalizedDescriptionKey: message]
            )
        case .invalidServerResponse:
            return NSError(
                domain: NSFileProviderErrorDomain,
                code: NSFileProviderError.cannotSynchronize.rawValue,
                userInfo: [NSLocalizedDescriptionKey: errorDescription ?? "Cannot synchronize"]
            )
        case .unsupportedOperation(let message):
            return NSError(
                domain: NSCocoaErrorDomain,
                code: NSFileWriteNoPermissionError,
                userInfo: [NSLocalizedDescriptionKey: message]
            )
        case .noSuchItem:
            return NSError(
                domain: NSFileProviderErrorDomain,
                code: NSFileProviderError.noSuchItem.rawValue,
                userInfo: [NSLocalizedDescriptionKey: errorDescription ?? "No such item"]
            )
        }
    }
}
