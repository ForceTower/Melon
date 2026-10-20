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
    func aBoardTheDeviceCouldNotRefreshStopsCountingDown() {
        let arrival = Self.arrival(state: .live, phase: .enRoute, earliest: 8, latest: 10, expected: 9)
        let later = Self.now.addingTimeInterval(120)
        let frozen = CommuteFormat.estimate(for: arrival, now: later, frozen: true)
        #expect(frozen.text == CommuteFormat.clockRange(arrival.earliest, arrival.latest))
        #expect(frozen == CommuteFormat.estimate(for: arrival, now: later.addingTimeInterval(60), frozen: true))

        var board = CommuteBoardFeature.State(direction: .toCampus, plan: TransitFixtures.plan)
        #expect(!board.isOutdated(at: Self.now))
        board.boardReceivedAt = Self.now
        #expect(!board.isOutdated(at: Self.now.addingTimeInterval(CommuteFormat.outdatedAfter)))
        #expect(board.isOutdated(at: Self.now.addingTimeInterval(CommuteFormat.outdatedAfter + 1)))
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
    func aSaveAlwaysSendsBothDirections() throws {
        let input = TransitCommutePlanInputDTO(CommutePlan(toCampus: TransitFixtures.plan.toCampus))
        let json = try #require(
            try JSONSerialization.jsonObject(with: JSONEncoder().encode(input)) as? [String: Any]
        )
        #expect(json.keys.sorted() == ["fromCampus", "toCampus"])
        #expect(json["fromCampus"] is NSNull)
        let leg = try #require(json["toCampus"] as? [String: Any])
        #expect(leg.keys.sorted() == ["lineNumbers", "stopId", "walkMinutes"])
    }

    @Test
    func theMirrorKeepsWhatTheServerAnsweredUntilLogout() async throws {
        let mirror = MirrorStore(writer: try inMemoryDatabase())
        #expect(try await mirror.cachedCommutePlan() == CommutePlan())
        try await mirror.applyCommutePlan(TransitFixtures.plan)
        #expect(try await mirror.cachedCommutePlan() == TransitFixtures.plan)
        try await mirror.applyCommutePlan(CommutePlan(fromCampus: TransitFixtures.plan.fromCampus))
        #expect(try await mirror.cachedCommutePlan() == CommutePlan(fromCampus: TransitFixtures.plan.fromCampus))
        try await mirror.applyCommutePlan(TransitFixtures.plan)
        try await mirror.wipe()
        #expect(try await mirror.cachedCommutePlan() == CommutePlan())
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
        let store = TestStore(initialState: CommuteDirectionFeature.State(plan: CommutePlan())) {
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
    func savingTheLastLegGoesThroughTheServer() async {
        let draft = CommuteSetupDraft(directions: [.fromCampus], existing: CommutePlan())
        let saved = LockIsolated<[CommutePlan]>([])
        let store = TestStore(initialState: CommuteLinesFeature.State(draft: draft, direction: .fromCampus)) {
            CommuteLinesFeature()
        } withDependencies: {
            $0.transitRepository.network = { TransitFixtures.network }
            $0.commutePlanRepository.save = { plan in
                saved.withValue { $0.append(plan) }
                return plan
            }
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
        let plan = CommutePlan(fromCampus: leg)
        await store.send(.saveTapped) {
            $0.draft.plan.fromCampus = leg
            $0.isSaving = true
        }
        await store.receive(.saveFinished(plan)) {
            $0.isSaving = false
        }
        await store.receive(.delegate(.finished(plan, .fromCampus)))
        #expect(saved.value == [plan])
    }

    @Test
    func aFailedSaveKeepsTheStudentOnTheStep() async {
        let draft = CommuteSetupDraft(directions: [.toCampus], existing: CommutePlan())
        let stop = TransitFixtures.network.route(line: "087", direction: .toCampus)!.stops[4]
        var state = CommuteLinesFeature.State(draft: draft, direction: .toCampus, stop: stop)
        state.network = TransitFixtures.network
        state.selected = ["087"]
        let store = TestStore(initialState: state) {
            CommuteLinesFeature()
        } withDependencies: {
            $0.commutePlanRepository.save = { _ in throw URLError(.notConnectedToInternet) }
        }
        await store.send(.saveTapped) {
            $0.draft.plan.toCampus = CommuteLeg(stopId: stop.id, stopName: stop.name, stopArea: stop.area, lineNumbers: ["087"], walkMinutes: 5)
            $0.isSaving = true
        }
        await store.receive(.saveFinished(nil)) {
            $0.isSaving = false
            $0.saveFailed = true
        }
        await store.send(.saveFailureDismissed) {
            $0.saveFailed = false
        }
    }

    @Test
    func removingThePlanWaitsForTheServer() async {
        let store = TestStore(initialState: CommuteDirectionFeature.State(plan: TransitFixtures.plan)) {
            CommuteDirectionFeature()
        } withDependencies: {
            $0.commutePlanRepository.remove = { throw URLError(.timedOut) }
        }
        await store.send(.removeConfirmed) {
            $0.isRemoving = true
        }
        await store.receive(.removeFinished(succeeded: false)) {
            $0.isRemoving = false
            $0.removeFailed = true
        }
        store.dependencies.commutePlanRepository.remove = {}
        await store.send(.removeFailureDismissed) {
            $0.removeFailed = false
        }
        await store.send(.removeConfirmed) {
            $0.isRemoving = true
        }
        await store.receive(.removeFinished(succeeded: true)) {
            $0.isRemoving = false
        }
        await store.receive(.delegate(.removed))
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

        let saved = CommutePlan(toCampus: TransitFixtures.plan.toCampus)
        await store.send(.path(.element(id: store.state.path.ids.last!, action: .commuteLines(.delegate(.finished(saved, .toCampus))))))
        #expect(store.state.path.count == 1)
        guard case let .commuteBoard(board) = store.state.path.last else {
            Issue.record("expected the board")
            return
        }
        #expect(board.direction == .toCampus)
        #expect(board.plan == saved)
        #expect(store.state.commute.plan == saved)
    }

    @Test
    func savingFromSetupDropsTheHerosBoardForTheOldStop() async {
        var state = HomeFeature.State()
        state.commute.plan = TransitFixtures.plan
        state.commute.direction = .toCampus
        state.commute.board = TransitFixtures.board(now: Self.now)
        state.commute.boardReceivedAt = Self.now
        let draft = CommuteSetupDraft(directions: [.toCampus], existing: TransitFixtures.plan)
        state.path.append(.commuteLines(CommuteLinesFeature.State(draft: draft, direction: .toCampus)))
        let store = TestStore(initialState: state) {
            HomeFeature()
        }
        store.exhaustivity = .off
        let moved = CommutePlan(
            toCampus: CommuteLeg(stopId: "2011", stopName: "Ponto da Oi", stopArea: "Centro", lineNumbers: ["087"], walkMinutes: 8)
        )

        await store.send(.path(.element(id: store.state.path.ids.last!, action: .commuteLines(.delegate(.finished(moved, .toCampus))))))
        #expect(store.state.commute.plan == moved)
        #expect(store.state.commute.board == nil)
        #expect(store.state.commute.boardReceivedAt == nil)
    }

    @Test
    func removingThePlanReturnsToHoje() async {
        var state = HomeFeature.State()
        state.commute.plan = TransitFixtures.plan
        state.path.append(.commuteBoard(CommuteBoardFeature.State(direction: .toCampus, plan: TransitFixtures.plan)))
        state.path.append(.commuteDirection(CommuteDirectionFeature.State(plan: TransitFixtures.plan)))
        let store = TestStore(initialState: state) {
            HomeFeature()
        }
        store.exhaustivity = .off
        await store.send(.path(.element(id: store.state.path.ids.last!, action: .commuteDirection(.delegate(.removed)))))
        #expect(store.state.path.isEmpty)
        #expect(store.state.commute.plan.isEmpty)
    }

    // MARK: Board

    @Test
    func aBoardForTheOtherDirectionIsDropped() async {
        let store = TestStore(initialState: CommuteBoardFeature.State(direction: .toCampus, plan: TransitFixtures.plan)) {
            CommuteBoardFeature()
        }
        await store.send(.boardLoaded(TransitFixtures.board(now: Self.now, direction: .fromCampus)))
    }

    @Test
    func aPlanChangedElsewhereRestartsTheBoardOnTheNewStop() async {
        let clock = TestClock()
        let moved = CommutePlan(
            toCampus: CommuteLeg(stopId: "2011", stopName: "Ponto da Oi", stopArea: "Centro", lineNumbers: ["087"], walkMinutes: 8),
            fromCampus: TransitFixtures.plan.fromCampus
        )
        let requested = LockIsolated<[String]>([])
        let store = TestStore(initialState: CommuteBoardFeature.State(direction: .toCampus, plan: TransitFixtures.plan)) {
            CommuteBoardFeature()
        } withDependencies: {
            $0.continuousClock = clock
            $0.date = .constant(Self.now)
            $0.transitRepository.arrivals = { stopId, _, _ in
                requested.withValue { $0.append(stopId) }
                throw CancellationError()
            }
        }
        await store.send(.planLoaded(moved)) {
            $0.plan = moved
        }
        await store.finish()
        #expect(requested.value == ["2011"])
        await store.send(.planLoaded(moved))
    }

    @Test
    func followingABusCarriesTheSavedStopAndTheSnapshotsAge() async {
        var state = CommuteBoardFeature.State(direction: .toCampus, plan: TransitFixtures.plan)
        let receivedAt = Self.now.addingTimeInterval(-40)
        state.boardReceivedAt = receivedAt
        let store = TestStore(initialState: state) {
            CommuteBoardFeature()
        } withDependencies: {
            $0.date = .constant(Self.now)
        }
        let arrival = TransitFixtures.board(now: Self.now).arrivals[0]
        await store.send(.arrivalTapped(arrival))
        await store.receive(.delegate(.follow(arrival, .toCampus, stopId: TransitFixtures.myStopId, receivedAt: receivedAt)))
    }

    @Test
    func pickingADirectionLeavesPollingToTheScreensTask() async {
        var state = CommuteBoardFeature.State(direction: .toCampus, plan: TransitFixtures.plan)
        state.board = TransitFixtures.board(now: Self.now)
        state.boardReceivedAt = Self.now
        // Exhaustive store: an effect started here would fail the test.
        let store = TestStore(initialState: state) {
            CommuteBoardFeature()
        }
        await store.send(.directionPicked(.fromCampus)) {
            $0.direction = .fromCampus
            $0.board = nil
            $0.boardReceivedAt = nil
        }
    }

    @Test
    func aBoardForAStopNoLongerSavedIsIgnored() async {
        let moved = CommutePlan(
            toCampus: CommuteLeg(stopId: "2011", stopName: "Ponto da Oi", stopArea: "Centro", lineNumbers: ["087"], walkMinutes: 8)
        )
        let store = TestStore(initialState: CommuteBoardFeature.State(direction: .toCampus, plan: moved)) {
            CommuteBoardFeature()
        }
        await store.send(.boardLoaded(TransitFixtures.board(now: Self.now)))
    }

    @Test
    func editingStartsFromTheBoardsPlan() async {
        let store = TestStore(initialState: CommuteBoardFeature.State(direction: .toCampus, plan: TransitFixtures.plan)) {
            CommuteBoardFeature()
        }
        await store.send(.settingsTapped)
        await store.receive(.delegate(.editPlan(TransitFixtures.plan)))
    }

    // MARK: Follow

    @Test
    func followingAHistoricalDepartureTakesTheBusThatLaterCarriesIt() async {
        let clock = TestClock()
        let historical = TransitFixtures.board(now: Self.now).arrivals[2]
        let live = {
            var arrival = TransitFixtures.board(now: Self.now).arrivals[0]
            arrival.id = historical.id
            return arrival
        }()
        let answers = LockIsolated([TransitFollow(generatedAt: Self.now, feed: TransitFeed(status: .ok), vehicles: [], arrival: live)])
        let requests = LockIsolated<[String]>([])
        let fallbacks = LockIsolated<[String?]>([])
        let store = TestStore(initialState: CommuteFollowFeature.State(
            arrival: historical,
            direction: .toCampus,
            stopId: TransitFixtures.myStopId,
            receivedAt: Self.now
        )) {
            CommuteFollowFeature()
        } withDependencies: {
            $0.continuousClock = clock
            $0.date = .constant(Self.now)
            $0.transitRepository.network = { TransitFixtures.network }
            $0.transitRepository.follow = { _, _, _, arrivalId, vehicleId in
                requests.withValue { $0.append(arrivalId) }
                fallbacks.withValue { $0.append(vehicleId) }
                return answers.withValue { $0.count > 1 ? $0.removeFirst() : $0[0] }
            }
        }
        store.exhaustivity = .off

        let task = await store.send(.task)
        await store.receive(\.followLoaded) {
            $0.arrival = live
            $0.vehicleId = live.vehicleId
            $0.followReceivedAt = Self.now
        }
        #expect(requests.value == [historical.id])

        answers.setValue([TransitFollow(generatedAt: Self.now, feed: TransitFeed(status: .ok), vehicles: [], arrival: nil)])
        await clock.advance(by: CommuteFollowFeature.refreshInterval)
        await store.receive(\.followLoaded) {
            $0.arrival = nil
        }
        // Still the same departure, now with its bus as the fallback.
        #expect(requests.value == [historical.id, historical.id])
        #expect(fallbacks.value == [nil, live.vehicleId])
        await task.cancel()
    }

    @Test
    func aFollowThatNeverGetsAnAnswerAgesFromTheBoardSnapshot() async {
        let snapshotAt = Self.now.addingTimeInterval(-30)
        let clock = TestClock()
        let store = TestStore(initialState: CommuteFollowFeature.State(
            arrival: TransitFixtures.board(now: Self.now).arrivals[0],
            direction: .toCampus,
            stopId: TransitFixtures.myStopId,
            receivedAt: snapshotAt
        )) {
            CommuteFollowFeature()
        } withDependencies: {
            $0.continuousClock = clock
            $0.transitRepository.network = { TransitFixtures.network }
            $0.transitRepository.follow = { _, _, _, _, _ in throw URLError(.notConnectedToInternet) }
        }
        store.exhaustivity = .off

        #expect(!store.state.isOutdated(at: Self.now))
        let task = await store.send(.task)
        await store.receive(\.followFailed) {
            $0.loadFailed = true
        }
        #expect(store.state.isOutdated(at: snapshotAt.addingTimeInterval(CommuteFormat.outdatedAfter + 1)))
        await task.cancel()
    }

    // MARK: Plan sync

    @Test
    func aSaveNeitherWaitsForAnOlderRefreshNorLetsItLand() async throws {
        let database = try inMemoryDatabase()
        let older = TransitFixtures.plan
        let saved = CommutePlan(toCampus: TransitFixtures.plan.toCampus)
        let (gate, release) = AsyncStream.makeStream(of: Void.self)
        let (started, start) = AsyncStream.makeStream(of: Void.self)
        // The GET stays unanswered until released (or until it is cancelled).
        let client = APIClient(send: { request in
            guard request.method == "GET" else { return try Self.planEnvelope(saved) }
            start.yield()
            for await _ in gate { break }
            return try Self.planEnvelope(older)
        })

        try await withDependencies {
            $0.apiClient = client
            $0.database = database
        } operation: {
            let repository = CommutePlanRepository.liveValue
            let refresh = Task { try await repository.refresh() }
            for await _ in started { break }

            let done = LockIsolated(false)
            let save = Task {
                defer { done.setValue(true) }
                return try await repository.save(saved)
            }
            // Bounded, so an implementation that queues the save behind the GET
            // fails here instead of hanging the suite.
            for _ in 0..<500 where !done.value {
                try await Task.sleep(for: .milliseconds(10))
            }
            #expect(done.value, "the save waited for the older refresh")
            release.yield()
            #expect(try await save.value == saved)
            _ = try? await refresh.value
        }
        #expect(try await MirrorStore(writer: database).cachedCommutePlan() == saved)
    }

    @Test
    func aRefreshStopsWithItsCallerAndWritesNothing() async throws {
        let database = try inMemoryDatabase()
        let (started, start) = AsyncStream.makeStream(of: Void.self)
        let (gate, _) = AsyncStream.makeStream(of: Void.self)
        let client = APIClient(send: { _ in
            start.yield()
            for await _ in gate { break }
            return try Self.planEnvelope(TransitFixtures.plan)
        })

        try await withDependencies {
            $0.apiClient = client
            $0.database = database
        } operation: {
            let refresh = Task { try await CommutePlanRepository.liveValue.refresh() }
            for await _ in started { break }
            refresh.cancel()
            await #expect(throws: (any Error).self) { try await refresh.value }
        }
        #expect(try await MirrorStore(writer: database).cachedCommutePlan() == CommutePlan())
    }

    private nonisolated static func planEnvelope(_ plan: CommutePlan) throws -> Data {
        let leg = { (leg: CommuteLeg?) -> Any in
            guard let leg else { return NSNull() }
            return [
                "stopId": leg.stopId,
                "stopName": leg.stopName,
                "stopArea": leg.stopArea.map { $0 as Any } ?? NSNull(),
                "lineNumbers": leg.lineNumbers,
                "walkMinutes": leg.walkMinutes,
            ] as [String: Any]
        }
        return try JSONSerialization.data(withJSONObject: [
            "ok": true,
            "message": "",
            "data": ["toCampus": leg(plan.toCampus), "fromCampus": leg(plan.fromCampus), "updatedAt": NSNull()],
        ])
    }

    // MARK: Hero

    @Test
    func aNewStopInTheSameDirectionDropsTheOldStopsBoard() async {
        var state = CommuteHeroFeature.State()
        state.plan = TransitFixtures.plan
        state.direction = .toCampus
        state.board = TransitFixtures.board(now: Self.now)
        state.boardReceivedAt = Self.now
        let moved = CommutePlan(
            toCampus: CommuteLeg(stopId: "2011", stopName: "Ponto da Oi", stopArea: "Centro", lineNumbers: ["087"], walkMinutes: 8),
            fromCampus: TransitFixtures.plan.fromCampus
        )
        let store = TestStore(initialState: state) {
            CommuteHeroFeature()
        } withDependencies: {
            $0.date = .constant(Self.now)
            $0.continuousClock = TestClock()
            $0.transitRepository.arrivals = { _, _, _ in throw URLError(.timedOut) }
        }
        store.exhaustivity = .off

        await store.send(.planLoaded(moved)) {
            $0.plan = moved
            $0.board = nil
            $0.boardReceivedAt = nil
        }
        await store.send(.boardLoaded(TransitFixtures.board(now: Self.now)))
        #expect(store.state.board == nil)
        await store.skipInFlightEffects()
    }

    @Test
    func theHeroPollsTheLegForTheTimeOfDay() async {
        let clock = TestClock()
        let state = CommuteHeroFeature.State()
        state.$isEnabled.withLock { $0 = true }
        let board = TransitFixtures.board(now: Self.now)
        let refreshed = LockIsolated(false)
        let store = TestStore(initialState: state) {
            CommuteHeroFeature()
        } withDependencies: {
            $0.date = .constant(Self.now)
            $0.continuousClock = clock
            $0.commutePlanRepository.observe = { AsyncStream { $0.yield(TransitFixtures.plan) } }
            $0.commutePlanRepository.refresh = { refreshed.setValue(true) }
            $0.transitRepository.arrivals = { stopId, direction, lines in
                #expect(stopId == TransitFixtures.myStopId)
                #expect(direction == .toCampus)
                #expect(lines == ["087", "088"])
                return board
            }
        }

        let schedule = CommuteDaySchedule(firstStartMinute: 24 * 60 - 1)
        let task = await store.send(.task(schedule)) {
            $0.schedule = schedule
        }
        await store.receive(.planLoaded(TransitFixtures.plan)) {
            $0.plan = TransitFixtures.plan
        }
        await store.receive(.directionResolved(.toCampus)) {
            $0.direction = .toCampus
        }
        await store.receive(.boardLoaded(board)) {
            $0.board = board
            $0.boardReceivedAt = Self.now
        }
        #expect(refreshed.value)
        await clock.advance(by: CommuteHeroFeature.refreshInterval)
        await store.receive(.directionResolved(.toCampus))
        await store.receive(.boardLoaded(board))
        await task.cancel()
    }
}
