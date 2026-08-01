import ComposableArchitecture
import Foundation
import Testing

@testable import UNESKit

@MainActor
struct CalendarFeatureTests {
    static nonisolated let calendar = Calendar.current

    static nonisolated func day(_ year: Int, _ month: Int, _ day: Int) -> Date {
        calendar.date(from: DateComponents(year: year, month: month, day: day))!
    }

    /// Mid-afternoon so the reducer's `startOfDay` normalization shows.
    static nonisolated let now = calendar.date(
        from: DateComponents(year: 2026, month: 4, day: 17, hour: 15, minute: 30)
    )!
    static nonisolated let today = day(2026, 4, 17)

    /// Earliest first, matching the live repository's sort contract.
    static nonisolated let upstream = [
        AcademicEvent(
            id: "e4",
            summary: "Feriado — Páscoa",
            start: "2026-04-03",
            end: "2026-04-05",
            fixed: true,
            closed: true,
            scope: .general,
            origin: .manual
        ),
        AcademicEvent(
            id: "e1",
            summary: "Período para trancamento de disciplinas — Estudante",
            start: "2026-04-13",
            end: "2026-04-20",
            fixed: false,
            closed: false,
            scope: .general,
            origin: .manual
        ),
        AcademicEvent(
            id: "e3",
            summary: "Feriado — Tiradentes",
            start: "2026-04-21",
            end: nil,
            fixed: true,
            closed: true,
            scope: .general,
            origin: .manual
        ),
        AcademicEvent(
            id: "e2",
            summary: "P1 — Cálculo Diferencial II",
            start: "2026-04-22",
            end: nil,
            fixed: false,
            closed: false,
            scope: .classScope,
            origin: .evaluation
        ),
    ]

    static nonisolated var mapped: [CalendarEvent] {
        upstream.compactMap { CalendarEvent($0) }
    }

    static nonisolated let mine = PersonalEvent(
        id: "u1",
        title: "Entregar relatório do Lab 3",
        start: "2026-04-19",
        end: nil,
        category: .task,
        discipline: PersonalEvent.DisciplineTag(id: "d1", code: "EXA412", name: "Física II"),
        reminder: .dayBefore,
        notes: "",
        createdAt: now
    )

    private func makeStore(
        events: @escaping @Sendable (Date) async throws -> [AcademicEvent] = { _ in upstream },
        personal: [PersonalEvent] = []
    ) -> TestStoreOf<CalendarFeature> {
        TestStore(initialState: CalendarFeature.State()) {
            CalendarFeature()
        } withDependencies: {
            $0.calendar = Self.calendar
            $0.date = .constant(Self.now)
            $0.uuid = .incrementing
            $0.eventsRepository.calendar = events
            $0.disciplinesRepository.cached = { _ in nil }
            $0.personalEventsRepository.observe = {
                AsyncStream { continuation in
                    continuation.yield(personal)
                    continuation.finish()
                }
            }
            $0.personalEventsRepository.reconcileReminders = {}
        }
    }

    /// `.task` fans out over four effects; only the feed's landing carries
    /// state the ordered assertions below depend on.
    private func startup(_ store: TestStoreOf<CalendarFeature>) async {
        store.exhaustivity = .off(showSkippedAssertions: false)
        await store.send(.task) {
            $0.today = Self.today
            $0.selectedDay = Self.today
        }
        await store.receive(\.eventsLoaded) {
            $0.events = Self.mapped
            $0.fetchedAt = Self.now
        }
        await store.finish()
        await store.skipReceivedActions(strict: false)
        store.exhaustivity = .on
    }

    @Test
    func taskAnchorsTodayAndLoadsTheFeed() async {
        let store = makeStore()
        await startup(store)

        #expect(store.state.hero?.id == "e1")
        #expect(store.state.agendaGroups.count == 1)
        #expect(store.state.agendaGroups.first?.events.map(\.id) == ["e1", "e3", "e2"])
    }

    @Test
    func personalEntriesJoinTheSameTimeline() async {
        let store = makeStore(personal: [Self.mine])
        await startup(store)

        #expect(store.state.personalEvents == [Self.mine])
        #expect(store.state.allEvents.map(\.id) == ["e4", "e1", "u1", "e3", "e2"])
        #expect(store.state.upcomingPersonalCount == 1)

        await store.send(.scopeSelected(.personal)) {
            $0.scopeFilter = .personal
        }
        #expect(store.state.filtered.map(\.id) == ["u1"])

        await store.send(.scopeSelected(.general)) {
            $0.scopeFilter = .general
        }
        #expect(store.state.filtered.contains { $0.id == "u1" } == false)
    }

