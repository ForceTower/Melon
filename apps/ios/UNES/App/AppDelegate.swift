import UIKit
import Umbrella

final class AppDelegate: NSObject, UIApplicationDelegate {
    var graph: UmbrellaGraph?
    
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        let config = UmbrellaConfig(baseUrl: "https://netherlands-dev.forcetower.dev")
        graph = UmbrellaGraph(config: config)
        return true
    }
}
