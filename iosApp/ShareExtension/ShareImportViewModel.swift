import Foundation
import SwiftUI
import UniformTypeIdentifiers

@MainActor
final class ShareImportViewModel: ObservableObject {
    @Published private(set) var state: ShareImportState = .loadingPayload

    var onUploadingStateChanged: ((Bool) -> Void)?
    var onStateChanged: ((ShareImportState) -> Void)?

    private let uploader: ShareImportUploader
    private var sharedFiles: [SharedImportFile] = []
    private var fileStatuses: [SharedFileUploadStatus] = []
    private var sessionContext: ShareImportUploader.UploadSessionContext?
    private var hasStarted = false

    init(uploader: ShareImportUploader) {
        self.uploader = uploader
    }

    func start(with extensionItems: [NSExtensionItem]) async {
        guard !hasStarted else { return }
        hasStarted = true

        transition(to: .loadingPayload)
        let files = await loadSharedFiles(from: extensionItems)
        guard !files.isEmpty else {
            transition(
                to: .error(
                    type: .payloadUnavailable,
                    message: "No PDF files were found in this share action.",
                    retryable: false
                )
            )
            return
        }

        sharedFiles = files
        fileStatuses = Array(repeating: .pending, count: files.count)

        await resolveSessionAndUpload()
    }

    func retry() async {
        guard !sharedFiles.isEmpty else {
            transition(
                to: .error(
                    type: .payloadUnavailable,
                    message: "No PDF files are available to retry.",
                    retryable: false
                )
            )
            return
        }

        if sessionContext == nil {
            await resolveSessionAndUpload()
        } else {
            await uploadRemainingFiles()
        }
    }

    func cleanupTemporaryFiles() {
        let fileManager = FileManager.default
        for file in sharedFiles {
            if fileManager.fileExists(atPath: file.fileURL.path) {
                try? fileManager.removeItem(at: file.fileURL)
            }
        }
    }

    private func resolveSessionAndUpload() async {
        transition(to: .resolvingSession)

        do {
            sessionContext = try await uploader.resolveSessionContext()
            await uploadRemainingFiles()
        } catch let failure as ShareImportFailure {
            transition(to: .error(type: failure.type, message: failure.message, retryable: failure.retryable))
        } catch {
            transition(
                to: .error(
                    type: .unknown,
                    message: "Unexpected error while preparing upload.",
                    retryable: true
                )
            )
        }
    }

    private func uploadRemainingFiles() async {
        guard let context = sessionContext else {
            transition(
                to: .error(
                    type: .unknown,
                    message: "Upload session is unavailable.",
                    retryable: true
                )
            )
            return
        }

        let totalCount = sharedFiles.count

        for index in sharedFiles.indices {
            if case .uploaded = fileStatuses[index] {
                continue
            }

            let file = sharedFiles[index]
            let completedBefore = fileStatuses.prefix(index).filter {
                if case .uploaded = $0 {
                    return true
                }
                return false
            }.count

            transition(
                to: .uploading(
                    UploadProgress(
                        currentIndex: index + 1,
                        totalCount: totalCount,
                        fileName: file.name,
                        fileProgress: 0,
                        overallProgress: Double(completedBefore) / Double(totalCount)
                    )
                )
            )

            do {
                let documentId = try await uploader.upload(file: file, context: context) { [weak self] progress in
                    Task { @MainActor in
                        guard let self else { return }
                        let uploadedCount = self.fileStatuses.filter {
                            if case .uploaded = $0 {
                                return true
                            }
                            return false
                        }.count
                        let overall = (Double(uploadedCount) + progress.clamped(to: 0...1)) / Double(totalCount)
                        self.transition(
                            to: .uploading(
                                UploadProgress(
                                    currentIndex: index + 1,
                                    totalCount: totalCount,
                                    fileName: file.name,
                                    fileProgress: progress.clamped(to: 0...1),
                                    overallProgress: overall.clamped(to: 0...1)
                                )
                            )
                        )
                    }
                }

                fileStatuses[index] = .uploaded(documentId: documentId)
            } catch let failure as ShareImportFailure {
                fileStatuses[index] = .failed(failure)
                transition(to: .error(type: failure.type, message: failure.message, retryable: failure.retryable))
                return
            } catch {
                let failure = ShareImportFailure(
                    type: .upload,
                    message: "Upload failed. Please try again.",
                    retryable: true
                )
                fileStatuses[index] = .failed(failure)
                transition(to: .error(type: failure.type, message: failure.message, retryable: failure.retryable))
                return
            }
        }

        let uploadedCount = fileStatuses.filter {
            if case .uploaded = $0 {
                return true
            }
            return false
        }.count

        transition(to: .success(uploadedCount: uploadedCount))
    }

