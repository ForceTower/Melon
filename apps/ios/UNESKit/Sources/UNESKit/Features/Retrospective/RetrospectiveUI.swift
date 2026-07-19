import SwiftUI

// The Retrospectiva's shared primitives: count-ups, the drawn Score chart,
// the attendance ring, trend chip, glass chrome and the `unes·` wordmark.
// Motion rides the house cubic-bezier(.2,.9,.3,1).

// MARK: - Easing

/// The CSS spring's curve, evaluated in Swift so JS-driven numbers and
/// SwiftUI reveals share one easing (Newton-Raphson x→t, sample y).
enum RetroEase {
    static func value(_ x: Double) -> Double {
        let (x1, y1, x2, y2) = (0.2, 0.9, 0.3, 1.0)
        let cx = 3 * x1, bx = 3 * (x2 - x1) - cx, ax = 1 - cx - bx
        let cy = 3 * y1, by = 3 * (y2 - y1) - cy, ay = 1 - cy - by
        func sx(_ t: Double) -> Double { ((ax * t + bx) * t + cx) * t }
        func dx(_ t: Double) -> Double { (3 * ax * t + 2 * bx) * t + cx }
        var t = x
        for _ in 0..<8 {
            let error = sx(t) - x
            if abs(error) < 1e-4 { break }
            let slope = dx(t)
            if abs(slope) < 1e-6 { break }
            t -= error / slope
        }
        t = min(1, max(0, t))
        return ((ay * t + by) * t + cy) * t
    }
}

// MARK: - Count-up number

/// Ramps 0 → target on the shared easing when it appears; Reduce Motion
/// lands on the target immediately.
struct RetroCountUp: View {
    var target: Double
    var decimals: Int = 0
    var delay: Double = 0.12
    var duration: Double = 1.15
    var render: (String) -> Text = { Text($0) }

    @State private var startedAt: Date?
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        Group {
            if reduceMotion {
                render(formatted(target))
            } else {
                TimelineView(.animation) { context in
                    render(formatted(current(at: context.date)))
                }
            }
        }
        .onAppear { startedAt = Date() }
    }

    private func current(at date: Date) -> Double {
        guard let startedAt else { return 0 }
        let progress = (date.timeIntervalSince(startedAt) - delay) / duration
        guard progress > 0 else { return 0 }
        guard progress < 1 else { return target }
        return RetroEase.value(progress) * target
    }

    private func formatted(_ value: Double) -> String {
        decimals == 0 ? String(Int(value.rounded())) : formatGrade(value)
    }
}

// MARK: - Score line chart

/// Sparkline of the cumulative Score, last point emphasized; path draw
/// animated on the shared easing.
struct RetroScoreChart: View {
    var points: [Double]
    var up: Bool
    var width: CGFloat = 260
    var height: CGFloat = 92

    @State private var drawn = false
    @State private var pinging = false
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    private var line: Color { up ? Color(hex: 0x5CE07A) : Color(hex: 0xFFB27A) }

    var body: some View {
        let coords = coordinates
        ZStack {
            area(coords)
                .fill(LinearGradient(
                    colors: [line.opacity(0.28), line.opacity(0)],
                    startPoint: .top,
                    endPoint: .bottom
                ))
                .opacity(drawn ? 1 : 0)
                .animation(.easeInOut(duration: 0.5).delay(0.7), value: drawn)
            path(coords)
                .trim(from: 0, to: drawn ? 1 : 0)
                .stroke(line, style: StrokeStyle(lineWidth: 3.2, lineCap: .round, lineJoin: .round))
                .animation(UNESMotion.ease(1.15).delay(0.18), value: drawn)
            if let last = coords.last {
                Circle()
                    .fill(line)
                    .frame(width: 13, height: 13)
                    .scaleEffect(drawn ? 1 : 0)
                    .animation(UNESMotion.ease(0.5, overshoot: 1.4).delay(1), value: drawn)
                    .position(last)
                Circle()
                    .strokeBorder(line.opacity(0.5), lineWidth: 2)
                    .frame(width: 13, height: 13)
                    .scaleEffect(pinging ? 2.4 : 1)
                    .opacity(pinging ? 0 : 0.6)
                    .position(last)
            }
        }
        .frame(width: width, height: height)
        .onAppear {
            drawn = reduceMotion ? true : false
            withAnimation(nil) { drawn = reduceMotion }
            if !reduceMotion {
                drawn = true
                withAnimation(.easeOut(duration: 2).delay(1.2).repeatForever(autoreverses: false)) {
                    pinging = true
                }
            }
        }
    }

    private var coordinates: [CGPoint] {
        guard points.count > 1 else {
            return points.map { _ in CGPoint(x: width / 2, y: height / 2) }
        }
        let min = (points.min() ?? 0) - 0.4
        let max = (points.max() ?? 10) + 0.4
        return points.enumerated().map { index, value in
            CGPoint(
                x: CGFloat(index) / CGFloat(points.count - 1) * width,
                y: height - CGFloat((value - min) / (max - min)) * height
            )
        }
    }

    private func path(_ coords: [CGPoint]) -> Path {
        Path { path in
            guard let first = coords.first else { return }
            path.move(to: first)
            coords.dropFirst().forEach { path.addLine(to: $0) }
        }
    }

    private func area(_ coords: [CGPoint]) -> Path {
        Path { path in
            guard let first = coords.first, let last = coords.last else { return }
            path.move(to: first)
            coords.dropFirst().forEach { path.addLine(to: $0) }
            path.addLine(to: CGPoint(x: last.x, y: height))
            path.addLine(to: CGPoint(x: first.x, y: height))
            path.closeSubpath()
        }
    }
}

// MARK: - Attendance ring

