/**
 * VaultStadio iOS App Entry Point
 * 
 * SwiftUI app that hosts the Compose Multiplatform UI.
 */

import SwiftUI
import ComposeApp

@main
struct VaultStadioApp: App {
    
    init() {
        // Initialize Koin dependency injection
        KoinHelperKt.doInitKoin()
    }
    
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}

struct ContentView: UIViewControllerRepresentable {
    
    func makeUIViewController(context: Context) -> UIViewController {
        // Create the Compose UI view controller
        return MainViewControllerKt.MainViewController()
    }
    
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
        // No-op for now
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}
