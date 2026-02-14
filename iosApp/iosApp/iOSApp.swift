import SwiftUI
import UIKit
import ComposeApp

@main
struct iOSApp: App {
    @Environment(\.scenePhase) private var scenePhase

    var body: some Scene {
        WindowGroup {
            ContentView()
                .task {
                    DokusFileProviderDomainRegistrar.shared.synchronizeRegistration()
                }
                .onOpenURL { url in
                    // Pass deep links to the Compose app via ExternalUriHandler
                    ExternalUriHandler.shared.onNewUri(uri: url.absoluteString)
                }
                .onReceive(NotificationCenter.default.publisher(for: UIApplication.didBecomeActiveNotification)) { _ in
                    // Force a unique share-import ping so repeated foreground events are not dropped.
                    ExternalUriHandler.shared.onNewUri(uri: "dokus://share/import?ts=\(Date().timeIntervalSince1970)")
                    DokusFileProviderDomainRegistrar.shared.synchronizeRegistration()
                }
                .onChange(of: scenePhase) { _, newPhase in
                    if newPhase == .active {
                        // Ensure pending share-extension payloads are consumed if app-open handoff fails.
                        ExternalUriHandler.shared.onNewUri(uri: "dokus://share/import?ts=\(Date().timeIntervalSince1970)")
                        DokusFileProviderDomainRegistrar.shared.synchronizeRegistration()
                    }
                }
        }
    }
}
