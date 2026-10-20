import ComposableArchitecture
import SwiftUI

struct CommuteBoardView: View {
    @Bindable var store: StoreOf<CommuteBoardFeature>

    var body: some View {
        TimelineView(.periodic(from: .now, by: 1)) { context in
            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    if store.plan.directions.count > 1 {
                        directionPicker
                            .padding(.bottom, 18)
                    }
                    if let leg = store.leg {
                        header(leg)
                        content(leg: leg, now: context.date)
                    } else {
                        CommuteIntro { store.send(.startTapped) }
                    }
                }
                .padding(EdgeInsets(top: 8, leading: 16, bottom: 24, trailing: 16))
            }
            .scrollIndicators(.hidden)
        }
        .background(UNESColor.surface.ignoresSafeArea())
        .navigationTitle(Text(.commuteTitle))
        .inlineNavigationBar()
        .toolbar {
            if store.leg != nil {
                ToolbarItem(placement: .trailingCompat) {
                    Button {
                        store.send(.settingsTapped)
                    } label: {
                        Image(systemName: "gearshape")
                    }
                    .accessibilityLabel(Text(.commuteSettings))
                }
            }
        }
        .sheet(isPresented: Binding(
            get: { store.isExplainerPresented },
            set: { if !$0 { store.send(.explainerDismissed) } }
        )) {
            CommuteHowSheet(arrival: store.explained, direction: store.direction)
        }
        .task(id: store.direction) { await store.send(.task).finish() }
    }

    private var directionPicker: some View {
        Picker(selection: $store.direction.sending(\.directionPicked)) {
            Text(.commuteDirectionToCampus).tag(TransitDirection.toCampus)
            Text(.commuteDirectionFromCampus).tag(TransitDirection.fromCampus)
        } label: {
            Text(.commuteDirectionPicker)
        }
        .segmentedPickerCompat()
        .labelsHidden()
    }

    // MARK: Header

    private func header(_ leg: CommuteLeg) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            Label {
                Text(store.direction == .toCampus ? .commuteBoardEyebrowToCampus : .commuteDirectionFromCampus)
            } icon: {
                Image(systemName: store.direction == .toCampus ? "mappin" : "figure.walk.departure")
            }
            .font(.system(size: 12.5, weight: .semibold))
            .foregroundStyle(UNESColor.ink3)
            Text(verbatim: headerTitle(leg))
                .font(.system(size: 28, weight: .bold))
                .tracking(-0.98)
                .foregroundStyle(UNESColor.ink)
                .padding(.top, 6)
                .fixedSize(horizontal: false, vertical: true)
            Text(verbatim: headerSubtitle(leg))
                .font(.system(size: 13.5))
                .foregroundStyle(UNESColor.ink3)
                .padding(.top, 4)
        }
        .padding(EdgeInsets(top: 6, leading: 4, bottom: 16, trailing: 4))
    }

    private func headerTitle(_ leg: CommuteLeg) -> String {
        store.direction == .fromCampus ? CommuteFormat.departureStopName(leg) : leg.stopName
    }

    private func headerSubtitle(_ leg: CommuteLeg) -> String {
        if store.direction == .fromCampus { return .localized(.commuteDepartureStopSubtitle) }
        let parts = [
            leg.stopArea,
            String.localized(.commuteWalkMinutes(leg.walkMinutes)),
            String.localized(.commuteLinesList(CommuteFormat.lineList(leg.lineNumbers))),
        ]
        return parts.compactMap(\.self).joined(separator: " · ")
    }

    // MARK: Content

    @ViewBuilder
    private func content(leg: CommuteLeg, now: Date) -> some View {
        if let board = store.board {
            if store.loadFailed || store.state.isOutdated(at: now), let receivedAt = store.boardReceivedAt {
                offlineNotice(since: receivedAt)
                    .padding(.bottom, 14)
            } else if board.feed.status != .ok {
                feedNotice(board.feed)
                    .padding(.bottom, 14)
            }
            if !board.serviceToday {
                CommuteNoServiceCard(board: board, leg: leg, network: store.network)
            } else {
                arrivalsSection(board, now: now)
                if store.direction == .toCampus {
                    todaySection(board, now: now)
                } else if let flaky = board.arrivals.first(where: \.isFlaky) {
                    flakyNotice(flaky)
                }
            }
            if store.direction == .toCampus {
                Text(.commuteBoardFootnote)
                    .font(.system(size: 12))
                    .foregroundStyle(UNESColor.ink4)
                    .padding(EdgeInsets(top: 4, leading: 4, bottom: 0, trailing: 4))
            }
        } else if store.loadFailed {
            UNESBanner(tone: .neutral, title: .localized(.commuteUnavailableTitle), symbol: "icloud.slash") {
                Text(.commuteUnavailableBodyNoData)
            }
        } else {
            ProgressView()
                .frame(maxWidth: .infinity)
                .padding(.top, 40)
        }
    }

    private func arrivalsSection(_ board: TransitStopBoard, now: Date) -> some View {
        let frozen = store.state.isOutdated(at: now)
        let degraded = board.feed.status != .ok || frozen
        // Without fresh data, a bus whose window already closed can't be vouched for.
        let arrivals = frozen ? board.arrivals.filter { $0.latest >= now } : board.arrivals
        return VStack(alignment: .leading, spacing: 0) {
            CommuteSectionHeader(
                title: .localized(store.direction == .toCampus ? .commuteNextTitle : .commuteNextDeparturesTitle),
                hint: sectionHint(board, degraded: degraded),
                action: .localized(.commuteHowTitle),
                onAction: { store.send(.explainTapped(nil)) }
            )
            if arrivals.isEmpty {
                Text(.commuteNothingSoon)
                    .font(.system(size: 13.5))
                    .foregroundStyle(UNESColor.ink4)
                    .padding(.horizontal, 4)
            } else {
                VStack(spacing: 0) {
                    ForEach(arrivals) { arrival in
                        Button {
                            store.send(.arrivalTapped(arrival))
                        } label: {
                            CommuteArrivalRow(arrival: arrival, direction: store.direction, now: now, frozen: frozen)
                        }
                        .buttonStyle(CommuteRowButtonStyle())
                        if arrival.id != arrivals.last?.id {
                            Divider().overlay(UNESColor.line)
                        }
                    }
                }
                .transitCard()
                .opacity(degraded ? 0.6 : 1)
            }
        }
        .padding(.bottom, 18)
    }

    private func sectionHint(_ board: TransitStopBoard, degraded: Bool) -> String {
        let lastInfo = board.feed.status != .ok ? board.feed.lastUpdateAt : store.boardReceivedAt
        if degraded, let lastInfo {
            return .localized(.commuteLastInfoAt(CommuteFormat.clock(lastInfo)))
        }
        return .localized(store.direction == .toCampus ? .commuteNextHint : .commuteNextDeparturesHint)
    }

    private func todaySection(_ board: TransitStopBoard, now: Date) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            CommuteSectionHeader(title: .localized(.commuteTodayTitle), hint: .localized(.commuteTodayHint))
            if board.today.isEmpty {
                Text(.commuteTodayEmpty)
                    .font(.system(size: 13.5))
                    .foregroundStyle(UNESColor.ink4)
                    .padding(.horizontal, 4)
            } else {
                CommuteTodayStrip(slots: CommuteTodayStrip.window(board.today))
                Text(.commuteTodayFootnote)
                    .font(.system(size: 11.5))
                    .foregroundStyle(UNESColor.ink4)
                    .padding(EdgeInsets(top: 4, leading: 4, bottom: 0, trailing: 4))
            }
        }
        .padding(.bottom, 18)
    }

    private func offlineNotice(since receivedAt: Date) -> some View {
        UNESBanner(tone: .neutral, title: .localized(.commuteOfflineTitle), symbol: "wifi.slash") {
            Text(.commuteOfflineBody(CommuteFormat.clock(receivedAt)))
        }
    }

    private func feedNotice(_ feed: TransitFeed) -> some View {
        UNESBanner(tone: .neutral, title: .localized(.commuteUnavailableTitle), symbol: "icloud.slash") {
            if let last = feed.lastUpdateAt {
                Text(.commuteUnavailableBody(CommuteFormat.clock(last)))
            } else {
                Text(.commuteUnavailableBodyNoData)
            }
        }
    }

    private func flakyNotice(_ arrival: TransitArrival) -> some View {
        let time = arrival.scheduledDeparture.map(clockLabel) ?? CommuteFormat.clock(arrival.expected)
        let ran = arrival.ranDays ?? 0, observed = arrival.observedDays ?? 0
        let evening = Calendar.current.component(.hour, from: arrival.expected) >= 18
        return UNESBanner(tone: .neutral, title: .localized(.commuteStateFlaky), symbol: "circle.dashed") {
            Text(evening
                ? .commuteFlakyBodyNights(arrival.lineNumber, time, ran, observed)
                : .commuteFlakyBody(arrival.lineNumber, time, ran, observed))
        }
    }

    private func clockLabel(_ hhmm: String) -> String {
        hhmm.replacingOccurrences(of: ":", with: String.localized(.commuteClockSeparator))
    }
}

