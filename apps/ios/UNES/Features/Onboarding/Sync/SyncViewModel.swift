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

    // Incremented by driveAnimation each time a step ticks. Poll loops watch
    // this: each animation tick resets their "iterations since last tick"
    // counter, so as long as the animation is still making visible progress,
    // the polls keep extending their budget. Once the animation settles they
    // get one final window before giving up.
    private var animTickEpoch = 0

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
        Self.logger.info("sync: start")
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
        // Poll onboarding-status until the backend reports the target
        // semester's backfill job has reached a terminal state
        // (activeSemesterReady=true), or backfill is no longer making
        // progress. Then fetch the list once.
        //
        // The iteration cap is **per animation tick**, not global — each
        // step tick resets `iterationsSinceTick` so the poll keeps going
        // as long as the animation is still advancing. This gives the
        // worker room to finish its batch even on slow cold-start backfills
        // without hard-coding a large absolute budget.
        var iterationsSinceTick = 0
        var lastSeenEpoch = animTickEpoch
        var totalIterations = 0

        while true {
            let currentEpoch = animTickEpoch
            if currentEpoch != lastSeenEpoch {
                Self.logger.info("classes: anim tick observed (\(currentEpoch)), resetting counter")
                lastSeenEpoch = currentEpoch
                iterationsSinceTick = 0
            }

            do {
                let outcome = try await useCases.onboardingStatus.invoke()
                switch onEnum(of: outcome) {
                case .ok(let wrapper):
                    if let status = wrapper.value {
                        let sem = status.semesters
                        Self.logger
                            .info(
                                "classes: iter=\(totalIterations) sinceTick=\(iterationsSinceTick) state=\(String(describing: sem.state)) total=\(sem.total) done=\(sem.done) failed=\(sem.failed) ready=\(status.activeSemesterReady)"
                            )
                        if status.activeSemesterReady {
                            Self.logger.info("classes: active ready -> fetching list")
                            return await fetchSemesterListAndStore()
                        }
                        let stillBackfilling = sem.state == .pending || sem.state == .running
                        if !stillBackfilling {
                            Self.logger.info("classes: backfill settled without ready -> fetching list")
                            return await fetchSemesterListAndStore()
                        }
                    } else {
                        Self.logger.info("classes: iter=\(totalIterations) status nil")
                    }
                case .err(let wrapper):
                    let authBroken = isUnauthorized(wrapper.error)
                    Self.logger
                        .warning(
                            "classes: status err iter=\(totalIterations) authBroken=\(authBroken) err=\(String(describing: wrapper.error))"
                        )
                    if authBroken {
                        anyAuthBroken = true
                        return .fail(authBroken: true)
                    }
                }
            } catch is CancellationError {
                Self.logger.info("classes: cancelled")
                return .fail(authBroken: false)
            } catch {
                Self.logger.warning("classes: status threw \(String(describing: error))")
            }

            iterationsSinceTick += 1
            totalIterations += 1
            if iterationsSinceTick >= 15 {
                Self.logger
                    .info(
                        "classes: \(iterationsSinceTick) iterations without an anim tick -> fetching list"
                    )
                return await fetchSemesterListAndStore()
            }
            try? await Task.sleep(nanoseconds: 1_000_000_000)
        }
    }

    private func fetchSemesterListAndStore() async -> StepResult {
        do {
            let outcome = try await useCases.semesterList.invoke()
            switch onEnum(of: outcome) {
            case .ok(let wrapper):
                let list = (wrapper.value as? [SyncSemesterSummary]) ?? []
                summaries = list
                let today = Self.today()
                let summary = list
                    .map { "\($0.id)[\($0.startDate)..\($0.endDate)]" }
                    .joined(separator: ",")
                Self.logger.info("classes: list count=\(list.count) today=\(today) list=[\(summary)]")
                return .ok
            case .err(let wrapper):
                let authBroken = isUnauthorized(wrapper.error)
                Self.logger
                    .warning("classes: list err authBroken=\(authBroken) err=\(String(describing: wrapper.error))")
                if authBroken { anyAuthBroken = true }
                return .fail(authBroken: authBroken)
            }
        } catch is CancellationError {
            Self.logger.info("classes: list fetch cancelled")
            return .fail(authBroken: false)
        } catch {
            Self.logger.warning("classes: list fetch threw \(String(describing: error))")
            return .fail(authBroken: false)
        }
    }

    private func activeSemesters(in list: [SyncSemesterSummary]) -> [SyncSemesterSummary] {
        let today = Self.today()
        return list.filter { Self.contains(today, $0.startDate, $0.endDate) }
    }

    private func runScheduleStep() async -> StepResult {
        guard let classes = classesTask else { return .ok }
        let classesResult = await classes.value
        if case .fail(let authBroken) = classesResult, authBroken {
            Self.logger.warning("schedule: classes auth broken, bailing")
            return .fail(authBroken: true)
        }

        let prioritized = pickActiveSemestersInOrder(summaries)
        guard let primary = prioritized.first else {
            Self.logger.info("schedule: no semesters to pull (empty summaries)")
            return .ok
        }

        let hasActiveByDate = !activeSemesters(in: summaries).isEmpty
        Self.logger.info("schedule: picked primary=\(primary.id) startDate=\(primary.startDate) endDate=\(primary.endDate) hasActiveByDate=\(hasActiveByDate) total=\(prioritized.count)")

        let extras = Array(prioritized.dropFirst())
        if !extras.isEmpty {
            // Detached so they outlive the view; the umbrella graph owns the
            // network + DAOs they touch.
            let extraIds = extras.map(\.id)
            let semesterUseCase = useCases.semester
            Self.logger.info("schedule: extras=[\(extraIds.joined(separator: ","))]")
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
                Self.logger.info("schedule: primary fetch ok id=\(primary.id)")
                return .ok
            case .err(let wrapper):
                let authBroken = isUnauthorized(wrapper.error)
                Self.logger.warning("schedule: primary fetch err id=\(primary.id) authBroken=\(authBroken) err=\(String(describing: wrapper.error))")
                if authBroken { anyAuthBroken = true }
                return .fail(authBroken: authBroken)
            }
        } catch is CancellationError {
            Self.logger.info("schedule: cancelled")
            return .fail(authBroken: false)
        } catch {
            Self.logger.warning("primary semester fetch threw: \(String(describing: error))")
            return .fail(authBroken: false)
        }
    }

    private func runMessagesStep() async -> StepResult {
        // `/sync/messages` reads from the server's mirror, which is empty
        // until the messages backfill job populates it. Poll status until
        // that job reaches a terminal state before fetching the first page —
        // otherwise we persist an empty page locally and the home screen
        // shows an empty inbox even though the user has unread messages.
        //
        // Same reset-on-tick policy as runClassesStep: each animation step
        // tick refreshes our 15-iteration budget, so we keep polling as long
        // as sync is visibly progressing.
        var iterationsSinceTick = 0
        var lastSeenEpoch = animTickEpoch
        var totalIterations = 0

        pollLoop: while true {
            let currentEpoch = animTickEpoch
            if currentEpoch != lastSeenEpoch {
                Self.logger.info("msgs: anim tick observed (\(currentEpoch)), resetting counter")
                lastSeenEpoch = currentEpoch
                iterationsSinceTick = 0
            }

            do {
                let outcome = try await useCases.onboardingStatus.invoke()
                switch onEnum(of: outcome) {
                case .ok(let wrapper):
                    if let status = wrapper.value {
                        let state = status.messages.state
                        Self.logger
                            .info(
                                "msgs: iter=\(totalIterations) sinceTick=\(iterationsSinceTick) state=\(String(describing: state))"
                            )
                        if state == .done || state == .failed {
                            break pollLoop
                        }
                    }
                case .err(let wrapper):
                    let authBroken = isUnauthorized(wrapper.error)
                    Self.logger
                        .warning(
                            "msgs: status err iter=\(totalIterations) authBroken=\(authBroken) err=\(String(describing: wrapper.error))"
                        )
                    if authBroken {
                        anyAuthBroken = true
                        return .fail(authBroken: true)
                    }
                }
            } catch is CancellationError {
                Self.logger.info("msgs: cancelled during status poll")
                return .fail(authBroken: false)
            } catch {
                Self.logger.warning("msgs: status threw \(String(describing: error))")
            }

            iterationsSinceTick += 1
            totalIterations += 1
            if iterationsSinceTick >= 15 {
                Self.logger
                    .info(
                        "msgs: \(iterationsSinceTick) iterations without an anim tick, proceeding anyway"
                    )
                break pollLoop
            }
            try? await Task.sleep(nanoseconds: 1_000_000_000)
        }

        do {
            Self.logger.info("msgs: requesting first page")
            let firstPage = try await useCases.messages.invoke(since: nil, cursor: nil)
            switch onEnum(of: firstPage) {
            case .ok(let wrapper):
                let nextCursor = wrapper.value?.nextCursor
                Self.logger.info("msgs: first page ok nextCursor=\(nextCursor ?? "<nil>")")
                if let next = nextCursor {
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
                Self.logger.warning("msgs: first page err authBroken=\(authBroken) err=\(String(describing: wrapper.error))")
                if authBroken { anyAuthBroken = true }
                return .fail(authBroken: authBroken)
            }
        } catch is CancellationError {
            Self.logger.info("msgs: cancelled")
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
            Self.logger.info("anim: step=\(step.key) begin")
            await waitForStep(step)
            Self.logger.info("anim: step=\(step.key) tick")
            _ = withAnimation(.spring(response: 0.5, dampingFraction: 0.75)) {
                doneKeys.insert(step.key)
            }
            animTickEpoch += 1
        }
        await waitForReadiness()
        if anyAuthBroken {
            Self.logger.info("sync: finish -> onAuthFailed")
            onAuthFailed()
        } else {
            Self.logger.info("sync: finish -> onDone")
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
        if elapsed >= step.maxDuration {
            Self.logger.info("anim: step=\(step.key) TIMED_OUT after \(String(format: "%.2f", elapsed))s")
        }
        let remaining = step.minDuration - elapsed
        if remaining > 0 {
            try? await Task.sleep(nanoseconds: UInt64(remaining * 1_000_000_000))
        }
    }

    private func waitForReadiness() async {
        Self.logger.info("readiness: awaiting profile + schedule + msgs")
        if let profileTask {
            _ = await profileTask.value
        }
        if let scheduleTask {
            await waitOrTimeout(scheduleTask, seconds: 2.0)
        }
        // Messages is "nice to have" per the onboarding design, but on the
        // happy path backfill lands within a few seconds of the semester
        // payload. A short grace window keeps onDone from firing right
        // before the msgs task persists its first page.
        if let msgsTask {
            await waitOrTimeout(msgsTask, seconds: 3.0)
        }
        Self.logger.info("readiness: done")
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
