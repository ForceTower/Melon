import ComposableArchitecture
import SwiftUI

/// The commute line inside the Home hero. Always drawn on the hero's dark mesh.
struct CommuteHeroLine: View {
    let store: StoreOf<CommuteHeroFeature>
    var schedule: CommuteDaySchedule
    /// Draws the hairline that separates it from the class above.
    var separated = true

    var body: some View {
        TimelineView(.periodic(from: .now, by: 1)) { context in
            content(now: context.date)
        }
        .padding(.top, separated ? 14 : 0)
        .overlay(alignment: .top) {
            if separated {
                Rectangle().fill(.white.opacity(0.16)).frame(height: 1)
            }
        }
        .padding(.top, separated ? 16 : 0)
        .task(id: schedule) {
            await store.send(.task(schedule)).finish()
        }
    }

    @ViewBuilder
    private func content(now: Date) -> some View {
        if store.plan.isEmpty {
            invitation
        } else {
            Button {
                store.send(.tapped)
            } label: {
                Group {
                    if let leg = store.leg, let direction = store.direction {
                        if let arrival = store.state.headline(now: now) {
                            switch direction {
                            case .toCampus: inbound(arrival, leg: leg, now: now)
                            case .fromCampus: goingHome(arrival, leg: leg, now: now)
                            }
                        } else if store.state.isOutdated(at: now) {
                            offline(leg: leg)
                        } else if let board = store.board {
                            quiet(board: board, leg: leg)
                        } else {
                            placeholder(leg: leg)
                        }
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
        }
    }

    // MARK: Variants

    private var invitation: some View {
        HStack(spacing: 12) {
            Image(systemName: "bus")
                .font(.system(size: 17, weight: .medium))
                .foregroundStyle(.white.opacity(0.75))
            VStack(alignment: .leading, spacing: 2) {
                Text(.commuteHeroInviteTitle)
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(.white)
                Text(.commuteHeroInviteBody)
                    .font(.system(size: 12.5))
                    .foregroundStyle(.white.opacity(0.72))
                    .fixedSize(horizontal: false, vertical: true)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            Button {
                store.send(.setupTapped)
            } label: {
                Text(.commuteHeroInviteAction)
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundStyle(.black)
                    .padding(.horizontal, 13)
                    .frame(height: 34)
                    .background(.white, in: Capsule())
            }
            .buttonStyle(.plain)
        }
    }

    private func inbound(_ arrival: TransitArrival, leg: CommuteLeg, now: Date) -> some View {
        let frozen = store.state.isOutdated(at: now)
        let estimate = CommuteFormat.estimate(for: arrival, now: now, frozen: frozen)
        let historical = arrival.state == .historical
        return HStack(alignment: .top, spacing: 12) {
            TransitLineBadge(number: arrival.lineNumber)
            VStack(alignment: .leading, spacing: 5) {
                TransitEstimateText(
                    estimate: estimate,
                    stale: arrival.signalLost || frozen,
                    size: arrival.state == .live ? 30 : 26,
                    onDark: true
                )
                small(historical
                    ? .localized(.commuteHeroUsuallyPasses(leg.stopName))
                    : .localized(.commuteHeroAtYourStop(leg.stopName)))
                if let tail = inboundTail(arrival, now: now) {
                    small(tail)
                }
                statusRow(arrival, now: now)
                    .padding(.top, 6)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
    }

    private func inboundTail(_ arrival: TransitArrival, now: Date) -> String? {
        if arrival.signalLost, let stop = arrival.lastSeenStopName {
            return .localized(.commuteLastSeenNear(stop))
        }
        switch arrival.state {
        case .live:
            return arrival.campusArrival.map { .localized(.commuteReachesCampus(CommuteFormat.clock($0))) }
        case .waiting:
            return arrival.originDeparture.map { .localized(.commuteUsuallyLeaves(CommuteFormat.clock($0))) }
        case .historical:
            return .localized(.commuteHeroNoBusYet)
        }
    }

    private func goingHome(_ arrival: TransitArrival, leg: CommuteLeg, now: Date) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(alignment: .center, spacing: 10) {
                TransitLineBadge(number: arrival.lineNumber)
                ViewThatFits(in: .horizontal) {
                    HStack(alignment: .firstTextBaseline, spacing: 8) {
                        goingHomeLabels(arrival)
                        departureEstimate(arrival)
                    }
                    VStack(alignment: .leading, spacing: 4) {
                        HStack(alignment: .firstTextBaseline, spacing: 8) { goingHomeLabels(arrival) }
                        departureEstimate(arrival)
                    }
                }
            }
            HStack(spacing: 8) {
                departureChip(arrival, now: now)
                small(CommuteFormat.departureStopName(leg))
                    .lineLimit(1)
            }
        }
    }

    @ViewBuilder
    private func goingHomeLabels(_ arrival: TransitArrival) -> some View {
        Text(.commuteHeroGoingHome)
            .font(.system(size: 14, weight: .semibold))
            .foregroundStyle(.white)
        small(.localized(.commuteHeroLeavesCampus(arrival.lineNumber)))
    }

    private func departureEstimate(_ arrival: TransitArrival) -> some View {
        TransitEstimateText(
            estimate: CommuteFormat.Estimate(
                text: CommuteFormat.clock(arrival.expected),
                isApproximate: arrival.state == .historical
            ),
            size: 24,
            onDark: true
        )
    }

    @ViewBuilder
    private func departureChip(_ arrival: TransitArrival, now: Date) -> some View {
        if store.state.isOutdated(at: now) {
            TransitChip(kind: .stale, text: .localized(.commuteStateOffline), onDark: true)
        } else {
            liveDepartureChip(arrival, now: now)
        }
    }

    @ViewBuilder
    private func liveDepartureChip(_ arrival: TransitArrival, now: Date) -> some View {
        switch arrival.phase {
        case .atStop, .atOrigin:
            TransitChip(kind: .live, text: .localized(.commuteBusAlreadyAtStop), onDark: true)
        case .inbound:
            if let originArrival = arrival.originArrival {
                TransitChip(
                    kind: .live,
                    text: .localized(.commuteArrivingAtCampusIn(CommuteFormat.minutesUntil(originArrival, now: now))),
                    onDark: true
                )
            } else {
                TransitChip(kind: TransitChip.Kind(arrival), onDark: true)
            }
        case .enRoute, nil:
            TransitChip(kind: arrival.isFlaky ? .flaky : TransitChip.Kind(arrival), onDark: true)
        }
    }

    private func quiet(board: TransitStopBoard, leg: CommuteLeg) -> some View {
        HStack(spacing: 12) {
            Image(systemName: board.serviceToday ? "clock" : "calendar")
                .font(.system(size: 16, weight: .medium))
                .foregroundStyle(.white.opacity(0.75))
            VStack(alignment: .leading, spacing: 2) {
                Text(board.serviceToday ? .commuteHeroNothingSoon : .commuteNoServiceTitle)
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(.white)
                if let next = board.nextService {
                    small(.localized(.commuteHeroNextService(
                        next.lineNumber,
                        HomeFormat.weekdayName(for: next.usual).lowercased(),
                        CommuteFormat.clock(next.usual)
                    )))
                } else {
                    small(stopName(leg))
                }
            }
        }
    }

    /// Nothing left to show from a board we can no longer refresh: say so
    /// rather than claim no bus is coming.
    private func offline(leg: CommuteLeg) -> some View {
        HStack(spacing: 12) {
            Image(systemName: "wifi.slash")
                .font(.system(size: 16, weight: .medium))
                .foregroundStyle(.white.opacity(0.75))
            VStack(alignment: .leading, spacing: 2) {
                Text(.commuteOfflineTitle)
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(.white)
                small(stopName(leg))
            }
        }
    }

    private func stopName(_ leg: CommuteLeg) -> String {
        store.direction == .fromCampus ? CommuteFormat.departureStopName(leg) : leg.stopName
    }

    private func placeholder(leg: CommuteLeg) -> some View {
        HStack(alignment: .top, spacing: 12) {
            TransitLineBadge(number: leg.lineNumbers.first ?? "")
                .redacted(reason: .placeholder)
            VStack(alignment: .leading, spacing: 5) {
                if store.loadFailed {
                    Text(.commuteHeroUnavailable)
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundStyle(.white)
                } else {
                    Text(verbatim: "8–10 min")
                        .font(.system(size: 26, weight: .bold))
                        .foregroundStyle(.white)
                        .redacted(reason: .placeholder)
                }
                small(.localized(.commuteHeroAtYourStop(leg.stopName)))
            }
        }
    }

    // MARK: Pieces

    private func statusRow(_ arrival: TransitArrival, now: Date) -> some View {
        let offline = store.state.isOutdated(at: now)
        return HStack(spacing: 10) {
            if offline {
                TransitChip(kind: .stale, text: .localized(.commuteStateOffline), onDark: true)
            } else {
                TransitChip(kind: TransitChip.Kind(arrival), onDark: true)
            }
            if arrival.isFlaky {
                TransitChip(kind: .flaky, onDark: true)
            }
            Spacer(minLength: 0)
            if !offline, arrival.state != .historical, let fix = arrival.lastFixAt {
                TransitFreshness(fixAt: fix, now: now, signalLost: arrival.signalLost, onDark: true, short: true)
            }
        }
    }

    private func small(_ text: String) -> some View {
        Text(verbatim: text)
            .font(.system(size: 12.5))
            .tracking(-0.06)
            .foregroundStyle(.white.opacity(0.72))
    }
}

/// The commute line on its own dark card, for days the hero has no class.
struct CommuteHeroCard: View {
    let store: StoreOf<CommuteHeroFeature>
    var schedule: CommuteDaySchedule

    var body: some View {
        CommuteHeroLine(store: store, schedule: schedule, separated: false)
            .padding(EdgeInsets(top: 18, leading: 20, bottom: 20, trailing: 20))
            .background {
                ZStack {
                    UNESColor.darkBg
                    MeshView(variant: .warm)
                    LinearGradient(
                        colors: [UNESColor.scrim.opacity(0.12), UNESColor.scrim.opacity(0.6)],
                        startPoint: .top,
                        endPoint: .bottom
                    )
                }
            }
            .environment(\.colorScheme, .dark)
            .clipShape(RoundedRectangle(cornerRadius: 30, style: .continuous))
            .shadow(color: Color(hex: 0x141020, opacity: 0.28), radius: 20, y: 18)
    }
}

@MainActor
private func heroPreview(plan: CommutePlan, schedule: CommuteDaySchedule) -> some View {
    let state = CommuteHeroFeature.State()
    state.$isEnabled.withLock { $0 = true }
    return CommuteHeroCard(
        store: Store(initialState: state) {
            CommuteHeroFeature()
        } withDependencies: {
            $0.commutePlanRepository = .preview(plan)
        },
        schedule: schedule
    )
    .padding(16)
}

#Preview("Ao vivo") { heroPreview(plan: TransitFixtures.plan, schedule: CommuteDaySchedule(firstStartMinute: 23 * 60)) }
#Preview("Voltando") { heroPreview(plan: TransitFixtures.plan, schedule: CommuteDaySchedule(firstStartMinute: 0)) }
#Preview("Convite") { heroPreview(plan: CommutePlan(), schedule: CommuteDaySchedule()) }
