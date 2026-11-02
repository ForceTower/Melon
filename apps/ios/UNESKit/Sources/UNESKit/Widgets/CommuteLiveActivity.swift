#if os(iOS)
import ActivityKit
import SwiftUI
import WidgetKit

/// "Seguindo o 087" outside the app: the lock-screen banner and the Dynamic
/// Island in its three sizes. Never seconds. A bus that lost its signal, or an
/// activity no push reached for a while, stops counting down and says so.
public struct CommuteLiveActivityWidget: Widget {
    public init() {}

    public var body: some WidgetConfiguration {
        ActivityConfiguration(for: CommuteActivityAttributes.self) { context in
            CommuteActivityBanner(snapshot: CommuteActivitySnapshot(context))
                .activityBackgroundTint(UNESColor.transitActivityBackground)
                .activitySystemActionForegroundColor(UNESColor.ink)
        } dynamicIsland: { context in
            let snapshot = CommuteActivitySnapshot(context)
            return DynamicIsland {
                DynamicIslandExpandedRegion(.leading) {
                    TransitLineBadge(number: snapshot.attributes.lineNumber)
                        .padding(.leading, 6)
                }
                DynamicIslandExpandedRegion(.trailing) {
                    CommuteActivityLineName(name: snapshot.attributes.lineName, onDark: true)
                        .padding(.trailing, 6)
                }
                DynamicIslandExpandedRegion(.bottom) {
                    CommuteActivityBody(snapshot: snapshot, onDark: true)
                        .padding(EdgeInsets(top: 2, leading: 6, bottom: 0, trailing: 6))
                }
            } compactLeading: {
                TransitLineBadge(number: snapshot.attributes.lineNumber, size: .small)
            } compactTrailing: {
                CommuteActivityCompact(snapshot: snapshot)
            } minimal: {
                CommuteActivityMinimal(snapshot: snapshot)
            }
            .keylineTint(UNESColor.transitLine(snapshot.attributes.lineNumber))
        }
    }
}

/// Everything the presentations read off one update.
struct CommuteActivitySnapshot {
    var attributes: CommuteActivityAttributes
    var state: CommuteActivityAttributes.ContentState
    /// No push reached the device for a while.
    var isStale: Bool
    var now: Date

    init(attributes: CommuteActivityAttributes, state: CommuteActivityAttributes.ContentState, isStale: Bool, now: Date) {
        self.attributes = attributes
        self.state = state
        self.isStale = isStale
        self.now = now
    }

    init(_ context: ActivityViewContext<CommuteActivityAttributes>) {
        self.init(attributes: context.attributes, state: context.state, isStale: context.isStale, now: .now)
    }

    var arrival: TransitArrival? { state.arrival(for: attributes) }

    /// The band holds still on the clock instead of counting down.
    var isFrozen: Bool { isStale || state.signalLost }

    var color: Color { UNESColor.transitLine(attributes.lineNumber) }
}

// MARK: - Lock screen

private struct CommuteActivityBanner: View {
    var snapshot: CommuteActivitySnapshot

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(spacing: 10) {
                TransitLineBadge(number: snapshot.attributes.lineNumber)
                Spacer(minLength: 0)
                CommuteActivityLineName(name: snapshot.attributes.lineName, onDark: false)
            }
            CommuteActivityBody(snapshot: snapshot, onDark: false)
        }
        .padding(EdgeInsets(top: 14, leading: 16, bottom: 12, trailing: 16))
    }
}

private struct CommuteActivityLineName: View {
    var name: String
    var onDark: Bool

    var body: some View {
        Text(verbatim: name)
            .font(.system(size: 13, weight: .semibold))
            .tracking(-0.13)
            .foregroundStyle(onDark ? Color.white : UNESColor.ink)
            .opacity(0.75)
            .lineLimit(1)
    }
}

/// The estimate to the stop (or the departure, boarding where the line
/// starts), the campus arrival beside it, and the route line underneath.
private struct CommuteActivityBody: View {
    var snapshot: CommuteActivitySnapshot
    var onDark: Bool

    private var ink: Color { onDark ? .white : UNESColor.ink }

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            if let arrival = snapshot.arrival {
                board(arrival)
            } else {
                Text(snapshot.state.status == .passed ? .commuteFollowPassed : .commuteFollowNoEstimate)
                    .font(.system(size: 17, weight: .semibold))
                    .tracking(-0.3)
                    .foregroundStyle(ink)
                    .lineLimit(2)
            }
            if snapshot.state.status != .ended {
                TransitRouteProgress(
                    stopNames: snapshot.attributes.stops.map(\.name),
                    busPosition: snapshot.attributes.busPosition(snapshot.state),
                    mineIndex: snapshot.attributes.mineIndex,
                    color: snapshot.color,
                    onDark: onDark
                )
            }
        }
    }

    private func board(_ arrival: TransitArrival) -> some View {
        HStack(alignment: .bottom, spacing: 12) {
            VStack(alignment: .leading, spacing: 5) {
                if snapshot.isFrozen {
                    frozenLabel
                } else {
                    caption(Text(leadingCaption))
                }
                TransitEstimateText(
                    estimate: CommuteFormat.estimate(for: arrival, now: snapshot.now, frozen: snapshot.isStale),
                    dim: arrival.state == .historical,
                    stale: snapshot.isFrozen,
                    size: 40,
                    onDark: onDark
                )
            }
            Spacer(minLength: 0)
            if let campus = arrival.campusArrival {
                VStack(alignment: .trailing, spacing: 5) {
                    caption(Text(.commuteActivityReachesCampus))
                    TransitEstimateText(
                        estimate: CommuteFormat.Estimate(text: CommuteFormat.clock(campus), isApproximate: true),
                        size: 24,
                        onDark: onDark
                    )
                    .fixedSize()
                }
            }
        }
    }

    private var leadingCaption: LocalizedStringResource {
        guard snapshot.attributes.boardsAtOrigin else { return .commuteActivityUntilStop }
        return snapshot.attributes.direction == .toCampus ? .commuteActivityLeavesTerminal : .commuteActivityLeavesCampus
    }

    private var frozenLabel: some View {
        HStack(spacing: 4) {
            Image(systemName: snapshot.isStale ? "wifi.slash" : "antenna.radiowaves.left.and.right.slash")
            Text(snapshot.isStale ? .commuteStateOffline : .commuteStateStale)
        }
        .font(.system(size: 11, weight: .semibold))
        .foregroundStyle(ink)
        .opacity(0.75)
    }

    private func caption(_ text: Text) -> some View {
        text
            .font(.system(size: 11, weight: .semibold))
            .tracking(0.22)
            .foregroundStyle(ink)
            .opacity(0.55)
            .lineLimit(1)
    }
}

