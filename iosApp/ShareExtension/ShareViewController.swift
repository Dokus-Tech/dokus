import UIKit
import UniformTypeIdentifiers

final class ShareViewController: UIViewController {
    private let appGroupIdentifier = "group.vision.invoid.dokus.share"
    private let sharedImportsDirectory = "SharedImports"
    private var hasProcessed = false

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)

        guard !hasProcessed else { return }
        hasProcessed = true

        processSharedPdfs()
    }

    private func processSharedPdfs() {
        guard let extensionItems = extensionContext?.inputItems as? [NSExtensionItem] else {
            finishRequest()
            return
        }

        let providers = extensionItems
            .flatMap({ $0.attachments ?? [] })
            .filter { $0.hasItemConformingToTypeIdentifier(UTType.pdf.identifier) }

        guard !providers.isEmpty else {
            finishRequest()
            return
        }

        do {
            let directoryUrl = try sharedImportsDirectoryUrl()
            let batchId = UUID().uuidString
            let group = DispatchGroup()
            let lock = NSLock()
            var persistedCount = 0

            for (index, provider) in providers.enumerated() {
                group.enter()
                provider.loadFileRepresentation(forTypeIdentifier: UTType.pdf.identifier) { [weak self] fileUrl, _ in
                    defer { group.leave() }
                    guard let self, let fileUrl else { return }

                    do {
                        try self.persistSharedPdf(
                            sourceUrl: fileUrl,
                            provider: provider,
                            batchId: batchId,
                            index: index,
                            directoryUrl: directoryUrl
                        )
                        lock.lock()
                        persistedCount += 1
                        lock.unlock()
                    } catch {
                        // Keep failure silent for share extension UX; host app import flow handles missing payload.
                    }
                }
            }

            group.notify(queue: .main) { [weak self] in
                guard let self else { return }

                if persistedCount > 0 {
                    do {
                        try self.writeBatchCount(
                            batchId: batchId,
                            expectedCount: providers.count,
                            directoryUrl: directoryUrl
                        )
                        self.openHostApp(batchId: batchId)
                    } catch {
                        self.cleanupBatch(
                            batchId: batchId,
                            expectedCount: providers.count,
                            directoryUrl: directoryUrl
                        )
                    }
                } else {
                    self.cleanupBatch(
                        batchId: batchId,
                        expectedCount: providers.count,
                        directoryUrl: directoryUrl
                    )
                }

                self.finishRequest()
            }
        } catch {
            finishRequest()
        }
    }

    private func sharedImportsDirectoryUrl() throws -> URL {
        let fileManager = FileManager.default

        guard let appGroupContainer = fileManager.containerURL(
            forSecurityApplicationGroupIdentifier: appGroupIdentifier
        ) else {
            throw NSError(domain: "ShareExtension", code: 1)
        }

        let directoryUrl = appGroupContainer
            .appendingPathComponent(sharedImportsDirectory, isDirectory: true)

        try fileManager.createDirectory(
            at: directoryUrl,
            withIntermediateDirectories: true,
            attributes: nil
        )

        return directoryUrl
    }

    private func persistSharedPdf(
        sourceUrl: URL,
        provider: NSItemProvider,
        batchId: String,
        index: Int,
        directoryUrl: URL
    ) throws {
        let fileManager = FileManager.default
        let pdfDestinationUrl = directoryUrl.appendingPathComponent("\(batchId).\(index).pdf")
        let nameDestinationUrl = directoryUrl.appendingPathComponent("\(batchId).\(index).name")

        if fileManager.fileExists(atPath: pdfDestinationUrl.path) {
            try fileManager.removeItem(at: pdfDestinationUrl)
        }
        if fileManager.fileExists(atPath: nameDestinationUrl.path) {
            try fileManager.removeItem(at: nameDestinationUrl)
        }

        try fileManager.copyItem(at: sourceUrl, to: pdfDestinationUrl)

        let displayName = provider.suggestedName
            ?? sourceUrl.deletingPathExtension().lastPathComponent
            ?? "shared"
        if let data = displayName.data(using: .utf8) {
            try data.write(to: nameDestinationUrl, options: .atomic)
        }
    }

    private func writeBatchCount(
        batchId: String,
        expectedCount: Int,
        directoryUrl: URL
    ) throws {
        let countUrl = directoryUrl.appendingPathComponent("\(batchId).count")
        let countString = String(expectedCount)
        guard let countData = countString.data(using: .utf8) else {
            throw NSError(domain: "ShareExtension", code: 2)
        }
        try countData.write(to: countUrl, options: .atomic)
    }

    private func cleanupBatch(batchId: String, expectedCount: Int, directoryUrl: URL) {
        let fileManager = FileManager.default

        for index in 0..<expectedCount {
            let pdfUrl = directoryUrl.appendingPathComponent("\(batchId).\(index).pdf")
            let nameUrl = directoryUrl.appendingPathComponent("\(batchId).\(index).name")
            if fileManager.fileExists(atPath: pdfUrl.path) {
                try? fileManager.removeItem(at: pdfUrl)
            }
            if fileManager.fileExists(atPath: nameUrl.path) {
                try? fileManager.removeItem(at: nameUrl)
            }
        }

        let countUrl = directoryUrl.appendingPathComponent("\(batchId).count")
        if fileManager.fileExists(atPath: countUrl.path) {
            try? fileManager.removeItem(at: countUrl)
        }
    }

    private func openHostApp(batchId: String) {
        guard let url = URL(string: "dokus://share/import?batch=\(batchId)") else { return }

        DispatchQueue.main.async {
            var responder: UIResponder? = self
            let selector = Selector(("openURL:"))

            while let currentResponder = responder {
                if currentResponder.responds(to: selector) {
                    _ = currentResponder.perform(selector, with: url)
                    return
                }
                responder = currentResponder.next
            }
        }
    }

    private func finishRequest() {
        DispatchQueue.main.async {
            self.extensionContext?.completeRequest(returningItems: nil, completionHandler: nil)
        }
    }
}
