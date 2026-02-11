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

        processSharedPdf()
    }

    private func processSharedPdf() {
        guard let extensionItems = extensionContext?.inputItems as? [NSExtensionItem] else {
            finishRequest()
            return
        }

        guard let provider = extensionItems
            .flatMap({ $0.attachments ?? [] })
            .first(where: { $0.hasItemConformingToTypeIdentifier(UTType.pdf.identifier) }) else {
            finishRequest()
            return
        }

        provider.loadFileRepresentation(forTypeIdentifier: UTType.pdf.identifier) { [weak self] fileUrl, _ in
            guard let self else { return }

            guard let fileUrl else {
                self.finishRequest()
                return
            }

            do {
                let batchId = try self.persistSharedPdf(sourceUrl: fileUrl, provider: provider)
                self.openHostApp(batchId: batchId)
            } catch {
                // Keep failure silent for share extension UX; host app import flow handles missing payload.
            }

            self.finishRequest()
        }
    }

    private func persistSharedPdf(sourceUrl: URL, provider: NSItemProvider) throws -> String {
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

        let batchId = UUID().uuidString
        let pdfDestinationUrl = directoryUrl.appendingPathComponent("\(batchId).pdf")
        let nameDestinationUrl = directoryUrl.appendingPathComponent("\(batchId).name")

        if fileManager.fileExists(atPath: pdfDestinationUrl.path) {
            try fileManager.removeItem(at: pdfDestinationUrl)
        }

        try fileManager.copyItem(at: sourceUrl, to: pdfDestinationUrl)

        let displayName = provider.suggestedName
            ?? sourceUrl.deletingPathExtension().lastPathComponent
            ?? "shared"
        if let data = displayName.data(using: .utf8) {
            try data.write(to: nameDestinationUrl, options: .atomic)
        }

        return batchId
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
