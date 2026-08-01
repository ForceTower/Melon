import ComposableArchitecture
import Foundation
import GRDB
import Testing

@testable import UNESKit

@MainActor
struct CalendarPersonalEventFeatureTests {
    static nonisolated let calendar = Calendar.current

    static nonisolated func day(_ year: Int, _ month: Int, _ day: Int) -> Date {
        calendar.date(from: DateComponents(year: year, month: month, day: day))!
    }

    static nonisolated let now = calendar.date(
        from: DateComponents(year: 2026, month: 4, day: 17, hour: 15, minute: 30)
    )!

    static nonisolated let stored = PersonalEvent(
        id: "u1",
        title: "Entregar relatório do Lab 3",
        start: "2026-04-20",
        end: nil,
        category: .task,
        discipline: nil,
        reminder: .dayBefore,
        notes: "",
        createdAt: day(2026, 4, 10)
    )

    private func makeStore(
        _ state: CalendarPersonalEventFeature.State,
        saved: LockIsolated<[PersonalEvent]> = LockIsolated([]),
        deleted: LockIsolated<[String]> = LockIsolated([])
    ) -> TestStoreOf<CalendarPersonalEventFeature> {
        TestStore(initialState: state) {
            CalendarPersonalEventFeature()
        } withDependencies: {
            $0.calendar = Self.calendar
            $0.date = .constant(Self.now)
            $0.uuid = .incrementing
            $0.personalEventsRepository.save = { event in saved.withValue { $0.append(event) } }
            $0.personalEventsRepository.delete = { id in deleted.withValue { $0.append(id) } }
        }
    }

    @Test
    func savingANewEntryStoresTheComposedShape() async {
        let saved = LockIsolated<[PersonalEvent]>([])
        let store = makeStore(
            CalendarPersonalEventFeature.State(
                day: Self.day(2026, 4, 20),
                disciplines: .preview,
                calendar: Self.calendar
            ),
            saved: saved
        )

        await store.send(.binding(.set(\.title, "  Entregar relatório  "))) {
            $0.title = "  Entregar relatório  "
        }
        await store.send(.binding(.set(\.category, .study))) {
            $0.category = .study
        }
        await store.send(.saveTapped)
        await store.finish()

        let event = saved.value.first
        #expect(saved.value.count == 1)
        #expect(event?.title == "Entregar relatório")
        #expect(event?.start == "2026-04-20")
        #expect(event?.end == nil)
        #expect(event?.category == .study)
        #expect(event?.createdAt == Self.now)
    }

    @Test
    func editingKeepsTheIdentityAndCreationStamp() async {
        let saved = LockIsolated<[PersonalEvent]>([])
        let store = makeStore(
            CalendarPersonalEventFeature.State(
                editing: Self.stored,
                disciplines: .preview,
                calendar: Self.calendar
            ),
            saved: saved
        )

        await store.send(.binding(.set(\.reminder, .week))) {
            $0.reminder = .week
        }
        await store.send(.saveTapped)
        await store.finish()

        #expect(saved.value.first?.id == Self.stored.id)
        #expect(saved.value.first?.createdAt == Self.stored.createdAt)
        #expect(saved.value.first?.reminder == .week)
    }

    @Test
    func aStartPastTheEndPushesTheEndAlong() async {
        let store = makeStore(
            CalendarPersonalEventFeature.State(
                day: Self.day(2026, 4, 20),
                disciplines: [],
                calendar: Self.calendar
            )
        )

        await store.send(.periodToggled(true)) {
            $0.end = Self.day(2026, 4, 22)
        }
        await store.send(.startPicked(Self.day(2026, 4, 30))) {
            $0.start = Self.day(2026, 4, 30)
            $0.end = Self.day(2026, 5, 2)
        }
        await store.send(.periodToggled(false)) {
            $0.end = nil
        }
    }

    @Test
    func blankTitlesNeverSave() async {
        let saved = LockIsolated<[PersonalEvent]>([])
        let store = makeStore(
            CalendarPersonalEventFeature.State(
                day: Self.day(2026, 4, 20),
                disciplines: [],
                calendar: Self.calendar
            ),
            saved: saved
        )

        await store.send(.binding(.set(\.title, "   "))) {
            $0.title = "   "
        }
        await store.send(.saveTapped)
        #expect(saved.value.isEmpty)
    }

