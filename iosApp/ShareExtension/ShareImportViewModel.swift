import Foundation
import SwiftUI
import UniformTypeIdentifiers

@MainActor
final class ShareImportViewModel: ObservableObject {
    @Published private(set) var state: ShareImportState = .loadingPayload

    var onUploadingStateChanged: ((Bool) -> Void)?

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
        if provider.hasItemConformingToTypeIdentifier(UTType.pdf.identifier) {
            if let file = await loadFileRepresentation(
                provider: provider,
                typeIdentifier: UTType.pdf.identifier,
                fallbackIndex: index
            ) {
                return file
            }

            if let data = await loadDataRepresentation(provider: provider, typeIdentifier: UTType.pdf.identifier) {
                return persistData(
                    data: data,
                    fileName: resolveFileName(provider: provider, fallback: provider.suggestedName, index: index)
                )
            }
        }

        if provider.hasItemConformingToTypeIdentifier(UTType.fileURL.identifier) {
            if let url = await loadFileURL(provider: provider), isLikelyPdf(url: url, provider: provider) {
                return copyIntoLocalFile(url: url, provider: provider, index: index)
            }
        }

        return nil
    }

    private func loadFileRepresentation(
        provider: NSItemProvider,
        typeIdentifier: String,
        fallbackIndex: Int
    ) async -> SharedImportFile? {
        await withCheckedContinuation { continuation in
            provider.loadFileRepresentation(forTypeIdentifier: typeIdentifier) { [weak self] fileURL, _ in
                guard
                    let self,
                    let fileURL,
                    self.isLikelyPdf(url: fileURL, provider: provider)
                else {
                    continuation.resume(returning: nil)
                    return
                }

                continuation.resume(returning: self.copyIntoLocalFile(url: fileURL, provider: provider, index: fallbackIndex))
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

    private func copyIntoLocalFile(url: URL, provider: NSItemProvider, index: Int) -> SharedImportFile? {
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
                    fileName: resolveFileName(provider: provider, fallback: url.lastPathComponent, index: index)
                )
            else {
                return nil
            }
            return fallbackFile
        }

        return SharedImportFile(
            name: resolveFileName(provider: provider, fallback: url.lastPathComponent, index: index),
            mimeType: "application/pdf",
            fileURL: destinationURL
        )
    }

    private func persistData(data: Data, fileName: String) -> SharedImportFile? {
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

    private func temporaryDirectory() -> URL {
        let tempDirectory = FileManager.default.temporaryDirectory.appendingPathComponent("ShareImport", isDirectory: true)
        try? FileManager.default.createDirectory(at: tempDirectory, withIntermediateDirectories: true)
        return tempDirectory
    }

    private func resolveFileName(provider: NSItemProvider, fallback: String?, index: Int) -> String {
        let rawName = provider.suggestedName?.trimmingCharacters(in: .whitespacesAndNewlines)
            .flatMap { $0.isEmpty ? nil : $0 }
            ?? fallback?.trimmingCharacters(in: .whitespacesAndNewlines)
                .flatMap { $0.isEmpty ? nil : $0 }
            ?? "shared-\(index + 1).pdf"

        let sanitized = rawName.replacingOccurrences(of: "/", with: "-")
        if sanitized.lowercased().hasSuffix(".pdf") {
            return sanitized
        }
        return "\(sanitized).pdf"
    }

    private func isLikelyPdf(url: URL, provider: NSItemProvider) -> Bool {
        if url.pathExtension.lowercased() == "pdf" {
            return true
        }
        if let suggestedName = provider.suggestedName?.lowercased(), suggestedName.hasSuffix(".pdf") {
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
            Color(.systemBackground)
                .ignoresSafeArea()

            VStack(spacing: 20) {
                switch viewModel.state {
                case .loadingPayload:
                    loadingView(title: "Preparing files")
                case .resolvingSession:
                    loadingView(title: "Resolving session")
                case .uploading(let progress):
                    uploadingView(progress: progress)
                case .success(let uploadedCount):
                    successView(uploadedCount: uploadedCount)
                case .error(let type, let message, let retryable):
                    errorView(type: type, message: message, retryable: retryable)
                }
            }
            .frame(maxWidth: 420)
            .padding(24)
        }
    }

    @ViewBuilder
    private func loadingView(title: String) -> some View {
        ProgressView()
            .progressViewStyle(.circular)
            .tint(.primary)
        Text(title)
            .font(.headline)
            .foregroundStyle(.primary)
    }

    @ViewBuilder
    private func uploadingView(progress: UploadProgress) -> some View {
        Text("Uploading to Dokus")
            .font(.title3.weight(.semibold))
            .foregroundStyle(.primary)

        Text("File \(progress.currentIndex) of \(progress.totalCount)")
            .font(.subheadline)
            .foregroundStyle(.secondary)

        ProgressView(value: progress.overallProgress)
            .progressViewStyle(.linear)
            .tint(.primary)
            .frame(maxWidth: .infinity)

        Text(progress.fileName)
            .font(.body)
            .foregroundStyle(.primary)
            .lineLimit(2)
            .multilineTextAlignment(.center)

        Text("\(Int(progress.overallProgress * 100))%")
            .font(.footnote)
            .foregroundStyle(.secondary)
    }

    @ViewBuilder
    private func successView(uploadedCount: Int) -> some View {
        CheckPulseView()
            .frame(height: 96)

        let summary = uploadedCount == 1 ? "1 file uploaded" : "\(uploadedCount) files uploaded"
        Text(summary)
            .font(.title3.weight(.semibold))
            .foregroundStyle(.primary)

        VStack(spacing: 12) {
            Button(action: onDone) {
                Text("Done")
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.borderedProminent)
            .tint(.primary)

            Button(action: onOpenApp) {
                Text("Open Dokus")
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.bordered)
        }
        .padding(.top, 4)
    }

    @ViewBuilder
    private func errorView(type: ShareImportErrorType, message: String, retryable: Bool) -> some View {
        Text("Upload failed")
            .font(.title3.weight(.semibold))
            .foregroundStyle(.primary)

        Text(message)
            .font(.body)
            .foregroundStyle(.secondary)
            .multilineTextAlignment(.center)

        VStack(spacing: 12) {
            if retryable {
                Button {
                    Task { await viewModel.retry() }
                } label: {
                    Text("Retry")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent)
                .tint(.primary)
            }

            if type.canOpenApp {
                Button(action: onOpenApp) {
                    Text("Open app")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.bordered)

                if type == .workspaceContextUnavailable {
                    Text("Switch workspace in app and try again.")
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                        .multilineTextAlignment(.center)
                }
            }

            Button(action: onDone) {
                Text("Close")
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.bordered)
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
            .foregroundStyle(.primary)
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
