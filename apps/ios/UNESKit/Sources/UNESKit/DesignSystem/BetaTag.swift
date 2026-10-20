import SwiftUI

/// Uppercase capsule marking a feature still in beta — coral on a faint
/// coral wash, sitting opposite the icon on hub tiles.
struct BetaTag: View {
    private let tone = UNESColor.readable(0xE85D4E)

    var body: some View {
        Text(.commonBeta)
            .textCase(.uppercase)
            .font(.system(size: 8.5, weight: .heavy))
            .tracking(0.5)
            .foregroundStyle(tone)
            .padding(.horizontal, 6)
            .padding(.vertical, 3)
            .background(tone.opacity(0.11), in: Capsule())
            .overlay {
                Capsule().strokeBorder(tone.opacity(0.28))
            }
    }
}

#Preview {
    BetaTag()
        .padding(28)
        .background(UNESColor.card)
}
