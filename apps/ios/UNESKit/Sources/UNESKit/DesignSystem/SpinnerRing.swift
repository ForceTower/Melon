import SwiftUI

/// Arc spinner: a quarter arc revolving over a faint track ring.
struct SpinnerRing: View {
    var size: CGFloat = 20
    var lineWidth: CGFloat = 2
    var color: Color
    var trackColor: Color
    var speed: Double = 0.8

    @State private var spinning = false

    var body: some View {
        ZStack {
            Circle()
                .stroke(trackColor, lineWidth: lineWidth)
            Circle()
                .trim(from: 0, to: 0.25)
                .stroke(color, style: StrokeStyle(lineWidth: lineWidth, lineCap: .round))
        }
        .frame(width: size, height: size)
        .rotationEffect(.degrees(spinning ? 270 : -90))
        .onAppear {
            withAnimation(.linear(duration: speed).repeatForever(autoreverses: false)) {
                spinning = true
            }
        }
    }
}

#Preview {
    HStack(spacing: 24) {
        SpinnerRing(size: 26, color: .white.opacity(0.75), trackColor: .white.opacity(0.2))
        SpinnerRing(size: 15, color: UNESColor.accent, trackColor: .white.opacity(0.3), speed: 0.7)
    }
    .padding(40)
    .background(UNESColor.darkBg)
}
