import SwiftUI

/// Tiny trend line with a dot on the last point — grade history on the
/// coefficient widgets.
struct Sparkline: View {
    var values: [Double]
    var color: Color = UNESColor.tangerine
    var size = CGSize(width: 90, height: 26)
    var lineWidth: CGFloat = 2.2

    var body: some View {
        let low = values.min() ?? 0
        let high = values.max() ?? 1
        let span = max(high - low, 0.1)
        let points = values.enumerated().map { index, value in
            CGPoint(
                x: CGFloat(index) / CGFloat(max(values.count - 1, 1)) * size.width,
                y: size.height - (value - low) / span * size.height
            )
        }

        ZStack(alignment: .topLeading) {
            Path { path in
                path.addLines(points)
            }
            .stroke(color, style: StrokeStyle(lineWidth: lineWidth, lineCap: .round, lineJoin: .round))

            if let last = points.last {
                Circle()
                    .fill(color)
                    .frame(width: 6, height: 6)
                    .position(last)
            }
        }
        .frame(width: size.width, height: size.height)
    }
}

#Preview {
    VStack(spacing: 20) {
        Sparkline(values: [7.9, 8.0, 7.7, 8.2, 8.1, 8.5])
        Sparkline(values: [7.9, 8.0, 7.7, 8.2, 8.1, 8.5], size: CGSize(width: 118, height: 34))
    }
    .padding(32)
    .background(UNESColor.card)
}
