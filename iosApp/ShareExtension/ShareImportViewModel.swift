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
                    ShareImportFailure(
                        type: .payloadUnavailable,
                        message: ShareLocalizedMessage(key: .errorNoPdfFound),
                        retryable: false
                    )
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
                    ShareImportFailure(
                        type: .payloadUnavailable,
                        message: ShareLocalizedMessage(key: .errorNoPdfRetry),
                        retryable: false
                    )
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
            transition(to: .error(failure))
        } catch {
            transition(
                to: .error(
                    ShareImportFailure(
                        type: .unknown,
                        message: ShareLocalizedMessage(key: .errorUnexpectedPrepareUpload),
                        retryable: true
                    )
                )
            )
        }
    }

    private func uploadRemainingFiles() async {
        guard let context = sessionContext else {
            transition(
                to: .error(
                    ShareImportFailure(
                        type: .unknown,
                        message: ShareLocalizedMessage(key: .errorUploadSessionUnavailable),
                        retryable: true
                    )
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
                        workspaceName: context.workspaceName,
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
                                    workspaceName: context.workspaceName,
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
                transition(to: .error(failure))
                return
            } catch {
                let failure = ShareImportFailure(
                    type: .upload,
                    message: ShareLocalizedMessage(key: .errorUploadFailed),
                    retryable: true
                )
                fileStatuses[index] = .failed(failure)
                transition(to: .error(failure))
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

            VStack(spacing: 22) {
                Text(ShareLocalizationKey.appLabel.localized())
                    .font(.caption.weight(.semibold))
                    .tracking(1.2)
                    .foregroundStyle(ShareImportTheme.textMuted)

                switch viewModel.state {
                case .loadingPayload:
                    loadingView(
                        heading: .headingPreparing,
                        description: .descriptionPreparing
                    )
                case .resolvingSession:
                    loadingView(
                        heading: .headingResolving,
                        description: .descriptionResolving
                    )
                case .uploading(let progress):
                    uploadingView(progress: progress)
                case .success(let uploadedCount):
                    successView(uploadedCount: uploadedCount)
                case .error(let failure):
                    errorView(failure: failure)
                }
            }
            .frame(maxWidth: 360)
            .padding(.horizontal, 24)
            .padding(.vertical, 32)
        }
    }

    @ViewBuilder
    private func loadingView(heading: ShareLocalizationKey, description: ShareLocalizationKey) -> some View {
        ShareRingLoader()

        Text(heading.localized())
            .font(.title3.weight(.semibold))
            .foregroundStyle(ShareImportTheme.textPrimary)
            .multilineTextAlignment(.center)

        Text(description.localized())
            .font(.body)
            .foregroundStyle(ShareImportTheme.textSecondary)
            .multilineTextAlignment(.center)

        Text(ShareLocalizationKey.keepOpen.localized())
            .font(.footnote)
            .foregroundStyle(ShareImportTheme.textMuted)
            .multilineTextAlignment(.center)
    }

    @ViewBuilder
    private func uploadingView(progress: UploadProgress) -> some View {
        ShareRingLoader()

        Text(
            ShareLocalizationKey.headingUploading.localized(
                arguments: [.string(progress.workspaceName)]
            )
        )
        .font(.title3.weight(.semibold))
        .foregroundStyle(ShareImportTheme.textPrimary)
        .multilineTextAlignment(.center)

        Text(
            ShareLocalizationKey.fileProgress.localized(
                arguments: [
                    .int(progress.currentIndex),
                    .int(progress.totalCount)
                ]
            )
        )
            .font(.footnote)
            .foregroundStyle(ShareImportTheme.textSecondary)
            .multilineTextAlignment(.center)

        ProgressView(value: progress.overallProgress)
            .progressViewStyle(.linear)
            .tint(ShareImportTheme.accent)
            .frame(maxWidth: .infinity)

        Text(progress.fileName)
            .font(.body)
            .foregroundStyle(ShareImportTheme.textPrimary)
            .lineLimit(2)
            .multilineTextAlignment(.center)
            .frame(maxWidth: .infinity)

        HStack {
            Text(ShareLocalizationKey.progressLabel.localized())
            Spacer()
            Text("\(Int(progress.overallProgress * 100).clamped(to: 0...100))%")
        }
        .font(.footnote)
        .foregroundStyle(ShareImportTheme.textSecondary)
    }

    @ViewBuilder
    private func successView(uploadedCount: Int) -> some View {
        ShareAnimatedCheck()
            .frame(height: 96)

        Text(ShareLocalizationKey.headingSuccess.localized())
            .font(.title3.weight(.semibold))
            .foregroundStyle(ShareImportTheme.textPrimary)
            .multilineTextAlignment(.center)

        let summary = if uploadedCount == 1 {
            ShareLocalizationKey.uploadedSingle.localized()
        } else {
            ShareLocalizationKey.uploadedMultiple.localized(arguments: [.int(uploadedCount)])
        }
        Text(summary)
            .font(.body)
            .foregroundStyle(ShareImportTheme.textPrimary)
            .multilineTextAlignment(.center)

        Text(ShareLocalizationKey.savedToDocuments.localized())
            .font(.footnote)
            .foregroundStyle(ShareImportTheme.textSecondary)

        Text(ShareLocalizationKey.closingAutomatically.localized())
            .font(.footnote)
            .foregroundStyle(ShareImportTheme.textMuted)
    }

    @ViewBuilder
    private func errorView(failure: ShareImportFailure) -> some View {
        ShareFailureGlyph()
            .frame(height: 96)

        Text(ShareLocalizationKey.headingError.localized())
            .font(.title3.weight(.semibold))
            .foregroundStyle(ShareImportTheme.textPrimary)
            .multilineTextAlignment(.center)

        Text(failure.localizedMessage())
            .font(.body)
            .foregroundStyle(ShareImportTheme.textSecondary)
            .multilineTextAlignment(.center)
            .frame(maxWidth: .infinity)

        VStack(spacing: 12) {
            if failure.retryable {
                Button {
                    Task { await viewModel.retry() }
                } label: {
                    Text(ShareLocalizationKey.buttonRetry.localized())
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(SharePrimaryButtonStyle())
            }

            if failure.type.canOpenApp {
                Button(action: onOpenApp) {
                    Text(ShareLocalizationKey.buttonOpenApp.localized())
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(ShareSecondaryButtonStyle())

                if failure.type == .workspaceContextUnavailable {
                    Text(ShareLocalizationKey.workspaceSwitchHint.localized())
                        .font(.footnote)
                        .foregroundStyle(ShareImportTheme.textMuted)
                        .multilineTextAlignment(.center)
                }
            }

            Button(action: onDone) {
                Text(ShareLocalizationKey.buttonClose.localized())
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(ShareSecondaryButtonStyle())
        }
        .padding(.top, 4)
    }
}

private struct ShareRingLoader: View {
    var body: some View {
        ProgressView()
            .progressViewStyle(.circular)
            .tint(ShareImportTheme.textPrimary)
            .scaleEffect(1.2)
            .frame(width: 72, height: 72)
    }
}

private struct ShareAnimatedCheck: View {
    @State private var circleProgress: CGFloat = 0
    @State private var checkProgress: CGFloat = 0
    @State private var scale: CGFloat = 0.94

    var body: some View {
        ZStack {
            Circle()
                .trim(from: 0, to: circleProgress)
                .stroke(
                    ShareImportTheme.textPrimary,
                    style: StrokeStyle(lineWidth: 2.0, lineCap: .round, lineJoin: .round)
                )
                .rotationEffect(.degrees(-90))
                .frame(width: 82, height: 82)

            ShareCheckShape()
                .trim(from: 0, to: checkProgress)
                .stroke(
                    ShareImportTheme.textPrimary,
                    style: StrokeStyle(lineWidth: 2.4, lineCap: .round, lineJoin: .round)
                )
                .frame(width: 34, height: 24)
        }
            .scaleEffect(scale)
            .onAppear {
                circleProgress = 0
                checkProgress = 0
                scale = 0.94

                withAnimation(.easeInOut(duration: 0.46)) {
                    circleProgress = 1
                }

                DispatchQueue.main.asyncAfter(deadline: .now() + 0.18) {
                    withAnimation(.easeInOut(duration: 0.28)) {
                        checkProgress = 1
                    }
                }

                withAnimation(.spring(response: 0.38, dampingFraction: 0.78)) {
                    scale = 1
                }
            }
    }
}

private struct ShareCheckShape: Shape {
    func path(in rect: CGRect) -> Path {
        var path = Path()
        path.move(to: CGPoint(x: rect.minX + rect.width * 0.06, y: rect.minY + rect.height * 0.54))
        path.addLine(to: CGPoint(x: rect.minX + rect.width * 0.38, y: rect.minY + rect.height * 0.86))
        path.addLine(to: CGPoint(x: rect.minX + rect.width * 0.94, y: rect.minY + rect.height * 0.12))
        return path
    }
}

private struct ShareFailureGlyph: View {
    var body: some View {
        ZStack {
            Circle()
                .stroke(ShareImportTheme.textSecondary, lineWidth: 1.6)
                .frame(width: 82, height: 82)
            Image(systemName: "xmark")
                .font(.system(size: 24, weight: .medium))
                .foregroundStyle(ShareImportTheme.textSecondary)
        }
    }
}

private enum ShareImportTheme {
    static let canvas = Color.black
    static let textPrimary = Color.white
    static let textSecondary = Color.white.opacity(0.78)
    static let textMuted = Color.white.opacity(0.56)
    static let accent = Color.white
}

private struct SharePrimaryButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.body.weight(.semibold))
            .foregroundStyle(Color.black)
            .padding(.vertical, 12)
            .background(
                RoundedRectangle(cornerRadius: 6, style: .continuous)
                    .fill(Color.white.opacity(configuration.isPressed ? 0.75 : 1))
            )
    }
}

private struct ShareSecondaryButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.body.weight(.medium))
            .foregroundStyle(ShareImportTheme.textPrimary.opacity(configuration.isPressed ? 0.72 : 1))
            .padding(.vertical, 12)
            .background(
                RoundedRectangle(cornerRadius: 6, style: .continuous)
                    .stroke(Color.white.opacity(0.28), lineWidth: 1)
            )
    }
}
