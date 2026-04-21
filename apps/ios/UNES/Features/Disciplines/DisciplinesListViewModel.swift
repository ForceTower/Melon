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

        do {
            let outcome = try await useCases.syncSemester.invoke(semesterId: dbId)
            switch onEnum(of: outcome) {
            case .ok:
                // DB write triggers the flow to re-emit and reclassify this
                // semester into `past` / `current`; no local mutation needed.
                return
            case .err(let wrapper):
                downloadError = wrapper.error.map(Self.describe) ?? "Falha ao baixar o semestre."
            }
        } catch {
            Self.logger.error("semester sync threw: \(error.localizedDescription, privacy: .public)")
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
        // Pack the KMP grade list into a single GradeSection so the iOS card's
        // derived helpers (`allGrades`, `partialAverage`, `nextEvaluation`,
        // `completedCount`, `totalEvaluations`) all resolve correctly. Full
        // section/group decomposition is a detail-view concern.
        let section = GradeSection(
            name: "Geral",
            group: nil,
            grades: buildGrades(for: raw)
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
            disciplineId: raw.disciplineId,
            offerId: raw.offerId,
            semesterId: raw.semesterId
        )
    }

    // Builds a fake-but-coherent evaluation list sized to `totalEvaluations`.
    // KMP only hands us `nextEvaluationTitle`/`Date` + the completed count, so
    // we synthesize placeholder rows for the completed grades (untitled, no
    // date — they feed `partialAverage` via a single weighted value) and a
    // titled row for the next upcoming evaluation. This is enough to satisfy
    // every computed helper on `Discipline` that the list card reads. The
    // detail view will re-query real evaluation data when it's wired up.
    private static func buildGrades(for raw: KmpItem) -> [GradeEntry] {
        var entries: [GradeEntry] = []
        let completed = Int(raw.completedEvaluations)
        let total = Int(raw.totalEvaluations)
        let average = raw.partialAverage?.doubleValue

        for _ in 0..<completed {
            entries.append(
                GradeEntry(
                    label: "",
                    title: "",
                    date: nil,
                    score: average
                )
            )
        }

        let pending = max(0, total - completed)
        if pending > 0 {
            // First pending slot carries the real next-evaluation metadata so
            // `nextEvaluation` on Discipline surfaces the correct title/date.
            entries.append(
                GradeEntry(
                    label: "",
                    title: raw.nextEvaluationTitle ?? "",
                    date: formatDdMmYyyy(iso: raw.nextEvaluationDateIso),
                    score: nil
                )
            )
            for _ in 1..<pending {
                entries.append(
                    GradeEntry(
                        label: "",
                        title: "",
                        date: nil,
                        score: nil
                    )
                )
            }
        }
        return entries
    }

    private static func buildGroups(from label: String?) -> [DisciplineGroup] {
        guard let label, !label.isEmpty else { return [] }
        return label
            .components(separatedBy: " · ")
            .map { kind in
                DisciplineGroup(code: "", kind: kind, prof: "")
            }
    }

    // KMP emits ISO "yyyy-MM-dd"; the iOS card reads "dd/MM/yyyy" strings
    // via `DisciplineDate.daysUntil`. Convert here so the existing helpers
    // don't have to special-case format.
    private static func formatDdMmYyyy(iso: String?) -> String? {
        guard let iso, iso.count >= 10 else { return nil }
        let year = iso.prefix(4)
        let month = iso.dropFirst(5).prefix(2)
        let day = iso.dropFirst(8).prefix(2)
        return "\(day)/\(month)/\(year)"
    }

    private static func describe(_ error: SyncSyncError) -> String {
        // Error classification shapes are in flux across features; fall back
        // to a generic message and surface details via logs.
        Self.logger.error("semester sync failed: \(String(describing: error), privacy: .public)")
        return "Falha ao baixar o semestre."
    }
}
