#if os(iOS)
import SwiftUI
import Testing
import UIKit

@testable import UNESKit

/// The system clips a Live Activity that outgrows its space: the route's stop
/// names were cut off and the estimate squeezed.
@MainActor
struct CommuteActivityLayoutTests {
    /// Apple's cap for a Live Activity on the lock screen.
    private static let lockScreenHeight: CGFloat = 160
    /// What the expanded island leaves its bottom region under the badge row,
    /// measured on an iPhone Pro Max.
    private static let islandBottomHeight: CGFloat = 96

    @Test
    func theLockScreenBannerFitsTheSystemCap() throws {
        for snapshot in try Self.snapshots() {
            let height = Self.height(of: CommuteActivityBanner(snapshot: snapshot), width: 343)
            #expect(height <= Self.lockScreenHeight, "\(snapshot.state.status): \(height)pt")
        }
    }

    @Test
    func theExpandedIslandFitsItsBottomRegion() throws {
        for snapshot in try Self.snapshots() {
            let body = CommuteActivityBody(snapshot: snapshot, placement: .island)
                .padding(EdgeInsets(top: 2, leading: 6, bottom: 0, trailing: 6))
            let height = Self.height(of: body, width: 340)
            #expect(height <= Self.islandBottomHeight, "\(snapshot.state.status): \(height)pt")
        }
    }

    private static func height(of view: some View, width: CGFloat) -> CGFloat {
        UIHostingController(rootView: view)
            .sizeThatFits(in: CGSize(width: width, height: .greatestFiniteMagnitude))
            .height
    }

    private static func snapshots() throws -> [CommuteActivitySnapshot] {
        let now = Date(timeIntervalSince1970: 1_776_000_000)
        let network = TransitFixtures.network
        let route = try #require(network.route(line: "087", direction: .toCampus))
        let line = try #require(network.line("087"))
        let attributes = try #require(CommuteActivityAttributes(
            arrival: TransitFixtures.board(now: now).arrivals[0],
            route: route,
            lineName: line.name,
            stopId: TransitFixtures.myStopId
        ))
        let follow = TransitFixtures.follow(now: now)
        let live = CommuteActivityAttributes.ContentState(
            arrival: try #require(follow.arrival),
            progressMeters: follow.vehicles.first?.progressMeters
        )
        var signalLost = live
        signalLost.signalLost = true
        var waiting = live
        waiting.status = .waiting
        return [
            CommuteActivitySnapshot(attributes: attributes, state: live, isStale: false, now: now),
            CommuteActivitySnapshot(attributes: attributes, state: signalLost, isStale: false, now: now),
            CommuteActivitySnapshot(attributes: attributes, state: waiting, isStale: true, now: now),
        ]
    }
}
#endif
