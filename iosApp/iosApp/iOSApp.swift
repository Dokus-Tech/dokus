import SwiftUI
import ComposeApp

@main
struct iOSApp: App {
    var body: some Scene {
        WindowGroup {
            ContentView()
                .onOpenURL { url in
                    // Pass deep links to the Compose app via ExternalUriHandler
                    ExternalUriHandler.shared.onNewUri(uri: url.absoluteString)
                }
        }
    }
}