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

    private func makeStore(
        events: @escaping @Sendable (Date) async throws -> [AcademicEvent] = { _ in upstream }
    ) -> TestStoreOf<CalendarFeature> {
        TestStore(initialState: CalendarFeature.State()) {
            CalendarFeature()
        } withDependencies: {
            $0.calendar = Self.calendar
            $0.date = .constant(Self.now)
            $0.eventsRepository.calendar = events
        }
    }

    @Test
    func taskAnchorsTodayAndLoadsTheFeed() async {
        let store = makeStore()

        await store.send(.task) {
            $0.today = Self.today
            $0.selectedDay = Self.today
        }
        await store.receive(.eventsLoaded(Self.mapped)) {
            $0.events = Self.mapped
            $0.fetchedAt = Self.now
        }

        #expect(store.state.hero?.id == "e1")
        #expect(store.state.agendaGroups.count == 1)
        #expect(store.state.agendaGroups.first?.events.map(\.id) == ["e1", "e3", "e2"])
    }

    @Test
    func failedFetchKeepsTheScreenAlive() async {
        let store = makeStore(events: { _ in throw APIError.emptyEnvelope })

        await store.send(.task) {
            $0.today = Self.today
            $0.selectedDay = Self.today
        }
        await store.receive(.eventsFailed)

        #expect(store.state.fetchedAt == nil)
    }

    @Test
    func filtersNarrowTheFeedAndTheHero() async {
        let store = makeStore()
        await store.send(.task) {
            $0.today = Self.today
            $0.selectedDay = Self.today
        }
        await store.receive(.eventsLoaded(Self.mapped)) {
            $0.events = Self.mapped
            $0.fetchedAt = Self.now
        }

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
        await store.send(.task) {
            $0.today = Self.today
            $0.selectedDay = Self.today
        }
        await store.receive(.eventsLoaded(Self.mapped)) {
            $0.events = Self.mapped
            $0.fetchedAt = Self.now
        }

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
