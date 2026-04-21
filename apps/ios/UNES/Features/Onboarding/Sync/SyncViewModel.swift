import Foundation
import Observation
import OSLog
import SwiftUI
import UIKit
@preconcurrency import Umbrella

struct SyncStep: Identifiable {
    let id = UUID()
    let key: String
    let label: String
    let minDuration: TimeInterval
    let maxDuration: TimeInterval
}

// Per-step durations are starting points from `docs/ios-sync-view-integration.md`
// §4. Tune after observing real-world latencies.
let SYNC_STEPS: [SyncStep] = [
    .init(key: "auth",     label: "Verificando matrícula",   minDuration: 1.2, maxDuration: 3.0),
    .init(key: "profile",  label: "Carregando seu perfil",   minDuration: 0.8, maxDuration: 4.0),
    .init(key: "classes",  label: "Conectando às suas turmas", minDuration: 0.8, maxDuration: 8.0),
    .init(key: "schedule", label: "Montando seu horário",    minDuration: 0.8, maxDuration: 8.0),
    .init(key: "grades",   label: "Baixando notas do semestre", minDuration: 0.8, maxDuration: 1.5),
    .init(key: "msgs",     label: "Sincronizando recados",   minDuration: 0.8, maxDuration: 6.0),
]

private enum StepResult {
    case ok
    case fail(authBroken: Bool)
}

@MainActor
@Observable
final class SyncViewModel {
    private(set) var currentStep: Int = 0
    private(set) var doneKeys: Set<String> = []

    private let useCases: SyncUseCases
    private let onDone: () -> Void
    private let onAuthFailed: () -> Void

    private var didStart = false

    // One task per logical work unit. `schedule` and `grades` deliberately
    // share `scheduleTask` — there's a single GET /sync/semesters/:id behind
    // both UI rows (see doc §3.3). Background work for additional semesters
    // and message pagination is detached from these.
    private var authTask: Task<StepResult, Never>?
    private var profileTask: Task<StepResult, Never>?
    private var classesTask: Task<StepResult, Never>?
    private var scheduleTask: Task<StepResult, Never>?
    private var msgsTask: Task<StepResult, Never>?

    private var summaries: [SyncSemesterSummary] = []
    private var anyAuthBroken = false

    private static let logger = Logger(subsystem: "dev.forcetower.melon", category: "sync")

    init(
        useCases: SyncUseCases,
        onDone: @escaping () -> Void,
        onAuthFailed: @escaping () -> Void,
    ) {
        self.useCases = useCases
        self.onDone = onDone
        self.onAuthFailed = onAuthFailed
    }

    func start() {
        guard !didStart else { return }
        didStart = true
        kickOffWork()
        Task { await driveAnimation() }
    }

    // MARK: - Work kick-off

    private func kickOffWork() {
        authTask = Task { await runAuthStep() }
        profileTask = Task { await runProfileStep() }
        classesTask = Task { await runClassesStep() }
        scheduleTask = Task { await runScheduleStep() }
        msgsTask = Task { await runMessagesStep() }
    }

    private func runAuthStep() async -> StepResult {
        // Bumps last_active_at so the server's cadence tier prioritizes the
        // student we just logged in as. Failure isn't actionable here.
        async let pingResult: () = sendPing()
        // If FCM has already given us a token (set in AppDelegate), forward
        // it. Permission prompt itself lives on ReadyView, so most fresh
        // users won't have a token yet — that's expected; skip silently.
        async let tokenResult: () = sendDeviceTokenIfPresent()

        _ = await pingResult
        _ = await tokenResult
        return .ok
    }

    private func sendPing() async {
        do {
            let outcome = try await useCases.ping.invoke()
            recordAuthIfFailed(outcome)
        } catch is CancellationError {
            return
        } catch {
            Self.logger.warning("ping failed: \(String(describing: error))")
        }
    }

    private func sendDeviceTokenIfPresent() async {
        guard let token = UserDefaults.standard.string(forKey: "messaging_notification_token") else { return }
        let appVersion = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String
        let deviceName = UIDevice.current.name
        let locale = Locale.current.identifier
        do {
            _ = try await useCases.registerToken.invoke(
                token: token,
                platform: "ios",
                deviceName: deviceName,
                appVersion: appVersion,
                locale: locale,
            )
        } catch is CancellationError {
            return
        } catch {
            Self.logger.warning("device token register failed: \(String(describing: error))")
        }
    }

