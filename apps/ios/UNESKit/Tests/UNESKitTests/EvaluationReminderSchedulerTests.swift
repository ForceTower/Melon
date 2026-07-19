import Foundation
import Testing

@testable import UNESKit

/// The pure half of the reminder scheduler: which evaluations earn an
/// evening-before request, when it fires, and the identifier that keeps a
/// reminder attached to its evaluation across full-scope mirror rewrites.
struct EvaluationReminderSchedulerTests {
    let calendar = Calendar.current

    private func date(day: Int, hour: Int, minute: Int = 0) -> Date {
        calendar.date(from: DateComponents(year: 2026, month: 4, day: day, hour: hour, minute: minute))!
    }

    private func evaluation(
        id: String = "evaluation/sem/disc/g1",
        title: String = "P2 — Cálculo II",
        dateStamp: String
    ) -> SpotlightEvaluation {
        SpotlightEvaluation(
            id: id,
            semesterId: "sem",
            disciplineId: "disc",
            gradeId: "g1",
            title: title,
            subtitle: "",
            dateStamp: dateStamp,
            keywords: []
        )
    }

    @Test
    func firesAtTwentyOnTheEve() {
        let reminders = EvaluationReminderScheduler.desiredReminders(
            evaluations: [evaluation(dateStamp: "2026-04-20")],
            now: date(day: 16, hour: 9),
            calendar: calendar
        )
        #expect(reminders.count == 1)
        #expect(reminders[0].fire == DateComponents(year: 2026, month: 4, day: 19, hour: 20, minute: 0))
        #expect(reminders[0].identifier == "evaluation-reminder/evaluation/sem/disc/g1")
        #expect(reminders[0].body == "P2 — Cálculo II")
    }

    @Test
    func skipsWhenTheEveMomentAlreadyPassed() {
        // Evaluation tomorrow, but it's already past 20:00 tonight — there
        // is no honest moment left to fire.
        let reminders = EvaluationReminderScheduler.desiredReminders(
            evaluations: [evaluation(dateStamp: "2026-04-17")],
            now: date(day: 16, hour: 21),
            calendar: calendar
        )
        #expect(reminders.isEmpty)
    }

    @Test
    func keepsTomorrowsEvaluationBeforeTwenty() {
        let reminders = EvaluationReminderScheduler.desiredReminders(
            evaluations: [evaluation(dateStamp: "2026-04-17")],
            now: date(day: 16, hour: 19, minute: 59),
            calendar: calendar
        )
        #expect(reminders.count == 1)
        #expect(reminders[0].fire.day == 16)
    }

    @Test
    func skipsUnparsableDateStamps() {
        let reminders = EvaluationReminderScheduler.desiredReminders(
            evaluations: [evaluation(dateStamp: "not-a-date")],
            now: date(day: 16, hour: 9),
            calendar: calendar
        )
        #expect(reminders.isEmpty)
    }
}