// MARK: - Rows

struct CommuteRowButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .background(configuration.isPressed ? UNESColor.surface2 : .clear)
            .contentShape(Rectangle())
    }
}

struct CommuteArrivalRow: View {
    var arrival: TransitArrival
    var direction: TransitDirection
    var now: Date
    /// The board could not be refreshed: no countdown, no live claims.
    var frozen = false

    var body: some View {
        let toCampus = direction == .toCampus
        HStack(alignment: toCampus ? .top : .center, spacing: 12) {
            TransitLineBadge(number: arrival.lineNumber)
                .padding(.top, toCampus ? 3 : 0)
            VStack(alignment: .leading, spacing: 0) {
                if toCampus {
                    inbound
                } else {
                    departure
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            Image(systemName: "chevron.right")
                .font(.system(size: 13, weight: .semibold))
                .foregroundStyle(UNESColor.ink4)
                .padding(.top, toCampus ? 8 : 0)
        }
        .padding(EdgeInsets(top: toCampus ? 14 : 13, leading: 16, bottom: toCampus ? 14 : 13, trailing: 14))
        .accessibilityElement(children: .combine)
    }

    @ViewBuilder
    private var inbound: some View {
        if arrival.missing {
            Text(.commuteMissingTitle(arrival.lineNumber, scheduledLabel))
                .font(.system(size: 15.5, weight: .semibold))
                .foregroundStyle(UNESColor.ink)
            Text(.commuteMissingBody(CommuteFormat.clock(arrival.expected)))
                .font(.system(size: 12.5))
                .foregroundStyle(UNESColor.ink3)
                .padding(.top, 4)
                .fixedSize(horizontal: false, vertical: true)
        } else {
            HStack(alignment: .firstTextBaseline, spacing: 8) {
                TransitEstimateText(
                    estimate: CommuteFormat.estimate(for: arrival, now: now, frozen: frozen),
                    dim: arrival.state == .historical || arrival.signalLost || frozen,
                    stale: arrival.signalLost || frozen,
                    size: arrival.state == .live ? 30 : 26
                )
                if arrival.state == .live, !arrival.signalLost, !frozen, let stops = arrival.stopsAway, stops > 0 {
                    Text(stops == 1 ? .commuteNextStop : .commuteStopsAway(stops))
                        .font(.system(size: 12.5, weight: .medium))
                        .monospacedDigit()
                        .foregroundStyle(UNESColor.ink3)
                }
            }
            if let detail = inboundDetail {
                Text(verbatim: detail)
                    .font(.system(size: 12.5))
                    .foregroundStyle(arrival.state == .historical ? UNESColor.ink4 : UNESColor.ink3)
                    .padding(.top, 3)
            }
        }
        chips
            .padding(.top, 9)
    }

    private var inboundDetail: String? {
        if arrival.signalLost, let stop = arrival.lastSeenStopName, let fix = arrival.lastFixAt {
            return .localized(.commuteLastSeenNearAt(stop, CommuteFormat.clock(fix)))
        }
        switch arrival.state {
        case .live:
            return arrival.campusArrival.map { .localized(.commuteReachesCampus(CommuteFormat.clock($0))) }
        case .waiting:
            return arrival.originDeparture.map { .localized(.commuteUsuallyLeavesTerminal(CommuteFormat.clock($0))) }
        case .historical:
            return .localized(.commuteUsuallyPassesAround(CommuteFormat.clock(arrival.expected)))
        }
    }

    @ViewBuilder
    private var departure: some View {
        TransitEstimateText(
            estimate: CommuteFormat.Estimate(text: CommuteFormat.clock(arrival.expected), isApproximate: false),
            dim: arrival.state == .historical || frozen,
            size: 24
        )
        FlowLayout(spacing: 6, lineSpacing: 6) {
            departureChip
            if !frozen, arrival.state != .historical, let fix = arrival.lastFixAt {
                TransitFreshness(fixAt: fix, now: now, signalLost: arrival.signalLost, short: true)
            }
        }
        .padding(.top, 7)
    }

    @ViewBuilder
    private var departureChip: some View {
        if frozen {
            TransitChip(kind: .stale, text: .localized(.commuteStateOffline), small: true)
        } else {
            liveDepartureChip
        }
    }

    @ViewBuilder
    private var liveDepartureChip: some View {
        switch arrival.phase {
        case .atStop, .atOrigin:
            TransitChip(kind: .live, text: .localized(.commuteBusAtStop), small: true)
        case .inbound:
            if let originArrival = arrival.originArrival {
                TransitChip(
                    kind: .live,
                    text: .localized(.commuteArrivingFromTerminalIn(CommuteFormat.minutesUntil(originArrival, now: now))),
                    small: true
                )
            } else {
                TransitChip(kind: TransitChip.Kind(arrival), small: true)
            }
        case .enRoute, nil:
            TransitChip(kind: arrival.isFlaky ? .flaky : TransitChip.Kind(arrival), small: true)
        }
    }

    private var chips: some View {
        FlowLayout(spacing: 6, lineSpacing: 6) {
            if frozen {
                TransitChip(kind: .stale, text: .localized(.commuteStateOffline), small: true)
            } else {
                TransitChip(kind: arrival.missing ? .historical : TransitChip.Kind(arrival), small: true)
            }
            if arrival.isFlaky {
                TransitChip(kind: .flaky, small: true)
            }
            if !frozen, arrival.state != .historical, let fix = arrival.lastFixAt {
                TransitFreshness(fixAt: fix, now: now, signalLost: arrival.signalLost)
            }
        }
    }

    private var scheduledLabel: String {
        guard let scheduled = arrival.scheduledDeparture else { return CommuteFormat.clock(arrival.expected) }
        return scheduled.replacingOccurrences(of: ":", with: String.localized(.commuteClockSeparator))
    }
}

// MARK: - Hoje

struct CommuteTodayStrip: View {
    var slots: [TransitTodaySlot]

    /// The server lists the whole day; a couple of passed slots give the
    /// next one its context.
    static func window(_ slots: [TransitTodaySlot]) -> [TransitTodaySlot] {
        let pivot = slots.firstIndex { $0.status != .passed } ?? slots.endIndex
        return Array(slots[max(0, pivot - 2)..<min(slots.endIndex, pivot + 8)])
    }

    var body: some View {
        ScrollViewReader { proxy in
            ScrollView(.horizontal) {
                HStack(spacing: 8) {
                    ForEach(slots) { slot in
                        tile(slot).id(slot.id)
                    }
                }
                .padding(EdgeInsets(top: 2, leading: 16, bottom: 6, trailing: 16))
            }
            .scrollIndicators(.hidden)
            .padding(.horizontal, -16)
            .onAppear {
                if let next = slots.first(where: { $0.status == .next }) {
                    proxy.scrollTo(next.id, anchor: .leading)
                }
            }
        }
    }

    private func tile(_ slot: TransitTodaySlot) -> some View {
        let isNext = slot.status == .next
        let passed = slot.status == .passed
        return VStack(alignment: .leading, spacing: 0) {
            HStack(spacing: 6) {
                Text(verbatim: slot.lineNumber)
                    .font(.system(size: 10.5, weight: .bold))
                    .foregroundStyle(isNext ? UNESColor.surface : UNESColor.transitLine(slot.lineNumber))
                Spacer(minLength: 0)
                if slot.isFlaky {
                    Image(systemName: "circle.dashed")
                        .font(.system(size: 10, weight: .semibold))
                        .foregroundStyle(UNESColor.ink4)
                }
            }
            Text(verbatim: CommuteFormat.clock(slot.usual))
                .font(.system(size: 19, weight: .bold))
                .tracking(-0.57)
                .strikethrough(passed)
                .padding(.top, 4)
            if let scheduled = slot.scheduledDeparture {
                Text(.commuteTodayOfficial(scheduled.replacingOccurrences(of: ":", with: String.localized(.commuteClockSeparator))))
                    .font(.system(size: 10))
                    .foregroundStyle(isNext ? UNESColor.surface.opacity(0.6) : UNESColor.ink4)
                    .padding(.top, 2)
            }
        }
        .monospacedDigit()
        .foregroundStyle(isNext ? UNESColor.surface : UNESColor.ink)
        .frame(minWidth: 74, alignment: .leading)
        .padding(EdgeInsets(top: 9, leading: 10, bottom: 8, trailing: 10))
        .background(isNext ? UNESColor.ink : UNESColor.card, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
        .overlay {
            let shape = RoundedRectangle(cornerRadius: 14, style: .continuous)
            if slot.isFlaky {
                shape.strokeBorder(UNESColor.ink4, style: StrokeStyle(lineWidth: 1, dash: [3, 2]))
            } else {
                shape.strokeBorder(isNext ? UNESColor.ink : UNESColor.cardLine)
            }
        }
        .opacity(passed ? 0.45 : 1)
    }
}

// MARK: - Edge states

struct CommuteNoServiceCard: View {
    var board: TransitStopBoard
    var leg: CommuteLeg
    var network: TransitNetwork?

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack(alignment: .top, spacing: 14) {
                Image(systemName: "calendar")
                    .font(.system(size: 19, weight: .medium))
                    .foregroundStyle(UNESColor.ink2)
                    .frame(width: 44, height: 44)
                    .background(UNESColor.surface2, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
                VStack(alignment: .leading, spacing: 8) {
                    Text(.commuteNoServiceTitle)
                        .font(.system(size: 22, weight: .bold))
                        .tracking(-0.66)
                        .foregroundStyle(UNESColor.ink)
                    Text(.commuteNoServiceBody(CommuteFormat.lineList(leg.lineNumbers)))
                        .font(.system(size: 13.5))
                        .foregroundStyle(UNESColor.ink3)
                        .fixedSize(horizontal: false, vertical: true)
                }
            }
            if let next = board.nextService {
                HStack(alignment: .firstTextBaseline) {
                    Text(.commuteNextTrip(HomeFormat.weekdayName(for: next.usual).lowercased()))
                        .font(.system(size: 13, weight: .medium))
                        .foregroundStyle(UNESColor.ink3)
                    Spacer()
                    HStack(alignment: .firstTextBaseline, spacing: 8) {
                        TransitLineBadge(number: next.lineNumber, size: .small)
                        TransitEstimateText(
                            estimate: CommuteFormat.Estimate(text: CommuteFormat.clock(next.usual), isApproximate: true),
                            dim: true,
                            size: 22
                        )
                    }
                }
                .padding(.top, 14)
                .overlay(alignment: .top) { Divider().overlay(UNESColor.line) }
                .padding(.top, 16)
            }
        }
        .transitCard(padding: EdgeInsets(top: 20, leading: 18, bottom: 18, trailing: 18))
        .padding(.bottom, 18)
    }
}

/// First use: what the screen will show, and the way into setup.
struct CommuteIntro: View {
    var onStart: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Image(systemName: "bus")
                .font(.system(size: 25, weight: .medium))
                .foregroundStyle(UNESColor.ink2)
                .frame(width: 56, height: 56)
                .background(UNESColor.surface2, in: RoundedRectangle(cornerRadius: 18, style: .continuous))
            Text(.commuteIntroTitle)
                .font(.system(size: 32, weight: .bold))
                .tracking(-1.28)
                .foregroundStyle(UNESColor.ink)
                .padding(.top, 18)
            Text(.commuteIntroBody)
                .font(.system(size: 15.5))
                .foregroundStyle(UNESColor.ink3)
                .lineSpacing(3)
                .padding(.top, 10)
                .fixedSize(horizontal: false, vertical: true)
            VStack(alignment: .leading, spacing: 10) {
                bullet("dot.radiowaves.left.and.right", .commuteStateLive, .commuteIntroLive)
                bullet("parkingsign.circle", .commuteStateWaiting, .commuteIntroWaiting)
                bullet("clock", .commuteStateHistorical, .commuteIntroHistorical)
            }
            .padding(.top, 26)
            Button(action: onStart) {
                Text(.commuteIntroAction)
            }
            .buttonStyle(.unesDark)
            .padding(.top, 32)
            Text(.commuteWorksWithoutLocation)
                .font(.system(size: 12.5))
                .foregroundStyle(UNESColor.ink4)
                .frame(maxWidth: .infinity)
                .padding(.top, 10)
        }
        .padding(EdgeInsets(top: 18, leading: 4, bottom: 0, trailing: 4))
    }

