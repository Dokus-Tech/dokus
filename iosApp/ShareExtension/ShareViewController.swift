import SwiftUI
import UIKit

final class ShareViewController: UIViewController {
    @objc private protocol OpenURLHandling {
        func openURL(_ url: URL)
    }

    private lazy var viewModel = ShareImportViewModel(
        uploader: ShareImportUploader(
            appGroupIdentifier: appGroupIdentifier,
            keychainAccessGroup: keychainAccessGroup
        )
    )

    private var hostingController: UIHostingController<ShareImportRootView>?
    private var didStart = false
    private var didCompleteRequest = false
    private var autoDismissWorkItem: DispatchWorkItem?

    private var appGroupIdentifier: String {
        (Bundle.main.object(forInfoDictionaryKey: "DokusShareAppGroupIdentifier") as? String)
            ?? "group.vision.invoid.dokus.share"
    }

    private var keychainAccessGroup: String? {
        let raw = Bundle.main.object(forInfoDictionaryKey: "DokusSharedKeychainAccessGroup") as? String
        let trimmed = raw?.trimmingCharacters(in: .whitespacesAndNewlines)
        if trimmed?.isEmpty == true {
            return nil
        }
        return trimmed
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = .systemBackground

        let rootView = ShareImportRootView(
            viewModel: viewModel,
            onDone: { [weak self] in
                self?.completeRequest()
            },
            onOpenApp: { [weak self] in
                self?.openHostApp(completeAfterOpen: true)
            }
        )

        let host = UIHostingController(rootView: rootView)
        addChild(host)
        host.view.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(host.view)
        NSLayoutConstraint.activate([
            host.view.topAnchor.constraint(equalTo: view.topAnchor),
            host.view.bottomAnchor.constraint(equalTo: view.bottomAnchor),
            host.view.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            host.view.trailingAnchor.constraint(equalTo: view.trailingAnchor)
        ])
        host.didMove(toParent: self)
        hostingController = host

        viewModel.onUploadingStateChanged = { [weak self] isUploading in
            DispatchQueue.main.async {
                self?.isModalInPresentation = isUploading
            }
        }
        viewModel.onStateChanged = { [weak self] state in
            guard let self else { return }
            switch state {
            case .success:
                self.scheduleAutoDismiss()
            case .loadingPayload, .resolvingSession, .uploading, .error:
                self.autoDismissWorkItem?.cancel()
                self.autoDismissWorkItem = nil
            }
        }
    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)

        guard !didStart else { return }
        didStart = true

        let extensionItems = (extensionContext?.inputItems as? [NSExtensionItem]) ?? []
        Task {
            await viewModel.start(with: extensionItems)
        }
    }

    private func openHostApp(completeAfterOpen: Bool) {
        guard let openUrl = hostAppDeepLinkUrl() else {
            if completeAfterOpen {
                completeRequest()
            }
            return
        }

        let completeIfNeeded = { [weak self] in
            guard completeAfterOpen else { return }
            self?.completeRequest()
        }

        guard let extensionContext else {
            if openHostAppViaResponder(url: openUrl) {
                completeIfNeeded()
            }
            return
        }

        extensionContext.open(openUrl) { [weak self] success in
            guard let self else { return }
            if success {
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.2) {
                    completeIfNeeded()
                }
                return
            }

            if self.openHostAppViaResponder(url: openUrl) {
                // Give the system a beat to switch apps before finishing the extension.
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.2) {
                    completeIfNeeded()
                }
            }
        }
    }

    private func hostAppDeepLinkUrl() -> URL? {
        URL(string: "dokus://share/import?source=extension")
    }

    @discardableResult
    private func openHostAppViaResponder(url: URL) -> Bool {
        var responder: UIResponder? = self
        let selector = #selector(OpenURLHandling.openURL(_:))

        while let current = responder {
            if current.responds(to: selector) {
                _ = current.perform(selector, with: url)
                return true
            }
            responder = current.next
        }

        return false
    }

    private func completeRequest() {
        guard !didCompleteRequest else { return }
        didCompleteRequest = true
        autoDismissWorkItem?.cancel()
        autoDismissWorkItem = nil
        viewModel.cleanupTemporaryFiles()
        extensionContext?.completeRequest(returningItems: nil, completionHandler: nil)
    }

    private func scheduleAutoDismiss() {
        guard autoDismissWorkItem == nil else { return }
        let workItem = DispatchWorkItem { [weak self] in
            self?.completeRequest()
        }
        autoDismissWorkItem = workItem
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.0, execute: workItem)
    }
}
