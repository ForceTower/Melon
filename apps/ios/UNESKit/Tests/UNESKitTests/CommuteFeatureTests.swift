import ComposableArchitecture
import Foundation
import Testing

@testable import UNESKit

@MainActor
struct CommuteFeatureTests {
    static nonisolated let now = Date(timeIntervalSince1970: 1_776_000_000)

    private nonisolated static func arrival(
        state: TransitArrivalState,
        phase: TransitVehiclePhase? = nil,
        earliest: Double,
        latest: Double,
        expected: Double,
        signalLost: Bool = false
    ) -> TransitArrival {
        TransitArrival(
            id: "a",
            lineNumber: "087",
            state: state,
            phase: phase,
            earliest: now.addingTimeInterval(earliest * 60),
            latest: now.addingTimeInterval(latest * 60),
            expected: now.addingTimeInterval(expected * 60),
            signalLost: signalLost,
            missing: false
        )
    }

    // MARK: Estimate rules

    @Test
    func aLiveBusShowsAMinuteBand() {
        let estimate = CommuteFormat.estimate(for: Self.arrival(state: .live, earliest: 8.2, latest: 9.6, expected: 9), now: Self.now)
        #expect(estimate == CommuteFormat.Estimate(text: String.localized(.commuteEstimateMinuteRange(8, 10)), isApproximate: false))
    }

    @Test
    func aBusMoreThanHalfAnHourAwayOnlyGetsAnApproximateTime() {
        let arrival = Self.arrival(state: .live, earliest: 31, latest: 38, expected: 34)
        let estimate = CommuteFormat.estimate(for: arrival, now: Self.now)
        #expect(estimate.isApproximate)
        #expect(estimate.text == CommuteFormat.clock(arrival.expected))
    }

    @Test
    func historyIsAlwaysApproximate() {
        let arrival = Self.arrival(state: .historical, earliest: 3, latest: 6, expected: 4)
        #expect(CommuteFormat.estimate(for: arrival, now: Self.now).isApproximate)
    }

    @Test
    func aWaitingBusShowsAClockBand() {
        let arrival = Self.arrival(state: .waiting, earliest: 12, latest: 18, expected: 15)
        let estimate = CommuteFormat.estimate(for: arrival, now: Self.now)
        #expect(estimate.text == CommuteFormat.clockRange(arrival.earliest, arrival.latest))
        #expect(!estimate.isApproximate)
    }

    @Test
    func aLostSignalStopsCountingDown() {
        let arrival = Self.arrival(state: .live, earliest: 8, latest: 10, expected: 9, signalLost: true)
        let later = Self.now.addingTimeInterval(120)
        #expect(CommuteFormat.estimate(for: arrival, now: Self.now) == CommuteFormat.estimate(for: arrival, now: later))
    }

    @Test
    func aBusAtTheStopOrAboutToArriveSaysSo() {
        let atStop = Self.arrival(state: .live, phase: .atStop, earliest: 0, latest: 1, expected: 0)
        #expect(CommuteFormat.estimate(for: atStop, now: Self.now).text == String.localized(.commuteEstimateAtStop))
        let arriving = Self.arrival(state: .live, phase: .enRoute, earliest: 0, latest: 0.8, expected: 0.4)
        #expect(CommuteFormat.estimate(for: arriving, now: Self.now).text == String.localized(.commuteEstimateArriving))
    }

    @Test
    func flakinessNeedsEnoughObservedDays() {
        var arrival = Self.arrival(state: .historical, earliest: 3, latest: 6, expected: 4)
        arrival.ranDays = 1
        arrival.observedDays = 2
        #expect(!arrival.isFlaky)
        arrival.ranDays = 6
        arrival.observedDays = 10
        #expect(arrival.isFlaky)
        arrival.ranDays = 9
        #expect(!arrival.isFlaky)
    }

    // MARK: Which leg the hero shows

