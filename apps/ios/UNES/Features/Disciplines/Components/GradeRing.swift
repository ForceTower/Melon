import SwiftUI

/// Circular progress toward 10, with the score rendered inside. Used both on
/// the list cards and on the detail screen's grades headline.
struct GradeRing: View {
    var score: Double?
    var size: CGFloat = 52
    var stroke: CGFloat = 4
    var color: Color?

    private var progress: CGFloat {
        guard let score else { return 0 }
        return CGFloat(max(0, min(1, score / 10)))
    }

    private var hue: Color {
        color ?? DisciplineScoreColor.color(for: score)
    }

    var body: some View {
        ZStack {
            Circle()
                .stroke(UNESColor.line, lineWidth: stroke)

            Circle()
                .trim(from: 0, to: progress)
                .stroke(hue, style: StrokeStyle(lineWidth: stroke, lineCap: .round))
                .rotationEffect(.degrees(-90))
                .animation(.timingCurve(0.22, 0.9, 0.3, 1, duration: 0.7), value: progress)

            if let score {
                Text(formatted(score))
                    .font(UNESFont.serif(size * 0.38))
                    .tracking(-size * 0.38 * 0.02)
                    .foregroundStyle(UNESColor.ink)
            } else {
                Text("—")
                    .font(UNESFont.serif(size * 0.38))
                    .foregroundStyle(UNESColor.ink4)
            }
        }
        .frame(width: size, height: size)
    }

    private func formatted(_ score: Double) -> String {
        String(format: "%.1f", score)
    }
}

#Preview {
    HStack(spacing: 20) {
        GradeRing(score: 8.3, color: UNESColor.coral)
        GradeRing(score: nil)
        GradeRing(score: 6.2, size: 74, stroke: 5)
    }
    .padding()
    .background(UNESColor.surface)
}
