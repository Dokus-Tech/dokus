import UIKit
import UniformTypeIdentifiers

final class ShareViewController: UIViewController {
    private struct LoadedPdf {
        let data: Data
        let displayName: String
    }

    private let appGroupIdentifier = "group.vision.invoid.dokus.share"
    private let sharedImportsDirectory = "SharedImports"
    private let latestBatchMarker = "latest.batch"
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
            .filter {
                $0.hasItemConformingToTypeIdentifier(UTType.pdf.identifier) ||
                    $0.hasItemConformingToTypeIdentifier(UTType.fileURL.identifier)
            }

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
                loadPdfPayload(from: provider) { [weak self] payload in
                    defer { group.leave() }
                    guard let self, let payload else { return }

                    do {
                        try self.persistSharedPdf(
                            pdfData: payload.data,
                            displayName: payload.displayName,
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
                        try self.writeLatestBatchMarker(
                            batchId: batchId,
                            directoryUrl: directoryUrl
                        )
                        self.openHostApp(batchId: batchId)
                    } catch {
                        self.cleanupBatch(
                            batchId: batchId,
                            expectedCount: providers.count,
                            directoryUrl: directoryUrl
                        )
                        self.finishRequest()
                    }
                } else {
                    self.cleanupBatch(
                        batchId: batchId,
                        expectedCount: providers.count,
                        directoryUrl: directoryUrl
                    )
                    self.finishRequest()
                }
            }
        } catch {
            finishRequest()
        }
    }

    private func loadPdfPayload(
        from provider: NSItemProvider,
        completion: @escaping (LoadedPdf?) -> Void
    ) {
        if provider.hasItemConformingToTypeIdentifier(UTType.pdf.identifier) {
            provider.loadDataRepresentation(forTypeIdentifier: UTType.pdf.identifier) { [weak self] data, _ in
                guard let self else {
                    completion(nil)
                    return
                }
                if let data, !data.isEmpty {
                    completion(
                        LoadedPdf(
                            data: data,
                            displayName: self.resolvedPdfFileName(
                                provider: provider,
                                fallbackName: provider.suggestedName
                            )
                        )
                    )
                    return
                }

                provider.loadFileRepresentation(forTypeIdentifier: UTType.pdf.identifier) { fileUrl, _ in
                    guard let fileUrl,
                          let fileData = try? Data(contentsOf: fileUrl),
                          !fileData.isEmpty else {
                        completion(nil)
                        return
                    }
                    completion(
                        LoadedPdf(
                            data: fileData,
                            displayName: self.resolvedPdfFileName(
                                provider: provider,
                                fallbackName: fileUrl.lastPathComponent
                            )
                        )
                    )
                }
            }
            return
        }

        if provider.hasItemConformingToTypeIdentifier(UTType.fileURL.identifier) {
            provider.loadItem(forTypeIdentifier: UTType.fileURL.identifier, options: nil) { [weak self] item, _ in
                guard let self,
                      let fileUrl = self.extractFileUrl(from: item),
                      self.isLikelyPdf(fileUrl: fileUrl, provider: provider),
                      let fileData = try? Data(contentsOf: fileUrl),
                      !fileData.isEmpty else {
                    completion(nil)
                    return
                }

                completion(
                    LoadedPdf(
                        data: fileData,
                        displayName: self.resolvedPdfFileName(
                            provider: provider,
                            fallbackName: fileUrl.lastPathComponent
                        )
                    )
                )
            }
            return
        }

        completion(nil)
    }

    private func extractFileUrl(from item: NSSecureCoding?) -> URL? {
        if let url = item as? URL {
            return url
        }
        if let nsUrl = item as? NSURL {
            return nsUrl as URL
        }
        if let data = item as? Data {
            return URL(dataRepresentation: data, relativeTo: nil)
        }
        if let nsData = item as? NSData {
            return URL(dataRepresentation: nsData as Data, relativeTo: nil)
        }
        if let path = item as? String {
            return URL(fileURLWithPath: path)
        }
        if let nsPath = item as? NSString {
            return URL(fileURLWithPath: nsPath as String)
        }
        return nil
    }

    private func isLikelyPdf(fileUrl: URL, provider: NSItemProvider) -> Bool {
        if fileUrl.pathExtension.lowercased() == "pdf" {
            return true
        }
        if let suggestedName = provider.suggestedName?.lowercased(), suggestedName.hasSuffix(".pdf") {
            return true
        }
        return false
    }

    private func resolvedPdfFileName(provider: NSItemProvider, fallbackName: String?) -> String {
        let suggestedName = provider.suggestedName?.trimmingCharacters(in: .whitespacesAndNewlines)
        let fallback = fallbackName?.trimmingCharacters(in: .whitespacesAndNewlines)

        let rawName = suggestedName?.isEmpty == false ? suggestedName : (fallback?.isEmpty == false ? fallback : "shared")
        let sanitized = (rawName ?? "shared").replacingOccurrences(of: "/", with: "-")
        if sanitized.lowercased().hasSuffix(".pdf") {
            return sanitized
        }
        return "\(sanitized).pdf"
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
        pdfData: Data,
        displayName: String,
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

        try pdfData.write(to: pdfDestinationUrl, options: .atomic)

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

    private func writeLatestBatchMarker(
        batchId: String,
        directoryUrl: URL
    ) throws {
        let markerUrl = directoryUrl.appendingPathComponent(latestBatchMarker)
        guard let markerData = batchId.data(using: .utf8) else {
            throw NSError(domain: "ShareExtension", code: 3)
        }
        try markerData.write(to: markerUrl, options: .atomic)
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

        clearLatestBatchMarkerIfMatches(batchId: batchId, directoryUrl: directoryUrl)
    }

    private func openHostApp(batchId: String) {
        guard let url = URL(string: "dokus://share/import?batch=\(batchId)") else {
            finishRequest()
            return
        }

        guard let extensionContext else {
            openHostAppViaResponder(url: url)
            finishRequest()
            return
        }

        extensionContext.open(url) { [weak self] success in
            guard let self else { return }
            if success {
                self.finishRequest()
                return
            }

            extensionContext.completeRequest(returningItems: nil) { _ in
                self.openHostAppViaResponder(url: url)
            }
        }
    }

    private func openHostAppViaResponder(url: URL) {
        DispatchQueue.main.async {
            var responder: UIResponder? = self
            let selector = Selector(("openURL:"))

            while let currentResponder = responder {
                if currentResponder.responds(to: selector) {
                    _ = currentResponder.perform(selector, with: url)
                    break
                }
                responder = currentResponder.next
            }
        }
    }

    private func clearLatestBatchMarkerIfMatches(batchId: String, directoryUrl: URL) {
        let markerUrl = directoryUrl.appendingPathComponent(latestBatchMarker)
        guard let markerData = try? Data(contentsOf: markerUrl),
              let markerBatchId = String(data: markerData, encoding: .utf8),
              markerBatchId == batchId else {
            return
        }

        try? FileManager.default.removeItem(at: markerUrl)
    }

    private func finishRequest() {
        DispatchQueue.main.async {
            self.extensionContext?.completeRequest(returningItems: nil, completionHandler: nil)
        }
    }
}
