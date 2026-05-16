import Foundation
import Observation
import SwiftUI
@preconcurrency import Umbrella

// Drives `SettingsView`. Subscribes to the credentials flow and surfaces
// the latest pair as `SettingsCredentials` for `CredentialCard`. Also owns
// the toggles + spoiler picker state — observe-on-flow + mutate-via-use-case
// keeps the screen reactive across devices via the profile mirror.
@MainActor
@Observable
final class SettingsViewModel {
    private(set) var credentials: SettingsCredentials?
    private(set) var lastSyncIso: String?
    // Ticked every 30s so the "há X min" label refreshes without forcing
    // KMP flows to re-emit. Mirrors the ticker in `OverviewViewModel`.
    private(set) var clock: Date = Date()

    // Toggles + spoiler. Hydrated by the settings flow on first emission;
    // every UI mutation goes through `setSpoiler`/`setToggle` so the local
    // value flips optimistically and the network PATCH lands afterwards.
    private(set) var state = SettingsState()

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
        async let s: Void = observeSettings(useCases: useCases)
        async let t: Void = runClockTicker()

        _ = await (c, l, s, t)
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

    private func observeSettings(useCases: SettingsUseCases) async {
        for await snapshot in useCases.observeSettings.invoke() {
            guard let snapshot else { continue }
            state = Self.applySnapshot(snapshot, onto: state)
        }
    }

    private func runClockTicker() async {
        while !Task.isCancelled {
            clock = Date()
            try? await Task.sleep(nanoseconds: 30 * 1_000_000_000)
        }
    }

    // MARK: - Mutations
    //
    // Each method optimistically writes to `state`, then forwards the change
    // to the KMP `UpdateSettingsUseCase` in a detached task. The KMP layer
    // does its own local DAO write + network PATCH, and the resulting flow
    // re-emit overwrites `state` with the canonical server values — which
    // is a no-op for boolean toggles but matters if the server clamps a
    // spoiler int (defense-in-depth against a stale client).

    func setSpoiler(_ value: SpoilerMode) {
        state.spoiler = value
        guard let useCases else { return }
        Task<Void, Never> {
            do {
                let outcome = try await useCases.updateSettings.invoke(
                    gradeSpoiler: KotlinInt(int: value.serverInt),
                    notifMsgBroadcast: nil,
                    notifMsgClass: nil,
                    notifMsgDirect: nil,
                    notifGradePosted: nil,
                    notifGradeChanged: nil,
                    notifGradeDateChanged: nil,
                    notifClassLocation: nil,
                    notifClassMaterial: nil,
                    notifClassSubject: nil
                )
                if case .err = onEnum(of: outcome) {
                    self.log.error("setSpoiler push failed; will reconcile on next profile sync")
                }
            } catch {
                self.log.error("setSpoiler push threw: \(error)")
            }
        }
    }

    func setToggle(_ keyPath: WritableKeyPath<SettingsState, Bool>, _ value: Bool) {
        state[keyPath: keyPath] = value
        guard let useCases else { return }
        let kbool = KotlinBoolean(bool: value)
        Task<Void, Never> {
            do {
                let outcome = try await useCases.updateSettings.invoke(
                    gradeSpoiler: nil,
                    notifMsgBroadcast: keyPath == \.notifMsgBroadcast ? kbool : nil,
                    notifMsgClass: keyPath == \.notifMsgClass ? kbool : nil,
                    notifMsgDirect: keyPath == \.notifMsgDirect ? kbool : nil,
                    notifGradePosted: keyPath == \.notifGradePosted ? kbool : nil,
                    notifGradeChanged: keyPath == \.notifGradeChanged ? kbool : nil,
                    notifGradeDateChanged: keyPath == \.notifGradeDateChanged ? kbool : nil,
                    notifClassLocation: keyPath == \.notifClassLocation ? kbool : nil,
                    notifClassMaterial: keyPath == \.notifClassMaterial ? kbool : nil,
                    notifClassSubject: keyPath == \.notifClassSubject ? kbool : nil
                )
                if case .err = onEnum(of: outcome) {
                    self.log.error("setToggle push failed; will reconcile on next profile sync")
                }
            } catch {
                self.log.error("setToggle push threw: \(error)")
            }
        }
    }

    // SwiftUI bindings that read live from `state` and pipe writes through
    // the appropriate mutator. Components stay binding-flavored even though
    // `state` is `private(set)`.
    func toggleBinding(_ keyPath: WritableKeyPath<SettingsState, Bool>) -> Binding<Bool> {
        Binding(
            get: { self.state[keyPath: keyPath] },
            set: { self.setToggle(keyPath, $0) }
        )
    }

    func spoilerBinding() -> Binding<SpoilerMode> {
        Binding(
            get: { self.state.spoiler },
            set: { self.setSpoiler($0) }
        )
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

    private static func applySnapshot(_ snapshot: SettingsUserSettings, onto current: SettingsState) -> SettingsState {
        var next = current
        next.spoiler = SpoilerMode(serverInt: Int(snapshot.gradeSpoiler)) ?? current.spoiler
        next.notifMsgBroadcast = snapshot.notifMsgBroadcast
        next.notifMsgClass = snapshot.notifMsgClass
        next.notifMsgDirect = snapshot.notifMsgDirect
        next.notifGradePosted = snapshot.notifGradePosted
        next.notifGradeChanged = snapshot.notifGradeChanged
        next.notifGradeDateChanged = snapshot.notifGradeDateChanged
        next.notifClassLocation = snapshot.notifClassLocation
        next.notifClassMaterial = snapshot.notifClassMaterial
        next.notifClassSubject = snapshot.notifClassSubject
        return next
    }
}