// MARK: - Dynamic Island

private struct CommuteActivityCompact: View {
    var snapshot: CommuteActivitySnapshot

    var body: some View {
        if let arrival = snapshot.arrival {
            if snapshot.isFrozen {
                word(Text(snapshot.isStale ? .commuteStateOffline : .commuteStateStale))
            } else {
                TransitEstimateText(estimate: estimate(arrival), size: 16, onDark: true)
            }
        } else {
            word(Text(snapshot.state.status == .passed ? .commuteActivityPassedShort : .commuteActivityEndedShort))
        }
    }

    /// A waiting bus's clock band doesn't fit beside the camera; its most
    /// likely time does.
    private func estimate(_ arrival: TransitArrival) -> CommuteFormat.Estimate {
        guard arrival.state == .waiting else { return CommuteFormat.estimate(for: arrival, now: snapshot.now) }
        return CommuteFormat.Estimate(text: CommuteFormat.clock(arrival.expected), isApproximate: true)
    }

    private func word(_ text: Text) -> some View {
        text
            .font(.system(size: 12, weight: .semibold))
            .foregroundStyle(.white.opacity(0.75))
            .lineLimit(1)
    }
}

private struct CommuteActivityMinimal: View {
    var snapshot: CommuteActivitySnapshot

    var body: some View {
        if let arrival = snapshot.arrival,
           let band = CommuteFormat.minuteBand(for: arrival, now: snapshot.now, frozen: snapshot.isFrozen)
        {
            Text(verbatim: band.count == 1 ? "\(band.upperBound)" : "\(band.lowerBound)–\(band.upperBound)")
                .font(.system(size: 11, weight: .semibold))
                .tracking(-0.33)
                .monospacedDigit()
                .foregroundStyle(.white)
                .minimumScaleFactor(0.8)
                .accessibilityLabel(Text(verbatim: CommuteFormat.estimate(for: arrival, now: snapshot.now).text))
        } else if snapshot.arrival != nil, snapshot.isFrozen {
            Image(systemName: "antenna.radiowaves.left.and.right.slash")
                .font(.system(size: 11, weight: .semibold))
                .foregroundStyle(.white.opacity(0.75))
        } else {
            Image(systemName: "bus.fill")
                .font(.system(size: 12, weight: .semibold))
                .foregroundStyle(snapshot.color)
        }
    }
}

// MARK: - Previews

private extension CommuteActivityAttributes {
    static var preview: CommuteActivityAttributes {
        let network = TransitFixtures.network
        return CommuteActivityAttributes(
            arrival: TransitFixtures.board(now: .now).arrivals[0],
            route: network.route(line: "087", direction: .toCampus)!,
            lineName: network.line("087")!.name,
            stopId: TransitFixtures.myStopId
        )!
    }
}

private extension CommuteActivityAttributes.ContentState {
    static var live: Self {
        let follow = TransitFixtures.follow(now: .now)
        return Self(arrival: follow.arrival!, progressMeters: follow.vehicles[0].progressMeters)
    }

    static var signalLost: Self {
        var state = live
        state.signalLost = true
        return state
    }

    static var passed: Self {
        Self(status: .passed, atStop: false, progressMeters: 99_999, signalLost: false)
    }
}

#Preview("Tela bloqueada", as: .content, using: CommuteActivityAttributes.preview) {
    CommuteLiveActivityWidget()
} contentStates: {
    CommuteActivityAttributes.ContentState.live
    CommuteActivityAttributes.ContentState.signalLost
    CommuteActivityAttributes.ContentState.passed
}

#Preview("Ilha expandida", as: .dynamicIsland(.expanded), using: CommuteActivityAttributes.preview) {
    CommuteLiveActivityWidget()
} contentStates: {
    CommuteActivityAttributes.ContentState.live
}

#Preview("Ilha compacta", as: .dynamicIsland(.compact), using: CommuteActivityAttributes.preview) {
    CommuteLiveActivityWidget()
} contentStates: {
    CommuteActivityAttributes.ContentState.live
    CommuteActivityAttributes.ContentState.signalLost
}

#Preview("Ilha mínima", as: .dynamicIsland(.minimal), using: CommuteActivityAttributes.preview) {
    CommuteLiveActivityWidget()
} contentStates: {
    CommuteActivityAttributes.ContentState.live
}
#endif
