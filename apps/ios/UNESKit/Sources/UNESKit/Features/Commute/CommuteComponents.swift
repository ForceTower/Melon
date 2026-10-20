import SwiftUI

// MARK: - Line badge

struct TransitLineBadge: View {
    enum Size {
        case small, regular, large
    }

    var number: String
    var size: Size = .regular
    /// Tinted outline instead of a solid fill — an unselected line.
    var ghost = false

    var body: some View {
        let color = UNESColor.transitLine(number)
        Text(verbatim: number)
            .font(.system(size: fontSize, weight: .bold))
            .tracking(0.3)
            .monospacedDigit()
            .foregroundStyle(ghost ? color : .white)
            .padding(.horizontal, size == .small ? 5 : 7)
            .frame(minWidth: minWidth, minHeight: height)
            .background(ghost ? color.opacity(0.15) : color, in: RoundedRectangle(cornerRadius: radius, style: .continuous))
            .accessibilityLabel(Text(.commuteLineAccessibility(number)))
    }

    private var fontSize: CGFloat {
        switch size {
        case .small: 10.5
        case .regular: 13
        case .large: 17
        }
    }

    private var minWidth: CGFloat {
        switch size {
        case .small: 30
        case .regular: 40
        case .large: 50
        }
    }

    private var height: CGFloat {
        switch size {
        case .small: 19
        case .regular: 25
        case .large: 32
        }
    }

    private var radius: CGFloat {
        switch size {
        case .small: 5
        case .regular: 7
        case .large: 9
        }
    }
}

// MARK: - State chip

/// Every state carries a symbol and a label; color only reinforces it.
struct TransitChip: View {
    enum Kind: Equatable {
        case live, waiting, historical, stale, flaky
    }

    var kind: Kind
    var text: String?
    var small = false
    var onDark = false

    var body: some View {
        HStack(spacing: small ? 4 : 5) {
            symbol
            Text(verbatim: text ?? defaultLabel)
                .lineLimit(1)
        }
        .font(.system(size: small ? 10.5 : 11.5, weight: .semibold))
        .foregroundStyle(onDark ? .white : tone)
        .padding(EdgeInsets(top: small ? 2 : 3, leading: small ? 6 : 7, bottom: small ? 2 : 3, trailing: small ? 7 : 9))
        .background(fill, in: Capsule())
        .overlay {
            if kind == .flaky {
                Capsule().strokeBorder(style: StrokeStyle(lineWidth: 1, dash: [3, 2]))
                    .foregroundStyle(onDark ? .white : tone)
            } else {
                Capsule().strokeBorder(onDark ? Color.white.opacity(0.22) : tone.opacity(0.3))
            }
        }
        .fixedSize()
    }

    @ViewBuilder
    private var symbol: some View {
        let image = Image(systemName: symbolName)
            .font(.system(size: small ? 10 : 11, weight: .semibold))
        if kind == .live {
            image.symbolEffect(.pulse, options: .repeating)
        } else {
            image
        }
    }

    private var symbolName: String {
        switch kind {
        case .live: "dot.radiowaves.left.and.right"
        case .waiting: "parkingsign.circle"
        case .historical: "clock"
        case .stale: "antenna.radiowaves.left.and.right.slash"
        case .flaky: "circle.dashed"
        }
    }

    private var defaultLabel: String {
        switch kind {
        case .live: .localized(.commuteStateLive)
        case .waiting: .localized(.commuteStateWaiting)
        case .historical: .localized(.commuteStateHistorical)
        case .stale: .localized(.commuteStateStale)
        case .flaky: .localized(.commuteStateFlaky)
        }
    }

    private var tone: Color {
        switch kind {
        case .live: UNESColor.accent
        case .waiting: UNESColor.transitWaiting
        case .historical, .flaky: UNESColor.ink3
        case .stale: UNESColor.ink4
        }
    }

    private var fill: Color {
        if onDark { return .white.opacity(0.14) }
        switch kind {
        case .live, .waiting: return tone.opacity(0.12)
        case .historical, .stale: return UNESColor.surface2
        case .flaky: return .clear
        }
    }
}

extension TransitChip.Kind {
    init(_ arrival: TransitArrival) {
        if arrival.signalLost {
            self = .stale
            return
        }
        switch arrival.state {
        case .live: self = .live
        case .waiting: self = .waiting
        case .historical: self = .historical
        }
    }
}