    private func runProfileStep() async -> StepResult {
        // Profile fetch + retry-on-null-course gate. The course can lag the
        // initial sync by a tick if the backfill worker is still running —
        // one or two extra polls is enough in the common case.
        for attempt in 0...2 {
            do {
                let outcome = try await useCases.profile.invoke()
                switch onEnum(of: outcome) {
                case .ok:
                    if attempt == 2 { return .ok }
                    let keepGoing = await shouldRetryProfileForCourse()
                    if !keepGoing { return .ok }
                    try? await Task.sleep(nanoseconds: 700_000_000)
                case .err(let wrapper):
                    let authBroken = isUnauthorized(wrapper.error)
                    if authBroken { anyAuthBroken = true }
                    return .fail(authBroken: authBroken)
                }
            } catch is CancellationError {
                return .fail(authBroken: false)
            } catch {
                Self.logger.warning("profile fetch threw: \(String(describing: error))")
                return .fail(authBroken: false)
            }
        }
        return .ok
    }

    private func shouldRetryProfileForCourse() async -> Bool {
        do {
            let outcome = try await useCases.onboardingStatus.invoke()
            switch onEnum(of: outcome) {
            case .ok(let wrapper):
                guard let status = wrapper.value else { return false }
                let state = status.semesters.state
                let backfilling = state == .pending || state == .running
                return !status.courseLinked && backfilling
            case .err:
                return false
            }
        } catch {
            return false
        }
    }

    private func runClassesStep() async -> StepResult {
        // Gate on the *target* semester being resolvable, not on list
        // non-emptiness. Historical-semester jobs often finish before the
        // currently-active one during initial backfill, so a non-empty list
        // can still be missing the semester we actually care about —
        // consulting onboarding-status (see semestersAreStillBackfilling)
        // lets us wait that out instead of racing past with a stale list.
        //
        // Resolution rules:
        //   - An active-by-date semester is in the list → resolved.
        //   - Backend reports backfill done and no active semester → genuine
        //     between-terms; fall through to the historical fallback in
        //     pickActiveSemestersInOrder.
        //   - Otherwise (still backfilling) → sleep and retry.
        for iteration in 0..<5 {
            do {
                let outcome = try await useCases.semesterList.invoke()
                switch onEnum(of: outcome) {
                case .ok(let wrapper):
                    let list = (wrapper.value as? [SyncSemesterSummary]) ?? []
                    summaries = list

                    if !activeSemesters(in: list).isEmpty { return .ok }

                    let stillBackfilling = await semestersAreStillBackfilling()
                    if !stillBackfilling { return .ok }
                    if iteration == 4 { return .ok }

                    try? await Task.sleep(nanoseconds: 1_000_000_000)
                case .err(let wrapper):
                    let authBroken = isUnauthorized(wrapper.error)
                    if authBroken { anyAuthBroken = true }
                    return .fail(authBroken: authBroken)
                }
            } catch is CancellationError {
                return .fail(authBroken: false)
            } catch {
                Self.logger.warning("semester list threw: \(String(describing: error))")
                return .fail(authBroken: false)
            }
        }
        return .ok
    }

    private func activeSemesters(in list: [SyncSemesterSummary]) -> [SyncSemesterSummary] {
        let today = Self.today()
        return list.filter { Self.contains(today, $0.startDate, $0.endDate) }
    }

    private func semestersAreStillBackfilling() async -> Bool {
        do {
            let outcome = try await useCases.onboardingStatus.invoke()
            switch onEnum(of: outcome) {
            case .ok(let wrapper):
                guard let state = wrapper.value?.semesters.state else { return false }
                return state == .pending || state == .running
            case .err:
                return false
            }
        } catch {
            return false
        }
    }

    private func runScheduleStep() async -> StepResult {
        guard let classes = classesTask else { return .ok }
        let classesResult = await classes.value
        if case .fail(let authBroken) = classesResult, authBroken {
            return .fail(authBroken: true)
        }

        let prioritized = pickActiveSemestersInOrder(summaries)
        guard let primary = prioritized.first else {
            // No semesters at all — common for brand-new accounts. Nothing
            // to pull; let downstream UI show its empty state.
            return .ok
        }

        let extras = Array(prioritized.dropFirst())
        if !extras.isEmpty {
            // Detached so they outlive the view; the umbrella graph owns the
            // network + DAOs they touch.
            let extraIds = extras.map(\.id)
            let semesterUseCase = useCases.semester
            Task.detached {
                for id in extraIds {
                    _ = try? await semesterUseCase.invoke(semesterId: id)
                }
            }
        }

        do {
            let primaryOutcome = try await useCases.semester.invoke(semesterId: primary.id)
            switch onEnum(of: primaryOutcome) {
            case .ok:
                return .ok
            case .err(let wrapper):
                let authBroken = isUnauthorized(wrapper.error)
                if authBroken { anyAuthBroken = true }
                return .fail(authBroken: authBroken)
            }
        } catch is CancellationError {
            return .fail(authBroken: false)
        } catch {
            Self.logger.warning("primary semester fetch threw: \(String(describing: error))")
            return .fail(authBroken: false)
        }
    }