    @Test
    func theHeroShowsGettingInUntilTheFirstClassThenGoingHome() throws {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = try #require(TimeZone(identifier: "America/Bahia"))
        let at = { (hour: Int, minute: Int) in
            calendar.date(from: DateComponents(year: 2026, month: 10, day: 20, hour: hour, minute: minute))!
        }
        let schedule = CommuteDaySchedule(firstStartMinute: 7 * 60 + 30)
        #expect(schedule.direction(for: TransitFixtures.plan, at: at(6, 50), calendar: calendar) == .toCampus)
        #expect(schedule.direction(for: TransitFixtures.plan, at: at(7, 30), calendar: calendar) == .fromCampus)
        #expect(CommuteDaySchedule().direction(for: TransitFixtures.plan, at: at(11, 0), calendar: calendar) == .toCampus)
        #expect(CommuteDaySchedule().direction(for: TransitFixtures.plan, at: at(13, 0), calendar: calendar) == .fromCampus)

        let inboundOnly = CommutePlan(toCampus: TransitFixtures.plan.toCampus)
        #expect(schedule.direction(for: inboundOnly, at: at(20, 0), calendar: calendar) == .toCampus)
        #expect(schedule.direction(for: CommutePlan(), at: at(20, 0), calendar: calendar) == nil)
    }

    // MARK: Network

    @Test
    func boardingStopsEndBeforeTheCampusAndLeavingHasOneStop() {
        let network = TransitFixtures.network
        let inbound = network.boardingStops(direction: .toCampus)
        #expect(!inbound.contains { $0.id == "5670" || $0.id == "5788" })
        #expect(Set(inbound.map(\.id)).count == inbound.count)
        #expect(network.campusDepartureStop?.id == TransitFixtures.campusDepartureStopId)
        #expect(network.lines(serving: TransitFixtures.myStopId, direction: .toCampus) == ["087", "088"])
    }

    // MARK: Wire

    @Test
    func arrivalsDecodeFromTheWireContract() throws {
        let json = """
        {
          "generatedAt": "2026-10-20T09:50:00.123Z",
          "stopId": "3203",
          "direction": "toCampus",
          "feed": { "status": "ok", "lastUpdateAt": "2026-10-20T09:49:54.000Z" },
          "arrivals": [{
            "id": "087-20311",
            "lineNumber": "087",
            "state": "live",
            "phase": "enRoute",
            "vehicleId": "20311",
            "window": { "earliest": "2026-10-20T09:58:00.000Z", "latest": "2026-10-20T10:00:00.000Z" },
            "expected": "2026-10-20T09:59:00.000Z",
            "originDeparture": null,
            "originArrival": null,
            "campusArrival": "2026-10-20T10:12:00.000Z",
            "stopsAway": 4,
            "lastFixAt": "2026-10-20T09:49:52.000Z",
            "signalLost": false,
            "lastSeenStopName": null,
            "scheduledDeparture": "06:30",
            "reliability": { "ranDays": 9, "observedDays": 10 },
            "missing": false,
            "basis": {
              "sampleCount": 34,
              "dayCount": 7,
              "hitRate": 0.88,
              "histogram": { "startsAt": "2026-10-20T09:52:00.000Z", "binMinutes": 1, "counts": [1, 4, 9, 4, 1] }
            }
          }],
          "today": [{ "lineNumber": "087", "usual": "2026-10-20T09:58:00.000Z", "scheduledDeparture": "06:45", "status": "next", "flaky": false }],
          "serviceToday": true,
          "nextService": null
        }
        """
        let board = try JSONDecoder().decode(TransitStopArrivalsDTO.self, from: Data(json.utf8)).domain()
        let arrival = try #require(board.arrivals.first)
        #expect(board.direction == .toCampus)
        #expect(board.feed.status == .ok)
        #expect(arrival.state == .live)
        #expect(arrival.phase == .enRoute)
        #expect(arrival.latest.timeIntervalSince(arrival.earliest) == 120)
        #expect(arrival.ranDays == 9)
        #expect(arrival.basis?.counts == [1, 4, 9, 4, 1])
        #expect(board.today.first?.status == .next)
    }

