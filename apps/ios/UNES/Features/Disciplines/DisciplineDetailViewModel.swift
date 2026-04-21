import Foundation
import Observation
import OSLog
import SwiftUI
@preconcurrency import Umbrella

// KMP type aliases — SKIE module-prefixes every generated symbol. Local
// aliases keep the mapping code readable against the iOS presentation structs
// defined in `DisciplineModels.swift`.
private typealias KmpDetail = DisciplinesDisciplineDetail
private typealias KmpDetailGroup = DisciplinesDisciplineDetailGroup
private typealias KmpDetailSection = DisciplinesDisciplineDetailSection
private typealias KmpDetailGrade = DisciplinesDisciplineDetailGrade
private typealias KmpDetailLecture = DisciplinesDisciplineDetailLecture
private typealias KmpDetailAttachment = DisciplinesDisciplineDetailAttachment

// Drives `DisciplineDetailView`. Seeded with the Discipline the parent list
// tapped — so the screen renders instantly against whatever fields were on
// the list card — then subscribes to `ObserveDisciplineDetailUseCase` to
// replace `discipline` with a fully-hydrated struct on each DB emission.
// Fixture seeds (offerId == nil, or useCases == nil for `#Preview`) skip the
// subscription entirely so previews don't need a real DI graph.
@MainActor
@Observable
final class DisciplineDetailViewModel {
    private(set) var discipline: Discipline

    private let useCases: DisciplinesUseCases?
    private var didStart = false

    private static let logger = Logger(subsystem: "dev.forcetower.melon", category: "disciplines")

    init(seed: Discipline, useCases: DisciplinesUseCases?) {
        self.discipline = seed
        self.useCases = useCases
    }

    // Factory-less init for `#Preview`.
    convenience init(seed: Discipline) {
        self.init(seed: seed, useCases: nil)
    }

    func observe() async {
        guard !didStart else { return }
        didStart = true
        guard let useCases, let offerId = discipline.offerId else { return }
        for await detail in useCases.observeDetail.invoke(offerId: offerId) {
            guard let detail else { continue }
            discipline = Self.map(detail: detail, seed: discipline)
        }
    }

    // MARK: - Mapping

    private static func map(detail raw: KmpDetail, seed: Discipline) -> Discipline {
        let hasMultipleGroups = raw.groups.count > 1
        let classIdToGroupName = Dictionary(
            uniqueKeysWithValues: raw.groups.map { ($0.classId, $0.code) }
        )
        let groups = raw.groups.map(mapGroup)
        let sections = raw.sections.map(mapSection)
        let classes = mapLectures(raw.lectures)
        let attachments = raw.attachments.map {
            mapAttachment(
                $0,
                showGroup: hasMultipleGroups,
                resolvedGroupName: classIdToGroupName[$0.classId]
            )
        }

        return Discipline(
            code: raw.code,
            fullCode: raw.code,
            title: raw.name,
            dept: raw.department ?? seed.dept,
            prof: primaryProf(from: raw.groups) ?? seed.prof,
            color: ColorFor.discipline(code: raw.code),
            hours: Int(raw.hours),
            absences: Int(raw.missedHours),
            allowedAbsences: Int(raw.allowedMissedHours),
            sections: sections,
            classes: classes,
            attachments: attachments,
            ementa: raw.ementa,
            groups: groups,
            finalGrade: raw.finalGrade?.doubleValue,
            disciplineId: raw.disciplineId,
            offerId: raw.offerId,
            semesterId: raw.semesterId
        )
    }

    private static func mapGroup(_ raw: KmpDetailGroup) -> DisciplineGroup {
        DisciplineGroup(
            code: raw.code,
            kind: kindLabel(raw.kind),
            prof: raw.teacherName ?? ""
        )
    }

