import SwiftUI

/// 240° radial dial (−210° → 30°) with zones for the three outcome bands:
/// reprovação (0–3, coral), final (3–7, amber), aprovação (7–10, green).
/// Ticks every integer, labels on 0/3/5/7/10. Needle + hub draw once the
/// average is non-nil. Mirrors the `Meter` component from the prototype.
///
/// The meter lives inside the verdict hero, which is always painted with
/// dark gradients regardless of the system theme — so every ink/needle
/// color here is a *fixed* light value (not `UNESColor.ink`, which flips
/// dark in light mode and would render invisibly on the hero).
struct FCMeter: View {
    let avg: Double?
    let tone: FCTone

    private let size: CGFloat = 220
    private let radius: CGFloat = 82
    private let startAngle: Double = -210
    private let endAngle: Double = 30

    // Hero-fixed palette: the hero card is always dark, so text + needle
    // must stay light regardless of the system appearance.
    private let heroInk     = Color(red: 0xFB/255, green: 0xF7/255, blue: 0xF2/255)
    private let heroInkSoft = Color(red: 0xFB/255, green: 0xF7/255, blue: 0xF2/255).opacity(0.55)
    private let heroInkFaint = Color(red: 0xFB/255, green: 0xF7/255, blue: 0xF2/255).opacity(0.4)
    private let heroTrack    = Color.white.opacity(0.08)

    private var sweep: Double { endAngle - startAngle }

    private var center: CGPoint {
        CGPoint(x: size / 2, y: size / 2 + 10)
    }

    private var pct: Double {
        guard let avg else { return 0 }
        return max(0, min(10, avg)) / 10
    }

    var body: some View {
        // Inner container runs in the full `size × size` canvas coord
        // system — `.position(x:y:)` then lets us place the center label
        // by its *center* directly in those coordinates. The outer frame
        // collapses the flow height to `size * 0.78` (so the next block
        // butts against the last tick row), while the extra canvas tail
        // below just overdraws — nothing's clipped.
        ZStack {
            meterShapes

            centerLabel
                .position(x: size / 2, y: center.y)
        }
        .frame(width: size, height: size)
        .frame(height: size * 0.78, alignment: .top)
    }

    private var meterShapes: some View {
        baseMeter
            .frame(width: size, height: size)
            .overlay(ticksLayer)
            .overlay(needleLayer)
    }

    private var baseMeter: some View {
        Canvas { ctx, _ in
            // Track
            ctx.stroke(
                arcPath(from: 0, to: 1),
                with: .color(heroTrack),
                style: StrokeStyle(lineWidth: 10, lineCap: .round)
            )
            // Zones — painted over the track so they read as tinted bands.
            ctx.stroke(
                arcPath(from: 0, to: 0.3),
                with: .color(FCTone.coral.bg.opacity(0.15)),
                style: StrokeStyle(lineWidth: 10, lineCap: .round)
            )
            ctx.stroke(
                arcPath(from: 0.3, to: 0.7),
                with: .color(FCTone.amber.bg.opacity(0.2)),
                style: StrokeStyle(lineWidth: 10, lineCap: .round)
            )
            ctx.stroke(
                arcPath(from: 0.7, to: 1),
                with: .color(FCTone.green.bg.opacity(0.2)),
                style: StrokeStyle(lineWidth: 10, lineCap: .round)
            )
            // Active fill — from 0 up to the current grade.
            if avg != nil {
                ctx.stroke(
                    arcPath(from: 0, to: pct),
                    with: .color(tone.bg),
                    style: StrokeStyle(lineWidth: 10, lineCap: .round)
                )
            }
        }
    }

    private var ticksLayer: some View {
        Canvas { ctx, _ in
            for v in 0...10 {
                let p = Double(v) / 10
                let major = v % 5 == 0 || v == 3 || v == 7
                let angle = (startAngle + p * sweep) * .pi / 180
                let r1 = radius + 6
                let r2 = radius + (major ? 14 : 10)
                var tickPath = Path()
                tickPath.move(to: CGPoint(
                    x: center.x + r1 * CGFloat(cos(angle)),
                    y: center.y + r1 * CGFloat(sin(angle))
                ))
                tickPath.addLine(to: CGPoint(
                    x: center.x + r2 * CGFloat(cos(angle)),
                    y: center.y + r2 * CGFloat(sin(angle))
                ))
                ctx.stroke(
                    tickPath,
                    with: .color(major ? heroInkSoft : heroInkFaint),
                    style: StrokeStyle(lineWidth: major ? 1.4 : 1, lineCap: .round)
                )

                if major {
                    let rL = radius + 22
                    let labelPos = CGPoint(
                        x: center.x + rL * CGFloat(cos(angle)),
                        y: center.y + rL * CGFloat(sin(angle)) + 3
                    )
                    let resolved = ctx.resolve(
                        Text("\(v)")
                            .font(UNESFont.mono(9))
                            .foregroundColor(heroInkSoft)
                    )
                    let measured = resolved.measure(in: CGSize(width: 40, height: 20))
                    ctx.draw(
                        resolved,
                        at: CGPoint(x: labelPos.x - measured.width / 2, y: labelPos.y - measured.height),
                        anchor: .topLeading
                    )
                }
            }
        }
    }

    @ViewBuilder
    private var needleLayer: some View {
        if let _ = avg {
            let needleAngle = (startAngle + pct * sweep) * .pi / 180
            let nR = radius - 6
            let tip = CGPoint(
                x: center.x + nR * CGFloat(cos(needleAngle)),
                y: center.y + nR * CGFloat(sin(needleAngle))
            )
            Canvas { ctx, _ in
                var needle = Path()
                needle.move(to: center)
                needle.addLine(to: tip)
                ctx.stroke(needle, with: .color(heroInk), style: StrokeStyle(lineWidth: 2.4, lineCap: .round))

                ctx.fill(Path(ellipseIn: CGRect(x: center.x - 6, y: center.y - 6, width: 12, height: 12)),
                         with: .color(heroInk))
                ctx.fill(Path(ellipseIn: CGRect(x: center.x - 2.5, y: center.y - 2.5, width: 5, height: 5)),
                         with: .color(tone.bg))

                ctx.stroke(
                    Path(ellipseIn: CGRect(x: tip.x - 3.5, y: tip.y - 3.5, width: 7, height: 7)),
                    with: .color(heroInk),
                    style: StrokeStyle(lineWidth: 1.5)
                )
                ctx.fill(Path(ellipseIn: CGRect(x: tip.x - 3.5, y: tip.y - 3.5, width: 7, height: 7)),
                         with: .color(tone.bg))
            }
            .animation(.spring(response: 0.55, dampingFraction: 0.72), value: avg)
        }
    }

    private var centerLabel: some View {
        VStack(spacing: 2) {
            Text("MÉDIA ATUAL")
                .font(UNESFont.mono(9))
                .tracking(1.62)
                .foregroundStyle(heroInkFaint)
            Text(FinalCountdownMath.formatGrade(avg))
                .font(UNESFont.serif(52))
                .tracking(-1.56)
                .foregroundStyle(heroInk)
                .lineLimit(1)
                .minimumScaleFactor(0.7)
        }
    }

    private func arcPath(from fromPct: Double, to toPct: Double) -> Path {
        var p = Path()
        guard toPct > fromPct else { return p }
        let a1 = Angle(degrees: startAngle + fromPct * sweep)
        let a2 = Angle(degrees: startAngle + toPct * sweep)
        p.addArc(center: center, radius: radius, startAngle: a1, endAngle: a2, clockwise: false)
        return p
    }
}