    @Test
    func deletingFromTheComposerConfirmsFirst() async {
        let deleted = LockIsolated<[String]>([])
        let store = makeStore(
            CalendarPersonalEventFeature.State(
                editing: Self.stored,
                disciplines: [],
                calendar: Self.calendar
            ),
            deleted: deleted
        )

        await store.send(.deleteTapped) {
            $0.confirmDelete = .deletePersonalEvent(titled: Self.stored.title)
        }
        await store.send(.confirmDelete(.presented(.confirmed))) {
            $0.confirmDelete = nil
        }
        await store.finish()
        #expect(deleted.value == [Self.stored.id])
    }
}

struct PersonalEventStoreTests {
    private static let sample = PersonalEvent(
        id: "u1",
        title: "Entregar relatório do Lab 3",
        start: "2026-04-20",
        end: "2026-04-22",
        category: .study,
        discipline: PersonalEvent.DisciplineTag(id: "d1", code: "EXA412", name: "Física II"),
        reminder: .threeDays,
        notes: "Anexar os gráficos.",
        createdAt: Date(timeIntervalSince1970: 1_776_000_000)
    )

    @Test
    func entriesRoundTripThroughTheMirror() async throws {
        let store = MirrorStore(writer: try inMemoryDatabase())

        try await store.savePersonalEvent(Self.sample)
        #expect(try await store.personalEvents() == [Self.sample])

        var edited = Self.sample
        edited.title = "Entregar relatório final"
        edited.discipline = nil
        edited.reminder = .none
        try await store.savePersonalEvent(edited)
        #expect(try await store.personalEvents() == [edited])

        try await store.deletePersonalEvent(id: edited.id)
        #expect(try await store.personalEvents().isEmpty)
    }

    @Test
    func entriesAreOrderedByStartDay() async throws {
        let store = MirrorStore(writer: try inMemoryDatabase())
        let later = PersonalEvent(
            id: "u2",
            title: Self.sample.title,
            start: "2026-05-02",
            end: nil,
            category: .task,
            discipline: nil,
            reminder: .none,
            notes: "",
            createdAt: Self.sample.createdAt
        )

        try await store.savePersonalEvent(later)
        try await store.savePersonalEvent(Self.sample)
        #expect(try await store.personalEvents().map(\.id) == ["u1", "u2"])
    }

    @Test
    func logoutTakesTheStudentsEntriesWithIt() async throws {
        let store = MirrorStore(writer: try inMemoryDatabase())
        try await store.savePersonalEvent(Self.sample)

        try await store.wipe()
        #expect(try await store.personalEvents().isEmpty)
    }
}

struct PersonalEventReminderSchedulerTests {
    private static let calendar = Calendar.current

    private static func event(
        id: String,
        start: String,
        reminder: PersonalEvent.Reminder
    ) -> PersonalEvent {
        PersonalEvent(
            id: id,
            title: "Entrega",
            start: start,
            end: nil,
            category: .task,
            discipline: nil,
            reminder: reminder,
            notes: "",
            createdAt: .distantPast
        )
    }

    @Test
    func remindersLandAtTheFireHourTheChosenNumberOfDaysBefore() {
        let now = Self.calendar.date(from: DateComponents(year: 2026, month: 4, day: 17, hour: 9))!
        let reminders = PersonalEventReminderScheduler.desiredReminders(
            events: [
                Self.event(id: "a", start: "2026-04-20", reminder: .dayBefore),
                Self.event(id: "b", start: "2026-05-01", reminder: .week),
            ],
            now: now,
            calendar: Self.calendar
        )

        #expect(reminders.map(\.identifier) == ["personal-event/a", "personal-event/b"])
        #expect(reminders[0].fire.day == 19)
        #expect(reminders[0].fire.hour == PersonalEventReminderScheduler.fireHour)
        #expect(reminders[1].fire.month == 4)
        #expect(reminders[1].fire.day == 24)
    }

    @Test
    func entriesWithoutAReminderOrAlreadyPastAreSkipped() {
        let now = Self.calendar.date(from: DateComponents(year: 2026, month: 4, day: 17, hour: 9))!
        let reminders = PersonalEventReminderScheduler.desiredReminders(
            events: [
                Self.event(id: "none", start: "2026-04-20", reminder: .none),
                Self.event(id: "past", start: "2026-04-10", reminder: .dayBefore),
                // The evening slot on the 16th is already behind `now`.
                Self.event(id: "today", start: "2026-04-17", reminder: .dayBefore),
            ],
            now: now,
            calendar: Self.calendar
        )

        #expect(reminders.isEmpty)
    }
}
