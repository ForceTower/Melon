import Foundation
import Observation
import OSLog
import SwiftUI
@preconcurrency import Umbrella

// KMP type aliases — the Umbrella framework module-prefixes every generated
// class. Local aliases keep the mapping code readable against the iOS
// presentation structs defined in `DisciplineModels.swift`.
private typealias KmpState = DisciplinesDisciplinesListState
private typealias KmpSemester = DisciplinesSemesterDisciplines
private typealias KmpPending = DisciplinesPendingSemester
private typealias KmpItem = DisciplinesDisciplineListItem
private typealias KmpGrade = DisciplinesListGradeEntry
private typealias KmpStatus = DisciplinesDisciplineStatusKind

// Drives `DisciplinesListView`. Subscribes to a single KMP flow that carries
// the full screen state (current semester, downloaded past, pending) and
// maps each emission into the iOS `Semester` / `Discipline` structs the
// existing cards already consume — no UI changes needed. The download action
// awaits `SyncSemesterUseCase` and lets the flow re-emit once the payload
// lands in the local DB.
@MainActor
@Observable
final class DisciplinesListViewModel {
    private(set) var current: Semester?
    private(set) var past: [Semester] = []
    private(set) var pending: [Semester] = []
    private(set) var downloading: Set<String> = []
    private(set) var downloadError: String?

    private let useCases: DisciplinesUseCases
    private var didStart = false

    private static let logger = Logger(subsystem: "dev.forcetower.melon", category: "disciplines")

    init(useCases: DisciplinesUseCases) {
        self.useCases = useCases
    }

    func observe() async {
        guard !didStart else { return }
        didStart = true
        for await value in useCases.observeList.invoke() {
            apply(state: value)
        }
    }

    func download(_ semesterCode: String) async {
        guard let dbId = pending.first(where: { $0.id == semesterCode })?.dbSemesterId else {
            Self.logger.error("download requested for unknown semester: \(semesterCode, privacy: .public)")
            return
        }
        guard !downloading.contains(semesterCode) else { return }
        downloading.insert(semesterCode)
        downloadError = nil
        defer { downloading.remove(semesterCode) }

        Self.logger.info("semester sync start: code=\(semesterCode, privacy: .public) dbId=\(dbId, privacy: .public)")
        do {
            let outcome = try await useCases.syncSemester.invoke(semesterId: dbId)
            switch onEnum(of: outcome) {
            case .ok:
                // DB write triggers the flow to re-emit and reclassify this
                // semester into `past` / `current`; no local mutation needed.
                Self.logger.info("semester sync ok: code=\(semesterCode, privacy: .public)")
                return
            case .err(let wrapper):
                downloadError = wrapper.error.map(Self.describe) ?? "Falha ao baixar o semestre."
            }
        } catch {
            // Bridging error from KMP — rare, since SyncSemesterUseCase wraps
            // failures in Outcome.Err. Log the full NSError payload so we can
            // see what slipped past the sealed-class envelope.
            let ns = error as NSError
            Self.logger.error(
                "semester sync threw: code=\(semesterCode, privacy: .public) domain=\(ns.domain, privacy: .public) code=\(ns.code, privacy: .public) desc=\(error.localizedDescription, privacy: .public) userInfo=\(String(describing: ns.userInfo), privacy: .public)",
            )
            downloadError = "Falha ao baixar o semestre."
        }
    }

    // MARK: - Slice applier

    private func apply(state: KmpState) {
        current = state.current.map { Self.map(semester: $0) }
        past = state.past.map { Self.map(semester: $0) }
        pending = state.pending.map { Self.map(pending: $0) }
    }

    // MARK: - Mapping

    private static func map(semester raw: KmpSemester) -> Semester {
        Semester(
            id: raw.semesterCode,
            disciplines: raw.disciplines.map { map(item: $0) },
            isDownloaded: true,
            estimatedCount: nil,
            dbSemesterId: raw.semesterId
        )
    }

    private static func map(pending raw: KmpPending) -> Semester {
        Semester(
            id: raw.semesterCode,
            disciplines: [],
            isDownloaded: false,
            estimatedCount: nil,
            dbSemesterId: raw.semesterId
        )
    }

    private static func map(item raw: KmpItem) -> Discipline {
        let code = raw.code
        // Per-evaluation detail from upstream in semester order. Packed into a
        // single "Geral" section here — section/group decomposition is a
        // detail-view concern.
        let section = GradeSection(
            name: "Geral",
            group: nil,
            grades: raw.grades.map(map(grade:))
        )
        let groups = buildGroups(from: raw.groupsLabel)
        return Discipline(
            code: code,
            fullCode: code,
            title: raw.name,
            dept: raw.department ?? "",
            prof: raw.teacherName ?? "",
            color: ColorFor.discipline(code: code),
            hours: Int(raw.hours),
            absences: Int(raw.missedHours),
            allowedAbsences: Int(raw.allowedMissedHours),
            sections: [section],
            classes: [],
            attachments: [],
            ementa: nil,
            groups: groups,
            finalGrade: raw.finalGrade?.doubleValue,
            storedPartialAverage: raw.partialAverage?.doubleValue,
            disciplineId: raw.disciplineId,
            offerId: raw.offerId,
            semesterId: raw.semesterId
        )
    }

    private static func map(grade raw: KmpGrade) -> GradeEntry {
        GradeEntry(
            label: (raw.nameShort ?? raw.name),
            title: raw.name,
            date: DisciplineDateFormatting.ddMmYyyy(iso: raw.date),
            score: raw.value?.doubleValue
        )
    }

    private static func buildGroups(from label: String?) -> [DisciplineGroup] {
        guard let label, !label.isEmpty else { return [] }
        return label
            .components(separatedBy: " · ")
            .map { kind in
                DisciplineGroup(code: "", kind: kind, prof: "")
            }
    }

    private static func describe(_ error: SyncSyncError) -> String {
        // KMP `SyncError` arrives as a sealed-subclass instance; its runtime
        // class name (e.g. "SyncSyncErrorNoConnection") identifies the
        // variant without depending on SKIE's `onEnum` bridging for this
        // type. `String(describing:)` on the instance includes data-class
        // payloads like `Server(message=...)`; use that so the backend's
        // message ends up in the log when present. For the catch-all
        // NoConnection case, cross-ref the `[MirrorRepository]` println the
        // KMP layer now emits — that's where the real throwable lives.
        let rendered = String(describing: error)
        let hint: String
        if rendered.contains("NoConnection") {
            hint = " (see `[MirrorRepository]` log in Xcode console for the caught throwable)"
        } else if rendered.contains("Unexpected") {
            hint = " (envelope decode or null data — see `[MirrorRepository]` log)"
        } else {
            hint = ""
        }
        Self.logger.error("semester sync failed: \(rendered, privacy: .public)\(hint, privacy: .public)")
        return "Falha ao baixar o semestre."
    }
}