    @Test
    func anUnknownPhaseDoesNotBreakTheBoard() throws {
        let json = """
        {
          "id": "x", "lineNumber": "088", "state": "waiting", "phase": "teleporting", "vehicleId": null,
          "window": { "earliest": "2026-10-20T10:00:00Z", "latest": "2026-10-20T10:05:00Z" },
          "expected": "2026-10-20T10:02:00Z", "originDeparture": null, "originArrival": null, "campusArrival": null,
          "stopsAway": null, "lastFixAt": null, "signalLost": false, "lastSeenStopName": null,
          "scheduledDeparture": null, "reliability": null, "missing": false, "basis": null
        }
        """
        let arrival = try JSONDecoder().decode(TransitArrivalDTO.self, from: Data(json.utf8)).domain()
        #expect(arrival.phase == nil)
        #expect(arrival.state == .waiting)
    }

    // MARK: Setup

    @Test
    func continuingKeepsTheDirectionsInTravelOrder() async {
        let store = TestStore(initialState: CommuteDirectionFeature.State()) {
            CommuteDirectionFeature()
        }
        await store.send(.toggled(.fromCampus)) {
            $0.selected = [.toCampus, .fromCampus]
        }
        await store.send(.toggled(.toCampus)) {
            $0.selected = [.fromCampus]
        }
        await store.send(.toggled(.fromCampus))
        await store.send(.toggled(.toCampus)) {
            $0.selected = [.toCampus, .fromCampus]
        }
        await store.send(.continueTapped)
        await store.receive(.delegate(.next(CommuteSetupDraft(directions: [.toCampus, .fromCampus], existing: CommutePlan()))))
    }

    @Test
    func savingTheLastLegWritesThePlan() async {
        let draft = CommuteSetupDraft(directions: [.fromCampus], existing: CommutePlan())
        let store = TestStore(initialState: CommuteLinesFeature.State(draft: draft, direction: .fromCampus)) {
            CommuteLinesFeature()
        } withDependencies: {
            $0.transitRepository.network = { TransitFixtures.network }
        }
        let departure = TransitFixtures.network.campusDepartureStop

        await store.send(.task)
        await store.receive(.networkLoaded(TransitFixtures.network)) {
            $0.network = TransitFixtures.network
            $0.stop = departure
            $0.selected = ["003", "087", "088"]
        }
        await store.send(.lineToggled("003")) {
            $0.selected = ["087", "088"]
        }
        await store.send(.walkChanged(99)) {
            $0.walkMinutes = 30
        }
        let leg = CommuteLeg(
            stopId: TransitFixtures.campusDepartureStopId,
            stopName: departure?.name ?? "",
            stopArea: departure?.area,
            lineNumbers: ["087", "088"],
            walkMinutes: 30
        )
        await store.send(.saveTapped) {
            $0.draft.plan.fromCampus = leg
            $0.$plan.withLock { $0 = CommutePlan(fromCampus: leg) }
        }
        await store.receive(.delegate(.finished(.fromCampus)))
    }

    @Test
    func aLegBeforeAnotherHandsTheDraftOn() async {
        let draft = CommuteSetupDraft(directions: [.toCampus, .fromCampus], existing: CommutePlan())
        let stop = TransitFixtures.network.route(line: "087", direction: .toCampus)!.stops[4]
        var state = CommuteLinesFeature.State(draft: draft, direction: .toCampus, stop: stop)
        state.network = TransitFixtures.network
        state.selected = ["087"]
        let store = TestStore(initialState: state) {
            CommuteLinesFeature()
        }
        var expected = draft
        expected.plan.toCampus = CommuteLeg(stopId: stop.id, stopName: stop.name, stopArea: stop.area, lineNumbers: ["087"], walkMinutes: 5)
        await store.send(.saveTapped) {
            $0.draft = expected
        }
        await store.receive(.delegate(.next(expected)))
    }

    // MARK: Home routing