    // KMP now emits a single merged section per discipline (groupName nil,
    // kind = "" on multi-group). The UI's section header reads `name`, so a
    // blank `kind` falls back to "Notas".
    private static func mapSection(_ raw: KmpDetailSection) -> GradeSection {
        let name = raw.kind.isEmpty ? "Notas" : kindLabel(raw.kind)
        return GradeSection(
            name: name,
            group: raw.groupName,
            grades: raw.grades.map { g in
                GradeEntry(
                    label: g.gradeNameShort ?? "",
                    title: g.evaluationName ?? "",
                    date: DisciplineDateFormatting.ddMmYyyy(iso: g.dateIso),
                    score: g.value?.doubleValue
                )
            }
        )
    }

    // Lectures ordered by ISO date ascending (nulls last, ordinal as the
    // tiebreaker). `past` = strictly before today; `isNext` = first index
    // whose date is on or after today. Uses the UI's pinned "today" so the
    // detail view's notion of "próxima aula" matches the list card.
    private static func mapLectures(_ rows: [KmpDetailLecture]) -> [ClassEntry] {
        let sorted = rows.sorted { lhs, rhs in
            switch (lhs.dateIso, rhs.dateIso) {
            case let (l?, r?) where l != r:
                return l < r
            case (nil, .some):
                return false
            case (.some, nil):
                return true
            default:
                return lhs.ordinal < rhs.ordinal
            }
        }
        let today = iso(from: DisciplineDate.today)
        let nextIndex = sorted.firstIndex { ($0.dateIso ?? "") >= today && $0.dateIso != nil }
        return sorted.enumerated().map { idx, row in
            let isPast: Bool = {
                guard let d = row.dateIso else { return false }
                return d < today
            }()
            let count = Int(row.attachmentCount)
            return ClassEntry(
                date: DisciplineDateFormatting.ddMmYyyy(iso: row.dateIso),
                title: row.subject ?? "",
                attachments: count > 0 ? count : nil,
                past: isPast,
                isNext: idx == nextIndex
            )
        }
    }

    private static func mapAttachment(
        _ raw: KmpDetailAttachment,
        showGroup: Bool,
        resolvedGroupName: String?
    ) -> Attachment {
        let caption = raw.caption?.isEmpty == false ? raw.caption : nil
        let name = caption ?? friendlyName(url: raw.url)
        return Attachment(
            name: name,
            kind: inferKind(url: raw.url),
            added: DisciplineDateFormatting.ddMm(iso: raw.lectureDateIso) ?? "",
            group: showGroup ? (resolvedGroupName ?? raw.groupName) : nil
        )
    }

    // MARK: - Helpers

    private static func primaryProf(from groups: [KmpDetailGroup]) -> String? {
        groups.lazy.compactMap(\.teacherName).first(where: { !$0.isEmpty })
    }

    private static func kindLabel(_ raw: String) -> String {
        switch raw.uppercased() {
        case "TEO", "TEORICA", "TEÓRICA": return "Teórica"
        case "PRA", "PRATICA", "PRÁTICA": return "Prática"
        case "LAB", "LABORATORIO", "LABORATÓRIO": return "Laboratório"
        default: return raw
        }
    }

    private static func inferKind(url: String) -> AttachmentKind {
        let lower = url.lowercased()
        if lower.hasSuffix(".pdf") { return .pdf }
        if lower.hasSuffix(".ppt") || lower.hasSuffix(".pptx") || lower.hasSuffix(".key") {
            return .slides
        }
        if lower.hasSuffix(".md") || lower.hasSuffix(".txt") { return .notes }
        if lower.hasPrefix("http://") || lower.hasPrefix("https://") { return .link }
        return .other
    }

    private static func friendlyName(url: String) -> String {
        if let parsed = URL(string: url) {
            let last = parsed.lastPathComponent
            if !last.isEmpty, last != "/" { return last }
        }
        return url
    }

    // `DisciplineDate.today` is pinned (2026-04-18) to match the rest of the
    // fixtures-driven rendering. Producing an ISO string here keeps the
    // comparison on the same format KMP emits.
    private static func iso(from date: Date) -> String {
        let c = Calendar.current.dateComponents([.year, .month, .day], from: date)
        return String(
            format: "%04d-%02d-%02d",
            c.year ?? 0,
            c.month ?? 0,
            c.day ?? 0
        )
    }
}
