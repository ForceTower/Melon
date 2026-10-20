import Foundation
import Testing

@testable import UNESKit

/// `contracts/v1/transit.json`, the synthetic `api/transit/*` payloads the
/// backend's serializers are checked against.
private struct TransitContract: Decodable {
    let network: TransitNetworkDTO
    let arrivals: TransitStopArrivalsDTO
    let follow: TransitFollowDTO
    let plan: TransitCommutePlanDTO

    static func load() throws -> TransitContract {
        var root = URL(fileURLWithPath: #filePath)
        for _ in 0..<6 { root.deleteLastPathComponent() }
        let data = try Data(contentsOf: root.appending(path: "contracts/v1/transit.json"))
        return try JSONDecoder().decode(TransitContract.self, from: data)
    }
}

struct TransitContractTests {
    @Test
    func theNetworkKeepsTravelOrderAndTheCampusStop() throws {
        let network = try TransitContract.load().network.domain()
        let inbound = try #require(network.route(line: "087", direction: .toCampus))
        #expect(inbound.stops.map(\.distanceMeters) == inbound.stops.map(\.distanceMeters).sorted())
        #expect(inbound.campusStopId == "5670")
        #expect(inbound.typicalMinutes == 35...40)
        #expect(network.boardingStops(direction: .toCampus).map(\.id) == ["4975", "9001"])
        #expect(network.campusDepartureStop?.id == "2452")
    }

    @Test
    func everyArrivalStateDecodes() throws {
        let board = try TransitContract.load().arrivals.domain()
        #expect(board.arrivals.map(\.state) == [.live, .waiting, .historical])
        #expect(board.arrivals[1].phase == .atOrigin)
        #expect(board.arrivals[2].isFlaky)
        #expect(board.today.map(\.status) == [.passed, .next, .upcoming])
        let basis = try #require(board.arrivals[0].basis)
        #expect(basis.counts.reduce(0, +) == 27)
    }

    @Test
    func aSavedPlanDecodesBothLegs() throws {
        let plan = try TransitContract.load().plan.domain
        #expect(plan.toCampus?.stopName == "Rua de Exemplo, 100")
        #expect(plan.toCampus?.lineNumbers == ["087"])
        #expect(plan.fromCampus?.stopId == "2452")
        #expect(plan.fromCampus?.walkMinutes == 3)
    }

    @Test
    func followListsTheLinesVehicles() throws {
        let follow = try TransitContract.load().follow.domain()
        #expect(follow.vehicles.map(\.id) == ["90001", "90002"])
        #expect(follow.vehicles[1].heading == nil)
        #expect(follow.arrival == nil)
    }
}
