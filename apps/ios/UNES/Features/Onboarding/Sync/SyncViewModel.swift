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
//
// Phase 1 can take a while for alumni users: Snowpiercer's listSemesters
// filters by student but is often missing semesters, so Phase 1 falls
// back to probing DB-known candidates until it finds the student's
// actual last enrollment. classes/schedule/grades are each generous
// enough to stage the wait visibly rather than bailing. grades in
// particular keeps polling until the backend reports appliedSemesters
// > 0, so the next screen opens with real data.
let SYNC_STEPS: [SyncStep] = [
    .init(key: "auth",     label: "Verificando matrícula",   minDuration: 1.2, maxDuration: 3.0),
    .init(key: "profile",  label: "Carregando seu perfil",   minDuration: 0.8, maxDuration: 4.0),
    .init(key: "classes",  label: "Conectando às suas turmas", minDuration: 0.8, maxDuration: 30.0),
    .init(key: "schedule", label: "Montando seu horário",    minDuration: 0.8, maxDuration: 30.0),
    .init(key: "grades",   label: "Baixando notas do semestre", minDuration: 0.8, maxDuration: 60.0),
    .init(key: "msgs",     label: "Sincronizando recados",   minDuration: 0.8, maxDuration: 90.0),
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
    private var gradesTask: Task<StepResult, Never>?
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
    private let log = Log.scoped("SyncViewModel")

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
        log.info("onboarding sync start")
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
        gradesTask = Task { await runGradesStep() }
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
        // Polls onboarding-status until Phase 1 reaches a terminal state
        // (initial.state == .done or .failed), then fetches the semester
        // list. 20 ticks × 1.5s = 30s budget; if Phase 1 hasn't settled
        // by then, schedule + grades keep polling, so we fall through
        // and fetch whatever list exists (possibly empty — grades will
        // block until the backend confirms a semester is on disk).
        let result = await pollUntilInitialTerminal(
            step: "classes",
            maxTicks: 20,
            tickInterval: 1_500_000_000,
        )
        if case .fail(let authBroken) = result, authBroken {
            return .fail(authBroken: true)
        }
        return await fetchSemesterListAndStore()
    }

    // Shared polling helper: waits up to `maxTicks` iterations (sleeping
    // `tickInterval` ns between each) for Phase 1 (initial) to reach a
    // terminal state. Returns .ok on terminal or budget exhaustion;
    // .fail(authBroken: true) only on server-side auth failure.
    private func pollUntilInitialTerminal(
        step: String,
        maxTicks: Int,
        tickInterval: UInt64,
    ) async -> StepResult {
        for iter in 0..<maxTicks {
            do {
                let outcome = try await useCases.onboardingStatus.invoke()
                switch onEnum(of: outcome) {
                case .ok(let wrapper):
                    if let status = wrapper.value {
                        let state = status.initial.state
                        let applied = status.initial.appliedSemesters
                        Self.logger
                            .info(
                                "\(step): iter=\(iter) initial=\(String(describing: state)) applied=\(applied)"
                            )
                        if state == .done || state == .failed {
                            return .ok
                        }
                    } else {
                        Self.logger.info("\(step): iter=\(iter) status nil")
                    }
                case .err(let wrapper):
                    let authBroken = isUnauthorized(wrapper.error)
                    Self.logger
                        .warning(
                            "\(step): status err iter=\(iter) authBroken=\(authBroken) err=\(String(describing: wrapper.error))"
                        )
                    if authBroken {
                        anyAuthBroken = true
                        return .fail(authBroken: true)
                    }
                }
            } catch is CancellationError {
                Self.logger.info("\(step): cancelled during poll")
                return .fail(authBroken: false)
            } catch {
                Self.logger.warning("\(step): status threw \(String(describing: error))")
            }
            try? await Task.sleep(nanoseconds: tickInterval)
        }
        Self.logger.info("\(step): \(maxTicks) ticks exhausted without phase 1 terminal")
        return .ok
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

        // If classes timed out before Phase 1 settled (alumni path with
        // many empty probes), summaries is still empty. Keep polling for
        // another 30s so the user sees visible progress through the
        // schedule step; then re-fetch the list once.
        if summaries.isEmpty {
            Self.logger.info("schedule: summaries empty after classes; continuing to poll")
            let result = await pollUntilInitialTerminal(
                step: "schedule",
                maxTicks: 20,
                tickInterval: 1_500_000_000,
            )
            if case .fail(let authBroken) = result, authBroken {
                return .fail(authBroken: true)
            }
            _ = await fetchSemesterListAndStore()
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

    // Gates onDone on "the backend has actually put a semester on disk
    // for this student." For enrolled students this is true immediately
    // after Phase 1 finishes; for alumni it may take tens of seconds
    // while Phase 1 probes through many empty newer candidates. Polls
    // indefinitely on 1.5s intervals — there's no "give up" budget here
    // since advancing would open the next screen onto empty data. The
    // only early exit is initial.state == .failed (Phase 1 genuinely
    // can't find anything, e.g. credentials became invalid).
    private func runGradesStep() async -> StepResult {
        var iter = 0
        while true {
            do {
                let outcome = try await useCases.onboardingStatus.invoke()
                switch onEnum(of: outcome) {
                case .ok(let wrapper):
                    if let status = wrapper.value {
                        let state = status.initial.state
                        let applied = status.initial.appliedSemesters
                        Self.logger
                            .info("grades: iter=\(iter) initial=\(String(describing: state)) applied=\(applied)")
                        if applied > 0 {
                            return .ok
                        }
                        if state == .failed {
                            Self.logger.info("grades: phase 1 failed with no applied semesters -> proceeding")
                            return .ok
                        }
                    }
                case .err(let wrapper):
                    let authBroken = isUnauthorized(wrapper.error)
                    Self.logger
                        .warning(
                            "grades: status err iter=\(iter) authBroken=\(authBroken) err=\(String(describing: wrapper.error))"
                        )
                    if authBroken {
                        anyAuthBroken = true
                        return .fail(authBroken: true)
                    }
                }
            } catch is CancellationError {
                Self.logger.info("grades: cancelled")
                return .fail(authBroken: false)
            } catch {
                Self.logger.warning("grades: status threw \(String(describing: error))")
            }
            iter += 1
            try? await Task.sleep(nanoseconds: 1_500_000_000)
        }
    }

    private func runMessagesStep() async -> StepResult {
        // Phase 1 fetches the first 20 messages synchronously as its
        // last step, then finalizes. grades may release on
        // appliedSemesters > 0 before that fetch lands (apply-semester
        // bumps the count mid-Phase-1; messages come after). Poll
        // initial.state terminal here so the first page we fetch
        // actually contains Phase 1's messages. Phase 2's deep-
        // pagination runs in the background and isn't gated on.
        let result = await pollUntilInitialTerminal(
            step: "msgs",
            maxTicks: 60,
            tickInterval: 1_500_000_000,
        )
        if case .fail(let authBroken) = result, authBroken {
            return .fail(authBroken: true)
        }

        do {
            Self.logger.info("msgs: requesting first page")
            let firstPage = try await useCases.messages.invoke(since: nil, cursor: nil)
            switch onEnum(of: firstPage) {
            case .ok(let wrapper):
                let appliedCount = wrapper.value?.appliedCount ?? 0
                let nextCursor = wrapper.value?.nextCursor
                Self.logger
                    .info(
                        "msgs: first page ok applied=\(appliedCount) nextCursor=\(nextCursor ?? "<nil>")"
                    )
                if appliedCount == 0 {
                    Self.logger
                        .warning(
                            "msgs: first page returned zero messages — server mirror empty or user has no inbox-matching scopes"
                        )
                }
                if let next = nextCursor {
                    let messagesUseCase = useCases.messages
                    let logger = Self.logger
                    Task.detached {
                        var cursor: String? = next
                        var pages = 0
                        var totalApplied = 0
                        let maxPages = 20
                        while let c = cursor, pages < maxPages {
                            guard let outcome = try? await messagesUseCase.invoke(since: nil, cursor: c) else {
                                logger.warning("msgs: background page \(pages + 1) threw or cancelled")
                                break
                            }
                            switch onEnum(of: outcome) {
                            case .ok(let w):
                                let applied = Int(w.value?.appliedCount ?? 0)
                                totalApplied += applied
                                cursor = w.value?.nextCursor
                                logger
                                    .info(
                                        "msgs: background page \(pages + 1) applied=\(applied) nextCursor=\(cursor ?? "<nil>")"
                                    )
                            case .err(let w):
                                logger
                                    .warning(
                                        "msgs: background page \(pages + 1) err=\(String(describing: w.error))"
                                    )
                                return
                            }
                            pages += 1
                        }
                        logger.info("msgs: background pagination done pages=\(pages) totalApplied=\(totalApplied)")
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
            log.warn("onboarding sync finished with broken auth")
            Self.logger.info("sync: finish -> onAuthFailed")
            onAuthFailed()
        } else {
            log.info("onboarding sync finished ok")
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
        Self.logger.info("readiness: awaiting profile + schedule + grades + msgs")
        if let profileTask {
            _ = await profileTask.value
        }
        if let scheduleTask {
            await waitOrTimeout(scheduleTask, seconds: 2.0)
        }
        // Grades blocks onDone until the backend confirms a semester is
        // on disk. This is the critical gate — opening the next screen
        // onto empty data is worse than a longer sync view.
        if let gradesTask {
            _ = await gradesTask.value
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
        case "schedule": return scheduleTask
        // grades now has its own polling task — it waits until the backend
        // reports appliedSemesters > 0 (or Phase 1 permanently failed)
        // before letting onDone fire.
        case "grades":   return gradesTask
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