    private func transition(to newState: ShareImportState) {
        state = newState
        onStateChanged?(newState)
        switch newState {
        case .uploading:
            onUploadingStateChanged?(true)
        case .loadingPayload, .resolvingSession, .success, .error:
            onUploadingStateChanged?(false)
        }
    }

    private func loadSharedFiles(from extensionItems: [NSExtensionItem]) async -> [SharedImportFile] {
        let providers = extensionItems
            .flatMap { $0.attachments ?? [] }
            .filter { provider in
                provider.hasItemConformingToTypeIdentifier(UTType.pdf.identifier) ||
                    provider.hasItemConformingToTypeIdentifier(UTType.fileURL.identifier)
            }

        guard !providers.isEmpty else { return [] }

        var loaded: [SharedImportFile] = []
        for (index, provider) in providers.enumerated() {
            if let file = await loadPdf(from: provider, index: index) {
                loaded.append(file)
            }
        }
        return loaded
    }

    private func loadPdf(from provider: NSItemProvider, index: Int) async -> SharedImportFile? {
        let suggestedName = provider.suggestedName

        if provider.hasItemConformingToTypeIdentifier(UTType.pdf.identifier) {
            if let file = await loadFileRepresentation(
                provider: provider,
                typeIdentifier: UTType.pdf.identifier,
                fallbackIndex: index,
                suggestedName: suggestedName
            ) {
                return file
            }

            if let data = await loadDataRepresentation(provider: provider, typeIdentifier: UTType.pdf.identifier) {
                let fileName = Self.resolveFileName(
                    suggestedName: suggestedName,
                    fallback: suggestedName,
                    index: index
                )
                return Self.persistData(
                    data: data,
                    fileName: fileName
                )
            }
        }

        if provider.hasItemConformingToTypeIdentifier(UTType.fileURL.identifier) {
            if let url = await loadFileURL(provider: provider), Self.isLikelyPdf(url: url, suggestedName: suggestedName) {
                let fileName = Self.resolveFileName(
                    suggestedName: suggestedName,
                    fallback: url.lastPathComponent,
                    index: index
                )
                return Self.copyIntoLocalFile(url: url, fileName: fileName, index: index)
            }
        }

        return nil
    }

    private func loadFileRepresentation(
        provider: NSItemProvider,
        typeIdentifier: String,
        fallbackIndex: Int,
        suggestedName: String?
    ) async -> SharedImportFile? {
        await withCheckedContinuation { continuation in
            provider.loadFileRepresentation(forTypeIdentifier: typeIdentifier) { fileURL, _ in
                guard let fileURL else {
                    continuation.resume(returning: nil)
                    return
                }

                guard Self.isLikelyPdf(url: fileURL, suggestedName: suggestedName) else {
                    continuation.resume(returning: nil)
                    return
                }

                let fileName = Self.resolveFileName(
                    suggestedName: suggestedName,
                    fallback: fileURL.lastPathComponent,
                    index: fallbackIndex
                )
                continuation.resume(
                    returning: Self.copyIntoLocalFile(
                        url: fileURL,
                        fileName: fileName,
                        index: fallbackIndex
                    )
                )
            }
        }
    }

    private func loadDataRepresentation(provider: NSItemProvider, typeIdentifier: String) async -> Data? {
        await withCheckedContinuation { continuation in
            provider.loadDataRepresentation(forTypeIdentifier: typeIdentifier) { data, _ in
                continuation.resume(returning: data)
            }
        }
    }

    private func loadFileURL(provider: NSItemProvider) async -> URL? {
        await withCheckedContinuation { continuation in
            provider.loadItem(forTypeIdentifier: UTType.fileURL.identifier, options: nil) { item, _ in
                if let url = item as? URL {
                    continuation.resume(returning: url)
                    return
                }
                if let nsUrl = item as? NSURL {
                    continuation.resume(returning: nsUrl as URL)
                    return
                }
                if let data = item as? Data {
                    continuation.resume(returning: URL(dataRepresentation: data, relativeTo: nil))
                    return
                }
                if let nsData = item as? NSData {
                    continuation.resume(returning: URL(dataRepresentation: nsData as Data, relativeTo: nil))
                    return
                }
                if let path = item as? String {
                    continuation.resume(returning: URL(fileURLWithPath: path))
                    return
                }
                continuation.resume(returning: nil)
            }
        }
    }