    private func bullet(_ symbol: String, _ title: LocalizedStringResource, _ body: LocalizedStringResource) -> some View {
        HStack(spacing: 12) {
            Image(systemName: symbol)
                .font(.system(size: 15, weight: .medium))
                .foregroundStyle(UNESColor.ink2)
                .frame(width: 34, height: 34)
                .background(UNESColor.card, in: RoundedRectangle(cornerRadius: 11, style: .continuous))
                .overlay {
                    RoundedRectangle(cornerRadius: 11, style: .continuous).strokeBorder(UNESColor.cardLine)
                }
            VStack(alignment: .leading, spacing: 1) {
                Text(title)
                    .font(.system(size: 14.5, weight: .semibold))
                    .foregroundStyle(UNESColor.ink)
                Text(body)
                    .font(.system(size: 12.5))
                    .foregroundStyle(UNESColor.ink3)
            }
        }
    }
}

@MainActor
private func boardPreview(_ direction: TransitDirection, mode: TransitFixtures.BoardMode, plan: CommutePlan = TransitFixtures.plan) -> some View {
    NavigationStack {
        CommuteBoardView(store: Store(initialState: CommuteBoardFeature.State(direction: direction, plan: plan)) {
            CommuteBoardFeature()
        } withDependencies: {
            $0.transitRepository = .mock(mode: mode)
            $0.commutePlanRepository = .preview(plan)
        })
    }
}

#Preview("Meu ponto") { boardPreview(.toCampus, mode: .normal) }
#Preview("Saindo da UEFS") { boardPreview(.fromCampus, mode: .normal) }
#Preview("Sem sinal") { boardPreview(.toCampus, mode: .signalLost) }
#Preview("Fora do sinal") { boardPreview(.toCampus, mode: .missing) }
#Preview("Dados indisponíveis") { boardPreview(.toCampus, mode: .feedUnavailable) }
#Preview("Domingo") { boardPreview(.toCampus, mode: .noService) }
#Preview("Primeiro uso") { boardPreview(.toCampus, mode: .normal, plan: CommutePlan()) }
