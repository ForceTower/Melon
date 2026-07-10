import Foundation
import Testing

@testable import UNESKit

struct EnrollmentSessionTests {
    @Test
    func seededProposalHasNoBlockers() {
        let session = EnrollmentSession.preview

        #expect(session.totalHours == 240)
        #expect(session.conflicts.isEmpty)
        #expect(session.blockers.isEmpty)
        #expect(session.waitlistedCount == 0)
    }

    @Test
    func overlappingPicksSurfaceAsConflicts() {
        var session = EnrollmentSession.preview
        // TEC502 T01 meets Monday 13:30, on top of EXA427 T01.
        session.setSection(30401, forDiscipline: 204)

        let conflicts = session.conflicts
        #expect(conflicts.count == 1)
        #expect(conflicts[0].aCode == "EXA427")
        #expect(conflicts[0].bCode == "TEC502")
        #expect(conflicts[0].day == 1)
        #expect(session.blockers == [.conflicts(1)])
    }

    @Test
    func clashReportsThePickBlockingACandidateSection() {
        let session = EnrollmentSession.preview
        let concurrency = session.discipline(204)!
        let clashing = concurrency.section(30401)!

        let clash = session.clash(with: clashing, excluding: concurrency.id)
        #expect(clash?.discipline.code == "EXA427")
        #expect(clash?.section.label == "T01")
        #expect(clash?.day == 1)

        // Excluding an unrelated discipline keeps reporting the clash…
        #expect(session.clash(with: clashing, excluding: 205) != nil)
        // …while excluding the clash's own discipline clears it.
        #expect(session.clash(with: clashing, excluding: 201) == nil)
    }

    @Test
    func sectionsWithoutScheduleNeverConflict() {
        var session = EnrollmentSession.preview
        // TEC505 T02 is still "a definir".
        session.setSection(30502, forDiscipline: 205)

        #expect(session.conflicts.isEmpty)
        let pending = session.discipline(205)!.section(30502)!
        #expect(!pending.hasSchedule)
        #expect(session.clash(with: pending, excluding: 0) == nil)
    }

    @Test
    func selectingAFullSectionQueuesAutomatically() {
        var session = EnrollmentSession.preview
        let probability = session.discipline(203)!
        let full = probability.section(30301)!
        #expect(full.seats.isFull)

        session.select(probability, section: full)

        let pick = session.pick(for: 203)
        #expect(pick?.sectionId == 30301)
        #expect(pick?.waitlist == true)
        #expect(session.waitlistedCount == 1)
        // Still one pick per discipline.
        #expect(session.picks.count == 4)
    }

    @Test
    func selectingWithoutAQueueNeverWaitlists() {
        var session = EnrollmentSession.preview
        session.window?.useQueue = false
        let probability = session.discipline(203)!

        session.select(probability, section: probability.section(30301)!)

        #expect(session.pick(for: 203)?.waitlist == false)
    }

    @Test
    func blockersTrackTheWorkloadBand() {
        var session = EnrollmentSession.preview

        session.remove(disciplineId: 203)
        #expect(session.blockers == [.underMinimum(missing: 60)])

        session.picks = []
        #expect(session.blockers == [.empty])
    }

    @Test
    func preseedRebuildsPicksFromSavedSections() {
        var session = EnrollmentSession(window: .preview, disciplines: .previewCatalogue)
        session.preseedFromSavedProposal()

        #expect(session.picks == [
            EnrollmentPick(disciplineId: 201, sectionId: 30101, allowsOther: true, waitlist: false),
            EnrollmentPick(disciplineId: 203, sectionId: 30302, allowsOther: true, waitlist: false),
            EnrollmentPick(disciplineId: 204, sectionId: 30402, allowsOther: true, waitlist: false),
            EnrollmentPick(disciplineId: 205, sectionId: 30501, allowsOther: true, waitlist: false),
        ])
    }

    @Test
    func selectionsMirrorThePicks() {
        var session = EnrollmentSession.preview
        session.setAllowsOther(false, disciplineId: 201)
        session.setWaitlist(true, disciplineId: 204)

        #expect(session.selections == [
            EnrollmentSelection(sectionId: 30101, allowsOther: false, waitlist: false),
            EnrollmentSelection(sectionId: 30402, allowsOther: true, waitlist: true),
            EnrollmentSelection(sectionId: 30501, allowsOther: true, waitlist: false),
            EnrollmentSelection(sectionId: 30302, allowsOther: false, waitlist: false),
        ])
    }

    @Test
    func periodsGroupAscendingWithOptativasLast() {
        let groups = [EnrollmentDiscipline].previewCatalogue.groupedByPeriod

        #expect(groups.map(\.period) == [4, 0])
        let allMandatory = groups[0].disciplines.allSatisfy(\.mandatory)
        #expect(allMandatory)
        #expect(groups[1].disciplines.map(\.code) == ["LET021", "CHF336", "TEC540"])
    }

    @Test
    func seatStates() {
        #expect(EnrollmentSeats(filled: 20, total: 45).isTight == false)
        #expect(EnrollmentSeats(filled: 27, total: 30).isTight == true)
        #expect(EnrollmentSeats(filled: 45, total: 45).isFull == true)
        #expect(EnrollmentSeats(filled: 45, total: 45).isTight == false)
    }

    @Test
    func timetableLayoutLanesAndConflicts() {
        var session = EnrollmentSession.preview
        session.setSection(30401, forDiscipline: 204)

        let columns = EnrollmentTimetableLayout.columns(for: session.resolvedPicks)
        let monday = columns[1]!

        // EXA427 and TEC502 share Monday 13:30 — side by side, both burning.
        #expect(monday.lanes == 2)
        let overlapping = monday.blocks.filter { $0.startMinute == 13 * 60 + 30 }
        #expect(overlapping.count == 2)
        #expect(Set(overlapping.map(\.lane)) == [0, 1])
        let allConflicting = overlapping.allSatisfy(\.conflicting)
        #expect(allConflicting)
    }

    @Test
    func windowCountdown() {
        let window = EnrollmentWindow.preview
        let now = window.startDate!.addingTimeInterval(2 * 86_400)

        #expect(window.daysLeft(now: now) == 6)
        let fraction = window.remainingFraction(now: now)
        #expect(fraction > 0.7 && fraction < 0.8)
        #expect(window.remainingFraction(now: window.endDate!.addingTimeInterval(86_400)) == 0)
    }
}

extension EnrollmentSession {
    /// Test helper: swap a discipline's picked section keeping the toggles.
    fileprivate mutating func setSection(_ sectionId: Int64, forDiscipline disciplineId: Int64) {
        guard let index = picks.firstIndex(where: { $0.disciplineId == disciplineId }) else { return }
        picks[index].sectionId = sectionId
    }
}
