import Foundation

extension CommuteActivityAttributes {
    /// Nil when `stopId` is not on the route.
    init?(arrival: TransitArrival, route: TransitRoute, lineName: String, stopId: String) {
        guard let mine = route.index(of: stopId) else { return nil }
        let end: Int
        if mine > 0 {
            end = mine
        } else if route.direction == .toCampus {
            end = route.campusStopId.flatMap(route.index(of:)) ?? route.stops.count - 1
        } else {
            end = route.stops.count - 1
        }
        self.init(
            lineNumber: arrival.lineNumber,
            lineName: lineName,
            direction: route.direction,
            stopId: stopId,
            arrivalId: arrival.id,
            stops: route.stops[...end].map {
                Stop(name: transitShortName($0, in: route), distanceMeters: $0.distanceMeters)
            },
            mineIndex: mine
        )
    }
}
