import Foundation
import Observation
@preconcurrency import Umbrella

// Drives `SettingsView`. Subscribes to the credentials flow and surfaces
// the latest pair as `SettingsCredentials` for `CredentialCard`. Mirrors
// `MeViewModel` so factory-less init keeps `#Preview` rendering against
// `SettingsFixtures`.
@MainActor
@Observable
final class SettingsViewModel {
    private(set) var credentials: SettingsCredentials?

    @ObservationIgnored private let useCases: SettingsUseCases?
    @ObservationIgnored private let log = Log.scoped("SettingsViewModel")

    init(useCases: SettingsUseCases?) {
        self.useCases = useCases
    }

    // Factory-less init — retained so `SettingsView()` and its `#Preview`
    // keep working against `SettingsFixtures` until a real graph is wired.
    convenience init() {
        self.init(useCases: nil)
    }

    func observe() async {
        guard let useCases else { return }
        log.info("subscribing to credentials")
        for await snapshot in useCases.observeCredentials.invoke() {
            credentials = snapshot.map {
                SettingsCredentials(username: $0.username, password: $0.password)
            }
        }
    }
}
