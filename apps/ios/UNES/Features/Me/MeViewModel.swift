import Foundation
import Observation
@preconcurrency import Umbrella

// Drives `MeView`. A single KMP Flow carries the full hero payload; everything
// visible on the screen that isn't fixture data (shortcut grid / settings
// rows) is projected out of `ProfileIdentity` here. Fixture mode (useCases ==
// nil) keeps `#Preview` rendering without a live graph.
@MainActor
@Observable
final class MeViewModel {
    /// Where we are in the logout flow. `confirming` pushes the sheet,
    /// `flashing` drives the brief transition overlay, and `loggedOut`
    /// shows the goodbye screen — the only terminal state that requires
    /// the caller to flip the root destination back to onboarding.
    enum LogoutStep {
        case idle, confirming, flashing, loggedOut
    }

    private(set) var identity: ProfileIdentity?
    private(set) var logoutStep: LogoutStep = .idle
    private(set) var logoutName: String = "Estudante"
    private(set) var lastSyncIso: String?
    // Ticked every 30s so the "há X min" label refreshes without forcing the
    // KMP flow to re-emit. Mirrors `OverviewViewModel`'s ticker.
    private(set) var clock: Date = Date()

    // Latest profile snapshot + lifetime CR cached separately so either flow
    // emitting refreshes `identity` against the freshest pair.
    @ObservationIgnored private var lastProfile: MeMeProfile?
    @ObservationIgnored private var overallScore: Double?

    @ObservationIgnored private let useCases: MeUseCases?
    @ObservationIgnored private let sessionStore: SessionSessionStore?
    @ObservationIgnored private let log = Log.scoped("MeViewModel")

    init(useCases: MeUseCases?, sessionStore: SessionSessionStore? = nil) {
        self.useCases = useCases
        self.sessionStore = sessionStore
    }

    // Factory-less init — retained so `MeView()` and its `#Preview` keep
    // working against `MeFixtures` until a real graph is wired.
    convenience init() {
        self.init(useCases: nil, sessionStore: nil)
    }

    func observe() async {
        guard let useCases else { return }
        log.info("subscribing to me flows")

        async let p: Void = observeProfile(useCases: useCases)
        async let s: Void = observeOverallScore(useCases: useCases)
        async let l: Void = observeLastSync(useCases: useCases)
        async let t: Void = runClockTicker()

        _ = await (p, s, l, t)
    }

    private func observeProfile(useCases: MeUseCases) async {
        for await snapshot in useCases.observeProfile.invoke() {
            lastProfile = snapshot
            rebuildIdentity()
        }
    }

    private func observeOverallScore(useCases: MeUseCases) async {
        for await value in useCases.overallScore.invoke(capSemesterId: nil) {
            overallScore = value.map { Double(truncating: $0) }
            rebuildIdentity()
        }
    }

    private func observeLastSync(useCases: MeUseCases) async {
        for await value in useCases.observeLastSync.invoke() {
            lastSyncIso = value
        }
    }

    private func rebuildIdentity() {
        guard let lastProfile else { return }
        identity = Self.map(profile: lastProfile, overallScore: overallScore)
    }

    private func runClockTicker() async {
        while !Task.isCancelled {
            clock = Date()
            try? await Task.sleep(nanoseconds: 30 * 1_000_000_000)
        }
    }

    // Hint rendered in the "Registro de sincronização" settings row — the
    // prefix is editorial copy, the relative part is driven by the flow.
    // Em-dash fallback while no sync has landed.
    var lastSyncHint: String {
        guard let iso = lastSyncIso,
              let relative = Self.formatRelative(iso: iso, against: clock)
        else { return "última: —" }
        return "última: \(relative)"
    }

    // MARK: - Logout flow

    func beginLogout() {
        log.info("begin logout")
        logoutStep = .confirming
    }

    func cancelLogout() {
        logoutStep = .idle
    }

    /// Clears the KMP session and walks the UI through the flash → logged-out
    /// states. `keepData` is captured for a future branch in `SessionStore`
    /// (keep-local-prefs vs. wipe everything); today it's UI-only.
    func confirmLogout(keepData _: Bool) async {
        log.info("confirm logout")
        logoutStep = .flashing
        logoutName = identity?.firstName ?? "Estudante"
        do {
            try await sessionStore?.logout()
            log.info("session logout ok")
        } catch {
            // Logout is idempotent from the UI's perspective — if the KMP
            // call fails we still want to drop the session on this device.
            log.warn("session logout failed; continuing", error: error)
        }
        try? await Task.sleep(nanoseconds: 900_000_000)
        logoutStep = .loggedOut
    }

    // MARK: - Mapping

    private static func map(profile raw: MeMeProfile, overallScore: Double?) -> ProfileIdentity {
        let name = raw.identity.userName
        let firstName = raw.identity.firstName.isEmpty ? name : raw.identity.firstName
        let avatarInitial = firstName.first.map { String($0).uppercased() } ?? "?"

        let semester = raw.semester

        return ProfileIdentity(
            name: name,
            firstName: firstName,
            course: raw.identity.courseName ?? "",
            campus: "Universidade Estadual de Feira de Santana",
            enrollment: raw.identity.enrollmentNumber,
            // Empty-string fallback so the eyebrow renders blank rather than
            // "nil" while the credentials flow hasn't emitted yet.
            username: raw.identity.username ?? "",
            avatarInitial: avatarInitial,
            semester: semester?.code ?? "",
            semesterWeek: Int(semester?.currentWeek ?? 0),
            semesterTotalWeeks: Int(semester?.totalWeeks ?? 0),
            progressPct: Int(semester?.progressPercent ?? 0),
            // Lifetime CR — sourced from CalculateOverallScoreUseCase, not the
            // profile flow's per-semester partial average. NaN signals the
            // score flow hasn't emitted yet (`IdentityCard` renders "—" for
            // it instead of a misleading 0,0).
            cr: overallScore ?? .nan,
            crDelta: "",
            creditsDone: Int(raw.enrollment.completedHours),
            creditsRequired: Int(raw.enrollment.totalHours),
            semesterStart: formatSemesterStart(semester?.startDate),
            semesterEnd: formatSemesterEnd(semester?.endDate),
            finalExam: formatFinalExam(raw.nextExam)
        )
    }

    private static func formatSemesterStart(_ iso: String?) -> String {
        guard let iso, let date = isoDayFormatter.date(from: iso) else { return "" }
        return "início · \(shortDateFormatter.string(from: date))"
    }

    private static func formatSemesterEnd(_ iso: String?) -> String {
        guard let iso, let date = isoDayFormatter.date(from: iso) else { return "" }
        return "fim · \(shortDateFormatter.string(from: date))"
    }

    private static func formatFinalExam(_ exam: MeMeNextExam?) -> String {
        guard let exam, let date = isoDayFormatter.date(from: exam.date) else { return "" }
        return "prova final · \(shortDateFormatter.string(from: date))"
    }

    private static let isoDayFormatter: DateFormatter = {
        let f = DateFormatter()
        f.locale = Locale(identifier: "en_US_POSIX")
        f.dateFormat = "yyyy-MM-dd"
        f.timeZone = TimeZone.current
        return f
    }()

    private static let shortDateFormatter: DateFormatter = {
        let f = DateFormatter()
        f.locale = Locale(identifier: "pt_BR")
        f.dateFormat = "d MMM"
        return f
    }()

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