    private nonisolated static func copyIntoLocalFile(url: URL, fileName: String, index: Int) -> SharedImportFile? {
        let fileManager = FileManager.default
        let destinationURL = temporaryDirectory().appendingPathComponent("\(UUID().uuidString)-\(index).pdf")

        do {
            if fileManager.fileExists(atPath: destinationURL.path) {
                try fileManager.removeItem(at: destinationURL)
            }

            // Action extension temp URLs are short-lived, so copy before callback returns.
            try fileManager.copyItem(at: url, to: destinationURL)
        } catch {
            guard
                let data = try? Data(contentsOf: url),
                let fallbackFile = persistData(
                    data: data,
                    fileName: fileName
                )
            else {
                return nil
            }
            return fallbackFile
        }

        return SharedImportFile(
            name: fileName,
            mimeType: "application/pdf",
            fileURL: destinationURL
        )
    }

    private nonisolated static func persistData(data: Data, fileName: String) -> SharedImportFile? {
        let destinationURL = temporaryDirectory().appendingPathComponent("\(UUID().uuidString)-\(fileName)")

        do {
            try data.write(to: destinationURL, options: .atomic)
            return SharedImportFile(
                name: fileName,
                mimeType: "application/pdf",
                fileURL: destinationURL
            )
        } catch {
            return nil
        }
    }

    private nonisolated static func temporaryDirectory() -> URL {
        let tempDirectory = FileManager.default.temporaryDirectory.appendingPathComponent("ShareImport", isDirectory: true)
        try? FileManager.default.createDirectory(at: tempDirectory, withIntermediateDirectories: true)
        return tempDirectory
    }

    private nonisolated static func resolveFileName(
        suggestedName: String?,
        fallback: String?,
        index: Int
    ) -> String {
        let trimmedSuggested = suggestedName?.trimmingCharacters(in: .whitespacesAndNewlines)
        let suggestedCandidate = trimmedSuggested.flatMap { $0.isEmpty ? nil : $0 }
        let trimmedFallback = fallback?.trimmingCharacters(in: .whitespacesAndNewlines)
        let fallbackCandidate = trimmedFallback.flatMap { $0.isEmpty ? nil : $0 }
        let rawName = suggestedCandidate ?? fallbackCandidate ?? "shared-\(index + 1).pdf"

        let sanitized = rawName.replacingOccurrences(of: "/", with: "-")
        if sanitized.lowercased().hasSuffix(".pdf") {
            return sanitized
        }
        return "\(sanitized).pdf"
    }

    private nonisolated static func isLikelyPdf(url: URL, suggestedName: String?) -> Bool {
        if url.pathExtension.lowercased() == "pdf" {
            return true
        }
        if let suggestedName = suggestedName?.lowercased(), suggestedName.hasSuffix(".pdf") {
            return true
        }
        return false
    }
}

struct ShareImportRootView: View {
    @ObservedObject var viewModel: ShareImportViewModel
    let onDone: () -> Void
    let onOpenApp: () -> Void

    var body: some View {
        ZStack {
            ShareImportTheme.canvas
                .ignoresSafeArea()

            VStack(spacing: 0) {
                Spacer(minLength: 12)

                VStack(alignment: .leading, spacing: 20) {
                    VStack(alignment: .leading, spacing: 6) {
                        Text("ADD TO DOKUS")
                            .font(.caption.weight(.semibold))
                            .tracking(1.1)
                            .foregroundStyle(ShareImportTheme.textSecondary)
                        Text(primaryHeading)
                            .font(.title3.weight(.semibold))
                            .foregroundStyle(ShareImportTheme.textPrimary)
                    }

                    VStack(spacing: 18) {
                        switch viewModel.state {
                        case .loadingPayload:
                            loadingView(description: "Preparing your PDF files.")
                        case .resolvingSession:
                            loadingView(description: "Checking account and workspace.")
                        case .uploading(let progress):
                            uploadingView(progress: progress)
                        case .success(let uploadedCount):
                            successView(uploadedCount: uploadedCount)
                        case .error(let type, let message, let retryable):
                            errorView(type: type, message: message, retryable: retryable)
                        }
                    }
                }
                .frame(maxWidth: 480, alignment: .leading)
                .padding(24)
                .background(ShareImportTheme.cardBackground)
                .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
                .overlay(
                    RoundedRectangle(cornerRadius: 22, style: .continuous)
                        .stroke(ShareImportTheme.cardBorder, lineWidth: 1)
                )
                .shadow(color: Color.black.opacity(0.05), radius: 18, y: 10)

                Spacer(minLength: 12)
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 16)
        }
    }

    private var primaryHeading: String {
        switch viewModel.state {
        case .loadingPayload:
            return "Preparing import"
        case .resolvingSession:
            return "Checking context"
        case .uploading:
            return "Uploading to Documents"
        case .success:
            return "Upload complete"
        case .error:
            return "Upload failed"
        }
    }