// MARK: - Freshness

/// How long ago the bus last reported a new position.
struct TransitFreshness: View {
    var fixAt: Date
    var now: Date
    var signalLost = false
    var onDark = false
    var short = false

    var body: some View {
        HStack(spacing: 5) {
            if signalLost {
                Image(systemName: "antenna.radiowaves.left.and.right.slash")
                    .font(.system(size: 11, weight: .semibold))
            } else {
                Circle().frame(width: 5, height: 5).opacity(0.8)
            }
            Text(verbatim: signalLost
                ? CommuteFormat.signalLost(since: fixAt, now: now)
                : CommuteFormat.freshness(since: fixAt, now: now, short: short))
        }
        .font(.system(size: 11, weight: .medium))
        .monospacedDigit()
        .foregroundStyle(color)
        .lineLimit(1)
    }

    private var color: Color {
        if onDark { return .white.opacity(signalLost ? 0.75 : 0.62) }
        return signalLost ? UNESColor.ink3 : UNESColor.ink4
    }
}

// MARK: - Estimate

/// The number that matters. Approximate times get a smaller, dimmer "~".
struct TransitEstimateText: View {
    var estimate: CommuteFormat.Estimate
    var dim = false
    var stale = false
    var size: CGFloat = 30
    var onDark = false

    var body: some View {
        HStack(alignment: .firstTextBaseline, spacing: 0) {
            if estimate.isApproximate {
                Text(verbatim: "~")
                    .font(.system(size: size * 0.72, weight: dim ? .semibold : .bold))
                    .opacity(0.55)
            }
            Text(verbatim: estimate.text)
                .font(.system(size: size, weight: dim ? .semibold : .bold))
        }
        .tracking(-size * 0.035)
        .monospacedDigit()
        .foregroundStyle(onDark ? .white : dim ? UNESColor.ink2 : UNESColor.ink)
        .opacity(stale ? 0.45 : 1)
        .lineLimit(1)
        .minimumScaleFactor(0.7)
        .contentTransition(.numericText())
        .accessibilityElement(children: .combine)
    }
}

// MARK: - Route progress

/// The route as a straight line from its origin to the end, the bus on it and
/// the student's stop ringed.
struct TransitRouteProgress: View {
    var stopNames: [String]
    /// Fractional stop index; nil when there is no bus to place.
    var busPosition: Double?
    var mineIndex: Int
    var color: Color
    var onDark = false

    var body: some View {
        GeometryReader { proxy in
            let width = proxy.size.width
            let inset = width * 0.08
            let span = width - inset * 2
            let last = CGFloat(max(1, stopNames.count - 1))
            let x: (Double) -> CGFloat = { index in inset + span * CGFloat(index) / last }
            let track = onDark ? Color.white.opacity(0.22) : UNESColor.surface3

            ZStack(alignment: .topLeading) {
                Capsule().fill(track)
                    .frame(width: span, height: 3)
                    .offset(x: inset, y: 12)
                if let busPosition {
                    Capsule().fill(color)
                        .frame(width: max(0, x(busPosition) - inset), height: 3)
                        .offset(x: inset, y: 12)
                }
                ForEach(stopNames.indices, id: \.self) { index in
                    let passed = busPosition.map { Double(index) <= $0 } ?? false
                    let mine = index == mineIndex
                    let dot: CGFloat = mine ? 11 : stopNames.count > 16 ? 4 : 6
                    Circle()
                        .fill(passed ? color : track)
                        .overlay {
                            if mine {
                                Circle().strokeBorder(onDark ? Color.white : UNESColor.ink, lineWidth: 2.5)
                            }
                        }
                        .frame(width: dot, height: dot)
                        .position(x: x(Double(index)), y: 13.5)
                }
                if let busPosition {
                    Image(systemName: "bus.fill")
                        .font(.system(size: 11, weight: .semibold))
                        .foregroundStyle(.white)
                        .frame(width: 22, height: 22)
                        .background(color, in: RoundedRectangle(cornerRadius: 7, style: .continuous))
                        .overlay {
                            RoundedRectangle(cornerRadius: 9, style: .continuous)
                                .strokeBorder(onDark ? Color.black.opacity(0.6) : UNESColor.card, lineWidth: 3)
                                .padding(-3)
                        }
                        .position(x: x(busPosition), y: 13.5)
                        .animation(UNESMotion.settle(0.8), value: busPosition)
                }
                labels(x: x)
            }
        }
        .frame(height: 44)
        .accessibilityHidden(true)
    }

