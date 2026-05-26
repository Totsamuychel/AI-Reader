import SwiftUI
import SharedModels

@main
struct BookMindApp: App {
    @StateObject private var container = AppContainer.makeDefault()

    var body: some Scene {
        WindowGroup {
            RootView()
                .environmentObject(container)
                .environmentObject(container.session)
                .preferredColorScheme(container.readerSettings.theme == .dark ? .dark : nil)
        }
    }
}

struct RootView: View {
    var body: some View {
        LibraryView()
    }
}