    @Test
    func setupStepsPushOnTheHomeStackAndSavingOpensTheBoard() async {
        let store = TestStore(initialState: HomeFeature.State()) {
            HomeFeature()
        }
        store.exhaustivity = .off

        await store.send(.commute(.setupTapped))
        await store.receive(.commute(.delegate(.openSetup)))
        #expect(store.state.path.count == 1)
        guard case .commuteDirection = store.state.path.last else {
            Issue.record("expected the direction step")
            return
        }

        let draft = CommuteSetupDraft(directions: [.toCampus], existing: CommutePlan())
        await store.send(.path(.element(id: store.state.path.ids.last!, action: .commuteDirection(.delegate(.next(draft))))))
        guard case .commuteStopPicker = store.state.path.last else {
            Issue.record("expected the stop step")
            return
        }

        let stop = TransitFixtures.network.route(line: "087", direction: .toCampus)!.stops[4]
        await store.send(.path(.element(id: store.state.path.ids.last!, action: .commuteStopPicker(.delegate(.next(draft, stop))))))
        guard case let .commuteLines(lines) = store.state.path.last else {
            Issue.record("expected the lines step")
            return
        }
        #expect(lines.stop == stop)
        #expect(store.state.path.count == 3)

        await store.send(.path(.element(id: store.state.path.ids.last!, action: .commuteLines(.delegate(.finished(.toCampus))))))
        #expect(store.state.path.count == 1)
        guard case let .commuteBoard(board) = store.state.path.last else {
            Issue.record("expected the board")
            return
        }
        #expect(board.direction == .toCampus)
    }

    @Test
    func removingThePlanReturnsToHoje() async {
        var state = HomeFeature.State()
        state.path.append(.commuteBoard(CommuteBoardFeature.State(direction: .toCampus)))
        state.path.append(.commuteDirection(CommuteDirectionFeature.State()))
        let store = TestStore(initialState: state) {
            HomeFeature()
        }
        store.exhaustivity = .off
        await store.send(.path(.element(id: store.state.path.ids.last!, action: .commuteDirection(.delegate(.removed)))))
        #expect(store.state.path.isEmpty)
    }

    // MARK: Board

    @Test
    func aBoardForTheOtherDirectionIsDropped() async {
        let store = TestStore(initialState: CommuteBoardFeature.State(direction: .toCampus)) {
            CommuteBoardFeature()
        }
        await store.send(.boardLoaded(TransitFixtures.board(now: Self.now, direction: .fromCampus)))
    }

    @Test
    func followingABusCarriesTheSavedStop() async {
        let state = CommuteBoardFeature.State(direction: .toCampus)
        state.$plan.withLock { $0 = TransitFixtures.plan }
        let store = TestStore(initialState: state) {
            CommuteBoardFeature()
        }
        let arrival = TransitFixtures.board(now: Self.now).arrivals[0]
        await store.send(.arrivalTapped(arrival))
        await store.receive(.delegate(.follow(arrival, .toCampus, stopId: TransitFixtures.myStopId)))
    }

    @Test
    func theHeroPollsTheLegForTheTimeOfDay() async {
        let clock = TestClock()
        let state = CommuteHeroFeature.State()
        state.$isEnabled.withLock { $0 = true }
        state.$plan.withLock { $0 = TransitFixtures.plan }
        let board = TransitFixtures.board(now: Self.now)
        let store = TestStore(initialState: state) {
            CommuteHeroFeature()
        } withDependencies: {
            $0.date = .constant(Self.now)
            $0.continuousClock = clock
            $0.transitRepository.arrivals = { stopId, direction, lines in
                #expect(stopId == TransitFixtures.myStopId)
                #expect(direction == .toCampus)
                #expect(lines == ["087", "088"])
                return board
            }
        }

        let task = await store.send(.task(CommuteDaySchedule(firstStartMinute: 24 * 60 - 1)))
        await store.receive(.directionResolved(.toCampus)) {
            $0.direction = .toCampus
        }
        await store.receive(.boardLoaded(board)) {
            $0.board = board
        }
        await clock.advance(by: CommuteHeroFeature.refreshInterval)
        await store.receive(.directionResolved(.toCampus))
        await store.receive(.boardLoaded(board))
        await task.cancel()
    }
}
