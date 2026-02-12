import SwiftUI
import UIKit

final class ShareViewController: UIViewController {
    private static let openAppUrl = URL(string: "dokus://")

    private lazy var viewModel = ShareImportViewModel(
        uploader: ShareImportUploader(
            appGroupIdentifier: appGroupIdentifier,
            keychainAccessGroup: keychainAccessGroup
        )
    )

    private var hostingController: UIHostingController<ShareImportRootView>?
    private var didStart = false

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
        guard let openUrl = Self.openAppUrl else { return }

        let completion: (Bool) -> Void = { [weak self] _ in
            guard completeAfterOpen else { return }
            self?.completeRequest()
        }

        guard let extensionContext else {
            if completeAfterOpen {
                completeRequest()
            }
            return
        }
        extensionContext.open(openUrl, completionHandler: completion)
    }

    private func completeRequest() {
        viewModel.cleanupTemporaryFiles()
        extensionContext?.completeRequest(returningItems: nil, completionHandler: nil)
    }
}
