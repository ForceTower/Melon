import Foundation
import Sharing

// The matrícula domain: the enrollment window published by SAGRES, the
// offered disciplines tree, and the student's in-progress proposal. Offers
// change by the second upstream, so nothing here touches the mirror — the
// session lives in memory for the duration of the flow.

// MARK: - Window

enum EnrollmentWindowState: String, Equatable, Sendable {
    case open = "OPEN"
    case upcoming = "UPCOMING"
    /// Either the student already finalized ("proposta enviada") or the
    /// window's end date passed.
    case closed = "CLOSED"
}

struct EnrollmentWindow: Equatable, Sendable {
    /// Semester label as upstream sends it, e.g. "2026.2".
    var semester: String
    var state: EnrollmentWindowState
    /// SAGRES sends offset datetimes that occasionally omit seconds; parsing
    /// is lenient, so a malformed bound degrades the labels, not the flow.
    var startDate: Date?
    var endDate: Date?
    var minHours: Int
    var maxHours: Int
    /// Whether full sections take waitlist entries instead of blocking.
    var useQueue: Bool
}

// MARK: - Offered disciplines

struct EnrollmentDiscipline: Equatable, Sendable, Identifiable {
    let id: Int64
    var code: String
    var name: String
    var workload: Int
    var mandatory: Bool
    /// Curriculum period; 0 marks optativas/eletivas.
    var gradePeriod: Int
    /// Suggested by the course for the student's current period.
    var suggestion: Bool
    /// Info-only: the live backend always reports prerequisites as met, since
    /// SAGRES pre-filters offers to what the student can request.
    var prereqs: [EnrollmentPrerequisite]
    var sections: [EnrollmentSection]
    /// Stable slot into the discipline tint palette, assigned at load.
    var colorIndex: Int

    var hasUnmetPrereq: Bool { prereqs.contains { !$0.met } }

    func section(_ id: Int64) -> EnrollmentSection? {
        sections.first { $0.id == id }
    }
}

struct EnrollmentPrerequisite: Equatable, Sendable {
    var code: String
    var name: String
    var met: Bool
}

/// A turma the student can request — what SAGRES calls a "turma de
/// matrícula" item. Its id is the one the submit endpoint expects.
struct EnrollmentSection: Equatable, Sendable, Identifiable {
    let id: Int64
    /// "T01", "T01P01" — theory and practice bundle into one section.
    var label: String
    /// The section's course holds priority over its seats.
    var coursePreferential: Bool
    var suggestion: Bool
    var vacancies: Int
    var proposalsCount: Int
    /// Seeds the "aceitar outra turma" toggle.
    var allowsOtherDefault: Bool
    /// Students already queued for a seat.
    var waitlistCount: Int
    /// Part of the proposal saved upstream — the resume mechanism.
    var selected: Bool
    var meetings: [EnrollmentMeeting]

    var slots: [EnrollmentSlot] { meetings.flatMap(\.slots) }
    /// Sections still "a definir" have no slots and never conflict.
    var hasSchedule: Bool { !slots.isEmpty }
    var seats: EnrollmentSeats { EnrollmentSeats(filled: proposalsCount, total: vacancies) }
    var shift: EnrollmentShift { meetings.first?.shift ?? .undefined }

    /// First weekday where this section overlaps `other`, or nil when they fit.
    func conflictDay(with other: EnrollmentSection) -> Int? {
        for a in slots {
            for b in other.slots where a.overlaps(b) {
                return a.day
            }
        }
        return nil
    }
}

struct EnrollmentSeats: Equatable, Sendable {
    var filled: Int
    var total: Int

    var fraction: Double { total > 0 ? Double(filled) / Double(total) : 0 }
    var isFull: Bool { filled >= total }
    var isTight: Bool { !isFull && fraction >= 0.85 }
}

enum EnrollmentShift: String, Equatable, Sendable {
    case morning = "MORNING"
    case afternoon = "AFTERNOON"
    case night = "NIGHT"
    case undefined = "UNDEFINED"
}

/// One meeting stream of a section (the theory or the practice class).
struct EnrollmentMeeting: Equatable, Sendable {
    /// "Teórica" / "Prática" as SAGRES sends it.
    var kind: String
    var shift: EnrollmentShift
    var professors: [String]
    var room: String?
    var slots: [EnrollmentSlot]
}

