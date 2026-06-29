import SwiftUI

// UNES — the student's in-progress enrollment proposal. Reconstructs the
// `enroll` controller the prototype screens read from: the pick list, the
// running workload, the conflict set, and the mutation helpers. Plain UI state
// (no upstream wiring yet), shared by reference across every flow screen.

/// One chosen turma in the proposal. `allowsOther` and `waitlist` carry the
/// per-pick toggles the review screen exposes.
struct EnrollmentPick: Identifiable {
    let discipline: OfferedDiscipline
    let section: ClassSection
    var allowsOther: Bool
    var waitlist: Bool

    var id: Int64 { discipline.id }
}

/// A resolved clash between two picked sections, with the labels and weekday
/// the warning banners render.
struct ScheduleConflict: Identifiable {
    let id = UUID()
    let a: OfferedDiscipline
    let aLabel: String
    let b: OfferedDiscipline
    let bLabel: String
    let day: Int
}

@Observable
final class EnrollmentState {
    /// At most one section per discipline, in insertion order.
    private(set) var picks: [EnrollmentPick] = []

    /// Running workload across the proposal, compared against the window bounds.
    var totalHours: Int { picks.reduce(0) { $0 + $1.discipline.workload } }

    /// The chosen section for a discipline, or nil when none is picked.
    func selection(for disciplineId: Int64) -> EnrollmentPick? {
        picks.first { $0.discipline.id == disciplineId }
    }

    /// Pick a section, replacing any prior choice for the same discipline.
    func select(_ discipline: OfferedDiscipline, _ section: ClassSection, waitlist: Bool) {
        picks.removeAll { $0.discipline.id == discipline.id }
        picks.append(
            EnrollmentPick(
                discipline: discipline,
                section: section,
                allowsOther: section.allowsOtherDefault,
                waitlist: waitlist
            )
        )
    }

    func remove(_ disciplineId: Int64) {
        picks.removeAll { $0.discipline.id == disciplineId }
    }

    func setAllowsOther(_ disciplineId: Int64, _ value: Bool) {
        guard let i = picks.firstIndex(where: { $0.discipline.id == disciplineId }) else { return }
        picks[i].allowsOther = value
    }

    func setWaitlist(_ disciplineId: Int64, _ value: Bool) {
        guard let i = picks.firstIndex(where: { $0.discipline.id == disciplineId }) else { return }
        picks[i].waitlist = value
    }

    /// Every clash among the picked sections, each unordered pair once.
    var conflicts: [ScheduleConflict] {
        var out: [ScheduleConflict] = []
        for i in picks.indices {
            for j in picks.index(after: i)..<picks.endIndex {
                guard let day = EnrollmentScheduling.conflictDay(picks[i].section, picks[j].section) else { continue }
                out.append(
                    ScheduleConflict(
                        a: picks[i].discipline, aLabel: picks[i].section.label,
                        b: picks[j].discipline, bLabel: picks[j].section.label,
                        day: day
                    )
                )
            }
        }
        return out
    }

    /// If picking `section` for `discipline` would clash with an already-picked
    /// section of a *different* discipline, the offending pick + weekday.
    func clash(for discipline: OfferedDiscipline, _ section: ClassSection)
        -> (discipline: OfferedDiscipline, section: ClassSection, day: Int)? {
        for pick in picks where pick.discipline.id != discipline.id {
            if let day = EnrollmentScheduling.conflictDay(section, pick.section) {
                return (pick.discipline, pick.section, day)
            }
        }
        return nil
    }
}

extension EnrollmentState {
    /// A rich proposal for previews: three picks that together exercise a
    /// schedule clash (EXA427 × TEC502 on Monday), a waitlisted full section
    /// (EXA866), and an under-minimum workload (180h < 240h).
    static var previewSeeded: EnrollmentState {
        let state = EnrollmentState()
        let disciplines = EnrollmentFixtures.disciplines
        if let exa427 = disciplines.first(where: { $0.code == "EXA427" }) {
            state.select(exa427, exa427.sections[0], waitlist: false)
        }
        if let tec502 = disciplines.first(where: { $0.code == "TEC502" }) {
            state.select(tec502, tec502.sections[0], waitlist: false)
        }
        if let exa866 = disciplines.first(where: { $0.code == "EXA866" }) {
            state.select(exa866, exa866.sections[0], waitlist: true)
        }
        return state
    }
}
