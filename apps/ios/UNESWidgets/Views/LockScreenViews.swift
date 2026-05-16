import SwiftUI
import WidgetKit

/// Lock-screen rectangular complication. Tinted monochrome by the system per
/// Apple guidelines. Mirrors `LockRectangular` in `screens-widgets.jsx`.
struct LockRectangularView: View {
    let entry: NextClassEntry

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack(spacing: 5) {
                Image(systemName: "clock")
                    .font(.system(size: 9, weight: .medium))
                Text("em \(formatCountdown(entry.startsIn))")
                    .font(WidgetFont.mono(9))
                    .tracking(1.26)
                    .textCase(.uppercase)
            }
            Spacer(minLength: 0)
            Text(entry.shortTitle)
                .font(WidgetFont.serif(16))
                .tracking(-0.16)
                .lineLimit(1)
            Text("\(entry.startTime) · \(entry.room)")
                .font(WidgetFont.mono(9.5))
                .tracking(0.38)
                .opacity(0.85)
                .padding(.top, 4)
        }
        .widgetAccentable()
    }
}

/// Lock-screen circular complication with the countdown rendered as a ring.
struct LockCircularView: View {
    let entry: NextClassEntry

    /// Same logic as the JS demo: the ring fills as the window closes. The
    /// original mocks a 90-minute window — keep that knob until we have real
    /// data so the visual matches the design.
    private var progress: Double {
        let totalWindow = 90.0
        let remaining = Double(entry.startsIn)
        let pct = 1 - min(1, max(0, remaining / totalWindow))
        return pct
    }

    var body: some View {
        ZStack {
            Circle()
                .stroke(Color.primary.opacity(0.25), lineWidth: 3.5)
            Circle()
                .trim(from: 0, to: progress)
                .stroke(Color.primary, style: StrokeStyle(lineWidth: 3.5, lineCap: .round))
                .rotationEffect(.degrees(-90))
            VStack(spacing: 1) {
                Text(entry.shortCode)
                    .font(WidgetFont.mono(8))
                    .tracking(1.12)
                    .opacity(0.85)
                Text(ringLabel)
                    .font(WidgetFont.serif(17))
                    .tracking(-0.34)
            }
        }
        .widgetAccentable()
    }

    /// Compact "1h12" / "42m" used inside the ring — keeps to 4 chars max so
    /// it fits in the 72pt circle even with Dynamic Type.
    private var ringLabel: String {
        let mins = entry.startsIn
        let h = mins / 60
        let m = mins % 60
        if h == 0 { return "\(m)m" }
        if m == 0 { return "\(h)h" }
        return "\(h)h\(m)"
    }
}

/// Inline lock-screen complication (under the clock). Single-line summary.
struct LockInlineView: View {
    let entry: NextClassEntry

    var body: some View {
        HStack(spacing: 4) {
            Image(systemName: "clock")
            Text("\(entry.code) · \(entry.room) · em \(formatCountdown(entry.startsIn))")
        }
        .widgetAccentable()
    }
}