    @ViewBuilder
    private func loadingView(description: String) -> some View {
        ProgressView()
            .progressViewStyle(.circular)
            .tint(ShareImportTheme.accent)

        Text(description)
            .font(.body)
            .foregroundStyle(ShareImportTheme.textPrimary)
            .multilineTextAlignment(.leading)

        Text("Keep this screen open.")
            .font(.footnote)
            .foregroundStyle(ShareImportTheme.textSecondary)
    }

    @ViewBuilder
    private func uploadingView(progress: UploadProgress) -> some View {
        Text("File \(progress.currentIndex) of \(progress.totalCount)")
            .font(.footnote)
            .foregroundStyle(ShareImportTheme.textSecondary)
            .frame(maxWidth: .infinity, alignment: .leading)

        ProgressView(value: progress.overallProgress)
            .progressViewStyle(.linear)
            .tint(ShareImportTheme.accent)
            .frame(maxWidth: .infinity)

        Text(progress.fileName)
            .font(.body)
            .foregroundStyle(ShareImportTheme.textPrimary)
            .lineLimit(2)
            .multilineTextAlignment(.leading)
            .frame(maxWidth: .infinity, alignment: .leading)

        HStack {
            Text("Progress")
            Spacer()
            Text("\(Int(progress.overallProgress * 100))%")
        }
        .font(.footnote)
        .foregroundStyle(ShareImportTheme.textSecondary)
    }

    @ViewBuilder
    private func successView(uploadedCount: Int) -> some View {
        CheckPulseView()
            .frame(height: 96)

        let summary = uploadedCount == 1 ? "1 file uploaded" : "\(uploadedCount) files uploaded"
        Text(summary)
            .font(.title3.weight(.semibold))
            .foregroundStyle(ShareImportTheme.textPrimary)
            .frame(maxWidth: .infinity, alignment: .leading)

        Text("Saved to Documents")
            .font(.footnote)
            .foregroundStyle(ShareImportTheme.textSecondary)
            .frame(maxWidth: .infinity, alignment: .leading)
        
        Text("Closing automatically...")
            .font(.footnote)
            .foregroundStyle(ShareImportTheme.textSecondary)
            .frame(maxWidth: .infinity, alignment: .leading)
    }

    @ViewBuilder
    private func errorView(type: ShareImportErrorType, message: String, retryable: Bool) -> some View {
        Text(message)
            .font(.body)
            .foregroundStyle(ShareImportTheme.textSecondary)
            .multilineTextAlignment(.leading)
            .frame(maxWidth: .infinity, alignment: .leading)

        VStack(spacing: 12) {
            if retryable {
                Button {
                    Task { await viewModel.retry() }
                } label: {
                    Text("Retry")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(SharePrimaryButtonStyle())
            }

            if type.canOpenApp {
                Button(action: onOpenApp) {
                    Text("Open app")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(ShareSecondaryButtonStyle())

                if type == .workspaceContextUnavailable {
                    Text("Switch workspace in app and try again.")
                        .font(.footnote)
                        .foregroundStyle(ShareImportTheme.textSecondary)
                        .multilineTextAlignment(.leading)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }
            }

            Button(action: onDone) {
                Text("Close")
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(ShareSecondaryButtonStyle())
        }
        .padding(.top, 4)
    }
}

private struct CheckPulseView: View {
    @State private var scale: CGFloat = 0.7
    @State private var opacity: Double = 0.2

    var body: some View {
        Image(systemName: "checkmark.circle.fill")
            .font(.system(size: 72, weight: .semibold))
            .foregroundStyle(ShareImportTheme.accent)
            .scaleEffect(scale)
            .opacity(opacity)
            .onAppear {
                withAnimation(.spring(response: 0.42, dampingFraction: 0.72)) {
                    scale = 1
                    opacity = 1
                }
            }
    }
}

private enum ShareImportTheme {
    static let canvas = Color(red: 0.964, green: 0.964, blue: 0.957)
    static let cardBackground = Color.white
    static let cardBorder = Color.black.opacity(0.1)
    static let textPrimary = Color.black
    static let textSecondary = Color.black.opacity(0.58)
    static let accent = Color.black
}

private struct SharePrimaryButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.body.weight(.semibold))
            .foregroundStyle(Color.white)
            .padding(.vertical, 12)
            .background(
                RoundedRectangle(cornerRadius: 10, style: .continuous)
                    .fill(Color.black.opacity(configuration.isPressed ? 0.75 : 1))
            )
    }
}

private struct ShareSecondaryButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.body.weight(.medium))
            .foregroundStyle(ShareImportTheme.textPrimary.opacity(configuration.isPressed ? 0.65 : 1))
            .padding(.vertical, 12)
            .background(
                RoundedRectangle(cornerRadius: 10, style: .continuous)
                    .stroke(Color.black.opacity(0.16), lineWidth: 1)
            )
    }
}
