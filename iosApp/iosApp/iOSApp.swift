import SwiftUI
import ComposeApp

@main
struct iOSApp: App {
    @Environment(\.scenePhase) private var scenePhase

    var body: some Scene {
        WindowGroup {
            ContentView()
                .onOpenURL { url in
                    // Pass deep links to the Compose app via ExternalUriHandler
                    ExternalUriHandler.shared.onNewUri(uri: url.absoluteString)
                }
                .onChange(of: scenePhase) { _, newPhase in
                    if newPhase == .active {
                        // Ensure pending share-extension payloads are consumed if app-open handoff fails.
                        ExternalUriHandler.shared.onNewUri(uri: "dokus://share/import")
                    }
                }
        }
    }
}
