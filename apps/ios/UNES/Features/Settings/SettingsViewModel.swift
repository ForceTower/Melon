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
    private(set) var lastSyncIso: String?
    // Ticked every 30s so the "há X min" label refreshes without forcing
    // KMP flows to re-emit. Mirrors the ticker in `OverviewViewModel`.
    private(set) var clock: Date = Date()

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
        log.info("subscribing to settings flows")

        async let c: Void = observeCredentials(useCases: useCases)
        async let l: Void = observeLastSync(useCases: useCases)
        async let t: Void = runClockTicker()

        _ = await (c, l, t)
    }

    private func observeCredentials(useCases: SettingsUseCases) async {
        for await snapshot in useCases.observeCredentials.invoke() {
            credentials = snapshot.map {
                SettingsCredentials(username: $0.username, password: $0.password)
            }
        }
    }

    private func observeLastSync(useCases: SettingsUseCases) async {
        for await value in useCases.observeLastSync.invoke() {
            lastSyncIso = value
        }
    }

    private func runClockTicker() async {
        while !Task.isCancelled {
            clock = Date()
            try? await Task.sleep(nanoseconds: 30 * 1_000_000_000)
        }
    }

    // Relative "há 2 min" stamp the Settings header renders after the
    // "◦ SINC." prefix. Falls back to an em-dash while no sync has landed.
    var lastSyncLabel: String {
        guard let iso = lastSyncIso,
              let relative = Self.formatRelative(iso: iso, against: clock)
        else { return "—" }
        return relative
    }

    private static let isoInstantFormatter: ISO8601DateFormatter = {
        let f = ISO8601DateFormatter()
        f.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        return f
    }()

    private static func formatRelative(iso: String, against now: Date) -> String? {
        let parsed = isoInstantFormatter.date(from: iso)
            ?? ISO8601DateFormatter().date(from: iso)
        guard let date = parsed else { return nil }
        let seconds = max(0, Int(now.timeIntervalSince(date)))
        let minutes = seconds / 60
        if minutes < 1 { return "agora mesmo" }
        if minutes < 60 { return "há \(minutes) min" }
        let hours = minutes / 60
        if hours < 24 { return "há \(hours) h" }
        let days = hours / 24
        return "há \(days) d"
    }
}
