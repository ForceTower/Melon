import ComposableArchitecture
import Foundation
import Testing

@testable import UNESKit

struct DisciplineTasksTests {
    static let calendar = Calendar.current

    static func day(_ year: Int, _ month: Int, _ day: Int) -> Date {
        calendar.date(from: DateComponents(year: year, month: month, day: day))!
    }

    static let today = day(2026, 4, 17)

    static func task(
        _ id: String,
        start: String,
        end: String? = nil,
        disciplineId: String? = "d3",
        createdAt: Date = .distantPast,
        completedAt: Date? = nil
    ) -> PersonalEvent {
        PersonalEvent(
            id: id,
            title: "Tarefa \(id)",
            start: start,
            end: end,
            category: .task,
            discipline: disciplineId.map { PersonalEvent.DisciplineTag(id: $0, code: "EXA412", name: "Física II") },
            reminder: .dayBefore,
            notes: "",
            createdAt: createdAt,
            completedAt: completedAt
        )
    }

    @Test
    func pendingTasksComeFirstAndCheckedOffOnesSinkInDueOrder() {
        let events = [
            Self.task("done-early", start: "2026-04-10", completedAt: Self.today),
            Self.task("late", start: "2026-04-25"),
            Self.task("other-class", start: "2026-04-11", disciplineId: "d1"),
            Self.task("untagged", start: "2026-04-11", disciplineId: nil),
            Self.task("done-late", start: "2026-04-30", completedAt: Self.today),
            Self.task("soon-b", start: "2026-04-18", createdAt: Self.day(2026, 4, 2)),
            Self.task("soon-a", start: "2026-04-18", createdAt: Self.day(2026, 4, 1)),
        ]

        let tasks = DisciplineTasks.tasks(for: "d3", in: events)

        #expect(tasks.map(\.id) == ["soon-a", "soon-b", "late", "done-early", "done-late"])
    }

    @Test
    func dueLabelsCountFromTheStartDay() {
        func due(_ start: String, end: String? = nil, completedAt: Date? = nil) -> DisciplineTaskDue {
            DisciplineTaskDue(
                Self.task("t", start: start, end: end, completedAt: completedAt),
                today: Self.today,
                calendar: Self.calendar
            )
        }

        #expect(due("2026-04-17") == .today)
        #expect(due("2026-04-18") == .tomorrow)
        #expect(due("2026-04-19") == .inDays(2))
        #expect(due("2026-04-24") == .inDays(7))
        #expect(due("2026-04-25") == .on(Self.day(2026, 4, 25)))
        #expect(due("2026-04-16") == .overdue(days: 1))
        #expect(due("2026-04-10") == .overdue(days: 7))
        #expect(due("2026-04-20", completedAt: Self.today) == .done)
        #expect(due("2026-04-10", completedAt: Self.today) == .done)
    }

    @Test
    func aRangeStillRunningIsDueTodayUntilItsLastDayPasses() {
        func due(_ start: String, end: String) -> DisciplineTaskDue {
            DisciplineTaskDue(Self.task("t", start: start, end: end), today: Self.today, calendar: Self.calendar)
        }

        #expect(due("2026-04-15", end: "2026-04-19") == .today)
        #expect(due("2026-04-15", end: "2026-04-17") == .today)
        #expect(due("2026-04-12", end: "2026-04-15") == .overdue(days: 2))
    }
}

@MainActor
struct DisciplineDetailTasksFeatureTests {
    static nonisolated let calendar = Calendar.current
    static nonisolated let now = calendar.date(
        from: DateComponents(year: 2026, month: 4, day: 17, hour: 15, minute: 30)
    )!
    static nonisolated let overview = DisciplinesOverview.preview(now: now)
    static nonisolated let summary = overview.current!.disciplines[2]

    static nonisolated let mine = DisciplineTasksTests.task("u1", start: "2026-04-20", disciplineId: "d3")
    static nonisolated let elsewhere = DisciplineTasksTests.task("u2", start: "2026-04-19", disciplineId: "d1")