    private func runMessagesStep() async -> StepResult {
        do {
            let firstPage = try await useCases.messages.invoke(since: nil, cursor: nil)
            switch onEnum(of: firstPage) {
            case .ok(let wrapper):
                if let next = wrapper.value?.nextCursor {
                    let messagesUseCase = useCases.messages
                    Task.detached {
                        var cursor: String? = next
                        var pages = 0
                        let maxPages = 20
                        while let c = cursor, pages < maxPages {
                            guard let outcome = try? await messagesUseCase.invoke(since: nil, cursor: c) else { break }
                            switch onEnum(of: outcome) {
                            case .ok(let w):
                                cursor = w.value?.nextCursor
                            case .err:
                                return
                            }
                            pages += 1
                        }
                    }
                }
                return .ok
            case .err(let wrapper):
                let authBroken = isUnauthorized(wrapper.error)
                if authBroken { anyAuthBroken = true }
                return .fail(authBroken: authBroken)
            }
        } catch is CancellationError {
            return .fail(authBroken: false)
        } catch {
            Self.logger.warning("messages first page threw: \(String(describing: error))")
            return .fail(authBroken: false)
        }
    }

    // MARK: - Animation

    private func driveAnimation() async {
        for (idx, step) in SYNC_STEPS.enumerated() {
            currentStep = idx
            await waitForStep(step)
            _ = withAnimation(.spring(response: 0.5, dampingFraction: 0.75)) {
                doneKeys.insert(step.key)
            }
        }
        await waitForReadiness()
        if anyAuthBroken {
            onAuthFailed()
        } else {
            onDone()
        }
    }

    private func waitForStep(_ step: SyncStep) async {
        let task = task(for: step.key)
        let started = Date()
        if let task {
            await waitOrTimeout(task, seconds: step.maxDuration)
        }
        let elapsed = Date().timeIntervalSince(started)
        let remaining = step.minDuration - elapsed
        if remaining > 0 {
            try? await Task.sleep(nanoseconds: UInt64(remaining * 1_000_000_000))
        }
    }

    private func waitForReadiness() async {
        if let profileTask {
            _ = await profileTask.value
        }
        if let scheduleTask {
            await waitOrTimeout(scheduleTask, seconds: 2.0)
        }
    }

    private func task(for key: String) -> Task<StepResult, Never>? {
        switch key {
        case "auth":     return authTask
        case "profile":  return profileTask
        case "classes":  return classesTask
        // grades shares the schedule task — both UI rows finish when the
        // first active semester's payload lands (see doc §3.3).
        case "schedule": return scheduleTask
        case "grades":   return scheduleTask
        case "msgs":     return msgsTask
        default:         return nil
        }
    }

    private func waitOrTimeout<T>(_ task: Task<T, Never>, seconds: TimeInterval) async {
        await withTaskGroup(of: Void.self) { group in
            group.addTask { _ = await task.value }
            group.addTask {
                try? await Task.sleep(nanoseconds: UInt64(seconds * 1_000_000_000))
            }
            _ = await group.next()
            group.cancelAll()
        }
    }

    // MARK: - Helpers

    private func pickActiveSemestersInOrder(_ all: [SyncSemesterSummary]) -> [SyncSemesterSummary] {
        if all.isEmpty { return [] }
        let sorted = all.sorted { $0.startDate > $1.startDate }
        let today = Self.today()
        let active = sorted.filter { Self.contains(today, $0.startDate, $0.endDate) }
        if !active.isEmpty { return active }
        // Between academic terms: pull the most-recent one as the fallback.
        return [sorted[0]]
    }

    private static func today() -> String {
        let f = DateFormatter()
        f.calendar = Calendar(identifier: .iso8601)
        f.dateFormat = "yyyy-MM-dd"
        f.locale = Locale(identifier: "en_US_POSIX")
        return f.string(from: Date())
    }

    private static func contains(_ today: String, _ start: String, _ end: String) -> Bool {
        // Lex compare of yyyy-MM-dd matches calendar order.
        return start <= today && today <= end
    }

    private func recordAuthIfFailed(_ outcome: CommonOutcome<KotlinUnit, SyncSyncError>) {
        if case .err(let wrapper) = onEnum(of: outcome), isUnauthorized(wrapper.error) {
            anyAuthBroken = true
        }
    }

    private func isUnauthorized(_ error: SyncSyncError?) -> Bool {
        guard let error else { return false }
        if case .unauthorized = onEnum(of: error) { return true }
        return false
    }
}