    @Test
    func addOpensAnEmptyComposerSeededWithTheDay() async {
        let store = makeStore()
        await startup(store)

        let day = Self.day(2026, 4, 25)
        await store.send(.addTapped(day: day)) {
            $0.composer = CalendarPersonalEventFeature.State(
                day: day,
                disciplines: [],
                calendar: Self.calendar
            )
        }
        #expect(store.state.composer?.isNew == true)
        #expect(store.state.composer?.start == day)

        await store.send(.composer(.dismiss)) {
            $0.composer = nil
        }
    }

    @Test
    func editOpensTheComposerOnTheStoredEntry() async {
        let store = makeStore(personal: [Self.mine])
        await startup(store)

        await store.send(.editTapped(Self.mine)) {
            $0.composer = CalendarPersonalEventFeature.State(
                editing: Self.mine,
                disciplines: [],
                calendar: Self.calendar
            )
        }
        #expect(store.state.composer?.isNew == false)
        #expect(store.state.composer?.title == Self.mine.title)

        await store.send(.composer(.dismiss)) {
            $0.composer = nil
        }
    }

    @Test
    func editingFromTheDetailSheetWaitsForItToClose() async {
        let clock = TestClock()
        let store = makeStore(personal: [Self.mine])
        store.dependencies.continuousClock = clock
        await startup(store)

        let row = store.state.allEvents.first { $0.id == Self.mine.id }!
        await store.send(.eventTapped(row)) {
            $0.detail = row
        }
        await store.send(.editTapped(Self.mine)) {
            $0.detail = nil
        }
        await clock.advance(by: .milliseconds(320))
        await store.receive(\.editTapped) {
            $0.composer = CalendarPersonalEventFeature.State(
                editing: Self.mine,
                disciplines: [],
                calendar: Self.calendar
            )
        }
        await store.send(.composer(.dismiss)) {
            $0.composer = nil
        }
    }

    @Test
    func deleteConfirmsBeforeRemoving() async {
        let deleted = LockIsolated<[String]>([])
        let store = makeStore(personal: [Self.mine])
        store.dependencies.personalEventsRepository.delete = { id in
            deleted.withValue { $0.append(id) }
        }
        await startup(store)

        await store.send(.deleteTapped(Self.mine)) {
            $0.confirmDelete = .deletePersonalEvent(id: Self.mine.id, titled: Self.mine.title)
        }
        await store.send(.confirmDelete(.presented(.confirmed(Self.mine.id)))) {
            $0.confirmDelete = nil
        }
        #expect(deleted.value == [Self.mine.id])
    }

    @Test
    func failedFetchKeepsTheScreenAlive() async {
        let store = makeStore(events: { _ in throw APIError.emptyEnvelope })

        store.exhaustivity = .off(showSkippedAssertions: false)
        await store.send(.task) {
            $0.today = Self.today
            $0.selectedDay = Self.today
        }
        await store.receive(\.eventsFailed)
        await store.finish()
        await store.skipReceivedActions(strict: false)

        #expect(store.state.fetchedAt == nil)
    }

    @Test
    func filtersNarrowTheFeedAndTheHero() async {
        let store = makeStore()
        await startup(store)

        await store.send(.categorySelected(.holiday)) {
            $0.category = .holiday
        }
        // Holidays never headline while running — the next one does.
        #expect(store.state.hero?.id == "e3")
        #expect(store.state.filtered.map(\.id) == ["e4", "e3"])
        #expect(store.state.agendaGroups.first?.events.map(\.id) == ["e3"])

        await store.send(.categorySelected(.all)) {
            $0.category = .all
        }
        await store.send(.scopeSelected(.classScope)) {
            $0.scopeFilter = .classScope
        }
        #expect(store.state.filtered.map(\.id) == ["e2"])
        #expect(store.state.hero?.id == "e2")
    }

    @Test
    func daySelectionNormalizesToMidnight() async {
        let store = makeStore()
        await startup(store)

        let afternoon = Self.calendar.date(
            from: DateComponents(year: 2026, month: 4, day: 21, hour: 14)
        )!
        await store.send(.daySelected(afternoon)) {
            $0.selectedDay = Self.day(2026, 4, 21)
        }
        #expect(store.state.selectedDayEvents.map(\.id) == ["e3"])
    }

    @Test
    func viewModeTogglesAndPersists() async {
        let store = makeStore()

        await store.send(.viewModeToggled) {
            $0.$viewMode.withLock { $0 = .grid }
        }
        await store.send(.viewModeToggled) {
            $0.$viewMode.withLock { $0 = .agenda }
        }
    }

    @Test
    func tappingAnEventDrivesTheDetailSheet() async {
        let store = makeStore()
        let event = Self.mapped[1]

        await store.send(.eventTapped(event)) {
            $0.detail = event
        }
        await store.send(.detailDismissed) {
            $0.detail = nil
        }
    }
}