    private func labels(x: (Double) -> CGFloat) -> some View {
        let sub = onDark ? Color.white.opacity(0.6) : UNESColor.ink4
        let mine = x(Double(mineIndex))
        // End labels give way to "seu ponto" rather than print over it.
        let clear = { (end: CGFloat) in abs(end - mine) > 80 }
        return ZStack(alignment: .topLeading) {
            if let first = stopNames.first, clear(x(0)) {
                Text(verbatim: first)
                    .foregroundStyle(sub)
                    .fixedSize()
                    .position(x: x(0) + 14, y: 34)
            }
            Text(.commuteRouteYourStop)
                .font(.system(size: 10, weight: .bold))
                .foregroundStyle(onDark ? .white : UNESColor.ink)
                .fixedSize()
                .position(x: x(Double(mineIndex)), y: 34)
            if stopNames.count > 1, let last = stopNames.last, clear(x(Double(stopNames.count - 1))) {
                Text(verbatim: last)
                    .foregroundStyle(sub)
                    .fixedSize()
                    .position(x: x(Double(stopNames.count - 1)) - 10, y: 34)
            }
        }
        .font(.system(size: 10, weight: .semibold))
        .lineLimit(1)
    }
}

// MARK: - Containers

extension View {
    /// The v2 card: hairline border, continuous 20pt corners, faint drop.
    func transitCard(padding: EdgeInsets = EdgeInsets()) -> some View {
        self
            .padding(padding)
            .background(UNESColor.card, in: RoundedRectangle(cornerRadius: 20, style: .continuous))
            .overlay {
                RoundedRectangle(cornerRadius: 20, style: .continuous)
                    .strokeBorder(UNESColor.cardLine)
            }
            .shadow(color: .black.opacity(0.04), radius: 9, y: 6)
    }
}

struct CommuteSectionHeader: View {
    var title: String
    var hint: String?
    var action: String?
    var onAction: (() -> Void)?

    var body: some View {
        HStack(alignment: .bottom, spacing: 10) {
            VStack(alignment: .leading, spacing: 2) {
                Text(verbatim: title)
                    .font(.system(size: 17, weight: .bold))
                    .tracking(-0.42)
                    .foregroundStyle(UNESColor.ink)
                if let hint {
                    Text(verbatim: hint)
                        .font(.system(size: 12, weight: .medium))
                        .foregroundStyle(UNESColor.ink4)
                }
            }
            Spacer(minLength: 0)
            if let action, let onAction {
                Button(action: onAction) {
                    Text(verbatim: action)
                        .font(.system(size: 14, weight: .medium))
                        .foregroundStyle(UNESColor.accent)
                }
                .buttonStyle(.plain)
            }
        }
        .padding(EdgeInsets(top: 0, leading: 4, bottom: 9, trailing: 4))
    }
}

/// A short stop name for the route line's end labels.
func transitShortName(_ stop: TransitStop, in route: TransitRoute) -> String {
    if stop.id == route.campusStopId { return "UEFS" }
    let terminal = route.direction == .toCampus ? route.stops.first : route.stops.last
    return stop.id == terminal?.id ? .localized(.commuteTerminal) : stop.name
}

#Preview {
    let now = Date()
    let arrival = TransitFixtures.board(now: now).arrivals[0]
    VStack(alignment: .leading, spacing: 16) {
        HStack {
            TransitLineBadge(number: "003")
            TransitLineBadge(number: "087", size: .large)
            TransitLineBadge(number: "088", size: .small, ghost: true)
        }
        HStack {
            TransitChip(kind: .live)
            TransitChip(kind: .waiting)
            TransitChip(kind: .flaky, small: true)
        }
        TransitEstimateText(estimate: CommuteFormat.estimate(for: arrival, now: now))
        TransitFreshness(fixAt: now.addingTimeInterval(-8), now: now)
        TransitRouteProgress(
            stopNames: ["Terminal", "a", "b", "c", "d", "e", "UEFS"],
            busPosition: 2.4,
            mineIndex: 4,
            color: UNESColor.transit087
        )
        .transitCard(padding: EdgeInsets(top: 14, leading: 14, bottom: 10, trailing: 14))
    }
    .padding(20)
    .background(UNESColor.surface)
}