/// A weekly time block, in minutes so "13:30" and "13:30:00" compare alike.
struct EnrollmentSlot: Equatable, Hashable, Sendable {
    /// Upstream weekday: 0 = Sunday … 6 = Saturday.
    var day: Int
    var startMinute: Int
    var endMinute: Int

    func overlaps(_ other: EnrollmentSlot) -> Bool {
        day == other.day && startMinute < other.endMinute && other.startMinute < endMinute
    }
}

// MARK: - Proposal draft

/// One desired section plus its per-pick toggles — at most one per discipline.
struct EnrollmentPick: Equatable, Sendable, Identifiable {
    var disciplineId: Int64
    var sectionId: Int64
    /// "Sem vaga nesta turma? Me matricule em outra do mesmo componente."
    var allowsOther: Bool
    /// Queue for a seat when the section is full.
    var waitlist: Bool

    var id: Int64 { disciplineId }
}

/// A pick joined back to its catalogue rows, for display.
struct EnrollmentResolvedPick: Equatable, Sendable, Identifiable {
    var discipline: EnrollmentDiscipline
    var section: EnrollmentSection
    var allowsOther: Bool
    var waitlist: Bool

    var id: Int64 { discipline.id }
}

struct EnrollmentConflict: Equatable, Sendable {
    var aCode: String
    var aLabel: String
    var bCode: String
    var bLabel: String
    var day: Int
}

/// Another discipline's pick clashing with a candidate section.
struct EnrollmentClash: Equatable, Sendable {
    var discipline: EnrollmentDiscipline
    var section: EnrollmentSection
    var day: Int
}

/// What `POST api/enrollment/submit` takes: the complete desired set — the
/// backend replaces the saved proposal wholesale, never applies a delta.
struct EnrollmentSelection: Equatable, Sendable {
    var sectionId: Int64
    var allowsOther: Bool
    var waitlist: Bool
}

/// Everything gating the submit button, ordered by severity.
enum EnrollmentBlocker: Equatable, Sendable {
    case conflicts(Int)
    case underMinimum(missing: Int)
    case overMaximum(excess: Int)
    case empty
}

/// Why an enrollment call failed, in terms the screens can phrase.
enum EnrollmentFailure: Error, Equatable {
    case sessionExpired
    case network
    case server(String?)
}

// MARK: - Session

/// The whole flow's shared state: window, catalogue, and the draft picks
/// every screen reads and edits.
struct EnrollmentSession: Equatable, Sendable {
    var window: EnrollmentWindow?
    var disciplines: [EnrollmentDiscipline] = []
    var picks: [EnrollmentPick] = []
    /// "Reabrir matrícula": re-enables editing and resubmission after the
    /// window reported the proposal as sent. Session-local — the backend
    /// reopens the step automatically on the next submit, which replaces
    /// the saved proposal wholesale.
    var reopened = false

    /// Closed and not reopened — every screen renders as the comprovante.
    var isReadonly: Bool {
        window?.state == .closed && !reopened
    }

    /// Picks may only mutate while the window is open or explicitly reopened.
    var canEdit: Bool {
        window?.state == .open || (window?.state == .closed && reopened)
    }

    func discipline(_ id: Int64) -> EnrollmentDiscipline? {
        disciplines.first { $0.id == id }
    }

    func pick(for disciplineId: Int64) -> EnrollmentPick? {
        picks.first { $0.disciplineId == disciplineId }
    }

    /// Picks joined to the catalogue, in selection order. Picks whose rows
    /// vanished from a re-fetched catalogue drop out silently.
    var resolvedPicks: [EnrollmentResolvedPick] {
        picks.compactMap { pick in
            guard let discipline = discipline(pick.disciplineId),
                  let section = discipline.section(pick.sectionId)
            else { return nil }
            return EnrollmentResolvedPick(
                discipline: discipline,
                section: section,
                allowsOther: pick.allowsOther,
                waitlist: pick.waitlist
            )
        }
    }

    var totalHours: Int {
        resolvedPicks.reduce(0) { $0 + $1.discipline.workload }
    }

    var waitlistedCount: Int {
        resolvedPicks.count(where: \.waitlist)
    }

    /// Every clashing pair among the picks.
    var conflicts: [EnrollmentConflict] {
        let resolved = resolvedPicks
        var out: [EnrollmentConflict] = []
        for i in resolved.indices {
            for j in resolved.indices.dropFirst(i + 1) {
                guard let day = resolved[i].section.conflictDay(with: resolved[j].section) else { continue }
                out.append(EnrollmentConflict(
                    aCode: resolved[i].discipline.code,
                    aLabel: resolved[i].section.label,
                    bCode: resolved[j].discipline.code,
                    bLabel: resolved[j].section.label,
                    day: day
                ))
            }
        }
        return out
    }