    private func makeStore(
        semesterId: String = "sem-2026-1",
        personal: [PersonalEvent] = [],
        toggled: LockIsolated<[String: Bool]> = LockIsolated([:])
    ) -> TestStoreOf<DisciplineDetailFeature> {
        TestStore(
            initialState: DisciplineDetailFeature.State(summary: Self.summary, semesterId: semesterId)
        ) {
            DisciplineDetailFeature()
        } withDependencies: {
            $0.calendar = Self.calendar
            $0.date = .constant(Self.now)
            $0.disciplinesRepository.observeDetail = { _, _ in .finished }
            $0.disciplinesRepository.cached = { _ in Self.overview }
            $0.materialsRepository.discipline = { _ in throw APIError.emptyEnvelope }
            $0.personalEventsRepository.observe = {
                AsyncStream { continuation in
                    continuation.yield(personal)
                    continuation.finish()
                }
            }
            $0.personalEventsRepository.setCompleted = { id, completed in
                toggled.withValue { $0[id] = completed }
            }
        }
    }

    /// `.task` fans out over several effects; the assertions below read the
    /// state they settle into.
    private func startup(_ store: TestStoreOf<DisciplineDetailFeature>) async {
        store.exhaustivity = .off(showSkippedAssertions: false)
        await store.send(.task)
        await store.finish()
        await store.skipReceivedActions(strict: false)
        store.exhaustivity = .on
    }

    @Test
    func aCurrentDisciplineListsOnlyItsOwnEntries() async {
        let store = makeStore(personal: [Self.elsewhere, Self.mine])
        await startup(store)

        #expect(store.state.today == Self.calendar.startOfDay(for: Self.now))
        #expect(store.state.tasks == [Self.mine])
        #expect(store.state.isCurrentSemester)
        #expect(store.state.composerOptions == Self.overview.composerOptions)
        #expect(store.state.showsTasks)
    }

    @Test
    func aPastDisciplineShowsTasksOnlyWhenItHasSome() async {
        let empty = makeStore(semesterId: "sem-2025-2")
        await startup(empty)
        #expect(!empty.state.isCurrentSemester)
        #expect(!empty.state.showsTasks)

        let withTasks = makeStore(semesterId: "sem-2025-2", personal: [Self.mine])
        await startup(withTasks)
        #expect(withTasks.state.showsTasks)

        await withTasks.send(.addTaskTapped)
        #expect(withTasks.state.composer == nil)
    }

    @Test
    func togglingChecksOffAndReopens() async {
        let toggled = LockIsolated<[String: Bool]>([:])
        let store = makeStore(personal: [Self.mine], toggled: toggled)
        await startup(store)

        await store.send(.taskToggled(Self.mine))
        await store.finish()
        #expect(toggled.value == [Self.mine.id: true])

        var checkedOff = Self.mine
        checkedOff.completedAt = Self.now
        await store.send(.personalEventsUpdated([checkedOff])) {
            $0.tasks = [checkedOff]
        }
        await store.send(.taskToggled(checkedOff))
        await store.finish()
        #expect(toggled.value == [Self.mine.id: false])
    }

    @Test
    func addingOpensTheComposerTaggedWithThisDiscipline() async {
        let store = makeStore()
        await startup(store)
        let tag = Self.overview.composerOptions.first { $0.id == Self.summary.id }!.tag

        await store.send(.addTaskTapped) {
            $0.composer = CalendarPersonalEventFeature.State(
                day: Self.now,
                disciplines: Self.overview.composerOptions,
                discipline: tag,
                calendar: Self.calendar
            )
        }
        #expect(store.state.composer?.category == .task)
        #expect(store.state.composer?.discipline?.id == Self.summary.id)

        await store.send(.composer(.dismiss)) {
            $0.composer = nil
        }
    }

    @Test
    func tappingATaskOpensItForEditing() async {
        let store = makeStore(personal: [Self.mine])
        await startup(store)

        await store.send(.taskTapped(Self.mine)) {
            $0.composer = CalendarPersonalEventFeature.State(
                editing: Self.mine,
                disciplines: Self.overview.composerOptions,
                calendar: Self.calendar
            )
        }
    }
}
