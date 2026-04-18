import SwiftUI

struct GradesIllustration: View {
    private struct Bar: Identifiable {
        let id = UUID()
        let targetHeight: CGFloat
        let color: Color
        let label: String
    }

    private let bars: [Bar] = [
        .init(targetHeight: 55, color: UNESColor.amber,   label: "7.5"),
        .init(targetHeight: 72, color: UNESColor.coral,   label: "8.8"),
        .init(targetHeight: 88, color: UNESColor.magenta, label: "9.4"),
        .init(targetHeight: 65, color: UNESColor.amber,   label: "8.1"),
        .init(targetHeight: 80, color: UNESColor.coral,   label: "9.0"),
    ]

    @State private var grown = false

    var body: some View {
        VStack(spacing: 0) {
            // headline number
            HStack(alignment: .firstTextBaseline, spacing: 0) {
                Text("8")
                    .foregroundStyle(UNESColor.ink)
                Text(",")
                    .foregroundStyle(UNESColor.accent)
                Text("5")
                    .foregroundStyle(UNESColor.ink)
                Text("/10")
                    .font(UNESFont.serif(28, italic: true))
                    .foregroundStyle(UNESColor.ink3)
            }
            .font(UNESFont.serif(92))
            .tracking(-3.7)
            .padding(.top, 16)
            .fadeUpOnAppear(delay: 0.1)

            Text("coeficiente · 2026.1")
                .font(UNESFont.mono(10))
                .tracking(2)
                .textCase(.uppercase)
                .foregroundStyle(UNESColor.ink3)
                .padding(.top, 4)
                .fadeInOnAppear(delay: 0.3)

            Spacer().frame(height: 28)

            HStack(alignment: .bottom, spacing: 10) {
                ForEach(Array(bars.enumerated()), id: \.element.id) { index, bar in
                    VStack(spacing: 6) {
                        Text(bar.label)
                            .font(UNESFont.mono(9))
                            .foregroundStyle(UNESColor.ink3)
                            .fadeInOnAppear(delay: 0.6 + Double(index) * 0.1)

                        RoundedRectangle(cornerRadius: 4, style: .continuous)
                            .fill(bar.color)
                            .frame(width: 28, height: grown ? bar.targetHeight : 0)
                            .animation(
                                .spring(response: 0.7, dampingFraction: 0.7)
                                    .delay(0.3 + Double(index) * 0.08),
                                value: grown
                            )
                    }
                }
            }
            .frame(height: 100, alignment: .bottom)
        }
        .frame(width: 260, height: 260)
        .onAppear { grown = true }
    }
}