struct RetroProgressRing<Content: View>: View {
    var percent: Int
    var size: CGFloat = 200
    var stroke: CGFloat = 14
    @ViewBuilder var content: Content

    @State private var shown = false
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        ZStack {
            Circle()
                .stroke(.white.opacity(0.16), lineWidth: stroke)
            Circle()
                .trim(from: 0, to: shown ? CGFloat(percent) / 100 : 0)
                .stroke(.white, style: StrokeStyle(lineWidth: stroke, lineCap: .round))
                .rotationEffect(.degrees(-90))
                .animation(reduceMotion ? nil : UNESMotion.ease(1.3).delay(0.2), value: shown)
            content
        }
        .frame(width: size, height: size)
        .onAppear { shown = true }
    }
}

// MARK: - Trend chip

struct RetroTrendChip: View {
    var score: RetrospectiveDeck.ScoreCard

    var body: some View {
        Group {
            if score.isFirst {
                HStack(spacing: 6) {
                    Image(systemName: "sparkle")
                        .font(.system(size: 12, weight: .bold))
                    Text(.retroScoreFirstChip)
                        .font(.system(size: 14, weight: .bold))
                }
                .foregroundStyle(.white)
                .padding(.horizontal, 14)
                .padding(.vertical, 7)
                .background(.white.opacity(0.16), in: Capsule())
            } else {
                let tone = score.isDown ? Color(hex: 0xFFB27A) : Color(hex: 0x5CE07A)
                HStack(spacing: 6) {
                    Image(systemName: "arrowtriangle.up.fill")
                        .font(.system(size: 11))
                        .scaleEffect(y: score.isDown ? -1 : 1)
                    Text(.retroScoreDeltaChip(retroSigned(score.delta ?? 0)))
                        .font(.system(size: 15, weight: .heavy))
                }
                .foregroundStyle(tone)
                .padding(.horizontal, 14)
                .padding(.vertical, 7)
                .background(tone.opacity(0.15), in: Capsule())
            }
        }
    }
}

/// "+0,4" / "−0,4" — locale-aware decimals, typographic minus like the design.
func retroSigned(_ value: Double) -> String {
    (value >= 0 ? "+" : "−") + formatGrade(abs(value))
}

// MARK: - Chrome

struct RetroGlassButton: View {
    var systemName: String
    var size: CGFloat = 38
    var action: () -> Void

    var body: some View {
        Button(action: action) {
            Image(systemName: systemName)
                .font(.system(size: size * 0.42, weight: .bold))
                .foregroundStyle(.white)
                .frame(width: size, height: size)
                .background(.white.opacity(0.16), in: Circle())
        }
    }
}

/// The app's mark — "unes" with the mesh-gradient dot, the only colored
/// element.
struct RetroUnesMark: View {
    var size: CGFloat = 20

    var body: some View {
        HStack(alignment: .lastTextBaseline, spacing: size * 0.16) {
            Text(verbatim: "unes")
                .font(.system(size: size, weight: .heavy))
                .tracking(-size * 0.05)
                .foregroundStyle(.white)
            Circle()
                .fill(LinearGradient(
                    colors: [Color(hex: 0xF4A23C), Color(hex: 0xE85D4E), Color(hex: 0xB23A7A)],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                ))
                .frame(width: size * 0.26, height: size * 0.26)
        }
    }
}

// MARK: - Card scaffold

/// Full-bleed mesh + bottom scrim + the design's content insets:
/// 108pt below the chrome, 120pt above the action band.
struct RetroCardShell<Content: View>: View {
    var mesh: MeshView.Variant
    var dim: Double = 0.74
    @ViewBuilder var content: Content

    var body: some View {
        ZStack {
            Color(hex: 0x0B0712).ignoresSafeArea()
            MeshView(variant: mesh).ignoresSafeArea()
            RadialGradient(
                colors: [.clear, Color(hex: 0x06040C, opacity: dim)],
                center: UnitPoint(x: 0.5, y: 0.04),
                startRadius: 60,
                endRadius: 720
            )
            .ignoresSafeArea()
            VStack(alignment: .leading, spacing: 0) {
                content
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
            .padding(EdgeInsets(top: 108, leading: 30, bottom: 120, trailing: 30))
        }
    }
}

struct RetroEyebrow: View {
    var text: String
    var delay: Double = 0.05

    var body: some View {
        Text(text.uppercased())
            .font(.system(size: 12.5, weight: .bold))
            .tracking(2)
            .foregroundStyle(.white.opacity(0.72))
            .fadeUp(delay: delay)
    }
}

struct RetroCaption: View {
    var text: String
    var delay: Double = 0.5

    var body: some View {
        Text(text)
            .font(.system(size: 16, weight: .medium))
            .lineSpacing(3)
            .foregroundStyle(.white.opacity(0.86))
            .frame(maxWidth: 300, alignment: .leading)
            .fadeUp(delay: delay)
    }
}

#Preview {
    ZStack {
        Color(hex: 0x0B0712).ignoresSafeArea()
        VStack(spacing: 24) {
            RetroUnesMark(size: 22)
            RetroCountUp(target: 7.8, decimals: 1) {
                Text($0).font(.system(size: 72, weight: .heavy)).foregroundStyle(.white)
            }
            RetroTrendChip(score: RetrospectiveDeck.ScoreCard(value: 7.8, previous: 7.4, series: [6.9, 7.1, 7.0, 7.4, 7.8]))
            RetroScoreChart(points: [6.9, 7.1, 7.0, 7.4, 7.8], up: true)
            RetroProgressRing(percent: 94, size: 160, stroke: 12) {
                Text(verbatim: "94%").font(.system(size: 36, weight: .heavy)).foregroundStyle(.white)
            }
        }
    }
}