    /// The pick of another discipline clashing with `section`, if any —
    /// what blocks a section card in the picker.
    func clash(with section: EnrollmentSection, excluding disciplineId: Int64) -> EnrollmentClash? {
        for pick in resolvedPicks where pick.discipline.id != disciplineId {
            if let day = section.conflictDay(with: pick.section) {
                return EnrollmentClash(discipline: pick.discipline, section: pick.section, day: day)
            }
        }
        return nil
    }

    var blockers: [EnrollmentBlocker] {
        guard !picks.isEmpty else { return [.empty] }
        guard let window else { return [] }
        var out: [EnrollmentBlocker] = []
        let conflictCount = conflicts.count
        if conflictCount > 0 { out.append(.conflicts(conflictCount)) }
        let total = totalHours
        if total < window.minHours { out.append(.underMinimum(missing: window.minHours - total)) }
        if total > window.maxHours { out.append(.overMaximum(excess: total - window.maxHours)) }
        return out
    }

    var selections: [EnrollmentSelection] {
        picks.map { EnrollmentSelection(sectionId: $0.sectionId, allowsOther: $0.allowsOther, waitlist: $0.waitlist) }
    }

    // MARK: Mutations

    /// Picks `section`, replacing the discipline's previous pick. Full
    /// sections queue automatically when the window runs a waitlist.
    mutating func select(_ discipline: EnrollmentDiscipline, section: EnrollmentSection) {
        let pick = makePick(disciplineId: discipline.id, section: section)
        if let index = picks.firstIndex(where: { $0.disciplineId == discipline.id }) {
            picks[index] = pick
        } else {
            picks.append(pick)
        }
    }

    mutating func remove(disciplineId: Int64) {
        picks.removeAll { $0.disciplineId == disciplineId }
    }

    mutating func setAllowsOther(_ value: Bool, disciplineId: Int64) {
        guard let index = picks.firstIndex(where: { $0.disciplineId == disciplineId }) else { return }
        picks[index].allowsOther = value
    }

    mutating func setWaitlist(_ value: Bool, disciplineId: Int64) {
        guard let index = picks.firstIndex(where: { $0.disciplineId == disciplineId }) else { return }
        picks[index].waitlist = value
    }

    /// Rebuilds the picks from the sections upstream marked as saved, so a
    /// returning student resumes their current proposal. The wire doesn't
    /// carry the saved toggle values, so they reset to their defaults.
    mutating func preseedFromSavedProposal() {
        picks = disciplines.compactMap { discipline in
            guard let saved = discipline.sections.first(where: \.selected) else { return nil }
            return makePick(disciplineId: discipline.id, section: saved)
        }
    }

    /// The one place a pick's defaults are decided, so a resumed proposal
    /// and a fresh selection can't drift apart.
    private func makePick(disciplineId: Int64, section: EnrollmentSection) -> EnrollmentPick {
        EnrollmentPick(
            disciplineId: disciplineId,
            sectionId: section.id,
            allowsOther: section.allowsOtherDefault,
            waitlist: section.seats.isFull && (window?.useQueue ?? false)
        )
    }
}

extension SharedKey where Self == InMemoryKey<EnrollmentSession>.Default {
    /// The one in-memory session every step of the matrícula flow shares —
    /// a typed key so a screen can't silently bind to a misspelled store.
    static var enrollmentSession: Self {
        Self[.inMemory("enrollmentSession"), default: EnrollmentSession()]
    }
}

// MARK: - Catalogue grouping

/// One "Nº período" section of the offers list; period 0 renders last as
/// "Optativas".
struct EnrollmentPeriodGroup: Equatable, Sendable, Identifiable {
    var period: Int
    var disciplines: [EnrollmentDiscipline]

    var id: Int { period }
}

extension [EnrollmentDiscipline] {
    var groupedByPeriod: [EnrollmentPeriodGroup] {
        let byPeriod = Dictionary(grouping: self, by: \.gradePeriod)
        return byPeriod.keys
            .sorted { a, b in
                if a == 0 { return false }
                if b == 0 { return true }
                return a < b
            }
            .map { EnrollmentPeriodGroup(period: $0, disciplines: byPeriod[$0] ?? []) }
    }
}
