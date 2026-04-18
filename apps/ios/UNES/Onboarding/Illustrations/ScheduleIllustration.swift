import SwiftUI

struct ScheduleIllustration: View {
    private struct Block: Identifiable {
        let id = UUID()
        let col: Int
        let row: Int
        let h: Int
        let color: Color
        let label: String
        let room: Int
    }

    private let blocks: [Block] = [
        .init(col: 0, row: 0, h: 2, color: UNESColor.coral,   label: "ALGI", room: 112),
        .init(col: 1, row: 1, h: 1, color: UNESColor.amber,   label: "CALC", room: 135),
        .init(col: 2, row: 0, h: 1, color: UNESColor.magenta, label: "LPOO", room: 107),
        .init(col: 2, row: 2, h: 2, color: UNESColor.plum,    label: "FIS2", room: 121),
        .init(col: 0, row: 3, h: 1, color: UNESColor.amber,   label: "PROJ", room: 109),
    ]

    @State private var appeared = false
    @State private var timelineOffset: CGFloat = 0

    var body: some View {
        ZStack(alignment: .topLeading) {
            Canvas { ctx, size in
                let gridColor = UNESColor.ink.opacity(0.1)
                // horizontal lines
                for i in 0...5 {
                    let y: CGFloat = 30 + CGFloat(i) * 40
                    var p = Path()
                    p.move(to: CGPoint(x: 20, y: y))
                    p.addLine(to: CGPoint(x: 240, y: y))
                    ctx.stroke(p, with: .color(gridColor), lineWidth: 1)
                }
                // vertical lines
                for i in 0...3 {
                    let x: CGFloat = 20 + CGFloat(i) * 73
                    var p = Path()
                    p.move(to: CGPoint(x: x, y: 30))
                    p.addLine(to: CGPoint(x: x, y: 230))
                    ctx.stroke(p, with: .color(gridColor), lineWidth: 1)
                }
            }
            .frame(width: 260, height: 260)

            // day labels
            ForEach(Array(["SEG", "TER", "QUA"].enumerated()), id: \.offset) { i, day in
                Text(day)
                    .font(UNESFont.mono(9))
                    .tracking(1)
                    .foregroundStyle(UNESColor.ink3)
                    .position(x: 20 + CGFloat(i) * 73 + 36, y: 15)
            }

            // time labels
            ForEach(Array(["08", "10", "12", "14", "16"].enumerated()), id: \.offset) { i, t in
                Text(t)
                    .font(UNESFont.mono(9))
                    .foregroundStyle(UNESColor.ink4)
                    .position(x: 10, y: 32 + CGFloat(i) * 40)
            }

            // animated "now" line — drawn in a Canvas so grid coordinates
            // are deterministic regardless of parent layout.
            TimelineView(.animation) { ctx in
                let t = ctx.date.timeIntervalSince1970
                let pulsePhase = t.truncatingRemainder(dividingBy: 1.6) / 1.6
                let dotSize: CGFloat = 6 + CGFloat(sin(pulsePhase * 2 * .pi) * 2)

                Canvas { context, _ in
                    var line = Path()
                    line.move(to: CGPoint(x: 20, y: 110))
                    line.addLine(to: CGPoint(x: 240, y: 110))
                    context.stroke(
                        line,
                        with: .color(UNESColor.coral),
                        style: StrokeStyle(
                            lineWidth: 1.5,
                            dash: [3, 2],
                            dashPhase: CGFloat(t * 10)
                        )
                    )

                    let dotRect = CGRect(
                        x: 20 - dotSize / 2,
                        y: 110 - dotSize / 2,
                        width: dotSize,
                        height: dotSize
                    )
                    context.fill(Path(ellipseIn: dotRect), with: .color(UNESColor.coral))
                }
                .frame(width: 260, height: 260)
            }

            // class blocks
            ForEach(Array(blocks.enumerated()), id: \.element.id) { index, b in
                VStack(alignment: .leading, spacing: 2) {
                    Text(b.label)
                        .font(UNESFont.mono(9, weight: .semibold))
                        .tracking(0.5)
                    Text("sala \(b.room)")
                        .font(UNESFont.mono(7))
                        .opacity(0.8)
                }
                .foregroundStyle(UNESColor.surface)
                .padding(8)
                .frame(width: 71, height: CGFloat(b.h * 40 - 2), alignment: .topLeading)
                .background(b.color, in: RoundedRectangle(cornerRadius: 10, style: .continuous))
                .shadow(color: .black.opacity(0.08), radius: 6, y: 4)
                .opacity(appeared ? 1 : 0)
                .offset(y: appeared ? 0 : 12)
                .animation(
                    .spring(response: 0.5, dampingFraction: 0.7).delay(0.1 + Double(index) * 0.12),
                    value: appeared
                )
                .position(
                    x: 21 + CGFloat(b.col) * 73 + 71 / 2,
                    y: 31 + CGFloat(b.row) * 40 + CGFloat(b.h * 40 - 2) / 2
                )
            }
        }
        .frame(width: 260, height: 260)
        .onAppear { appeared = true }
    }
}
